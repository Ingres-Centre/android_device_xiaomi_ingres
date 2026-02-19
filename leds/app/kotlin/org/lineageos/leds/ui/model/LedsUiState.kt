package org.lineageos.leds.ui.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.lineageos.leds.Application
import org.lineageos.leds.proto.settings

data class LedsUiState(
    val enabled: Boolean,

    val currentEffect: Int,
    val availableEffects: List<String>,

    val frequency: Int,

    val colors: List<Color>,

    val useCustomColors: Boolean,
    val restoreOnReboot: Boolean
) {
    companion object {
        fun load(context: Context): LedsUiState {
            val ledsService = Application.INSTANCE.ledsService
            val data = runBlocking { context.settings.data.first() }

            return LedsUiState(
                enabled = ledsService.isEnabled,
                currentEffect = ledsService.currentEffect,
                availableEffects = ledsService.availableEffects,
                frequency = ledsService.frequency,
                colors = ledsService.allColors.toList().stream().map {
                    Color(
                        (it and 0xFF0000) shr 16,
                        (it and 0x00FF00) shr 8,
                        (it and 0x0000FF) shr 0
                    )
                }.toList(),
                useCustomColors = data.useCustomColors,
                restoreOnReboot = data.restoreOnReboot,
            )
        }
    }
}
