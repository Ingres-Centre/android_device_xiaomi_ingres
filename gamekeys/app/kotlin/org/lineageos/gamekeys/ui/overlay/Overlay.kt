package org.lineageos.gamekeys.ui.overlay

import android.content.Intent
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.lineageos.gamekeys.Application
import org.lineageos.gamekeys.R
import org.lineageos.gamekeys.proto.GameKey
import org.lineageos.gamekeys.proto.Position
import org.lineageos.gamekeys.service.OverlayService
import org.lineageos.gamekeys.touch.GameKeyIndex
import org.lineageos.gamekeys.ui.model.OverlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Overlay(overlayViewModel: OverlayViewModel) {
    val context = LocalContext.current

    val overlayUiState by overlayViewModel.state.collectAsStateWithLifecycle()

    val appViewModel = Application.INSTANCE.viewModel
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    var upperGameKey by remember { mutableStateOf(overlayUiState.currentAppSettings.upper) }
    var lowerGameKey by remember { mutableStateOf(overlayUiState.currentAppSettings.lower) }

    LaunchedEffect(overlayUiState) {
        if (!overlayUiState.waitForClose)
            return@LaunchedEffect

        val intent = Intent(context.applicationContext, OverlayService::class.java)
        context.stopService(intent)
    }

    LaunchedEffect(appState.packageName) {
        overlayViewModel.sync()
    }

    LaunchedEffect(overlayUiState) {
        upperGameKey = overlayUiState.currentAppSettings.upper
        lowerGameKey = overlayUiState.currentAppSettings.lower

        if (upperGameKey.pos.x == 0 && upperGameKey.pos.y == 0) {
            upperGameKey = GameKey
                .newBuilder(upperGameKey)
                .setPos(
                    Position.newBuilder()
                        .setX(1080 / 2)
                        .setY(2100 / 2)
                        .build()
                )
                .build()
        }

        if (lowerGameKey.pos.x == 0 && lowerGameKey.pos.y == 0) {
            lowerGameKey = GameKey
                .newBuilder(lowerGameKey)
                .setPos(
                    Position.newBuilder()
                        .setX(1080 / 2)
                        .setY(2100 / 2)
                        .build()
                )
                .build()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0, 0, 0, 150))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                // or the camera will overlap the button
                .padding(50.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            if (overlayUiState.packageName?.isEmpty() == true)
                SettingsWontBeSavedWarnTooltip()

            FilledIconButton(
                {
                    overlayViewModel.updateGameKeys(
                        upperGameKey,
                        lowerGameKey
                    )
                    overlayViewModel.close()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            ) { Icon(Icons.Rounded.Close, "Close", Modifier.size(50.dp)) }
        }

        DraggableCircle(
            GameKeyIndex.UPPER,
            upperGameKey
        ) { upperGameKey = it }

        DraggableCircle(
            GameKeyIndex.LOWER,
            lowerGameKey
        ) { lowerGameKey = it }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsWontBeSavedWarnTooltip() {
    val coroutineScope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(initialIsVisible = true, isPersistent = true)
    val tooltipPosition = TooltipDefaults.rememberTooltipPositionProvider()

    val showTooltip: () -> Unit = {
        coroutineScope.launch {
            tooltipState.show(MutatePriority.PreventUserInput)
        }
    }

    TooltipBox(
        positionProvider = tooltipPosition,
        state = tooltipState,
        modifier = Modifier,
        tooltip = {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(
                    stringResource(R.string.overlay_warn_will_not_be_saved),
                    Modifier.padding(10.dp)
                )
            }
        }
    ) {
        FilledIconButton(
            showTooltip,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(10.dp)
        ) { Icon(Icons.Rounded.Warning, "Warning", Modifier.size(50.dp)) }
    }
}
