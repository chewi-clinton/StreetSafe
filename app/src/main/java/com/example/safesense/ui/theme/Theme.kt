package com.safesense.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Color Scheme ─────────────────────────────────────────────────────────────
// We define only ONE scheme (light). SafeSense does not use a dark theme —
// the UI is built on red + greyscale and every screen specifies its own bg.
private val SafeSenseColorScheme = lightColorScheme(
    primary          = PrimaryRed,
    onPrimary        = White,
    primaryContainer = RedLight,
    onPrimaryContainer = DeepRed,

    secondary        = Gray600,
    onSecondary      = White,
    secondaryContainer = Gray100,
    onSecondaryContainer = Gray900,

    background       = OffWhite,
    onBackground     = Gray900,

    surface          = White,
    onSurface        = Gray900,
    surfaceVariant   = Gray100,
    onSurfaceVariant = Gray600,

    outline          = Gray200,
    outlineVariant   = RedSoft,

    error            = PrimaryRed,
    onError          = White,
)

// ─── Theme Entry Point ────────────────────────────────────────────────────────
@Composable
fun SafeSenseTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SafeSenseColorScheme
    val view = LocalView.current

    // This makes the status bar color match the top of your screen.
    // SideEffect runs after every recomposition — it keeps the bar in sync.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false // white icons on red bar
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = SafeSenseTypography,
        content     = content
    )
}