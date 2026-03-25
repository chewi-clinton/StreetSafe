package com.example.safesense.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private val PrimaryRed = Color(0xFFD32F2F)
private val White = Color(0xFFFFFFFF)
private val Gray100 = Color(0xFFF5F5F5)
private val Gray200 = Color(0xFFEEEEEE)
private val Gray400 = Color(0xFFBDBDBD)
private val Gray600 = Color(0xFF757575)
private val Gray900 = Color(0xFF212121)

@Composable
fun SettingsScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCountdown: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPermDeniedDialog by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onMicrophoneDetectionChange(true)
        } else {
            viewModel.onMicrophoneDetectionChange(false)
            val canShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                context as ComponentActivity,
                Manifest.permission.RECORD_AUDIO
            )
            if (canShowRationale) {
                scope.launch {
                    snackbarHostState.showSnackbar("Microphone permission is required for audio detection.")
                }
            } else {
                showPermDeniedDialog = true
            }
        }
    }

    fun onMicToggleChanged(enabled: Boolean) {
        if (!enabled) {
            viewModel.onMicrophoneDetectionChange(false)
            return
        }
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.onMicrophoneDetectionChange(true)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showPermDeniedDialog) {
        MicrophonePermissionDeniedDialog(
            onDismiss = { showPermDeniedDialog = false },
            onOpenSettings = {
                showPermDeniedDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    Scaffold(
        containerColor = White,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Gray900,
                    contentColor = White
                )
            }
        },
        bottomBar = {
            SettingsBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            SettingsHeader()
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(title = "DETECTION")
            Spacer(modifier = Modifier.height(8.dp))
            SensitivitySliderRow(
                sensitivity = uiState.sensitivity,
                onSensitivityChange = viewModel::onSensitivityChange
            )
            SettingsDivider()
            SwitchSettingRow(
                title = "Shake-to-Alert",
                subtitle = "3 rapid shakes triggers alert",
                checked = uiState.shakeToAlertEnabled,
                onCheckedChange = viewModel::onShakeToAlertChange
            )
            SettingsDivider()
            SwitchSettingRow(
                title = "Microphone detection",
                subtitle = "Detects loud distress sounds",
                checked = uiState.microphoneDetectionEnabled,
                onCheckedChange = ::onMicToggleChanged
            )
            SettingsDivider()
            CountdownDurationRow(
                durationLabel = uiState.countdownDurationLabel,
                onClick = viewModel::onCountdownDurationClick
            )
            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel(title = "APP")
            Spacer(modifier = Modifier.height(8.dp))
            LanguageSelectionRow(
                selectedLanguage = uiState.selectedLanguage,
                onLanguageSelect = viewModel::onLanguageSelect
            )
            SettingsDivider()
            SwitchSettingRow(
                title = "Auto-start on reboot",
                subtitle = "Resume monitoring after restart",
                checked = uiState.autoStartOnReboot,
                onCheckedChange = viewModel::onAutoStartOnRebootChange
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun vibrateDevice(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 300), -1)
        )
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 300), -1)
        )
    }
}

@Composable
private fun MicrophonePermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                text = "Microphone permission denied",
                fontWeight = FontWeight.Bold,
                color = Gray900,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = "Microphone permission was permanently denied. To enable audio detection, open App Settings and grant the Microphone permission manually.",
                color = Gray600,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = "Open Settings", color = PrimaryRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Gray400)
            }
        }
    )
}

@Composable
private fun SettingsHeader() {
    Text(text = "Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
}

@Composable
private fun SectionLabel(title: String) {
    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryRed, letterSpacing = 1.2.sp)
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = Gray200)
}

@Composable
private fun SensitivitySliderRow(sensitivity: Float, onSensitivityChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(text = "Sensitivity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gray900)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "Higher = more sensitive, more alerts", fontSize = 13.sp, color = Gray600)
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sensitivity,
            onValueChange = onSensitivityChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = PrimaryRed,
                activeTrackColor = PrimaryRed,
                inactiveTrackColor = Gray100
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Low", fontSize = 11.sp, color = Gray400)
            Text(text = "Medium", fontSize = 11.sp, color = Gray400)
            Text(text = "High", fontSize = 11.sp, color = Gray400)
        }
    }
}

@Composable
private fun SwitchSettingRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 13.sp, color = Gray600)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = PrimaryRed,
                uncheckedThumbColor = Gray100,
                uncheckedTrackColor = Gray200,
                uncheckedBorderColor = Gray200
            )
        )
    }
}

@Composable
private fun CountdownDurationRow(durationLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Countdown duration", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Time before alert is sent", fontSize = 13.sp, color = Gray600)
        }
        Box(
            modifier = Modifier.clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = durationLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryRed)
        }
    }
}

@Composable
private fun LanguageSelectionRow(selectedLanguage: String, onLanguageSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Language", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "SMS alerts use selected language", fontSize = 13.sp, color = Gray600)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            LanguageButton(label = "FR", isActive = selectedLanguage == "FR", isFirst = true, isLast = false, onClick = { onLanguageSelect("FR") })
            LanguageButton(label = "EN", isActive = selectedLanguage == "EN", isFirst = false, isLast = true, onClick = { onLanguageSelect("EN") })
        }
    }
}

@Composable
private fun LanguageButton(label: String, isActive: Boolean, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val shape = when {
        isFirst -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp)
        isLast -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
        else -> RoundedCornerShape(0.dp)
    }
    if (isActive) {
        Box(
            modifier = Modifier.background(color = PrimaryRed, shape = shape).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = shape,
            border = BorderStroke(1.dp, Gray200),
            modifier = Modifier.padding(0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Gray900)
        }
    }
}

private data class BottomNavItem(val label: String, val icon: ImageVector, val isActive: Boolean)

@Composable
private fun SettingsBottomNavigationBar(
    onNavigateToHome: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Filled.Home, false),
        BottomNavItem("Contacts", Icons.Filled.People, false),
        BottomNavItem("History", Icons.Filled.History, false),
        BottomNavItem("Settings", Icons.Filled.Settings, true)
    )
    val actions = listOf(onNavigateToHome, onNavigateToContacts, onNavigateToHistory, {})

    NavigationBar(containerColor = White, tonalElevation = 0.dp) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = item.isActive,
                onClick = actions[index],
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(text = item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryRed,
                    selectedTextColor = PrimaryRed,
                    unselectedIconColor = Gray400,
                    unselectedTextColor = Gray400,
                    indicatorColor = White
                )
            )
        }
    }
}
