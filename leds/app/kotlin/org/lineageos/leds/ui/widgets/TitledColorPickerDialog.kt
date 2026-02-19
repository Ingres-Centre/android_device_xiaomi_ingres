package org.lineageos.leds.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.lineageos.leds.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TitledColorPickerDialog(
    value: Color,
    onValueChange: (Color) -> Unit,

    title: String,
    description: String?,

    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier
            .fillMaxWidth()
            .clickable { expanded = true },
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)

            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
            }
        }

        Box(
            Modifier
                .size(25.dp)
                .border(1.dp, MaterialTheme.colorScheme.inverseSurface, CircleShape)
                .clip(CircleShape)
                .background(value)
        )
    }

    if (expanded) {
        Dialog({ expanded = false }) {
            Card {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.padding(24.dp)) {
                    var colorH by remember { mutableFloatStateOf(0f) }
                    var colorS by remember { mutableFloatStateOf(0f) }
                    var colorV by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(value) {
                        val hsv = rgbToHsv(
                            r = value.red,
                            g = value.green,
                            b = value.blue
                        )
                        colorH = hsv.h
                        colorS = hsv.s
                        colorV = hsv.v
                    }

                    val targetColor = Color.hsv(colorH, colorS, colorV)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(
                                    color = targetColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ARGB: ${targetColor.toHexArgb()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "H: ${colorH.roundToInt()}  S: ${(colorS * 100).roundToInt()}%  V: ${(colorV * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Hue", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = colorH,
                        onValueChange = { colorH = it },
                        valueRange = 0f..360f
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Saturation", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = colorS,
                        onValueChange = { colorS = it },
                        valueRange = 0f..1f
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("Value", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = colorV,
                        onValueChange = { colorV = it },
                        valueRange = 0f..1f
                    )

                    Spacer(Modifier.height(12.dp))

                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        TextButton({
                            onValueChange(targetColor)
                            expanded = false
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

private data class HSV(val h: Float, val s: Float, val v: Float)

private fun rgbToHsv(r: Float, g: Float, b: Float): HSV {
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min

    val v = max
    val s = if (max <= 0f) 0f else (delta / max)

    val h = when {
        delta <= 0f -> 0f
        max == r -> (60f * ((g - b) / delta)).mod360()
        max == g -> (60f * (((b - r) / delta) + 2f)).mod360()
        else -> (60f * (((r - g) / delta) + 4f)).mod360()
    }

    return HSV(h, s, v)
}

private fun Float.mod360(): Float {
    var x = this % 360f
    if (x < 0f) x += 360f
    return x
}

private fun Color.toHexArgb(): String {
    val argb = this.toArgb()
    return String.format("#%08X", argb)
}

@Preview
@Composable
fun TitledColorPickerDialogDemo() {
    val picked by remember { mutableStateOf(Color.Red) } // or mutableStateOf(Color())

    Column(Modifier.fillMaxWidth()) {
        TitledColorPickerDialog(
            picked, {}, "Picker",
            description = null,
            modifier = Modifier
        )
    }
}