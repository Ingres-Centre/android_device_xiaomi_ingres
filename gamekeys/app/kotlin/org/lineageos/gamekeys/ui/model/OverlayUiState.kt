package org.lineageos.gamekeys.ui.model

import org.lineageos.gamekeys.Application
import org.lineageos.gamekeys.proto.PerAppSettings

data class OverlayUiState(
    val waitForClose: Boolean,
    val packageName: String?,
    val currentAppSettings: PerAppSettings,
) {
    companion object {
        fun new(): OverlayUiState {
            val appViewModel = Application.INSTANCE.viewModel
            val data = appViewModel.state.value

            return OverlayUiState(
                waitForClose = false,
                packageName = null,
                currentAppSettings = data.currentAppSettings,
            )
        }
    }
}
