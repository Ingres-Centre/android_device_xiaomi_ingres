package org.lineageos.gamekeys.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.lineageos.gamekeys.Application
import org.lineageos.gamekeys.proto.GameKey
import org.lineageos.gamekeys.proto.PerAppSettings
import org.lineageos.gamekeys.proto.settings

data class AppState(
    val packageName: String,

    val currentAppSettings: PerAppSettings
) {
    companion object {
        fun new() =
            AppState(
                packageName = "",
                currentAppSettings = PerAppSettings.newBuilder()
                    .setUpper(GameKey.getDefaultInstance())
                    .setLower(GameKey.getDefaultInstance())
                    .build(),
            )
    }
}

class AppViewModel(private val application: Application) : ViewModel() {
    private val _state = MutableStateFlow(AppState.new())
    val state = _state.asStateFlow()

    fun setPackageName(packageName: String?) {
        if (packageName == null) {
            _state.update {
                it.copy(
                    packageName = "",
                    currentAppSettings = PerAppSettings.newBuilder()
                        .setUpper(GameKey.getDefaultInstance())
                        .setLower(GameKey.getDefaultInstance())
                        .build(),
                )
            }

            return
        }

        val gamekeys =
            runBlocking { application.settings.data.map { it.appsMap[packageName] }.first() }
                ?: PerAppSettings.getDefaultInstance()

        _state.update {
            it.copy(
                packageName = packageName,
                currentAppSettings = PerAppSettings.newBuilder()
                    .setUpper(gamekeys.upper)
                    .setLower(gamekeys.lower)
                    .build(),
            )
        }
    }

    fun updateCurrentGameKeys(upper: GameKey, lower: GameKey) {
        _state.update {
            it.copy(
                currentAppSettings = PerAppSettings.newBuilder()
                    .setUpper(upper)
                    .setLower(lower)
                    .build(),
            )
        }
    }

    fun save() {
        val state = _state.value

        if (state.packageName.isEmpty())
            return

        runBlocking {
            application
                .settings
                .updateData {
                    it
                        .toBuilder()
                        .putApps(state.packageName, state.currentAppSettings)
                        .build()
                }
        }
    }
}
