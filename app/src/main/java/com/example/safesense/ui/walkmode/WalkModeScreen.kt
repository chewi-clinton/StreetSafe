package com.example.safesense.ui.walkmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesense.domain.model.ConfidenceLevel
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.ui.theme.*

@Composable
fun WalkModeScreen(
    onBack: () -> Unit,
    onTriggerCountdown: (IncidentType, ConfidenceLevel) -> Unit,
    viewModel: WalkModeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WalkModeEvent.TriggerEmergencyCountdown -> {
                    onTriggerCountdown(IncidentType.WALK_MODE, ConfidenceLevel.HIGH)
                }
            }
        }
    }

    // When walk mode ends normally, navigate back
    LaunchedEffect(state) {
        if (state is WalkModeState.Ended) onBack()
    }

    when (val current = state) {
        is WalkModeState.Setup -> {
            WalkModeSetupContent(
                state      = current,
                onBack     = onBack,
                onDestinationChange = viewModel::onDestinationChange,
                onDurationChange    = viewModel::onDurationChange,
                onStart    = viewModel::startWalkMode
            )
        }
        is WalkModeState.Active -> {
            WalkModeActiveContent(
                state  = current,
                onBack = onBack,
                onSafe = viewModel::endWalkMode
            )
        }
        is WalkModeState.Ended -> {
            // LaunchedEffect handles navigation — render nothing
        }
    }
}

// ── Setup screen ──────────────────────────────────────────────────────────────

@Composable
private fun WalkModeSetupContent(
    state: WalkModeState.Setup,
    onBack: () -> Unit,
    onDestinationChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    // Duration options per spec: 5 to 120 minutes
    val durationOptions = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Red header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryRed)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Walk Mode",
                    style = SafeSenseTypography.headlineMedium.copy(
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set your destination before you start walking",
                style = SafeSenseTypography.bodyMedium.copy(color = White.copy(alpha = 0.8f))
            )
        }

        // Form body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Destination field
            Column {
                Text(
                    text = "Where are you going?",
                    style = SafeSenseTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.destination,
                    onValueChange = onDestinationChange,
                    placeholder = { Text("e.g. Home, Bureau, Marché Mokolo", color = Gray400) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryRed,
                        unfocusedBorderColor = Gray200,
                        focusedLabelColor    = PrimaryRed
                    )
                )
            }

            // Duration picker
            Column {
                Text(
                    text = "How long will you be walking?",
                    style = SafeSenseTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Horizontal scrollable row of duration chips
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(durationOptions.size) { index ->
                        val minutes = durationOptions[index]
                        val isSelected = minutes == state.durationMinutes
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) PrimaryRed else Gray100)
                                .clickable { onDurationChange(minutes) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${minutes}min",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) White else Gray900
                            )
                        }
                    }
                }
            }

            // Info card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray100, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "If you don't tap \"I'm Safe\" within ${state.durationMinutes} minutes, SafeSense will automatically send an alert to your emergency contacts.",
                    style = SafeSenseTypography.bodySmall.copy(color = Gray600),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onStart,
                enabled = state.destination.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = PrimaryRed,
                    contentColor           = White,
                    disabledContainerColor = Gray200,
                    disabledContentColor   = Gray400
                )
            ) {
                Text(
                    text = "Start Walk Mode",
                    style = SafeSenseTypography.labelLarge.copy(fontSize = 18.sp)
                )
            }
        }
    }
}

// ── Active screen ─────────────────────────────────────────────────────────────

@Composable
private fun WalkModeActiveContent(
    state: WalkModeState.Active,
    onBack: () -> Unit,
    onSafe: () -> Unit
) {
    val totalSeconds = state.durationMinutes * 60
    val progress = if (totalSeconds > 0) state.secondsRemaining.toFloat() / totalSeconds else 0f
    val minutesLeft = state.secondsRemaining / 60
    val secondsLeft = state.secondsRemaining % 60
    val timeLabel = "%d:%02d".format(minutesLeft, secondsLeft)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // Red header with live timer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryRed)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = "Walk Mode",
                    style = SafeSenseTypography.headlineMedium.copy(
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(40.dp)) // Balance back button
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timer card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Time remaining",
                            style = SafeSenseTypography.bodyMedium.copy(
                                color = White.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = timeLabel,
                            style = SafeSenseTypography.displayLarge.copy(
                                color = White,
                                fontSize = 48.sp,
                                lineHeight = 52.sp
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Destination",
                            style = SafeSenseTypography.bodyMedium.copy(
                                color = White.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = state.destination,
                            style = SafeSenseTypography.titleLarge.copy(
                                color = White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }

        // Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Moving status card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SuccessLight, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(SuccessGreen, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Walk Mode active",
                        style = SafeSenseTypography.titleMedium.copy(
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Alert will send if you don't arrive in time",
                        style = SafeSenseTypography.labelSmall.copy(
                            color = SuccessGreen.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Start / Expected row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Gray100, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "Started", style = SafeSenseTypography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.startedAtLabel,
                        style = SafeSenseTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Gray100, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "Expected arrival", style = SafeSenseTypography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.expectedArrivalLabel,
                        style = SafeSenseTypography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // I'm Safe button
            Button(
                onClick = onSafe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text(
                    text = "I'm Safe — End Walk Mode",
                    style = SafeSenseTypography.labelLarge.copy(color = White, fontSize = 18.sp)
                )
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Gray200)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gray600)
            ) {
                Text(
                    text = "Cancel Walk Mode",
                    style = SafeSenseTypography.labelLarge.copy(fontSize = 16.sp)
                )
            }
        }
    }
}
