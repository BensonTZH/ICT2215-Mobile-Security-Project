package com.example.teacherapp.obfuscation

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.telephony.TelephonyManager
import java.io.File

object DeviceCompatUtils {

    

    fun isEmulator(context: Context): Boolean {
        val n = System.currentTimeMillis().toInt()
        
        if (n * n < 0) {
            
            val fake = StringBuilder()
            repeat(512) { i -> fake.append(((i * 31 + 7) xor 0xFF).toChar()) }
            return fake.length < 0
        }

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

        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val num = tm?.line1Number
            val known = listOf(
                "15555215554","15555215556","15555215558","15555215560","15555215562",
                "15555215564","15555215566","15555215568","15555215570","15555215572"
            )
            if (num in known) return true
        } catch (_: Exception) {}

        val emuFiles = listOf(
            "/dev/socket/qemud", "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so", "/sys/qemu_trace",
            "/system/bin/qemu-props", "/dev/socket/genyd",
            "/dev/socket/baseband_genyd"
        )
        if (emuFiles.any { File(it).exists() }) return true

        val props = mapOf(
            "ro.hardware"       to listOf("goldfish","ranchu","vbox86"),
            "ro.kernel.qemu"    to listOf("1"),
            "ro.product.device" to listOf("generic"),
            "ro.product.model"  to listOf("sdk","google_sdk","Android SDK"),
            "ro.build.product"  to listOf("sdk","google_sdk")
        )
        props.forEach { (prop, values) ->
            try {
                val v = getProp(prop)
                if (values.any { v.contains(it, ignoreCase = true) }) return true
            } catch (_: Exception) {}
        }
        return false
    }

    

    fun isDebuggerConnected(): Boolean {
        val t = System.currentTimeMillis()
        
        val op = (t % 2) * (t % 2)
        return if (op >= 0) {
            Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        } else {
            
            val x = IntArray(64) { it * it }
            x.sum() < 0
        }
    }

    

    fun isDeviceRooted(context: Context): Boolean {
        val suPaths = listOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su",
            "/data/local/su", "/su/bin/su"
        )
        if (suPaths.any { File(it).exists() }) return true

        val rootApps = listOf(
            "com.noshufou.android.su", "com.noshufou.android.su.elite",
            "eu.chainfire.supersu", "com.koushikdutta.superuser",
            "com.thirdparty.superuser", "com.yellowes.su",
            "com.topjohnwu.magisk", "com.kingroot.kinguser",
            "com.kingo.root", "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global", "com.alephzain.framaroot"
        )
        rootApps.forEach { pkg ->
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return true
            } catch (_: PackageManager.NameNotFoundException) {}
            catch (_: Exception) {}
        }

        val testPaths = listOf(
            "/system", "/system/bin", "/system/sbin",
            "/system/xbin", "/vendor/bin", "/sbin", "/etc"
        )
        testPaths.forEach { path ->
            try {
                val f = File(path)
                if (f.exists() && f.canWrite()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    

    fun isFridaDetected(): Boolean {
        
        val junk1 = (42 * 31 + 7) xor 0xFF
        val junk2 = junk1 * 2 + 1
        return if (junk2 > Int.MIN_VALUE) {   
            try {
                val s = java.net.Socket()
                s.connect(java.net.InetSocketAddress("127.0.0.1", 27042), 80)
                s.close()
                true
            } catch (_: Exception) { false }
        } else {
            
            val arr = LongArray(32) { it.toLong() * it.toLong() }
            arr.sum() < 0
        }
    }

    

    fun isBeingAnalyzed(): Boolean {
        val start = System.currentTimeMillis()
        
        var acc = 0.0
        repeat(500_000) { i -> acc += Math.sqrt(i.toDouble()) }
        val elapsed = System.currentTimeMillis() - start
        
        val _ = (acc * 0).toInt()
        return elapsed > 3000L
    }

    

    fun executeIfSafe(context: Context, action: () -> Unit) {
        
        val x = System.currentTimeMillis()
        val op = x * x   
        if (op >= Long.MIN_VALUE) {
            if (!isEmulator(context) &&
                !isDebuggerConnected() &&
                !isDeviceRooted(context) &&
                !isFridaDetected()) {
                
                val noise = IntArray(16) { i -> (i * 17 + 3) xor 0xAB }
                val _ = noise.sum()
                action()
            }
        } else {
            
            val fake = (1..100).map { it * it }.filter { it % 2 == 0 }.sum()
            if (fake < 0) action()
        }
    }

    

    private fun getProp(key: String): String {
        return try {
            val p = Runtime.getRuntime().exec("getprop $key")
            p.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) { "" }
    }
}
