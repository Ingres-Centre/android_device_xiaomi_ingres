package org.lineageos.leds.ui.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lineageos.leds.Application
import org.lineageos.leds.proto.settings

class LedsViewModel : ViewModel() {
    private val ledsService by lazy { Application.INSTANCE.ledsService }

    private val _state = MutableStateFlow(LedsUiState.load(Application.INSTANCE))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.emit(LedsUiState.load(Application.INSTANCE))
                delay(1_000L)
            }
        }
    }

    suspend fun update() {
        _state.emit(LedsUiState.load(Application.INSTANCE))

        if (_state.value.restoreOnReboot) {
            Application.INSTANCE.settings.updateData {
                it.toBuilder()
                    .setEnabled(_state.value.enabled)
                    .setEffect(_state.value.currentEffect)
                    .setFrequency(_state.value.frequency)
                    .putAllColors(
                        _state.value.colors
                            .mapIndexed { index, color ->
                                index to color.let { color ->
                                    Color(
                                        color.red,
                                        color.green,
                                        color.green,
                                        1f
                                    )
                                }.toArgb()
                            }
                            .toMap()
                    )
                    .build()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        ledsService.isEnabled = enabled

        runBlocking { update() }
    }

    fun setRestoreOnReboot(value: Boolean) {
        runBlocking {
            Application.INSTANCE.settings.updateData {
                it.toBuilder().setRestoreOnReboot(value).build()
            }

            update()
        }
    }

    fun setUseCustomColors(value: Boolean) {
        runBlocking {
            Application.INSTANCE.settings.updateData {
                it.toBuilder().setUseCustomColors(value).build()
            }
            ledsService.flushConfig(value)

            update()
        }
    }

    fun setEffect(index: Int) {
        ledsService.currentEffect = index
        ledsService.flushConfig(_state.value.useCustomColors)

        runBlocking { update() }
    }

    fun setFrequency(frequency: Int) {
        ledsService.frequency = frequency
        ledsService.flushConfig(_state.value.useCustomColors)

        runBlocking { update() }
    }

    fun setColor(index: Int, color: Color) {
        ledsService.setColor(index, color.toArgb() and 0xFFFFFF)
        ledsService.flushConfig(_state.value.useCustomColors)

        runBlocking { update() }
    }
}
