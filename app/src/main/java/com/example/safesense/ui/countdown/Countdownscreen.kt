package com.example.safesense.ui.countdown

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesense.R
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.EmergencyContact
import com.example.safesense.domain.model.IncidentType
import kotlinx.coroutines.delay

private val PrimaryRed = Color(0xFFD32F2F)
private val DeepRed = Color(0xFFB71C1C)
private val White = Color(0xFFFFFFFF)
private val ErrorYellow = Color(0xFFFFD600)

@Composable
fun CountdownScreen(
    incidentType: IncidentType,
    confidence: ConfidenceLevel,
    onCancelled: () -> Unit,
    onAlertSent: () -> Unit,
    viewModel: CountdownViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startCountdown(incidentType, confidence)
    }

    LaunchedEffect(state) {
        val currentState = state
        
        // ── VIBRATION TRIGGER ────────────────────────────────────────────────
        // Pulse vibration every second once we reach the final 5 seconds
        if (currentState is CountdownState.CountdownRunning && currentState.secondsRemaining <= 5) {
            vibratePulse(context)
        }

        // ── NAVIGATION TRIGGER ───────────────────────────────────────────────
        when (currentState) {
            is CountdownState.CancelledByUser -> {
                viewModel.reset()
                onCancelled()
            }
            is CountdownState.AlertDispatched -> {
                // Stay on "Alert Sent" screen for 2 seconds so user sees confirmation
                delay(2000)
                viewModel.reset()
                onAlertSent()
            }
            is CountdownState.Error -> {
                // Show error message for 4 seconds then auto-close
                delay(4000)
                viewModel.reset()
                onCancelled()
            }
            is CountdownState.Dispatching -> Unit // waiting — no navigation yet
            else -> Unit
        }
    }

    Scaffold(containerColor = PrimaryRed) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = state) {
                is CountdownState.CountdownRunning -> {
                    CountdownRunningContent(
                        state = current,
                        contacts = contacts,
                        onCancel = viewModel::cancelByUser,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    )
                }

                is CountdownState.Dispatching -> {
                    DispatchingContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    )
                }

                is CountdownState.AlertDispatched -> {
                    AlertDispatchedContent(
                        state = current,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    )
                }

                is CountdownState.Error -> {
                    ErrorContent(
                        message = current.reason,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    )
                }

                else -> Unit
            }
        }
    }
}

/**
 * Helper to trigger a short vibration pulse.
 */
private fun vibratePulse(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
}

@Composable
private fun CountdownRunningContent(
    state: CountdownState.CountdownRunning,
    contacts: List<EmergencyContact>,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        CountdownHeader(incidentType = state.incidentType)

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = state.secondsRemaining.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            lineHeight = 120.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Alert sending in...",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SMS will go to your emergency contacts",
            fontSize = 14.sp,
            color = White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        SendingToList(contacts = contacts)

        Spacer(modifier = Modifier.weight(1f))

        CancelButton(onCancel = onCancel)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Shake your phone to cancel too",
            fontSize = 13.sp,
            color = White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DispatchingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = White,
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sending alert...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Do not close the app",
            fontSize = 14.sp,
            color = White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AlertDispatchedContent(
    state: CountdownState.AlertDispatched,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.safesense_logo),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Alert Sent",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${state.contactsNotified} contact${if (state.contactsNotified != 1) "s" else ""} notified successfully.",
            fontSize = 18.sp,
            color = White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = ErrorYellow,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Alert Failed",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            color = White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Returning to home screen...",
            fontSize = 14.sp,
            color = White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun CountdownHeader(incidentType: IncidentType) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(id = R.drawable.safesense_logo),
            contentDescription = "SafeSense Logo",
            modifier = Modifier.height(28.dp)
        )

        AlertTypeCapsule(incidentType = incidentType)
    }
}

@Composable
private fun AlertTypeCapsule(incidentType: IncidentType) {
    val label = when (incidentType) {
        IncidentType.FALL      -> "FALL DETECTED"
        IncidentType.COLLISION -> "COLLISION DETECTED"
        IncidentType.SHAKE     -> "SHAKE DETECTED"
        IncidentType.MANUAL    -> "MANUAL ALERT"
        IncidentType.SOUND     -> "SOUND DETECTED"
        IncidentType.WALK_MODE -> "WALK MODE"
    }

    Box(
        modifier = Modifier
            .background(White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = White,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SendingToList(contacts: List<EmergencyContact>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SENDING TO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (contacts.isEmpty()) {
                Text(text = "No emergency contacts found", color = White.copy(alpha = 0.5f), fontSize = 14.sp)
            } else {
                contacts.take(3).forEach { contact ->
                    ContactAvatar(name = contact.name)
                }
                if (contacts.size > 3) {
                    ContactAvatar(name = "+${contacts.size - 3}")
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(name: String) {
    val initials = if (name.startsWith("+")) name else name.take(1).uppercase()
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun CancelButton(onCancel: () -> Unit) {
    Button(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepRed,
            contentColor = White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "I'M OK, CANCEL ALERT",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
