package org.lineageos.leds.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.lineageos.leds.Application
import org.lineageos.leds.proto.settings

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        p1?.action // suppress warning

        val data = runBlocking { Application.INSTANCE.settings.data.first() }

        if (!data.restoreOnReboot)
            return

        val ledsService = Application.INSTANCE.ledsService

        ledsService.isEnabled = data.enabled
        ledsService.currentEffect = data.effect
        ledsService.frequency = data.frequency

        for (color in data.colorsMap) {
            ledsService.setColor(color.key, color.value)
        }

        ledsService.flushConfig(data.useCustomColors)
    }
}