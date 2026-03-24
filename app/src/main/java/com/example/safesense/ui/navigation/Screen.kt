package com.example.safesense.ui.navigation

// ─────────────────────────────────────────────────────────────────────────────
// Screen.kt
// Location: ui/navigation/Screen.kt
//
// PURPOSE:
//   A sealed class that holds every route string in one place.
//   This means route names are never typed as raw strings anywhere else in
//   the app. If you ever rename a route, you change it here and nowhere else.
//
// HOW TO USE:
//   navController.navigate(Screen.Home.route)
//   navController.navigate(Screen.Splash.route)
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    // New — the red splash screen shown on first open
    object Splash               : Screen("splash")

    // Onboarding — 5-step setup, shown once ever
    object Onboarding           : Screen("onboarding")

    // Core screens
    object Home                 : Screen("home")
    object Countdown            : Screen("countdown")
    object WalkMode             : Screen("walk_mode")

    // Contacts
    object Contacts             : Screen("contacts")
    object AddEditContact       : Screen("add_edit_contact")

    // History
    object IncidentHistory      : Screen("incident_history")
    object IncidentDetail       : Screen("incident_detail")

    // Misc
    object Settings             : Screen("settings")
    object WhitelistInstructions: Screen("whitelist_instructions")
}