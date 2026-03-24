package com.example.safesense.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.safesense.ui.theme.DeepRed
import com.example.safesense.ui.theme.Gray200
import com.example.safesense.ui.theme.Gray400
import com.example.safesense.ui.theme.Gray600
import com.example.safesense.ui.theme.Gray900
import com.example.safesense.ui.theme.PrimaryRed
import com.example.safesense.ui.theme.SuccessGreen
import com.example.safesense.ui.theme.SuccessLight
import com.example.safesense.ui.theme.White

// ─────────────────────────────────────────────────────────────────────────────
// StepPermissions.kt
// Location: ui/onboarding/StepPermissions.kt
//
// HOW PERMISSION STATE MACHINE WORKS:
//
// Each permission cycles through these states:
//
//   IDLE          → Button says "ALLOW". We haven't asked yet.
//                   Tap → fires system dialog.
//
//   ASKED_ONCE    → We fired the dialog and user came back denied.
//                   shouldShowRequestPermissionRationale = true means Android
//                   is willing to show the dialog again.
//                   Button still says "ALLOW". Tap → fires dialog again.
//
//   PERMANENTLY_DENIED → shouldShowRequestPermissionRationale = false AND
//                   we already asked at least once AND still not granted.
//                   Android will never show the dialog again.
//                   Button changes to "OPEN SETTINGS".
//                   Tap → opens the exact permission group page in Settings
//                   using ACTION_APPLICATION_DETAILS_SETTINGS (deepest Android
//                   allows — individual permission toggles live on that page).
//
//   GRANTED       → Green checkmark. Done.
//
// WHY WE CAN'T GO DEEPER THAN APP SETTINGS:
//   Android intentionally blocks direct deep-links into individual permission
//   toggles since API 30 (Android 11). There is no public Intent that opens
//   e.g. "Settings > Apps > SafeSense > Permissions > SMS". The closest we
//   can get is the app's permission list page, which is one tap away from
//   each individual toggle. Attempting undocumented intents will crash on
//   most devices. This is the correct and reliable approach.
// ─────────────────────────────────────────────────────────────────────────────

// Represents the three possible states for a permission that isn't yet granted.
private enum class PermissionButtonState {
    ALLOW,              // Haven't asked, or can ask again
    OPEN_SETTINGS       // Permanently denied — must go to Settings
}

@Composable
fun StepPermissions(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    val context = LocalContext.current

    // ── Per-permission ask-count trackers ─────────────────────────────────────
    // We count how many times we have launched each permission request.
    // askCount > 0 AND still denied AND shouldShowRationale == false
    // → permanently denied → show OPEN SETTINGS button.
    var smsAskCount      by remember { mutableIntStateOf(0) }
    var locationAskCount by remember { mutableIntStateOf(0) }
    var notifAskCount    by remember { mutableIntStateOf(0) }

    // ── Permission launchers ──────────────────────────────────────────────────
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        smsAskCount++
        viewModel.onSmsPermissionResult(granted)
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationAskCount++
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        viewModel.onLocationPermissionResult(granted)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifAskCount++
        viewModel.onNotificationPermissionResult(granted)
    }

    // ── Determine button state for each permission ────────────────────────────
    // We need the Activity to call shouldShowRequestPermissionRationale.
    // LocalContext gives us the Context; we cast to Activity to call it.
    // If the cast fails (shouldn't in a normal Compose Activity setup) we
    // default to ALLOW so the user always has a button to tap.
    val activity = context as? androidx.activity.ComponentActivity

    fun smsButtonState(): PermissionButtonState {
        if (state.smsPermissionGranted) return PermissionButtonState.ALLOW // won't be shown
        if (smsAskCount == 0) return PermissionButtonState.ALLOW
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.SEND_SMS)
        } ?: true
        return if (canAskAgain) PermissionButtonState.ALLOW else PermissionButtonState.OPEN_SETTINGS
    }

    fun locationButtonState(): PermissionButtonState {
        if (state.locationPermissionGranted) return PermissionButtonState.ALLOW
        if (locationAskCount == 0) return PermissionButtonState.ALLOW
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } ?: true
        return if (canAskAgain) PermissionButtonState.ALLOW else PermissionButtonState.OPEN_SETTINGS
    }

    fun notifButtonState(): PermissionButtonState {
        if (state.notificationPermissionGranted) return PermissionButtonState.ALLOW
        if (notifAskCount == 0) return PermissionButtonState.ALLOW
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return PermissionButtonState.ALLOW
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it, Manifest.permission.POST_NOTIFICATIONS
            )
        } ?: true
        return if (canAskAgain) PermissionButtonState.ALLOW else PermissionButtonState.OPEN_SETTINGS
    }

    // Opens the app's Permission list page in System Settings.
    // This is the deepest Android allows since API 30.
    // The user sees the list of all permissions for SafeSense and can tap
    // SMS / Location / Notifications to toggle each one individually.
    fun openAppPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Permissions required",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "SafeSense requires the following permissions. Your data never leaves your device.",
            fontSize = 14.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        // SMS
        PermissionRow(
            title = "Send SMS",
            description = "Delivers emergency alerts to your contacts without internet",
            isGranted = state.smsPermissionGranted,
            buttonState = smsButtonState(),
            onAllow = { smsLauncher.launch(Manifest.permission.SEND_SMS) },
            onOpenSettings = { openAppPermissionSettings() }
        )

        HorizontalDivider(color = Gray200)

        // Location
        PermissionRow(
            title = "Location",
            description = "Attaches your GPS coordinates to the alert message",
            isGranted = state.locationPermissionGranted,
            buttonState = locationButtonState(),
            onAllow = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onOpenSettings = { openAppPermissionSettings() }
        )

        // Notifications — API 33+ only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            HorizontalDivider(color = Gray200)
            PermissionRow(
                title = "Notifications",
                description = "Alerts you if monitoring is interrupted by the system",
                isGranted = state.notificationPermissionGranted,
                buttonState = notifButtonState(),
                onAllow = {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onOpenSettings = { openAppPermissionSettings() }
            )
        }

        // ── Hint shown only when at least one permission is permanently denied ─
        val anyPermanentlyDenied =
            smsButtonState() == PermissionButtonState.OPEN_SETTINGS ||
                    locationButtonState() == PermissionButtonState.OPEN_SETTINGS ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            notifButtonState() == PermissionButtonState.OPEN_SETTINGS)

        if (anyPermanentlyDenied) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Tap OPEN SETTINGS, then tap Permissions to find and enable the blocked permission.",
                fontSize = 12.sp,
                color = Gray600,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PERMISSION ROW — three visual states
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonState: PermissionButtonState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Gray600,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        when {
            // ── GRANTED ───────────────────────────────────────────────────────
            isGranted -> {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Granted",
                        tint = SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── PERMANENTLY DENIED — must go to Settings ──────────────────────
            buttonState == PermissionButtonState.OPEN_SETTINGS -> {
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepRed,
                        contentColor = White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "OPEN SETTINGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // ── NOT YET GRANTED — fire system dialog ──────────────────────────
            else -> {
                Button(
                    onClick = onAllow,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRed,
                        contentColor = White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "ALLOW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}