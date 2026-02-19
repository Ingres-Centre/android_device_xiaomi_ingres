package org.lineageos.gamekeys.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lineageos.gamekeys.service.EventListenerService

class BootCompletedReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent?) {
        context.startService(Intent(context, EventListenerService::class.java))
    }
}
