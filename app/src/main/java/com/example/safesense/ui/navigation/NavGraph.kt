package com.example.safesense.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.data.preferences.UserPreferencesRepository
import com.example.safesense.ui.countdown.CountdownScreen
import com.example.safesense.ui.history.IncidentHistoryScreen
import com.example.safesense.ui.history.IncidentDetailScreen
import com.example.safesense.ui.contacts.AddEditContactScreen
import com.example.safesense.ui.contacts.ContactsScreen
import com.example.safesense.ui.home.HomeScreen
import com.example.safesense.ui.onboarding.OnboardingScreen
import com.example.safesense.ui.settings.SettingsScreen
import com.example.safesense.ui.splash.SplashScreen
import com.example.safesense.ui.walkmode.WalkModeScreen
import com.example.safesense.ui.walkmode.WalkModeViewModel

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
fun SafeSenseNavGraph(
    userPreferencesRepository: UserPreferencesRepository,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val onboardingComplete by userPreferencesRepository.isOnboardingComplete
        .collectAsState(initial = false)

    val navigateToHome = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    val navigateToHistory = {
        navController.navigate(Screen.IncidentHistory.route) {
            popUpTo(Screen.Home.route)
        }
    }

    val navigateToContacts = {
        navController.navigate(Screen.Contacts.route) {
            popUpTo(Screen.Home.route)
        }
    }

    val navigateToSettings = {
        navController.navigate(Screen.Settings.route) {
            popUpTo(Screen.Home.route)
        }
    }

    val navigateToCountdown = { type: IncidentType, confidence: ConfidenceLevel ->
        navController.navigate(
            Screen.Countdown.route
                .replace("{incidentType}", type.name)
                .replace("{confidence}", confidence.name)
        )
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {

        composable(route = Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val destination = if (onboardingComplete) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

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
            HomeScreen(
                onNavigateToHistory = navigateToHistory,
                onNavigateToContacts = navigateToContacts,
                onNavigateToSettings = navigateToSettings,
                onNavigateToWalkMode = { navController.navigate(Screen.WalkMode.route) },
                onPanicButtonPressed = { navigateToCountdown(IncidentType.MANUAL, ConfidenceLevel.HIGH) }
            )
        }

        composable(
            route = Screen.Countdown.route,
            arguments = listOf(
                navArgument("incidentType") { type = NavType.StringType },
                navArgument("confidence") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val incidentType = backStackEntry.arguments?.getString("incidentType")
                ?.let { runCatching { IncidentType.valueOf(it) }.getOrDefault(IncidentType.MANUAL) }
                ?: IncidentType.MANUAL
            val confidence = backStackEntry.arguments?.getString("confidence")
                ?.let { runCatching { ConfidenceLevel.valueOf(it) }.getOrDefault(ConfidenceLevel.HIGH) }
                ?: ConfidenceLevel.HIGH

            CountdownScreen(
                incidentType = incidentType,
                confidence = confidence,
                onCancelled = { navController.popBackStack() },
                onAlertSent = { navController.popBackStack() }
            )
        }

        composable(route = Screen.WalkMode.route) {
            val viewModel: WalkModeViewModel = hiltViewModel()
            WalkModeScreen(
                onBack = { navController.popBackStack() },
                onTriggerCountdown = { type, confidence ->
                    navigateToCountdown(type, confidence)
                },
                viewModel = viewModel
            )
        }

        composable(route = Screen.Contacts.route) {
            ContactsScreen(
                onNavigateToHome = navigateToHome,
                onNavigateToHistory = navigateToHistory,
                onNavigateToSettings = navigateToSettings,
                onNavigateToManualAdd = { navController.navigate("add_contact") },
                onNavigateToEdit = { id -> navController.navigate("edit_contact/$id") }
            )
        }

        composable("add_contact") {
            AddEditContactScreen(
                contactId = null,
                onSaveComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("edit_contact/{contactId}") { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")?.toIntOrNull()
            AddEditContactScreen(
                contactId = contactId,
                onSaveComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.IncidentHistory.route) {
            IncidentHistoryScreen(
                onNavigateToHome = navigateToHome,
                onNavigateToContacts = navigateToContacts,
                onNavigateToSettings = navigateToSettings,
                onIncidentClick = { id ->
                    navController.navigate(Screen.IncidentDetail.route + "/$id")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IncidentDetail.route + "/{incidentId}",
            arguments = listOf(navArgument("incidentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getLong("incidentId") ?: 0L
            IncidentDetailScreen(
                incidentId = incidentId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateToHome = navigateToHome,
                onNavigateToContacts = navigateToContacts,
                onNavigateToHistory = navigateToHistory,
                onNavigateToCountdown = { navigateToCountdown(IncidentType.MANUAL, ConfidenceLevel.HIGH) }
            )
        }

        composable(route = Screen.WhitelistInstructions.route) {
            PlaceholderScreen(name = "Whitelist Instructions")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
