package com.example.safesense.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesense.R
import com.example.safesense.ui.components.SafeSenseBottomNavBar
import com.example.safesense.ui.theme.DeepRed
import com.example.safesense.ui.theme.Gray100
import com.example.safesense.ui.theme.Gray200
import com.example.safesense.ui.theme.Gray400
import com.example.safesense.ui.theme.Gray600
import com.example.safesense.ui.theme.Gray900
import com.example.safesense.ui.theme.PrimaryRed
import com.example.safesense.ui.theme.RedLight
import com.example.safesense.ui.theme.SuccessGreen
import com.example.safesense.ui.theme.White

private val PulseDot          = Color(0xFF69F0AE)
private val WarningLight      = Color(0xFFFFF8E1)
private val WarningBorder     = Color(0xFFF57F17)
private val WarningAmberLocal = Color(0xFFF57F17)

@Composable
fun HomeScreen(
    onNavigateToWalkMode: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Accelerometer dot now comes from SensorForegroundService.accelerometerActive
        // GPS dot now comes from GPSTracker.hasValidFix
        // Only proximity and audio still come from PackageManager/SensorManager here
        viewModel.updateSensorStatus(
            proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null,
            audio     = false
        )
    }

    Scaffold(
        bottomBar = {
            SafeSenseBottomNavBar(
                selectedIndex = 0,
                onItemSelected = { index ->
                    when (index) {
                        1 -> onNavigateToHistory()
                        2 -> onNavigateToContacts()
                        3 -> onNavigateToSettings()
                    }
                }
            )
        },
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryRed)
                    .padding(bottom = 12.dp)
            ) {
                Column {
                    TopHeader()
                    MonitoringStatusCard(
                        isMonitoring = state.isMonitoring,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (state.showFalsePositiveNudge) {
                    FalsePositiveNudgeBanner(onDismiss = { viewModel.dismissFalsePositiveNudge() })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SensorGrid(
                    accelerometerActive = state.accelerometerActive,
                    gpsActive           = state.gpsActive,
                    proximityActive     = state.proximityActive,
                    audioActive         = state.audioActive
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isMonitoring) {
                    OutlinedButton(
                        onClick = { viewModel.stopMonitoring() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryRed)
                    ) {
                        Text("Stop Monitoring", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PrimaryRed)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startMonitoring() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed, contentColor = White)
                    ) {
                        Text("Start Monitoring", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                PanicAlertButton(onClick = { /* Phase 3: launch countdown */ })

                Spacer(modifier = Modifier.height(12.dp))

                ActionCards(
                    recentIncidentCount = state.recentIncidentCount,
                    walkModeActive      = state.walkModeActive,
                    onWalkMode          = onNavigateToWalkMode,
                    onHistory           = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(20.dp))

                RecentActivityList()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TopHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            Image(
                painter = painterResource(id = R.drawable.safesense_logo),
                contentDescription = "SafeSense Logo",
                modifier = Modifier.height(129.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepRed.copy(alpha = 0.55f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Notifications, "Notifications", tint = White, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MonitoringStatusCard(isMonitoring: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val dotColor     = if (isMonitoring) PulseDot else Gray400
    val statusText   = if (isMonitoring) "Monitoring: Active" else "Monitoring: Paused"
    val subtitleText = if (isMonitoring) "Fall · Shake · Snatch detection running" else "Tap Start Monitoring to activate"

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(DeepRed.copy(alpha = 0.6f)).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(14.dp).scale(if (isMonitoring) pulseScale else 1f).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(statusText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White)
            Text(subtitleText, fontSize = 13.sp, color = White.copy(alpha = 0.75f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FalsePositiveNudgeBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(WarningLight).border(1.dp, WarningBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, tint = WarningAmberLocal, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Too many false alerts? Adjust sensitivity in Settings.", fontSize = 13.sp,
            color = Color(0xFF5D4037), modifier = Modifier.weight(1f), lineHeight = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.Filled.Close, "Dismiss", tint = Color(0xFF8D6E63),
            modifier = Modifier.size(18.dp).clickable { onDismiss() })
    }
}

@Composable
private fun SensorGrid(
    accelerometerActive: Boolean, gpsActive: Boolean,
    proximityActive: Boolean, audioActive: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorPill("Accelerometer", accelerometerActive, Modifier.weight(1f))
            SensorPill("GPS",           gpsActive,           Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorPill("Proximity",  proximityActive, Modifier.weight(1f))
            SensorPill("Microphone", audioActive,     Modifier.weight(1f))
        }
    }
}

@Composable
private fun SensorPill(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val dotColor by animateColorAsState(if (active) SuccessGreen else Gray400, label = "sensorDotColor")
    Row(
        modifier = modifier.clip(RoundedCornerShape(50.dp)).background(Gray100)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 14.sp, color = Gray900, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PanicAlertButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C), contentColor = White)
    ) {
        Icon(Icons.Filled.Warning, null, modifier = Modifier.size(24.dp), tint = White)
        Spacer(modifier = Modifier.width(10.dp))
        Text("Panic Alert", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun ActionCards(recentIncidentCount: Int, walkModeActive: Boolean, onWalkMode: () -> Unit, onHistory: () -> Unit) {
    val historySubtitle    = if (recentIncidentCount == 0) "No events today" else "$recentIncidentCount events today"
    val walkModeBg         = if (walkModeActive) RedLight   else Gray100
    val walkModeIconTint   = if (walkModeActive) PrimaryRed else Gray900
    val walkModeTitleColor = if (walkModeActive) DeepRed    else Gray900
    val walkModeSubColor   = if (walkModeActive) PrimaryRed else Gray600
    val walkModeSub        = if (walkModeActive) "Active — destination set" else "Set destination"

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(walkModeBg).clickable { onWalkMode() }.padding(16.dp)) {
            Column {
                Icon(Icons.Outlined.Place, null, tint = walkModeIconTint, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Walk Mode", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = walkModeTitleColor)
                Text(walkModeSub, fontSize = 13.sp, color = walkModeSubColor)
            }
        }
        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Gray100).clickable { onHistory() }.padding(16.dp)) {
            Column {
                Icon(Icons.Outlined.DateRange, null, tint = Gray900, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("History", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gray900)
                Text(historySubtitle, fontSize = 13.sp, color = Gray600)
            }
        }
    }
}

private data class ActivityItem(val icon: ImageVector, val iconTint: Color, val iconBg: Color, val title: String, val subtitle: String, val time: String)

@Composable
private fun RecentActivityList() {
    val items = listOf(
        ActivityItem(Icons.Filled.Accessibility, Gray600, Gray100, "Fall detected — cancelled", "False positive — user OK", "14:22"),
        ActivityItem(Icons.Filled.Warning, WarningAmberLocal, WarningLight, "Shake alert sent", "SMS to 2 contacts", "11:05")
    )
    Text("RECENT ACTIVITY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gray600, letterSpacing = 1.2.sp)
    Spacer(modifier = Modifier.height(10.dp))
    Column {
        items.forEachIndexed { index, item ->
            ActivityRow(item)
            if (index < items.lastIndex) HorizontalDivider(color = Gray200, thickness = 1.dp, modifier = Modifier.padding(start = 56.dp))
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(item.iconBg), contentAlignment = Alignment.Center) {
            Icon(item.icon, null, tint = item.iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Gray900)
            Text(item.subtitle, fontSize = 13.sp, color = Gray600)
        }
        Text(item.time, fontSize = 12.sp, color = Gray400)
    }
}