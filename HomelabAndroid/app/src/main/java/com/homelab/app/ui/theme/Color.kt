package com.homelab.app.ui.theme

import androidx.compose.ui.graphics.Color

// Homelab Brand Red Palette
val Red500 = Color(0xFFEF4444)
val Red600 = Color(0xFFDC2626)
val Red700 = Color(0xFFB91C1C)
val Red800 = Color(0xFF991B1B)
val Red900 = Color(0xFF7F1D1D)
val Red950 = Color(0xFF450A0A)

val Red100 = Color(0xFFFEE2E2)
val Red200 = Color(0xFFFECACA)

val StatusGreen: Color
    @androidx.compose.runtime.Composable
    get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF4CAF50)

val StatusRed: Color
    @androidx.compose.runtime.Composable
    get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFE57373) else Color(0xFFF44336)

val StatusOrange: Color
    @androidx.compose.runtime.Composable
    get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFFFB74D) else Color(0xFFFF9800)

val StatusBlue: Color
    @androidx.compose.runtime.Composable
    get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3)

val StatusPurple: Color
    @androidx.compose.runtime.Composable
    get() = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFA78BFA) else Color(0xFF8B5CF6)
