package com.example.safesense.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safesense.R

// ── Colours imported directly from your theme file ────────────────────────────
import com.example.safesense.ui.theme.DeepRed
import com.example.safesense.ui.theme.Gray100
import com.example.safesense.ui.theme.Gray200
import com.example.safesense.ui.theme.Gray400
import com.example.safesense.ui.theme.Gray600
import com.example.safesense.ui.theme.Gray900
import com.example.safesense.ui.theme.NearBlack
import com.example.safesense.ui.theme.OffWhite
import com.example.safesense.ui.theme.PrimaryRed
import com.example.safesense.ui.theme.SuccessGreen
import com.example.safesense.ui.theme.SuccessLight
import com.example.safesense.ui.theme.White

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onOnboardingComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = OffWhite
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { w -> if (forward) w else -w } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> if (forward) -w else w } + fadeOut())
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                        onRunCheck = { viewModel.runSensorCheck() }
                    )
                }
            }

            BottomNavigationBar(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                canAdvance = canAdvanceFromStep(state),
                onBack = { viewModel.goToPreviousStep() },
                onNext = { viewModel.goToNextStep() }
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isLastStep = currentStep == totalSteps - 1

    Column {
        HorizontalDivider(color = Gray200, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.CenterStart) {
                TextButton(onClick = onBack) {
                    Text(
                        text = if (currentStep == 0) "SKIP" else "BACK",
                        color = Gray600,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    val isActive = index == currentStep
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (isActive) 20.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) NearBlack else Gray400)
                    )
                }
            }

            Box(modifier = Modifier.width(88.dp), contentAlignment = Alignment.CenterEnd) {
                Button(
                    onClick = onNext,
                    enabled = canAdvance,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRed,
                        contentColor = White,
                        disabledContainerColor = Gray400,
                        disabledContentColor = White
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = if (isLastStep) "FINISH" else "NEXT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

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

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.safesense_logo),
            contentDescription = "SafeSense Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Your silent safety net",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SafeSense monitors your phone's sensors in the background. " +
                    "If a fall, collision, or distress situation is detected, it sends " +
                    "an SMS alert with your GPS location to your emergency contacts — " +
                    "automatically, with no internet required.",
            fontSize = 15.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Setup takes approximately 3 minutes. You will only do this once.",
            fontSize = 13.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StepBatteryWhitelist(
    showStep: Boolean,
    confirmed: Boolean,
    onConfirm: () -> Unit
) {
    if (!showStep) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SuccessLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No action required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your device does not require battery optimisation adjustments. " +
                        "SafeSense will run reliably in the background.",
                fontSize = 14.sp,
                color = Gray600,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Battery optimisation",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your device restricts background apps. Without this step, SafeSense " +
                    "will appear active but will not detect emergencies.",
            fontSize = 14.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Gray100)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InstructionRow(number = "1", text = "Open Settings and go to App Management")
            InstructionRow(number = "2", text = "Find and tap SafeSense")
            InstructionRow(number = "3", text = "Tap Battery, then select No Restrictions")
            InstructionRow(number = "4", text = "Return here and confirm below")
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Menu names vary by firmware. Visit dontkillmyapp.com for device-specific screenshots.",
            fontSize = 12.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConfirm,
            enabled = !confirmed,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryRed,
                contentColor = White,
                disabledContainerColor = Gray400,
                disabledContentColor = White
            )
        ) {
            Text(
                text = if (confirmed) "CONFIRMED" else "I HAVE DONE THIS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun InstructionRow(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(PrimaryRed),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Gray600,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StepAddContact(onContactAdded: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var contactSaved by remember { mutableStateOf(false) }

    val phoneIsValid = phone.startsWith("+237") && phone.length >= 12

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryRed,
        focusedLabelColor = PrimaryRed,
        cursorColor = PrimaryRed,
        unfocusedBorderColor = Gray400,
        unfocusedLabelColor = Gray600,
        errorBorderColor = DeepRed,
        errorLabelColor = DeepRed,
        errorSupportingTextColor = DeepRed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Emergency contact",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "This person will receive your SOS message if an incident is detected. " +
                    "You can add up to 5 contacts later in Settings.",
            fontSize = 14.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null, tint = Gray600)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone number (+237...)") },
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Gray600)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = phone.isNotEmpty() && !phoneIsValid,
            supportingText = {
                if (phone.isNotEmpty() && !phoneIsValid) {
                    Text(
                        text = "Must start with +237 \u2014 e.g. +237612345678",
                        color = DeepRed,
                        fontSize = 12.sp
                    )
                }
            },
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = relationship,
            onValueChange = { relationship = it },
            label = { Text("Relationship (e.g. Mother, Brother)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (contactSaved) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Contact saved. You can add more in Settings.",
                    fontSize = 14.sp,
                    color = Gray600
                )
            }
        } else {
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneIsValid) {
                        contactSaved = true
                        onContactAdded()
                    }
                },
                enabled = name.isNotBlank() && phoneIsValid,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed,
                    contentColor = White,
                    disabledContainerColor = Gray400,
                    disabledContentColor = White
                )
            ) {
                Text(
                    text = "SAVE CONTACT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StepSensorCheck(
    sensorsHealthy: Boolean,
    onRunCheck: () -> Unit
) {
    var checking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Sensors,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = PrimaryRed
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sensor verification",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Gray900,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "SafeSense will confirm that your accelerometer and proximity sensor " +
                    "are functioning correctly before activating monitoring.",
            fontSize = 14.sp,
            color = Gray600,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        when {
            sensorsHealthy -> {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SuccessLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All sensors verified",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray900
                )
            }

            checking -> {
                CircularProgressIndicator(
                    color = PrimaryRed,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Verifying sensors...",
                    fontSize = 14.sp,
                    color = Gray600
                )
                
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    onRunCheck()
                    checking = false
                }
            }

            else -> {
                Button(
                    onClick = { checking = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRed,
                        contentColor = White
                    )
                ) {
                    Text(
                        text = "RUN SENSOR CHECK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
