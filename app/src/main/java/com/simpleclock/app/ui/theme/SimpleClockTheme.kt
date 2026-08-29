package com.simpleclock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.simpleclock.app.data.AppSettings
import com.simpleclock.app.data.AppThemeColor
import com.simpleclock.app.data.AppThemeMode

data class ThemePreviewColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
)

fun previewColors(
    color: AppThemeColor,
    dark: Boolean,
    randomRainbowColors: List<Long> = emptyList(),
): ThemePreviewColors {
    if (dark) {
        return when (color) {
            AppThemeColor.CORAL -> ThemePreviewColors(Color(0xFFFFB2BC), Color(0xFF1E1013), Color(0xFF2A171B))
            AppThemeColor.TANGERINE -> ThemePreviewColors(Color(0xFFFFB68A), Color(0xFF20120A), Color(0xFF2D1A10))
            AppThemeColor.SUNFLOWER -> ThemePreviewColors(Color(0xFFFFD95B), Color(0xFF1C1808), Color(0xFF29230D))
            AppThemeColor.MINT -> ThemePreviewColors(Color(0xFF72DCBD), Color(0xFF091915), Color(0xFF10241E))
            AppThemeColor.SKY -> ThemePreviewColors(Color(0xFF82CFFF), Color(0xFF09171F), Color(0xFF10232E))
            AppThemeColor.GRAPE -> ThemePreviewColors(Color(0xFFD0BCFF), Color(0xFF171020), Color(0xFF22182E))
            AppThemeColor.RAINBOW -> ThemePreviewColors(Color(0xFFE9C7FF), Color(0xFF171020), Color(0xFF251735))
            AppThemeColor.RANDOM_RAINBOW -> {
                val primaryColor = randomRainbowColors.firstOrNull()?.let { Color(it) } ?: Color(0xFFE9C7FF)
                ThemePreviewColors(primaryColor, Color(0xFF171020), Color(0xFF251735))
            }
        }
    }
    return when (color) {
        AppThemeColor.CORAL -> ThemePreviewColors(Color(0xFFC92F48), Color(0xFFFFF7F7), Color(0xFFFFEDEF))
        AppThemeColor.TANGERINE -> ThemePreviewColors(Color(0xFFA94400), Color(0xFFFFF8F3), Color(0xFFFFEEDC))
        AppThemeColor.SUNFLOWER -> ThemePreviewColors(Color(0xFF765A00), Color(0xFFFFFBEA), Color(0xFFFFF1B8))
        AppThemeColor.MINT -> ThemePreviewColors(Color(0xFF006B50), Color(0xFFF0FCF8), Color(0xFFDDF8EE))
        AppThemeColor.SKY -> ThemePreviewColors(Color(0xFF006493), Color(0xFFF2FAFF), Color(0xFFDCEFFC))
        AppThemeColor.GRAPE -> ThemePreviewColors(Color(0xFF6546B8), Color(0xFFFAF7FF), Color(0xFFEDE5FF))
        AppThemeColor.RAINBOW -> ThemePreviewColors(Color(0xFF7C3AED), Color(0xFFFFF7FF), Color(0xFFF2E7FF))
        AppThemeColor.RANDOM_RAINBOW -> {
            val primaryColor = randomRainbowColors.firstOrNull()?.let { Color(it) } ?: Color(0xFF7C3AED)
            ThemePreviewColors(primaryColor, Color(0xFFFFF7FF), Color(0xFFF2E7FF))
        }
    }
}

@Composable
fun SimpleClockTheme(
    settings: AppSettings,
    content: @Composable (isDark: Boolean) -> Unit,
) {
    val isDark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colors = previewColors(settings.themeColor, isDark, settings.randomRainbowColors)
    val scheme = if (isDark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = Color(0xFF281218),
            background = colors.background,
            onBackground = Color(0xFFF8F0F2),
            surface = colors.surface,
            onSurface = Color(0xFFF8F0F2),
            surfaceVariant = colors.surface,
            onSurfaceVariant = Color(0xFFD8C8CC),
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            background = colors.background,
            onBackground = Color(0xFF23191C),
            surface = colors.surface,
            onSurface = Color(0xFF23191C),
            surfaceVariant = colors.surface,
            onSurfaceVariant = Color(0xFF64575A),
        )
    }

    MaterialTheme(colorScheme = scheme) {
        content(isDark)
    }
}
