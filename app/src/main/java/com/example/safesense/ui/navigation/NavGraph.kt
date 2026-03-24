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
// WHAT CHANGED FROM THE PREVIOUS VERSION:
//   1. startDestination is now always Screen.Splash.route — every launch
//      begins at the splash screen, no exceptions.
//   2. Added the splash composable. When its 3-second animation finishes,
//      it reads the DataStore flag and navigates to either Onboarding or Home.
//   3. The DataStore read moved INSIDE the splash composable's onSplashComplete
//      lambda rather than controlling the start destination. This is cleaner
//      because the splash screen always shows regardless of onboarding state.
//
// NAVIGATION FLOW:
//   Every launch  → Splash (3s)
//                      ↓
//              onboarding_complete?
//              YES → Home
//              NO  → Onboarding → (on finish) → Home
// ─────────────────────────────────────────────────────────────────────────────

private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

@Composable
fun SafeSenseNavGraph(
    dataStore: DataStore<Preferences>,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // Read onboarding flag — default false while loading.
    // This is used inside the splash onSplashComplete lambda below.
    val onboardingComplete by dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
        .collectAsState(initial = false)

    NavHost(
        navController = navController,
        // Splash is ALWAYS the start destination — every single launch.
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {

        // ── SPLASH ────────────────────────────────────────────────────────────
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    // Decide where to go after the splash animation finishes.
                    val destination = if (onboardingComplete) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    navController.navigate(destination) {
                        // Remove Splash from the back stack so pressing Back
                        // from Onboarding or Home does not return to the splash.
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