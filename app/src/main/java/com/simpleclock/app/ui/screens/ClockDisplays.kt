package com.simpleclock.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal fun sevenSegmentAspect(text: String): Float {
    val content = text.sumOf { if (it == ':') 18.0 else 56.0 }.toFloat() / 100f
    return content + (text.length - 1).coerceAtLeast(0) * 0.055f
}

internal fun glassClockAspect(text: String): Float {
    val content = text.sumOf { if (it == ':') 16.0 else 62.0 }.toFloat() / 100f
    return content + (text.length - 1).coerceAtLeast(0) * 0.045f
}

internal fun dotMatrixAspect(text: String): Float {
    val content = text.sumOf { if (it == ':') 16.0 else 46.0 }.toFloat() / 100f
    return content + (text.length - 1).coerceAtLeast(0) * 0.06f
}

@Composable
internal fun SevenSegmentDisplay(
    text: String,
    color: Color,
    colonVisible: Boolean = true,
    thicknessRatio: Float = 0.105f,
    inactiveAlpha: Float = 0.08f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val gap = size.height * 0.055f
        val totalWidth = text.fold(0f) { width, character ->
            width + if (character == ':') size.height * 0.18f else size.height * 0.56f
        } +
            gap * (text.length - 1).coerceAtLeast(0)
        var x = (size.width - totalWidth) / 2f
        text.forEach { character ->
            if (character == ':') {
                val radius = size.height * 0.042f
                val colonColor = if (colonVisible) color else Color.Transparent
                drawCircle(colonColor, radius, Offset(x + size.height * 0.09f, size.height * 0.37f))
                drawCircle(colonColor, radius, Offset(x + size.height * 0.09f, size.height * 0.63f))
                x += size.height * 0.18f + gap
            } else {
                drawDigit(
                    character = character,
                    x = x,
                    height = size.height,
                    color = color,
                    thicknessRatio = thicknessRatio,
                    inactiveAlpha = inactiveAlpha,
                )
                x += size.height * 0.56f + gap
            }
        }
    }
}

private fun DrawScope.drawDigit(
    character: Char,
    x: Float,
    height: Float,
    color: Color,
    thicknessRatio: Float,
    inactiveAlpha: Float,
) {
    val active = when (character) {
        '0' -> setOf(0, 1, 2, 3, 4, 5)
        '1' -> setOf(1, 2)
        '2' -> setOf(0, 1, 6, 4, 3)
        '3' -> setOf(0, 1, 2, 3, 6)
        '4' -> setOf(5, 6, 1, 2)
        '5' -> setOf(0, 5, 6, 2, 3)
        '6' -> setOf(0, 5, 6, 4, 3, 2)
        '7' -> setOf(0, 1, 2)
        '8' -> (0..6).toSet()
        '9' -> setOf(0, 1, 2, 3, 5, 6)
        else -> emptySet()
    }
    val width = height * 0.56f
    val thickness = height * thicknessRatio
    val radius = CornerRadius(thickness / 2f)
    val horizontalWidth = width - thickness * 1.6f
    val verticalHeight = height * 0.39f
    val segments = listOf(
        Pair(Offset(x + thickness * 0.8f, 0f), Size(horizontalWidth, thickness)),
        Pair(Offset(x + width - thickness, thickness * 0.75f), Size(thickness, verticalHeight)),
        Pair(Offset(x + width - thickness, height * 0.535f), Size(thickness, verticalHeight)),
        Pair(Offset(x + thickness * 0.8f, height - thickness), Size(horizontalWidth, thickness)),
        Pair(Offset(x, height * 0.535f), Size(thickness, verticalHeight)),
        Pair(Offset(x, thickness * 0.75f), Size(thickness, verticalHeight)),
        Pair(Offset(x + thickness * 0.8f, height * 0.4625f), Size(horizontalWidth, thickness)),
    )
    segments.forEachIndexed { index, (offset, segmentSize) ->
        drawRoundRect(
            color = if (index in active) color else color.copy(alpha = inactiveAlpha),
            topLeft = offset,
            size = segmentSize,
            cornerRadius = radius,
        )
    }
}

@Composable
internal fun GlassClockDisplay(
    text: String,
    color: Color,
    colonVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val fontScale = LocalDensity.current.fontScale
        val displayHeight = maxHeight
        val digitWidth = displayHeight * 0.62f
        val digitHeight = displayHeight * 0.90f
        val colonWidth = displayHeight * 0.16f
        val useHighContrastCard = color == Color.White
        val cardColor = if (useHighContrastCard) Color(0x99130F24) else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(
                displayHeight * 0.045f,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            text.forEach { character ->
                if (character == ':') {
                    Box(
                        modifier = Modifier.width(colonWidth),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ":",
                            color = if (colonVisible) color else Color.Transparent,
                            fontSize = (displayHeight.value * 0.56f / fontScale).sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    val shape = RoundedCornerShape(displayHeight * 0.16f)
                    Box(
                        modifier = Modifier
                            .width(digitWidth)
                            .height(digitHeight)
                            .shadow(8.dp, shape)
                            .clip(shape)
                            .background(cardColor)
                            .border(1.dp, color.copy(alpha = 0.38f), shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = character.toString(),
                            color = color,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = (displayHeight.value * 0.62f / fontScale).sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AnalogClockDisplay(
    hour: Int,
    minute: Int,
    second: Int?,
    color: Color,
    minimal: Boolean,
    swissRailway: Boolean,
    modifier: Modifier = Modifier,
) {
    val highContrast = color == Color.White
    val faceColor = if (highContrast) Color(0xD9191226) else MaterialTheme.colorScheme.surface
    val dialColor = if (highContrast) Color.White else MaterialTheme.colorScheme.onSurface
    val secondColor = Color(0xFFFF6B6B)

    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * 0.46f
        val secondValue = second?.toFloat() ?: 0f

        if (swissRailway) {
            drawSwissRailwayClock(
                center = center,
                radius = radius,
                hour = hour,
                minute = minute,
                second = second,
            )
            return@Canvas
        }

        if (minimal) {
            drawCircle(
                color = color.copy(alpha = 0.34f),
                radius = radius,
                center = center,
                style = Stroke(width = radius * 0.025f),
            )
        } else {
            drawCircle(color = faceColor, radius = radius, center = center)
            drawCircle(
                color = color.copy(alpha = 0.58f),
                radius = radius,
                center = center,
                style = Stroke(width = radius * 0.035f),
            )
        }

        repeat(if (minimal) 12 else 60) { index ->
            val tickIndex = if (minimal) index * 5 else index
            val angle = tickIndex * 6f
            val major = tickIndex % 5 == 0
            if (minimal) {
                drawCircle(
                    color = if (index % 3 == 0) color else color.copy(alpha = 0.55f),
                    radius = if (index % 3 == 0) radius * 0.035f else radius * 0.022f,
                    center = clockPoint(center, radius * 0.86f, angle),
                )
            } else {
                drawLine(
                    color = dialColor.copy(alpha = if (major) 0.88f else 0.34f),
                    start = clockPoint(center, radius * if (major) 0.78f else 0.86f, angle),
                    end = clockPoint(center, radius * 0.92f, angle),
                    strokeWidth = radius * if (major) 0.035f else 0.014f,
                    cap = StrokeCap.Round,
                )
            }
        }

        val hourAngle = ((hour % 12) + minute / 60f + secondValue / 3600f) * 30f
        val minuteAngle = (minute + secondValue / 60f) * 6f
        drawLine(
            color = dialColor,
            start = center,
            end = clockPoint(center, radius * 0.50f, hourAngle),
            strokeWidth = radius * if (minimal) 0.085f else 0.095f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = center,
            end = clockPoint(center, radius * 0.73f, minuteAngle),
            strokeWidth = radius * if (minimal) 0.045f else 0.060f,
            cap = StrokeCap.Round,
        )
        if (second != null) {
            val secondAngle = secondValue * 6f
            drawLine(
                color = secondColor,
                start = clockPoint(center, radius * 0.13f, secondAngle + 180f),
                end = clockPoint(center, radius * 0.82f, secondAngle),
                strokeWidth = radius * 0.025f,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = if (second != null) secondColor else color,
            radius = radius * 0.065f,
            center = center,
        )
    }
}

private fun DrawScope.drawSwissRailwayClock(
    center: Offset,
    radius: Float,
    hour: Int,
    minute: Int,
    second: Int?,
) {
    val frameDark = Color(0xFF076B6E)
    val railwayTeal = Color(0xFF008C8C)
    val innerRim = Color(0xFFC9CED0)
    val secondRed = Color(0xFFDF2434)
    val secondValue = second?.toFloat() ?: 0f

    drawCircle(color = frameDark, radius = radius, center = center)
    drawCircle(color = railwayTeal, radius = radius * 0.965f, center = center)
    drawCircle(color = innerRim, radius = radius * 0.885f, center = center)
    drawCircle(color = Color(0xFFFAFAF8), radius = radius * 0.865f, center = center)

    repeat(60) { index ->
        val major = index % 5 == 0
        val angle = index * 6f
        drawLine(
            color = railwayTeal,
            start = clockPoint(center, radius * if (major) 0.64f else 0.74f, angle),
            end = clockPoint(center, radius * 0.81f, angle),
            strokeWidth = radius * if (major) 0.080f else 0.026f,
            cap = StrokeCap.Butt,
        )
    }

    val hourAngle = ((hour % 12) + minute / 60f + secondValue / 3600f) * 30f
    val minuteAngle = (minute + secondValue / 60f) * 6f
    drawLine(
        color = railwayTeal,
        start = clockPoint(center, radius * 0.11f, hourAngle + 180f),
        end = clockPoint(center, radius * 0.48f, hourAngle),
        strokeWidth = radius * 0.105f,
        cap = StrokeCap.Butt,
    )
    drawLine(
        color = railwayTeal,
        start = clockPoint(center, radius * 0.10f, minuteAngle + 180f),
        end = clockPoint(center, radius * 0.69f, minuteAngle),
        strokeWidth = radius * 0.075f,
        cap = StrokeCap.Butt,
    )

    if (second != null) {
        val secondAngle = secondValue * 6f
        val discCenter = clockPoint(center, radius * 0.68f, secondAngle)
        drawLine(
            color = secondRed,
            start = clockPoint(center, radius * 0.14f, secondAngle + 180f),
            end = discCenter,
            strokeWidth = radius * 0.024f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = secondRed, radius = radius * 0.075f, center = discCenter)
        drawCircle(color = secondRed, radius = radius * 0.050f, center = center)
    } else {
        drawCircle(color = railwayTeal, radius = radius * 0.050f, center = center)
    }
}

private fun clockPoint(center: Offset, length: Float, degrees: Float): Offset {
    val radians = (degrees - 90f) * PI.toFloat() / 180f
    return Offset(
        x = center.x + cos(radians) * length,
        y = center.y + sin(radians) * length,
    )
}

@Composable
internal fun LcdDisplay(
    text: String,
    color: Color,
    colonVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(4.dp),
    ) {
        SevenSegmentDisplay(
            text = text,
            color = color,
            colonVisible = colonVisible,
            thicknessRatio = 0.065f,
            inactiveAlpha = 0.06f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun DotMatrixDisplay(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val digitWidth = size.height * 0.46f
        val colonWidth = size.height * 0.16f
        val gap = size.height * 0.06f
        val totalWidth = text.fold(0f) { width, character ->
            width + if (character == ':') colonWidth else digitWidth
        } + gap * (text.length - 1).coerceAtLeast(0)
        var x = (size.width - totalWidth) / 2f
        val radius = size.height * 0.035f

        text.forEach { character ->
            if (character == ':') {
                drawCircle(color, radius, Offset(x + colonWidth / 2f, size.height * 0.36f))
                drawCircle(color, radius, Offset(x + colonWidth / 2f, size.height * 0.64f))
                x += colonWidth + gap
            } else {
                val pattern = dotPattern(character)
                val xStep = (digitWidth - radius * 2f) / 4f
                val yStep = (size.height - radius * 2f) / 6f
                pattern.forEachIndexed { row, values ->
                    values.forEachIndexed { column, value ->
                        drawCircle(
                            color = if (value == '1') color else color.copy(alpha = 0.07f),
                            radius = radius,
                            center = Offset(
                                x + radius + column * xStep,
                                radius + row * yStep,
                            ),
                        )
                    }
                }
                x += digitWidth + gap
            }
        }
    }
}

private fun dotPattern(character: Char): List<String> = when (character) {
    '0' -> listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110")
    '1' -> listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110")
    '2' -> listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111")
    '3' -> listOf("11110", "00001", "00001", "01110", "00001", "00001", "11110")
    '4' -> listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010")
    '5' -> listOf("11111", "10000", "10000", "11110", "00001", "00001", "11110")
    '6' -> listOf("01110", "10000", "10000", "11110", "10001", "10001", "01110")
    '7' -> listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000")
    '8' -> listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110")
    '9' -> listOf("01110", "10001", "10001", "01111", "00001", "00001", "01110")
    else -> List(7) { "00000" }
}
