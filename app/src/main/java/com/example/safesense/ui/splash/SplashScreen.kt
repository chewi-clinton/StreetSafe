package com.example.safesense.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.safesense.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// SplashScreen.kt
// Location: ui/splash/SplashScreen.kt
//
// PURPOSE:
//   Displays a red background with the SafeSense logo for 3 seconds.
//   After 3 seconds the logo scales up and fades out, then onOnboardingReady()
//   is called which NavGraph uses to navigate to the real Onboarding screen.
//
// ANIMATION BREAKDOWN:
//   0ms   → 2000ms  : Logo sits still at scale 1.0, alpha 1.0 (visible)
//   2000ms → 3000ms : Logo scales from 1.0 → 1.4 AND fades from 1.0 → 0.0
//   3000ms           : Navigate away
//
// NOTE ON THE RED COLOUR:
//   Change the hex value in Color(0xFFCC0000) to match your exact brand red.
//   0xFFCC0000 is a strong red. If you have a hex code from your design,
//   replace CC0000 with your value. The FF prefix means fully opaque.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    // Called by NavGraph when the 3-second splash is done.
    // NavGraph will then navigate to Onboarding.
    onSplashComplete: () -> Unit
) {
    // ── Animation values ──────────────────────────────────────────────────────
    // Animatable lets us drive scale and alpha independently with precise timing.
    // We start at scale 1.0 (normal size) and alpha 1.0 (fully visible).
    val scale = remember { Animatable(initialValue = 1f) }
    val alpha = remember { Animatable(initialValue = 1f) }

    // LaunchedEffect runs once when the composable first appears.
    // The key `true` means it never restarts — it runs exactly once.
    LaunchedEffect(key1 = true) {
        // Hold the logo still for 2 seconds so the user can read it.
        delay(2000L)

        // Scale and fade run simultaneously using separate coroutine launches.
        // We use kotlinx.coroutines launch to run them in parallel — if we
        // called them sequentially the fade would only start after scale finished.
        coroutineScope {
            // Scale up: 1.0 → 1.4 over 800ms with an ease-in curve
            launch {
                scale.animateTo(
                    targetValue = 1.4f,
                    animationSpec = tween(durationMillis = 800)
                )
            }
            // Fade out: 1.0 → 0.0 over 800ms
            launch {
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 800)
                )
            }
        }

        // Both animations are done — tell NavGraph to navigate away.
        onSplashComplete()
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Replace 0xFFCC0000 with your exact brand red hex code.
            // Format: 0xFF followed by your 6-digit hex (e.g. 0xFFE63946)
            .background(Color(0xFFB71C1C)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.safesense_logo),
            contentDescription = "SafeSense Logo",
            modifier = Modifier
                .size(200.dp)           // Adjust size to fit your logo
                .scale(scale.value)     // Drives the zoom-out animation
                .alpha(alpha.value)     // Drives the fade-out animation
        )
    }
}
