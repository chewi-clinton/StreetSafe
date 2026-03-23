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
import kotlinx.coroutines.flow.map

private val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

@Composable
fun SafeSenseNavGraph(
    dataStore: DataStore<Preferences>,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // Collect the onboarding status. We default to 'false' (showing onboarding) 
    // while the DataStore is still loading to ensure the user sees the setup screen 
    // immediately rather than a blank white page.
    val onboardingComplete by dataStore.data
        .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
        .collectAsState(initial = false)

    val startDestination = if (onboardingComplete) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            PlaceholderScreen(name = "Home")
        }

        composable(route = Screen.Countdown.route) {
            PlaceholderScreen(name = "Countdown")
        }

        composable(route = Screen.WalkMode.route) {
            PlaceholderScreen(name = "Walk Mode")
        }

        composable(route = Screen.Contacts.route) {
            PlaceholderScreen(name = "Contacts")
        }

        composable(route = Screen.AddEditContact.route) {
            PlaceholderScreen(name = "Add / Edit Contact")
        }

        composable(route = Screen.IncidentHistory.route) {
            PlaceholderScreen(name = "Incident History")
        }

        composable(route = Screen.IncidentDetail.route) {
            PlaceholderScreen(name = "Incident Detail")
        }

        composable(route = Screen.Settings.route) {
            PlaceholderScreen(name = "Settings")
        }

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
