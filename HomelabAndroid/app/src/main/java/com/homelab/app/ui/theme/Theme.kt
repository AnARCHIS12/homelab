package com.homelab.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF4444),
    onPrimary = Color(0xFF450A0A),
    primaryContainer = Color(0xFF7F1D1D),
    onPrimaryContainer = Color(0xFFFECACA),
    secondary = Color(0xFFF87171),
    onSecondary = Color(0xFF450A0A),
    secondaryContainer = Color(0xFF501818),
    onSecondaryContainer = Color(0xFFFEE2E2),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF431407),
    tertiaryContainer = Color(0xFF7C2D12),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF0F0E0E),
    onBackground = Color(0xFFF5EDED),
    surface = Color(0xFF121111),
    onSurface = Color(0xFFF5EDED),
    surfaceVariant = Color(0xFF241E1E),
    onSurfaceVariant = Color(0xFFD6C3C1),
    surfaceContainerLowest = Color(0xFF0A0909),
    surfaceContainerLow = Color(0xFF161313),
    surfaceContainer = Color(0xFF1D1818),
    surfaceContainerHigh = Color(0xFF262020),
    surfaceContainerHighest = Color(0xFF322A2A),
    outline = Color(0xFF8C7775),
    outlineVariant = Color(0xFF423837)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFDC2626),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEE2E2),
    onPrimaryContainer = Color(0xFF7F1D1D),
    secondary = Color(0xFFB91C1C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = Color(0xFF7F1D1D),
    tertiary = Color(0xFFEA580C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF2F1),
    surfaceContainer = Color(0xFFF8EAE9),
    surfaceContainerHigh = Color(0xFFF2E3E2),
    surfaceContainerHighest = Color(0xFFECDDDC),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF)
)

@Composable
fun HomelabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
