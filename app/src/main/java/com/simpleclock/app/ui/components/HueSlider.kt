package com.simpleclock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.simpleclock.app.data.normalizeHue

@Composable
fun HueSlider(
    hue: Float,
    colorAtHue: (Float) -> Color,
    onHueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackColorAtHue: (Float) -> Color = colorAtHue,
    onHueChangeFinished: (() -> Unit)? = null,
) {
    val gradientColors = remember(trackColorAtHue) {
        (0..12).map { index -> trackColorAtHue(index * 30f) }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(horizontal = 10.dp),
        ) {
            drawRoundRect(
                brush = Brush.horizontalGradient(gradientColors),
                cornerRadius = CornerRadius(size.height / 2f),
            )
        }
        Slider(
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = "${normalizeHue(hue).toInt()}°"
            },
            value = normalizeHue(hue),
            onValueChange = onHueChange,
            onValueChangeFinished = onHueChangeFinished,
            valueRange = 0f..359f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = colorAtHue(hue),
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent,
            ),
        )
    }
}
