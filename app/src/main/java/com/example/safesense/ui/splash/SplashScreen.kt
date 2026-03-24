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
//   Displays a full red background with the SafeSense logo for 3 seconds.
//   After 3 seconds the logo scales up and fades out, then navigates away.
//
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(
    // Called by NavGraph when the 3-second splash is done.
    onSplashComplete: () -> Unit
) {
    val scale = remember { Animatable(initialValue = 1f) }
    val alpha = remember { Animatable(initialValue = 1f) }

    LaunchedEffect(key1 = true) {
        // Hold the logo still for 2 seconds.
        delay(2000L)

        coroutineScope {
            // Scale up: 1.0 → 1.4 over 800ms
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

        // Animation done
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Restored the exact CC0000 red for full-screen background
            .background(Color(0xFFCC0000)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.safesense_logo),
            contentDescription = "SafeSense Logo",
            modifier = Modifier
                .size(200.dp)           
                .scale(scale.value)     
                .alpha(alpha.value)
        )
    }
}
