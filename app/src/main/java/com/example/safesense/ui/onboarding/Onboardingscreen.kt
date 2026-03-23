package com.example.safesense.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen.kt
// Location: ui/onboarding/OnboardingScreen.kt
//
// PURPOSE:
//   A 5-step guided setup that every new user completes exactly once.
//   After completion, NavGraph will never route here again.
//
// STEPS:
//   0 → Welcome         (just read, tap Continue)
//   1 → Permissions     (SMS + Location + Notifications)
//   2 → Battery whitelist (only shown on Tecno / Infinix)
//   3 → Add first contact
//   4 → Sensor check
//
// HOW TO USE:
//   In NavGraph.kt, call this composable for the "Onboarding" route.
//   Pass onOnboardingComplete as the lambda that navigates to Home.
//
//   NavGraph reads the DataStore flag on startup. If the flag is true it skips
//   this screen entirely and routes directly to Home.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    // Called by NavGraph when onboarding is fully done.
    // NavGraph will pop this screen and navigate to Home.
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    // Collect the UI state as Compose state.
    // Every time the ViewModel updates uiState, Compose recomposes this screen.
    val state by viewModel.uiState.collectAsState()

    // When isComplete becomes true, tell NavGraph to move to Home.
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onOnboardingComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Progress dots ─────────────────────────────────────────────────
            StepProgressIndicator(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Step content — slides left/right on step change ───────────────
            // AnimatedContent swaps between step composables with a horizontal
            // slide animation so the transition feels intentional, not abrupt.
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    // Slide in from right, slide out to left (forward direction)
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()) togetherWith
                            (slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "OnboardingStepAnimation"
            ) { step ->
                when (step) {
                    0 -> StepWelcome()
                    1 -> StepPermissions(state = state, viewModel = viewModel)
                    2 -> StepBatteryWhitelist(
                        showStep = state.showBatteryWhitelistStep,
                        confirmed = state.batteryWhitelistDone,
                        onConfirm = { viewModel.onBatteryWhitelistConfirmed() }
                    )
                    3 -> StepAddContact(onContactAdded = { viewModel.onContactAdded() })
                    4 -> StepSensorCheck(
                        sensorsHealthy = state.sensorsHealthy,
                        onCheckResult = { healthy -> viewModel.onSensorCheckResult(healthy) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Navigation buttons ────────────────────────────────────────────
            NavigationButtons(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                canAdvance = canAdvanceFromStep(state),
                onBack = { viewModel.goToPreviousStep() },
                onNext = { viewModel.goToNextStep() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Determines whether the Continue button is enabled for each step.
// This is pure logic — no side effects.
// ─────────────────────────────────────────────────────────────────────────────
private fun canAdvanceFromStep(state: OnboardingUiState): Boolean {
    return when (state.currentStep) {
        0 -> true
        1 -> state.smsPermissionGranted && state.locationPermissionGranted
        2 -> !state.showBatteryWhitelistStep || state.batteryWhitelistDone
        3 -> state.hasAtLeastOneContact
        4 -> state.sensorsHealthy
        else -> false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 0 — WELCOME
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to SafeSense",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SafeSense watches over you silently through your phone's sensors.\n\n" +
                    "If a fall, collision, or distress situation is detected, it sends an " +
                    "SMS alert with your GPS location to your emergency contacts — " +
                    "automatically, with no internet required.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "⚡ Setup takes about 3 minutes. You only do this once.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 1 — PERMISSIONS
// We request SMS, Location, and (on API 33+) Notifications.
// Each permission gets its own launcher so we can track them independently.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepPermissions(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    // Launchers for each permission request.
    // rememberLauncherForActivityResult keeps the launcher stable across recompositions.
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onSmsPermissionResult(granted) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        viewModel.onLocationPermissionResult(fineGranted)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onNotificationPermissionResult(granted) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SafeSense needs these to protect you. None of your data leaves your phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // SMS permission row
        PermissionRow(
            title = "Send SMS",
            description = "To send emergency alerts to your contacts without internet",
            isGranted = state.smsPermissionGranted,
            onRequest = { smsLauncher.launch(Manifest.permission.SEND_SMS) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Location permission row
        PermissionRow(
            title = "Location",
            description = "To include your GPS coordinates in the alert SMS",
            isGranted = state.locationPermissionGranted,
            onRequest = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notifications permission — only needed on Android 13 (API 33) and above.
        // On older versions this permission doesn't exist, so we skip it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = "Notifications",
                description = "To alert you if the sensor service is paused by the system",
                isGranted = state.notificationPermissionGranted,
                onRequest = {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }
    }
}

// A single permission row with a status indicator and a request button.
@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Allow")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 2 — BATTERY WHITELIST
// Only shown on Tecno and Infinix devices.
// On all other devices (Samsung, Pixel, etc.) this step is skipped automatically.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepBatteryWhitelist(
    showStep: Boolean,
    confirmed: Boolean,
    onConfirm: () -> Unit
) {
    // If this device doesn't need the whitelist, show a success message and
    // let the user proceed immediately.
    if (!showStep) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your device doesn't need battery whitelist setup.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Tecno / Infinix path — mandatory instructions
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Important: Battery Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your device (Tecno/Infinix) aggressively stops background apps to save " +
                    "battery. This will silently disable SafeSense while showing you a " +
                    "notification that it is running — giving you false confidence.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Steps to fix this:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Open Settings → App Management → SafeSense\n" +
                            "2. Tap Battery → select \"No Restrictions\" or \"Unrestricted\"\n" +
                            "3. Return here and tap the button below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Device settings look different on each firmware version.\n" +
                    "Visit dontkillmyapp.com for device-specific screenshots.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirm,
            enabled = !confirmed,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (confirmed)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (confirmed) "✓ Done — step complete" else "I Have Done This")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 3 — ADD FIRST CONTACT
// A minimal inline contact form — Name, phone number, relationship.
// On save it calls onContactAdded() which updates the ViewModel.
//
// NOTE: In a full implementation this would write directly to Room via
// a ContactRepository. For Block 3 we keep it self-contained so the screen
// compiles. You will wire the repository in Block 4/5.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepAddContact(
    onContactAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var contactSaved by remember { mutableStateOf(false) }

    // Phone validation — must start with +237 (Cameroon) and have 12 digits total
    val phoneIsValid = phone.startsWith("+237") && phone.length >= 12

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ContactPhone,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add Your First Emergency Contact",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "This person will receive your SOS SMS if an incident is detected.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number (+237...)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phone.isNotEmpty() && !phoneIsValid,
            supportingText = {
                if (phone.isNotEmpty() && !phoneIsValid) {
                    Text("Must start with +237 (e.g. +237612345678)")
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = relationship,
            onValueChange = { relationship = it },
            label = { Text("Relationship (e.g. Mother, Brother)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (contactSaved) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "  Contact saved — you can add more in Settings later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        } else {
            Button(
                onClick = {
                    // TODO (Block 5): replace this with a real Room insert via ContactRepository
                    // For now we just mark it done so the step unlocks.
                    if (name.isNotBlank() && phoneIsValid) {
                        contactSaved = true
                        onContactAdded()
                    }
                },
                enabled = name.isNotBlank() && phoneIsValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Contact")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 4 — SENSOR CHECK
// Verifies that the accelerometer and proximity sensor respond.
// In Block 3 this is a placeholder that auto-passes after 2 seconds.
// In a later block you will wire SensorManager to do a real check.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSensorCheck(
    sensorsHealthy: Boolean,
    onCheckResult: (Boolean) -> Unit
) {
    var checking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sensor Check",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SafeSense will verify that your accelerometer and proximity sensor " +
                    "are working correctly.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            sensorsHealthy -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF2E7D32) // green
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All sensors are working correctly.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            checking -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Checking sensors...")

                // Simulate check — replace with real SensorManager check in Block 7
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    checking = false
                    onCheckResult(true)
                }
            }

            else -> {
                Button(
                    onClick = { checking = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run Sensor Check")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS INDICATOR — Row of dots showing which step you're on
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalSteps) { index ->
            val isActive = index == currentStep
            val isPast = index < currentStep

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.primary
                            isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NAVIGATION BUTTONS — Back + Continue/Finish at the bottom of every step
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isLastStep = currentStep == totalSteps - 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button — hidden on the first step
        if (currentStep > 0) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        } else {
            // Empty box to keep Continue button right-aligned on Step 0
            Box(modifier = Modifier.size(0.dp))
        }

        Button(
            onClick = onNext,
            enabled = canAdvance
        ) {
            Text(if (isLastStep) "Finish Setup" else "Continue")
        }
    }
}