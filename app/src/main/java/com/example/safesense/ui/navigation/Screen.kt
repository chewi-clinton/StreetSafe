package com.example.safesense.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Countdown : Screen("countdown/{incidentType}/{confidence}")
    object WalkMode : Screen("walk_mode")
    object Contacts : Screen("contacts")
    object AddContact : Screen("add_contact")
    object IncidentHistory : Screen("incident_history")
    object IncidentDetail : Screen("incident_detail")
    object Settings : Screen("settings")
    object WhitelistInstructions : Screen("whitelist_instructions")
}