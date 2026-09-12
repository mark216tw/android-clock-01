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
    customHue: Float = com.simpleclock.app.data.DEFAULT_SYSTEM_THEME_HUE,
): ThemePreviewColors {
    if (color == AppThemeColor.CUSTOM) {
        val palette = systemThemePalette(customHue, dark)
        return ThemePreviewColors(Color(palette.primary), Color(palette.background), Color(palette.surface))
    }
    if (dark) {
        return when (color) {
            AppThemeColor.CORAL -> ThemePreviewColors(Color(0xFFFFB2BC), Color(0xFF1E1013), Color(0xFF2A171B))
            AppThemeColor.TANGERINE -> ThemePreviewColors(Color(0xFFFFB68A), Color(0xFF20120A), Color(0xFF2D1A10))
            AppThemeColor.SUNFLOWER -> ThemePreviewColors(Color(0xFFFFD95B), Color(0xFF1C1808), Color(0xFF29230D))
            AppThemeColor.MINT -> ThemePreviewColors(Color(0xFF72DCBD), Color(0xFF091915), Color(0xFF10241E))
            AppThemeColor.SKY -> ThemePreviewColors(Color(0xFF82CFFF), Color(0xFF09171F), Color(0xFF10232E))
            AppThemeColor.GRAPE -> ThemePreviewColors(Color(0xFFD0BCFF), Color(0xFF171020), Color(0xFF22182E))
            AppThemeColor.CUSTOM -> error("Custom colors are returned before this branch")
        }
    }
    return when (color) {
        AppThemeColor.CORAL -> ThemePreviewColors(Color(0xFFC92F48), Color(0xFFFFF7F7), Color(0xFFFFEDEF))
        AppThemeColor.TANGERINE -> ThemePreviewColors(Color(0xFFA94400), Color(0xFFFFF8F3), Color(0xFFFFEEDC))
        AppThemeColor.SUNFLOWER -> ThemePreviewColors(Color(0xFF765A00), Color(0xFFFFFBEA), Color(0xFFFFF1B8))
        AppThemeColor.MINT -> ThemePreviewColors(Color(0xFF006B50), Color(0xFFF0FCF8), Color(0xFFDDF8EE))
        AppThemeColor.SKY -> ThemePreviewColors(Color(0xFF006493), Color(0xFFF2FAFF), Color(0xFFDCEFFC))
        AppThemeColor.GRAPE -> ThemePreviewColors(Color(0xFF6546B8), Color(0xFFFAF7FF), Color(0xFFEDE5FF))
        AppThemeColor.CUSTOM -> error("Custom colors are returned before this branch")
    }
}

@Composable
fun SimpleClockTheme(
    settings: AppSettings,
    systemThemeHue: Float = settings.systemThemeHue,
    content: @Composable (isDark: Boolean) -> Unit,
) {
    val isDark = when (settings.themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colors = systemThemePalette(systemThemeHue, isDark)
    val scheme = if (isDark) {
        darkColorScheme(
            primary = Color(colors.primary),
            onPrimary = Color(colors.onPrimary),
            primaryContainer = Color(colors.primaryContainer),
            onPrimaryContainer = Color(colors.onPrimaryContainer),
            secondary = Color(colors.secondary),
            onSecondary = Color(colors.onSecondary),
            secondaryContainer = Color(colors.secondaryContainer),
            onSecondaryContainer = Color(colors.onSecondaryContainer),
            tertiary = Color(colors.tertiary),
            onTertiary = Color(colors.onTertiary),
            tertiaryContainer = Color(colors.tertiaryContainer),
            onTertiaryContainer = Color(colors.onTertiaryContainer),
            error = Color(colors.error),
            onError = Color(colors.onError),
            errorContainer = Color(colors.errorContainer),
            onErrorContainer = Color(colors.onErrorContainer),
            background = Color(colors.background),
            onBackground = Color(colors.onBackground),
            surface = Color(colors.surface),
            onSurface = Color(colors.onSurface),
            surfaceVariant = Color(colors.surfaceVariant),
            onSurfaceVariant = Color(colors.onSurfaceVariant),
            surfaceDim = Color(colors.surfaceDim),
            surfaceBright = Color(colors.surfaceBright),
            surfaceContainerLowest = Color(colors.surfaceContainerLowest),
            surfaceContainerLow = Color(colors.surfaceContainerLow),
            surfaceContainer = Color(colors.surfaceContainer),
            surfaceContainerHigh = Color(colors.surfaceContainerHigh),
            surfaceContainerHighest = Color(colors.surfaceContainerHighest),
            outline = Color(colors.outline),
            outlineVariant = Color(colors.outlineVariant),
            inverseSurface = Color(colors.inverseSurface),
            inverseOnSurface = Color(colors.inverseOnSurface),
            inversePrimary = Color(colors.inversePrimary),
            surfaceTint = Color(colors.primary),
        )
    } else {
        lightColorScheme(
            primary = Color(colors.primary),
            onPrimary = Color(colors.onPrimary),
            primaryContainer = Color(colors.primaryContainer),
            onPrimaryContainer = Color(colors.onPrimaryContainer),
            secondary = Color(colors.secondary),
            onSecondary = Color(colors.onSecondary),
            secondaryContainer = Color(colors.secondaryContainer),
            onSecondaryContainer = Color(colors.onSecondaryContainer),
            tertiary = Color(colors.tertiary),
            onTertiary = Color(colors.onTertiary),
            tertiaryContainer = Color(colors.tertiaryContainer),
            onTertiaryContainer = Color(colors.onTertiaryContainer),
            error = Color(colors.error),
            onError = Color(colors.onError),
            errorContainer = Color(colors.errorContainer),
            onErrorContainer = Color(colors.onErrorContainer),
            background = Color(colors.background),
            onBackground = Color(colors.onBackground),
            surface = Color(colors.surface),
            onSurface = Color(colors.onSurface),
            surfaceVariant = Color(colors.surfaceVariant),
            onSurfaceVariant = Color(colors.onSurfaceVariant),
            surfaceDim = Color(colors.surfaceDim),
            surfaceBright = Color(colors.surfaceBright),
            surfaceContainerLowest = Color(colors.surfaceContainerLowest),
            surfaceContainerLow = Color(colors.surfaceContainerLow),
            surfaceContainer = Color(colors.surfaceContainer),
            surfaceContainerHigh = Color(colors.surfaceContainerHigh),
            surfaceContainerHighest = Color(colors.surfaceContainerHighest),
            outline = Color(colors.outline),
            outlineVariant = Color(colors.outlineVariant),
            inverseSurface = Color(colors.inverseSurface),
            inverseOnSurface = Color(colors.inverseOnSurface),
            inversePrimary = Color(colors.inversePrimary),
            surfaceTint = Color(colors.primary),
        )
    }

    MaterialTheme(colorScheme = scheme) {
        content(isDark)
    }
}
