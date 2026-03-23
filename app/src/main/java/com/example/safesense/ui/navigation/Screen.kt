package com.example.safesense.ui.navigation

// A sealed class is a closed set — the compiler knows every possible Screen.
// This means if you forget to handle a route, the app won't compile.
// That is exactly what you want for navigation — no typos, no missing routes.

sealed class Screen(val route: String) {
    object Onboarding            : Screen("onboarding")
    object Home                  : Screen("home")
    object Countdown             : Screen("countdown")
    object WalkMode              : Screen("walk_mode")
    object Contacts              : Screen("contacts")
    object AddEditContact        : Screen("add_edit_contact")
    object IncidentHistory       : Screen("incident_history")
    object IncidentDetail        : Screen("incident_detail")
    object Settings              : Screen("settings")
    object WhitelistInstructions : Screen("whitelist_instructions")
}