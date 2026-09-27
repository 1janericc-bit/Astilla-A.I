package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AstillaDarkColorScheme = darkColorScheme(
    primary = AstillaCyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = AstillaCyanLight,
    secondary = AstillaVioletSecondary,
    onSecondary = Color(0xFF2E004D),
    secondaryContainer = AstillaVioletDark,
    onSecondaryContainer = AstillaVioletLight,
    tertiary = AstillaAmberTertiary,
    onTertiary = Color(0xFF452B00),
    background = AstillaDarkBackground,
    onBackground = AstillaDarkTextPrimary,
    surface = AstillaDarkSurface,
    onSurface = AstillaDarkTextPrimary,
    surfaceVariant = AstillaDarkSurfaceVariant,
    onSurfaceVariant = AstillaDarkTextSecondary,
    outline = CardBorderDark
)

private val AstillaLightColorScheme = lightColorScheme(
    primary = AstillaLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = AstillaLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = AstillaLightTertiary,
    onTertiary = Color.White,
    background = AstillaLightBackground,
    onBackground = AstillaLightTextPrimary,
    surface = AstillaLightSurface,
    onSurface = AstillaLightTextPrimary,
    surfaceVariant = AstillaLightSurfaceVariant,
    onSurfaceVariant = AstillaLightTextSecondary,
    outline = CardBorderLight
)

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

@Composable
fun AstillaAITheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AstillaDarkColorScheme
        else -> AstillaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
