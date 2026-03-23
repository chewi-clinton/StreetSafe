package com.example.safesense.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Each screen is a placeholder that just shows its name.
// This lets you verify ALL navigation routes work before writing a single real screen.
// Replace each placeholder one at a time in Block 3+.

@Composable
fun SafeSenseNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            Text(text = "Onboarding Screen")
        }
        composable(Screen.Home.route) {
            Text(text = "Home Screen")
        }
        composable(Screen.Countdown.route) {
            Text(text = "Countdown Screen")
        }
        composable(Screen.WalkMode.route) {
            Text(text = "Walk Mode Screen")
        }
        composable(Screen.Contacts.route) {
            Text(text = "Contacts Screen")
        }
        composable(Screen.AddEditContact.route) {
            Text(text = "Add / Edit Contact Screen")
        }
        composable(Screen.IncidentHistory.route) {
            Text(text = "Incident History Screen")
        }
        composable(Screen.IncidentDetail.route) {
            Text(text = "Incident Detail Screen")
        }
        composable(Screen.Settings.route) {
            Text(text = "Settings Screen")
        }
        composable(Screen.WhitelistInstructions.route) {
            Text(text = "Whitelist Instructions Screen")
        }
    }
}