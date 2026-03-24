package com.example.safesense.ui.countdown

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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.Image

private val PrimaryRed = Color(0xFFD32F2F)
private val DeepRed = Color(0xFFB71C1C)
private val White = Color(0xFFFFFFFF)

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

    LaunchedEffect(Unit) {
        viewModel.startCountdown(incidentType, confidence)
    }

    LaunchedEffect(state) {
        when (state) {
            is CountdownState.CancelledByUser -> {
                viewModel.reset()
                onCancelled()
            }
            is CountdownState.AlertDispatched -> onAlertSent()
            else -> Unit
        }
    }

    Scaffold(containerColor = PrimaryRed) { innerPadding ->
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

            is CountdownState.AlertDispatched -> {
                AlertDispatchedContent(
                    state = current,
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
private fun AlertDispatchedContent(
    state: CountdownState.AlertDispatched,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Alert Sent",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${state.contactsNotified} contact${if (state.contactsNotified != 1) "s" else ""} notified",
            fontSize = 18.sp,
            color = White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
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
        IncidentType.FALL -> "FALL DETECTED"
        IncidentType.COLLISION -> "COLLISION DETECTED"
        IncidentType.SHAKE -> "SHAKE DETECTED"
        IncidentType.MANUAL -> "MANUAL ALERT"
        IncidentType.SOUND -> "SOUND DETECTED"
    }

    Box(
        modifier = Modifier
            .background(color = DeepRed, shape = RoundedCornerShape(50.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun SendingToList(contacts: List<EmergencyContact>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SENDING TO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (contacts.isEmpty()) {
            ContactPill(name = "No contacts set", phone = "")
        } else {
            contacts.forEach { contact ->
                ContactPill(name = contact.name, phone = contact.phoneNumber)
            }
        }
    }
}

@Composable
private fun ContactPill(name: String, phone: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = DeepRed, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = White, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = if (phone.isNotEmpty()) "$name · $phone" else name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = White
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
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = White,
            contentColor = PrimaryRed
        )
    ) {
        Text(
            text = "I'm OK — Cancel Alert",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryRed
        )
    }
}