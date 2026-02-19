package org.lineageos.gamekeys.ui.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.lineageos.gamekeys.Application
import org.lineageos.gamekeys.proto.GameKey
import org.lineageos.gamekeys.proto.PerAppSettings

class OverlayViewModel : ViewModel() {
    private val _state = MutableStateFlow(OverlayUiState.new())
    val state = _state.asStateFlow()

    fun close() {
        _state.update {
            it.copy(waitForClose = true)
        }
    }

    fun sync() {
        val appViewModel = Application.INSTANCE.viewModel
        val appState = appViewModel.state.value

        _state.update {
            it.copy(
                packageName = appState.packageName,
                currentAppSettings = appState.currentAppSettings
            )
        }
    }

    fun updateGameKeys(
        upper: GameKey,
        lower: GameKey,
    ) {
        if (_state.value.packageName?.isNotEmpty() != true)
            return

        _state.update {
            it.copy(
                currentAppSettings = PerAppSettings
                    .newBuilder()
                    .setUpper(upper)
                    .setLower(lower)
                    .build()
            )
        }

        val appViewModel = Application.INSTANCE.viewModel

        appViewModel.updateCurrentGameKeys(
            _state.value.currentAppSettings.upper,
            _state.value.currentAppSettings.lower,
        )

        appViewModel.save()
    }
}
