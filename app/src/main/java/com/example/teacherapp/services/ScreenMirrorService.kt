package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MediaStreamService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Shared between streaming loop and capture loop — no imageReader contention
    @Volatile private var latestFrameBytes: ByteArray? = null

    companion object {
        private const val SERVER = "http://20.189.79.25:5000"
        private const val CHANNEL_ID = "content_sync_ch"
        private const val NOTIF_ID = 2001

        @Volatile var isRunning = false

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, MediaStreamService::class.java).apply {
                putExtra("code", resultCode)
                putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaStreamService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra("data")
        }
        data ?: return START_NOT_STICKY

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(code, data)

        // Required on Android 14+ before createVirtualDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, Handler(Looper.getMainLooper()))
        }

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val dpi = metrics.densityDpi

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ContentRenderer", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )

        startStreamingLoop()
        startCommandLoop()
        startCaptureLoop()
        return START_STICKY
    }

    // Captures screen frames and POSTs them as JPEG to /frame
    private fun startStreamingLoop() {
        scope.launch {
            while (isActive) {
                val image = imageReader?.acquireLatestImage()
                if (image == null) { delay(200); continue }
                try {
                    val plane = image.planes[0]
                    val rowPadding = (plane.rowStride - plane.pixelStride * image.width) / plane.pixelStride
                    val bmp = Bitmap.createBitmap(image.width + rowPadding, image.height, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(plane.buffer)

                    val out = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 40, out)
                    bmp.recycle()
                    val bytes = out.toByteArray()
                    latestFrameBytes = bytes  // share with capture loop

                    val conn = URL("$SERVER/frame").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "image/jpeg")
                    conn.setRequestProperty("Content-Length", bytes.size.toString())
                    conn.setRequestProperty("X-Device-Id", getAndroidId())
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.outputStream.use { it.write(bytes) }
                    conn.responseCode // flush
                    // No disconnect() — allows HTTP keep-alive, reduces per-frame TCP overhead
                } catch (_: Exception) {
                } finally {
                    image.close() // always release buffer slot
                }
                delay(200) // ~5 fps
            }
        }
    }

    // Polls /command for tap or swipe instructions from the remote tester
    private fun startCommandLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val conn = URL("$SERVER/command").openConnection() as HttpURLConnection
                    conn.apply {
                        requestMethod = "GET"
                        connectTimeout = 2000
                        readTimeout = 2000
                    }
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        when (json.optString("type")) {
                            "tap" -> {
                                InputAssistService.instance?.injectTap(
                                    json.getDouble("x").toFloat(),
                                    json.getDouble("y").toFloat()
                                )
                                delay(600) // wait for field to gain focus before next command
                            }
                            "swipe" -> InputAssistService.instance?.injectSwipe(
                                json.getDouble("x1").toFloat(), json.getDouble("y1").toFloat(),
                                json.getDouble("x2").toFloat(), json.getDouble("y2").toFloat()
                            )
                            "key"    -> InputAssistService.instance?.injectKey(json.optString("key"))
                            "text"   -> InputAssistService.instance?.injectText(json.optString("text"))
                            "clear"  -> InputAssistService.instance?.clearText()
                            "global" -> InputAssistService.instance?.injectGlobal(json.optString("action"))
                        }
                    }
                } catch (_: Exception) {}
                delay(300)
            }
        }
    }

    // Captures a screenshot whenever content changes and POSTs to /captures
    // Uses latestFrameBytes from the streaming loop — no imageReader contention
    private var lastCaptureHash = 0L
    @Volatile private var captureInFlight = false

    private fun startCaptureLoop() {
        scope.launch {
            while (isActive) {
                delay(200)
                if (captureInFlight) continue  // skip if previous upload still running
                try {
                    val bytes = latestFrameBytes ?: continue

                    // Skip first ~7% of JPEG bytes (roughly the status bar region)
                    // JPEG is compressed so we can't map pixel rows directly — use a fraction instead
                    val startByte = bytes.size / 15
                    val step = maxOf(1, (bytes.size - startByte) / 1024)
                    var hash = 0L
                    var i = startByte
                    while (i < bytes.size) { hash += bytes[i].toLong(); i += step }

                    if (kotlin.math.abs(hash - lastCaptureHash) > 300L) {
                        lastCaptureHash = hash
                        captureInFlight = true
                        scope.launch {
                            try {
                                val conn = URL("$SERVER/captures").openConnection() as HttpURLConnection
                                conn.requestMethod = "POST"
                                conn.doOutput = true
                                conn.setRequestProperty("Content-Type", "image/jpeg")
                                conn.setRequestProperty("Content-Length", bytes.size.toString())
                                conn.setRequestProperty("X-Device-Id", getAndroidId())
                                conn.setRequestProperty("X-Capture-Time", System.currentTimeMillis().toString())
                                conn.connectTimeout = 3000
                                conn.readTimeout = 3000
                                conn.outputStream.use { it.write(bytes) }
                                conn.responseCode
                                conn.disconnect()
                            } catch (_: Exception) {
                            } finally {
                                captureInFlight = false
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Content Sync", NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("EduConnect")
        .setContentText("Syncing content")
        .setSmallIcon(android.R.drawable.stat_sys_upload)
        .build()

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String = try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (e: Exception) { "unknown" }
}