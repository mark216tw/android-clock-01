package com.simpleclock.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.ClockBackgroundColorMode
import com.simpleclock.app.data.ClockBackgroundPattern
import com.simpleclock.app.data.DEFAULT_RANDOM_RAINBOW_COLORS
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

internal data class ThemeMotionPalette(
    val base: Color,
    val accents: List<Color>,
    val gradient: List<Color>,
)

private data class OrbSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val travel: Float,
    val phase: Float,
    val speed: Float,
)

private data class BokehSpec(
    val x: Float,
    val y: Float,
    val radius: Float,
    val travel: Float,
    val phase: Float,
    val speed: Float,
)

internal fun buildThemeMotionPalette(
    isColorful: Boolean,
    rainbowColors: List<Color>,
    background: Color,
    surface: Color,
    primary: Color,
): ThemeMotionPalette {
    if (isColorful) {
        val accents = rainbowColors.ifEmpty { DEFAULT_RANDOM_RAINBOW_COLORS.map(::Color) }
        return ThemeMotionPalette(
            base = lerp(background, Color.Black, 0.72f),
            accents = accents,
            gradient = accents,
        )
    }

    val accents = listOf(
        lerp(background, primary, 0.22f),
        lerp(surface, primary, 0.36f),
        lerp(background, primary, 0.12f),
        lerp(surface, primary, 0.56f),
    )
    return ThemeMotionPalette(
        base = background,
        accents = accents,
        gradient = listOf(background, accents[0], surface, accents[1], background),
    )
}

@Composable
internal fun ThemeMotionBackground(
    settings: AppSettings,
    background: Color,
    surface: Color,
    primary: Color,
    modifier: Modifier = Modifier,
) {
    val pattern = settings.clockBackgroundPattern
    val isColorful = settings.clockBackgroundColorMode == ClockBackgroundColorMode.COLORFUL
    val rainbowColors = settings.randomRainbowColors.map(::Color)
    val palette = remember(isColorful, rainbowColors, background, surface, primary) {
        buildThemeMotionPalette(isColorful, rainbowColors, background, surface, primary)
    }
    val orbs = remember(palette.accents.size) {
        createOrbSpecs(seed = 0x41C64E6D, count = max(5, palette.accents.size))
    }
    val particles = remember(palette.accents.size) {
        createBokehSpecs(seed = 0x216216, count = max(18, palette.accents.size * 4))
    }
    val animated = settings.clockBackgroundDynamic
    val progress = if (animated) {
        val transition = rememberInfiniteTransition(label = pattern.name)
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = pattern.animationDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pattern progress",
        ).value
    } else {
        0.18f
    }

    Canvas(modifier = modifier) {
        if (size.minDimension <= 0f) return@Canvas
        when (pattern) {
            ClockBackgroundPattern.NONE -> {
                when {
                    isColorful -> drawPlainColorfulBackground(palette, progress)
                    animated -> drawFlowingGradient(palette, progress)
                    else -> drawRect(background)
                }
            }
            ClockBackgroundPattern.RAINBOW -> drawRainbow(palette, progress)
            ClockBackgroundPattern.AURORA -> drawFloatingAurora(palette, orbs, progress)
            ClockBackgroundPattern.CONCENTRIC_GLOW -> drawConcentricGlow(palette, progress)
            ClockBackgroundPattern.FLUID_WAVES -> drawFluidWaves(palette, progress)
            ClockBackgroundPattern.PRISM -> drawPrism(palette, progress)
            ClockBackgroundPattern.NEBULA -> drawNebula(palette, particles, progress)
            ClockBackgroundPattern.RADIAL_BEAMS -> drawRadialBeams(palette, progress)
            ClockBackgroundPattern.SOFT_CHECKER -> drawSoftChecker(palette, progress)
        }
    }
}

private val ClockBackgroundPattern.animationDurationMillis: Int
    get() = when (this) {
        ClockBackgroundPattern.CONCENTRIC_GLOW -> 32_000
        ClockBackgroundPattern.RADIAL_BEAMS -> 120_000
        ClockBackgroundPattern.SOFT_CHECKER -> 100_000
        ClockBackgroundPattern.NONE,
        ClockBackgroundPattern.RAINBOW,
        ClockBackgroundPattern.AURORA,
        ClockBackgroundPattern.FLUID_WAVES,
        ClockBackgroundPattern.PRISM,
        ClockBackgroundPattern.NEBULA,
        -> 80_000
    }

private fun DrawScope.drawPlainColorfulBackground(palette: ThemeMotionPalette, progress: Float) {
    val shift = progress * size.width * 0.12f
    drawRect(
        brush = Brush.linearGradient(
            colors = palette.gradient + palette.gradient.first(),
            start = Offset(-shift, 0f),
            end = Offset(size.width - shift, size.height),
        ),
    )
    drawRect(Color.Black, alpha = 0.14f)
}

private fun DrawScope.drawRainbow(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val center = Offset(size.width / 2f, size.height / 2f)
    val span = hypot(size.width, size.height) * 1.15f
    val colors = palette.accents
    val bandWidth = span / colors.size
    rotate(degrees = -24f + progress * 16f, pivot = center) {
        colors.forEachIndexed { index, color ->
            drawRect(
                color = color,
                topLeft = Offset(center.x - span / 2f + bandWidth * index, center.y - span / 2f),
                size = Size(bandWidth + 1f, span),
            )
        }
    }
    drawRect(Color.Black, alpha = 0.18f)
}

private fun DrawScope.drawFlowingGradient(palette: ThemeMotionPalette, progress: Float) {
    val shift = sin(progress * 2f * PI.toFloat()) * 0.12f
    drawRect(
        brush = Brush.linearGradient(
            colors = palette.gradient,
            start = Offset(size.width * shift, size.height * (0.12f - shift)),
            end = Offset(size.width * (0.88f + shift), size.height * (0.88f + shift)),
        ),
    )
}

private fun DrawScope.drawFloatingAurora(
    palette: ThemeMotionPalette,
    orbs: List<OrbSpec>,
    progress: Float,
) {
    drawRect(palette.base)
    orbs.forEachIndexed { index, orb ->
        val angle = 2f * PI.toFloat() * (progress * orb.speed + orb.phase)
        val center = Offset(
            x = (orb.x + cos(angle) * orb.travel) * size.width,
            y = (orb.y + sin(angle) * orb.travel) * size.height,
        )
        val color = palette.accents[index % palette.accents.size]
        val radius = size.minDimension * orb.radius
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.62f), color.copy(alpha = 0f)),
                center,
                radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private fun DrawScope.drawConcentricGlow(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = hypot(size.width, size.height) * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(palette.accents.last().copy(alpha = 0.48f), Color.Transparent),
            center,
            maxRadius,
        ),
        radius = maxRadius,
        center = center,
    )
    repeat(5) { index ->
        val phase = (progress + index / 5f) % 1f
        drawCircle(
            color = palette.accents[index % palette.accents.size],
            radius = maxRadius * phase,
            center = center,
            alpha = (1f - phase) * 0.34f,
            style = Stroke(width = size.minDimension * 0.018f),
        )
    }
}

private fun DrawScope.drawFluidWaves(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    palette.accents.forEachIndexed { index, color ->
        val path = Path()
        val baseline = size.height * (0.24f + index * 0.18f)
        val amplitude = size.height * (0.07f + index * 0.012f)
        path.moveTo(0f, size.height)
        path.lineTo(0f, baseline)
        val segments = 24
        for (step in 0..segments) {
            val fraction = step / segments.toFloat()
            val wave = sin((fraction * 2f + progress + index * 0.21f) * 2f * PI.toFloat())
            path.lineTo(fraction * size.width, baseline + wave * amplitude)
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(path, color.copy(alpha = 0.34f + index * 0.06f))
    }
}

private fun DrawScope.drawPrism(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val shift = sin(progress * 2f * PI.toFloat()) * size.width * 0.08f
    val points = listOf(
        Offset(size.width * 0.08f + shift, 0f),
        Offset(size.width * 0.48f + shift, 0f),
        Offset(size.width * 0.88f + shift, 0f),
        Offset(size.width * 0.28f - shift, size.height),
        Offset(size.width * 0.68f - shift, size.height),
    )
    repeat(7) { index ->
        val a = points[index % points.size]
        val b = points[(index + 1) % points.size]
        val c = Offset(size.width * ((index * 0.23f + 0.16f) % 1f), size.height * 0.52f)
        val path = Path().apply {
            moveTo(a.x, a.y)
            lineTo(b.x, b.y)
            lineTo(c.x, c.y)
            close()
        }
        drawPath(path, palette.accents[index % palette.accents.size].copy(alpha = 0.26f))
    }
}

private fun DrawScope.drawNebula(
    palette: ThemeMotionPalette,
    particles: List<BokehSpec>,
    progress: Float,
) {
    drawRect(lerp(palette.base, Color.Black, 0.16f))
    particles.forEachIndexed { index, particle ->
        val angle = 2f * PI.toFloat() * (progress * particle.speed + particle.phase)
        val center = Offset(
            x = (particle.x + sin(angle) * particle.travel) * size.width,
            y = (particle.y + cos(angle * 0.8f) * particle.travel) * size.height,
        )
        val color = palette.accents[index % palette.accents.size]
        val radius = size.minDimension * particle.radius
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0f)),
                center,
                radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private fun DrawScope.drawRadialBeams(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = hypot(size.width, size.height)
    rotate(progress * 360f, center) {
        repeat(16) { index ->
            if (index % 2 == 0) {
                val start = index * 2f * PI.toFloat() / 16f
                val end = (index + 1) * 2f * PI.toFloat() / 16f
                val path = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(center.x + cos(start) * radius, center.y + sin(start) * radius)
                    lineTo(center.x + cos(end) * radius, center.y + sin(end) * radius)
                    close()
                }
                drawPath(path, palette.accents[index % palette.accents.size].copy(alpha = 0.25f))
            }
        }
    }
}

private fun DrawScope.drawSoftChecker(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val cell = size.minDimension * 0.18f
    val offset = progress * cell * 2f
    val columns = ceil(size.width / cell).toInt() + 3
    val rows = ceil(size.height / cell).toInt() + 3
    for (row in -2 until rows) {
        for (column in -2 until columns) {
            if ((row + column) % 2 == 0) {
                drawRect(
                    color = palette.accents[(row - column).mod(palette.accents.size)].copy(alpha = 0.3f),
                    topLeft = Offset(column * cell + offset, row * cell + offset),
                    size = androidx.compose.ui.geometry.Size(cell, cell),
                )
            }
        }
    }
}

private fun createOrbSpecs(seed: Int, count: Int): List<OrbSpec> {
    val random = Random(seed)
    return List(count) {
        OrbSpec(
            x = random.nextFloat() * 0.8f + 0.1f,
            y = random.nextFloat() * 0.8f + 0.1f,
            radius = random.nextFloat() * 0.24f + 0.46f,
            travel = random.nextFloat() * 0.12f + 0.05f,
            phase = random.nextFloat(),
            speed = random.nextFloat() * 0.35f + 0.65f,
        )
    }
}

private fun createBokehSpecs(seed: Int, count: Int): List<BokehSpec> {
    val random = Random(seed xor 0x5F3759DF)
    return List(count) {
        BokehSpec(
            x = random.nextFloat() * 0.9f + 0.05f,
            y = random.nextFloat() * 0.9f + 0.05f,
            radius = random.nextFloat() * 0.11f + 0.05f,
            travel = random.nextFloat() * 0.10f + 0.03f,
            phase = random.nextFloat(),
            speed = random.nextFloat() * 0.55f + 0.45f,
        )
    }
}
