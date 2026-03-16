package com.teacherapp.obfuscation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.telephony.TelephonyManager
import java.io.File

object AntiAnalysisUtils {

    /**
     * Check if running in emulator
     */
    fun isEmulator(context: Context): Boolean {
        // Check Build properties
        val buildProperties = listOf(
            Build.FINGERPRINT.contains("generic"),
            Build.FINGERPRINT.contains("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK"),
            Build.MANUFACTURER.contains("Genymotion"),
            Build.HARDWARE.contains("goldfish"),
            Build.HARDWARE.contains("ranchu"),
            Build.PRODUCT.contains("sdk"),
            Build.PRODUCT.contains("google_sdk"),
            Build.PRODUCT.contains("sdk_x86"),
            Build.PRODUCT.contains("vbox86p"),
            Build.BOARD.lowercase().contains("nox"),
            Build.BOOTLOADER.lowercase().contains("nox"),
            Build.HARDWARE.lowercase().contains("nox"),
            Build.PRODUCT.lowercase().contains("nox"),
            Build.SERIAL.lowercase().contains("nox")
        )

        if (buildProperties.any { it }) return true

        // Check telephony
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val phoneNumber = telephonyManager?.line1Number
            val knownEmulatorNumbers = listOf(
                "15555215554", "15555215556", "15555215558", "15555215560", "15555215562",
                "15555215564", "15555215566", "15555215568", "15555215570", "15555215572"
            )
            if (phoneNumber in knownEmulatorNumbers) return true
        } catch (e: Exception) {
            // Ignore
        }

        // Check for emulator files
        val emulatorFiles = listOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd"
        )

        if (emulatorFiles.any { File(it).exists() }) return true

        // Check system properties
        val knownProps = mapOf(
            "ro.hardware" to listOf("goldfish", "ranchu", "vbox86"),
            "ro.kernel.qemu" to listOf("1"),
            "ro.product.device" to listOf("generic"),
            "ro.product.model" to listOf("sdk", "google_sdk", "Android SDK"),
            "ro.build.product" to listOf("sdk", "google_sdk")
        )

        knownProps.forEach { (prop, values) ->
            try {
                val value = getSystemProperty(prop)
                if (values.any { value.contains(it, ignoreCase = true) }) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return false
    }

    /**
     * Check if debugger is attached
     */
    fun isDebuggerConnected(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Check if device is rooted
     */
    fun isDeviceRooted(context: Context): Boolean {
        // Check for su binary
        val suPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )

        if (suPaths.any { File(it).exists() }) return true

        // Check for root management apps
        val rootApps = listOf(
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.topjohnwu.magisk",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )

        rootApps.forEach { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found - this is good
            } catch (e: Exception) {
                // Other error
            }
        }

        // Check for writable system paths
        val testPaths = listOf(
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc"
        )

        testPaths.forEach { path ->
            try {
                val file = File(path)
                if (file.exists() && file.canWrite()) {
                    return true
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        return false
    }

    /**
     * Get system property
     */
    private fun getSystemProperty(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Execute action only if environment is safe
     */
    fun executeIfSafe(context: Context, action: () -> Unit) {
        if (!isEmulator(context) && !isDebuggerConnected() && !isDeviceRooted(context)) {
            action()
        }
    }
}