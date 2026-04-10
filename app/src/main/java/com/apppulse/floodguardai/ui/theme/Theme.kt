package com.apppulse.floodguardai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────
val Navy900     = Color(0xFF0A0E1A)
val Navy800     = Color(0xFF121929)
val Navy700     = Color(0xFF1C2740)
val Navy600     = Color(0xFF243352)

val Cyan400     = Color(0xFF00BCD4)
val Cyan200     = Color(0xFF80DEEA)
val Teal400     = Color(0xFF4DB6AC)
val Teal200     = Color(0xFFA7FFEB)

val RiskRed     = Color(0xFFEF5350)
val RiskAmber   = Color(0xFFFFB300)
val RiskGreen   = Color(0xFF66BB6A)

val OnDark      = Color(0xFFE3EAF4)
val OnDarkMuted = Color(0xFF8A9BB8)

// ── Color scheme ─────────────────────────────────────────────────────────────
private val FloodDarkColors = darkColorScheme(
    primary             = Cyan400,
    onPrimary           = Navy900,
    primaryContainer    = Navy700,
    onPrimaryContainer  = Cyan200,

    secondary           = Teal400,
    onSecondary         = Navy900,
    secondaryContainer  = Navy700,
    onSecondaryContainer = Teal200,

    background          = Navy900,
    onBackground        = OnDark,

    surface             = Navy800,
    onSurface           = OnDark,

    surfaceVariant      = Navy700,
    onSurfaceVariant    = OnDarkMuted,

    outline             = Navy600,
    error               = RiskRed,
    onError             = OnDark,
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun FloodGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FloodDarkColors,
        content = content
    )
}
