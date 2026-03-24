package com.example.safesense.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.safesense.ui.onboarding.OnboardingScreen
import com.example.safesense.ui.splash.SplashScreen
import kotlinx.coroutines.flow.map

// ─────────────────────────────────────────────────────────────────────────────
// NavGraph.kt
// Location: ui/navigation/NavGraph.kt
//
// UPDATED: Restored the custom SplashScreen to provide the full-screen red
// experience the user preferred. Every launch starts at the Splash screen.
// ─────────────────────────────────────────────────────────────────────────────

private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

@Composable
fun SafeSenseNavGraph(
    dataStore: DataStore<Preferences>,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // Read onboarding flag — used to decide where to go AFTER splash.
    val onboardingComplete by dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
        .collectAsState(initial = false)

    NavHost(
        navController = navController,
        // Start at Splash to show the full red background brand experience.
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {

        // ── SPLASH ────────────────────────────────────────────────────────────
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val destination = if (onboardingComplete) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── ONBOARDING ────────────────────────────────────────────────────────
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // ── HOME ──────────────────────────────────────────────────────────────
        composable(route = Screen.Home.route) {
            PlaceholderScreen(name = "Home")
        }

        // ── COUNTDOWN ─────────────────────────────────────────────────────────
        composable(route = Screen.Countdown.route) {
            PlaceholderScreen(name = "Countdown")
        }

        // ── WALK MODE ─────────────────────────────────────────────────────────
        composable(route = Screen.WalkMode.route) {
            PlaceholderScreen(name = "Walk Mode")
        }

        // ── CONTACTS ──────────────────────────────────────────────────────────
        composable(route = Screen.Contacts.route) {
            PlaceholderScreen(name = "Contacts")
        }

        // ── ADD / EDIT CONTACT ────────────────────────────────────────────────
        composable(route = Screen.AddEditContact.route) {
            PlaceholderScreen(name = "Add / Edit Contact")
        }

        // ── INCIDENT HISTORY ──────────────────────────────────────────────────
        composable(route = Screen.IncidentHistory.route) {
            PlaceholderScreen(name = "Incident History")
        }

        // ── INCIDENT DETAIL ───────────────────────────────────────────────────
        composable(route = Screen.IncidentDetail.route) {
            PlaceholderScreen(name = "Incident Detail")
        }

        // ── SETTINGS ──────────────────────────────────────────────────────────
        composable(route = Screen.Settings.route) {
            PlaceholderScreen(name = "Settings")
        }

        // ── WHITELIST INSTRUCTIONS ────────────────────────────────────────────
        composable(route = Screen.WhitelistInstructions.route) {
            PlaceholderScreen(name = "Whitelist Instructions")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name Screen",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
