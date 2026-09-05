package com.simpleclock.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.DEFAULT_RANDOM_RAINBOW_COLORS
import com.simpleclock.app.data.ThemeColorMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private const val EFFECT_CROSSFADE_MILLIS = 1_200

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
    isRainbow: Boolean,
    rainbowColors: List<Color>,
    background: Color,
    surface: Color,
    primary: Color,
): ThemeMotionPalette {
    if (isRainbow) {
        val accents = rainbowColors.ifEmpty { DEFAULT_RANDOM_RAINBOW_COLORS.map(::Color) }
        return ThemeMotionPalette(
            base = lerp(background, Color.Black, 0.72f),
            accents = accents,
            gradient = accents,
        )
    }

    val accents = listOf(
        lerp(background, primary, 0.18f),
        lerp(surface, primary, 0.28f),
        lerp(background, primary, 0.10f),
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
    randomThemeMotion: ThemeColorMotion,
    background: Color,
    surface: Color,
    primary: Color,
    modifier: Modifier = Modifier,
) {
    val isRainbow = settings.themeColor == AppThemeColor.RANDOM_RAINBOW
    val rainbowColors = settings.randomRainbowColors.map(::Color)
    val palette = remember(isRainbow, rainbowColors, background, surface, primary) {
        buildThemeMotionPalette(
            isRainbow = isRainbow,
            rainbowColors = rainbowColors,
            background = background,
            surface = surface,
            primary = primary,
        )
    }

    if (settings.themeColorMotion == ThemeColorMotion.STATIC) {
        StaticThemeBackground(
            isRainbow = isRainbow,
            rainbowColors = palette.accents,
            background = background,
            modifier = modifier,
        )
        return
    }

    val effect = when (settings.themeColorMotion) {
        ThemeColorMotion.RANDOM_DYNAMIC -> randomThemeMotion
        else -> settings.themeColorMotion
    }
    Crossfade(
        targetState = effect,
        modifier = modifier,
        animationSpec = tween(EFFECT_CROSSFADE_MILLIS),
        label = "theme motion",
    ) { activeEffect ->
        AnimatedThemeMotion(
            effect = activeEffect,
            palette = palette,
            isRainbow = isRainbow,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun StaticThemeBackground(
    isRainbow: Boolean,
    rainbowColors: List<Color>,
    background: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        if (isRainbow) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = rainbowColors,
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
        } else {
            drawRect(background)
        }
    }
}

@Composable
private fun AnimatedThemeMotion(
    effect: ThemeColorMotion,
    palette: ThemeMotionPalette,
    isRainbow: Boolean,
    modifier: Modifier,
) {
    val durationMillis = when (effect) {
        ThemeColorMotion.FLOWING_GRADIENT -> 90_000
        ThemeColorMotion.FLOATING_AURORA -> 75_000
        ThemeColorMotion.ROTATING_GLOW -> 120_000
        ThemeColorMotion.EXPANDING_RIPPLES -> 24_000
        ThemeColorMotion.FLOATING_BOKEH -> 80_000
        ThemeColorMotion.STATIC,
        ThemeColorMotion.RANDOM_DYNAMIC,
        -> error("A concrete theme motion effect is required")
    }
    val repeatMode = when (effect) {
        ThemeColorMotion.ROTATING_GLOW,
        ThemeColorMotion.EXPANDING_RIPPLES,
        -> RepeatMode.Restart
        ThemeColorMotion.FLOWING_GRADIENT,
        ThemeColorMotion.FLOATING_AURORA,
        ThemeColorMotion.FLOATING_BOKEH,
        -> RepeatMode.Reverse
        ThemeColorMotion.STATIC,
        ThemeColorMotion.RANDOM_DYNAMIC,
        -> error("A concrete theme motion effect is required")
    }
    val transition = rememberInfiniteTransition(label = effect.name)
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = repeatMode,
        ),
        label = "theme motion progress",
    )
    val orbs = remember(palette.accents.size) {
        createOrbSpecs(seed = 0x41C64E6D, count = max(4, palette.accents.size))
    }
    val bokeh = remember(palette.accents.size) {
        createBokehSpecs(seed = 0x216216, count = max(12, palette.accents.size * 2))
    }

    Canvas(modifier = modifier) {
        when (effect) {
            ThemeColorMotion.FLOWING_GRADIENT -> drawFlowingGradient(palette, progress.value)
            ThemeColorMotion.FLOATING_AURORA -> drawFloatingAurora(palette, orbs, progress.value)
            ThemeColorMotion.ROTATING_GLOW -> drawRotatingGlow(palette, progress.value)
            ThemeColorMotion.EXPANDING_RIPPLES -> drawExpandingRipples(palette, progress.value)
            ThemeColorMotion.FLOATING_BOKEH -> drawFloatingBokeh(palette, bokeh, progress.value)
            ThemeColorMotion.STATIC,
            ThemeColorMotion.RANDOM_DYNAMIC,
            -> error("A concrete theme motion effect is required")
        }
        if (isRainbow) {
            drawRect(Color.Black, alpha = 0.18f)
        }
    }
}

private fun DrawScope.drawFlowingGradient(palette: ThemeMotionPalette, progress: Float) {
    val shift = progress * 0.16f
    drawRect(
        brush = Brush.linearGradient(
            colors = palette.gradient,
            start = Offset(size.width * shift, size.height * shift),
            end = Offset(size.width * (0.84f + shift), size.height * (0.84f + shift)),
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
                colors = listOf(color.copy(alpha = 0.52f), color.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

private fun DrawScope.drawRotatingGlow(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val center = Offset(size.width / 2f, size.height / 2f)
    val colors = if (palette.gradient.first() == palette.gradient.last()) {
        palette.gradient
    } else {
        palette.gradient + palette.gradient.first()
    }
    rotate(degrees = progress * 360f, pivot = center) {
        drawCircle(
            brush = Brush.sweepGradient(colors = colors, center = center),
            radius = hypot(size.width, size.height) / 2f,
            center = center,
        )
    }
}

private fun DrawScope.drawExpandingRipples(palette: ThemeMotionPalette, progress: Float) {
    drawRect(palette.base)
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxRadius = hypot(size.width, size.height) * 0.62f
    palette.accents.forEachIndexed { index, color ->
        val phase = (progress + index.toFloat() / palette.accents.size) % 1f
        drawCircle(
            color = color,
            radius = maxRadius * phase,
            center = center,
            alpha = (1f - phase) * 0.42f,
            style = Stroke(width = size.minDimension * 0.025f),
        )
    }
}

private fun DrawScope.drawFloatingBokeh(
    palette: ThemeMotionPalette,
    particles: List<BokehSpec>,
    progress: Float,
) {
    drawRect(palette.base)
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
                colors = listOf(color.copy(alpha = 0.44f), color.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
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
