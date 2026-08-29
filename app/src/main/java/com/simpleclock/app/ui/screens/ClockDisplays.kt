package com.simpleclock.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
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
    val content = text.sumOf { if (it == ':') 14.0 else 54.0 }.toFloat() / 100f
    return content + (text.length - 1).coerceAtLeast(0) * 0.04f
}

internal fun nixieTubeAspect(text: String): Float {
    val content = text.sumOf { if (it == ':') 14.0 else 48.0 }.toFloat() / 100f
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
        val digitWidth = displayHeight * 0.54f
        val digitHeight = displayHeight * 0.82f
        val colonWidth = displayHeight * 0.14f
        val useHighContrastCard = color == Color.White

        val glassBackground = if (useHighContrastCard) {
            Brush.verticalGradient(
                listOf(
                    Color(0x40FFFFFF),
                    Color(0x18FFFFFF),
                    Color(0x28130F24),
                    Color(0x4D000000),
                ),
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.15f),
                    color.copy(alpha = 0.08f),
                    color.copy(alpha = 0.16f),
                ),
            )
        }

        val glassBorder = if (useHighContrastCard) {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.20f),
                    Color.Black.copy(alpha = 0.30f),
                ),
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.75f),
                    Color.White.copy(alpha = 0.30f),
                    color.copy(alpha = 0.25f),
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(
                displayHeight * 0.04f,
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
                        Canvas(
                            modifier = Modifier
                                .width(colonWidth)
                                .height(digitHeight),
                        ) {
                            val dotRadius = size.width * 0.25f
                            val dotColor = if (colonVisible) color else Color.Transparent
                            val topCenter = Offset(size.width / 2f, size.height * 0.38f)
                            val bottomCenter = Offset(size.width / 2f, size.height * 0.62f)
                            if (colonVisible) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = 0.7f), dotColor),
                                        center = topCenter,
                                        radius = dotRadius * 1.3f,
                                    ),
                                    radius = dotRadius,
                                    center = topCenter,
                                )
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color.White.copy(alpha = 0.7f), dotColor),
                                        center = bottomCenter,
                                        radius = dotRadius * 1.3f,
                                    ),
                                    radius = dotRadius,
                                    center = bottomCenter,
                                )
                            }
                        }
                    }
                } else {
                    val shape = RoundedCornerShape(displayHeight * 0.20f)
                    Box(
                        modifier = Modifier
                            .width(digitWidth)
                            .height(digitHeight)
                            .shadow(
                                elevation = 6.dp,
                                shape = shape,
                                ambientColor = if (useHighContrastCard) Color.Black else color.copy(alpha = 0.2f),
                                spotColor = if (useHighContrastCard) Color.Black else color.copy(alpha = 0.35f),
                            )
                            .clip(shape)
                            .background(glassBackground)
                            .border(1.2.dp, glassBorder, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = character.toString(),
                            color = color,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = (displayHeight.value * 0.58f / fontScale).sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = if (useHighContrastCard) Color.Black.copy(alpha = 0.4f) else color.copy(alpha = 0.25f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 3f,
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NixieTubeDisplay(
    text: String,
    color: Color,
    colonVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val fontScale = LocalDensity.current.fontScale
        val displayHeight = maxHeight
        val tubeWidth = displayHeight * 0.48f
        val tubeHeight = displayHeight * 0.88f
        val colonWidth = displayHeight * 0.14f
        val tubeShape = RoundedCornerShape(displayHeight * 0.22f)

        // Warm luminous ember color palette
        val glowColor = if (color == Color.White) Color(0xFFFF9500) else color
        val coreColor = if (color == Color.White) Color(0xFFFFFAF0) else Color.White

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
                        modifier = Modifier
                            .width(colonWidth)
                            .height(tubeHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val dotRadius = size.width * 0.26f
                            val dotColor = if (colonVisible) glowColor else Color.Transparent
                            val topCenter = Offset(size.width / 2f, size.height * 0.38f)
                            val bottomCenter = Offset(size.width / 2f, size.height * 0.62f)
                            if (colonVisible) {
                                drawCircle(glowColor.copy(alpha = 0.35f), dotRadius * 2.2f, topCenter)
                                drawCircle(dotColor, dotRadius, topCenter)
                                drawCircle(coreColor, dotRadius * 0.45f, topCenter)

                                drawCircle(glowColor.copy(alpha = 0.35f), dotRadius * 2.2f, bottomCenter)
                                drawCircle(dotColor, dotRadius, bottomCenter)
                                drawCircle(coreColor, dotRadius * 0.45f, bottomCenter)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(tubeWidth)
                            .height(tubeHeight)
                            .shadow(6.dp, shape = tubeShape, spotColor = glowColor.copy(alpha = 0.3f))
                            .clip(tubeShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x352E1D10),
                                        Color(0x201A1008),
                                        Color(0x40100A04),
                                    ),
                                ),
                            )
                            .border(
                                1.dp,
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        glowColor.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.10f),
                                    ),
                                ),
                                tubeShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Glass vertical reflection streak on the left
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = (displayHeight.value * 0.015f).dp)
                                .width((displayHeight.value * 0.015f).dp)
                                .height(tubeHeight * 0.75f)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.16f)),
                        )

                        // Ambient internal filament glow behind number
                        Box(
                            modifier = Modifier
                                .size((displayHeight.value * 0.36f).dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            glowColor.copy(alpha = 0.28f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        Text(
                            text = character.toString(),
                            color = glowColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = (displayHeight.value * 0.60f / fontScale).sp,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = glowColor.copy(alpha = 0.95f),
                                    blurRadius = (displayHeight.value * 0.12f).coerceAtLeast(8f),
                                ),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AuraGlowDisplay(
    text: String,
    color: Color,
    colonVisible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val displayHeight = maxHeight
        val adjustedSize = (displayHeight.value * 0.86f / fontScale).sp
        val parts = text.split(':')
        val hourPart = parts.getOrNull(0) ?: text
        val minutePart = parts.getOrNull(1)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = hourPart,
                color = color,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = adjustedSize,
                lineHeight = adjustedSize,
                style = TextStyle(
                    shadow = Shadow(
                        color = color.copy(alpha = 0.85f),
                        blurRadius = (displayHeight.value * 0.18f).coerceAtLeast(12f),
                    ),
                ),
            )

            if (minutePart != null) {
                Text(
                    text = ":",
                    color = if (colonVisible) color.copy(alpha = 0.85f) else Color.Transparent,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = adjustedSize,
                    lineHeight = adjustedSize,
                    modifier = Modifier.padding(horizontal = (displayHeight.value * 0.015f).dp),
                    style = TextStyle(
                        shadow = Shadow(
                            color = color.copy(alpha = 0.85f),
                            blurRadius = (displayHeight.value * 0.18f).coerceAtLeast(12f),
                        ),
                    ),
                )
                Text(
                    text = minutePart,
                    color = if (color == Color.White) Color(0xFF93C5FD) else color,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = adjustedSize,
                    lineHeight = adjustedSize,
                    style = TextStyle(
                        shadow = Shadow(
                            color = (if (color == Color.White) Color(0xFF60A5FA) else color).copy(alpha = 0.85f),
                            blurRadius = (displayHeight.value * 0.18f).coerceAtLeast(12f),
                        ),
                    ),
                )
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
    minimal: Boolean = false,
    swissRailway: Boolean = false,
    bauhaus: Boolean = false,
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

        if (bauhaus) {
            drawBauhausClock(
                center = center,
                radius = radius,
                hour = hour,
                minute = minute,
                second = second,
                themeColor = color,
            )
            return@Canvas
        }

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

private fun DrawScope.drawBauhausClock(
    center: Offset,
    radius: Float,
    hour: Int,
    minute: Int,
    second: Int?,
    themeColor: Color,
) {
    val secondValue = second?.toFloat() ?: 0f
    val bauhausBlue = Color(0xFF1E40AF)
    val bauhausRed = Color(0xFFDC2626)
    val bauhausYellow = Color(0xFFF59E0B)

    // Outer geometric frame
    drawCircle(
        color = themeColor.copy(alpha = 0.25f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.04f),
    )

    // 4 cardinal geometric discs
    drawCircle(color = bauhausRed, radius = radius * 0.05f, center = clockPoint(center, radius * 0.85f, 0f))
    drawCircle(color = bauhausBlue, radius = radius * 0.05f, center = clockPoint(center, radius * 0.85f, 90f))
    drawCircle(color = bauhausYellow, radius = radius * 0.05f, center = clockPoint(center, radius * 0.85f, 180f))
    drawCircle(color = themeColor, radius = radius * 0.05f, center = clockPoint(center, radius * 0.85f, 270f))

    // Hour Hand (Bold Bauhaus Blue Pointer with Disc)
    val hourAngle = ((hour % 12) + minute / 60f + secondValue / 3600f) * 30f
    val hourTarget = clockPoint(center, radius * 0.52f, hourAngle)
    drawLine(
        color = bauhausBlue,
        start = center,
        end = hourTarget,
        strokeWidth = radius * 0.11f,
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = bauhausBlue,
        radius = radius * 0.09f,
        center = hourTarget,
    )

    // Minute Hand (Bauhaus Yellow / Amber Pointer)
    val minuteAngle = (minute + secondValue / 60f) * 6f
    val minuteTarget = clockPoint(center, radius * 0.76f, minuteAngle)
    drawLine(
        color = bauhausYellow,
        start = center,
        end = minuteTarget,
        strokeWidth = radius * 0.07f,
        cap = StrokeCap.Square,
    )

    // Second Hand (Bauhaus Red with circle counterweight)
    if (second != null) {
        val secondAngle = secondValue * 6f
        val secondTip = clockPoint(center, radius * 0.84f, secondAngle)
        val secondTail = clockPoint(center, radius * 0.22f, secondAngle + 180f)
        drawLine(
            color = bauhausRed,
            start = secondTail,
            end = secondTip,
            strokeWidth = radius * 0.024f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = bauhausRed,
            radius = radius * 0.065f,
            center = secondTail,
            style = Stroke(width = radius * 0.02f),
        )
    }

    // Center pivot
    drawCircle(color = Color(0xFF1E293B), radius = radius * 0.075f, center = center)
    drawCircle(color = Color.White, radius = radius * 0.035f, center = center)
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
