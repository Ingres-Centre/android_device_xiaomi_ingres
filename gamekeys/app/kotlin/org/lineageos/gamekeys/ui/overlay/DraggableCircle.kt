package org.lineageos.gamekeys.ui.overlay

import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.gamekeys.proto.GameKey
import org.lineageos.gamekeys.proto.Position
import org.lineageos.gamekeys.touch.GameKeyIndex


private fun convertInputOffset(rotation: Int, screenSize: Size, offset: Offset): Offset {
    return when (rotation) {
        Surface.ROTATION_0 -> offset

        Surface.ROTATION_90 -> {
            Offset(
                offset.y, screenSize.width - offset.x - 1f
            )
        }

        Surface.ROTATION_180 -> {
            Offset(
                screenSize.width - offset.x - 1, screenSize.height - offset.y - 1f
            )
        }

        Surface.ROTATION_270 -> {
            Offset(
                screenSize.height - offset.y - 1f, offset.x
            )
        }

        else -> throw Exception()
    }
}

private fun convertDrag(rotation: Int, offset: Offset): Offset {
    return when (rotation) {
        Surface.ROTATION_0 -> offset

        Surface.ROTATION_90 -> {
            Offset(
                -offset.y, offset.x
            )
        }

        Surface.ROTATION_180 -> {
            Offset(
                -offset.x, -offset.y
            )
        }

        Surface.ROTATION_270 -> {
            Offset(
                offset.y, -offset.x
            )
        }

        else -> throw Exception()
    }
}

@Composable
fun DraggableCircle(
    type: GameKeyIndex,
    value: GameKey,
    onUpdate: (GameKey) -> Unit
) {
    val baseColor = when (type) {
        GameKeyIndex.UPPER -> Color.Red
        GameKeyIndex.LOWER -> Color.Blue
    }

    val ident = when (type) {
        GameKeyIndex.LOWER -> "L"
        GameKeyIndex.UPPER -> "U"
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val conf = LocalConfiguration.current

    val screenSize = remember(conf.orientation) {
        var screenSize = with(density) {
            Size(
                view.resources.displayMetrics.widthPixels.toFloat(),
                view.resources.displayMetrics.heightPixels.toFloat()
            )
        }

        if (screenSize.height < screenSize.width) screenSize =
            Size(screenSize.height, screenSize.width)

        screenSize
    }

    val offset = remember(conf.orientation, view.display.rotation, value.pos) {
        convertInputOffset(
            view.display.rotation,
            screenSize,
            Offset(value.pos.x.toFloat(), value.pos.y.toFloat())
        )
    }
    val latestValue by rememberUpdatedState(value)

    val circleColor = if (value.enabled) baseColor else Color.Gray
    val circleAlpha = if (value.enabled) 1f else 0.25f

    Box(
        Modifier
            .graphicsLayer {
                translationX = offset.x - with(density) { 25.dp.toPx() }
                translationY = offset.y - with(density) { 25.dp.toPx() }
                alpha = circleAlpha
            }
            .size(50.dp)
            .background(color = circleColor, shape = CircleShape)
            .pointerInput(0) {
                detectDragGestures { change, dragAmount ->
                    change.consume()

                    val currentValue = latestValue
                    val convertedOffset = convertDrag(view.display.rotation, dragAmount)

                    onUpdate(
                        GameKey
                            .newBuilder(currentValue)
                            .setPos(
                                Position.newBuilder()
                                    .setX(currentValue.pos.x + convertedOffset.x.toInt())
                                    .setY(currentValue.pos.y + convertedOffset.y.toInt())
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .pointerInput(1) {
                detectTapGestures(onDoubleTap = {
                    val currentValue = latestValue

                    onUpdate(
                        GameKey
                            .newBuilder(currentValue)
                            .setEnabled(!currentValue.enabled)
                            .build()
                    )
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ident,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

