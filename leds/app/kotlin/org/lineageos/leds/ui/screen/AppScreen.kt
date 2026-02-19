package org.lineageos.leds.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.lineageos.leds.R
import org.lineageos.leds.ui.model.LedsViewModel
import org.lineageos.leds.ui.theme.AppTheme
import org.lineageos.leds.ui.widgets.CategoryChip
import org.lineageos.leds.ui.widgets.TitledColorPickerDialog
import org.lineageos.leds.ui.widgets.TitledDropdownMenuMap
import org.lineageos.leds.ui.widgets.TitledSwitcher
import org.lineageos.leds.ui.widgets.TitledVerticalPagerDialog

@Composable
fun RawSetScreen() {
    val viewModel by remember { mutableStateOf(LedsViewModel()) }
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.padding(20.dp, 0.dp)) {
        val modifier = Modifier.padding(0.dp, 10.dp)

        TitledSwitcher(
            uiState.enabled,
            { viewModel.setEnabled(it) },
            title = stringResource(R.string.led_hwen_title),
            description = "/sys/class/leds/aw22xxx_led/hwen",
            modifier = modifier
        )

        val effects = uiState.availableEffects

        TitledDropdownMenuMap(
            uiState.currentEffect,
            { viewModel.setEffect(it) },
            values = effects,
            valueKey = { it },
            title = stringResource(R.string.led_effect_title),
            description = "/sys/class/leds/aw22xxx_led/effect",
            modifier = modifier
        )

        CategoryChip(stringResource(R.string.cat_application), modifier)

        TitledSwitcher(
            uiState.restoreOnReboot,
            { viewModel.setRestoreOnReboot(it) },
            title = stringResource(R.string.led_use_saved_settings_title),
            description = stringResource(R.string.led_use_saved_settings_description),
            modifier = modifier
        )

        TitledSwitcher(
            uiState.useCustomColors,
            { viewModel.setUseCustomColors(it) },
            title = stringResource(R.string.led_use_own_values_title),
            description = stringResource(R.string.led_use_own_values_description),
            modifier = modifier
        )

        CategoryChip(stringResource(R.string.cat_own_values), modifier)

        // https://github.com/MiCode/Xiaomi_Kernel_OpenSource/blob/ffbfcd5d72d0c8b84512a613f05c4c7fa9faffb6/drivers/leds/leds-aw22xxx.c#L900
        val frequencies: List<Int> = listOf(
            0,
            64,
            128,
            192,
            256,
            320,
            384,
            448,
            512,
            576,
            640,
            704,
            768,
            832,
            896,
            960,
            1024,
            1088,
            1152,
            1216,
            1280,
            1344,
            1408,
            1472,
            1536,
            1600,
            1664,
            1728,
            1792,
            1856,
            1920,
            1984,
            2048,
            2112,
            2176,
            2240,
            2304,
            2368,
            2432,
            2496,
            2560,
            2624,
            2688,
            2752,
            2816,
            2880,
            2944,
            3008,
            3072,
            3136,
            3200,
            3264,
            3328,
            3392,
            3456,
            3520,
            3584,
            3648,
            3712,
            3776,
            3840,
            3904,
            3968,
            4032
        )

        TitledVerticalPagerDialog(
            frequencies.indexOf(uiState.frequency),
            { viewModel.setFrequency(frequencies[it]) },
            values = frequencies,
            valueKey = { "$it Hz" },
            title = stringResource(R.string.led_frq_title),
            description = "/sys/class/leds/aw22xxx_led/frq",
            modifier = modifier
        )

        LazyColumn {
            itemsIndexed(uiState.colors) { index, color ->
                TitledColorPickerDialog(
                    color,
                    { viewModel.setColor(index, it) },
                    stringResource(R.string.led_rgb_title, index),
                    description = "/sys/class/leds/aw22xxx_led/rgb:$index",
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun AppScreen() {
    AppTheme {
        Surface {
            Box(Modifier.windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Vertical))) {
                RawSetScreen()
            }
        }
    }
}