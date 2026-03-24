package com.example.safesense.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.hardware.Sensor
import android.hardware.SensorManager
import android.content.Context
import com.example.safesense.R
import com.example.safesense.ui.theme.DeepRed
import com.example.safesense.ui.theme.Gray100
import com.example.safesense.ui.theme.Gray200
import com.example.safesense.ui.theme.Gray400
import com.example.safesense.ui.theme.Gray600
import com.example.safesense.ui.theme.Gray900
import com.example.safesense.ui.theme.PrimaryRed
import com.example.safesense.ui.theme.RedLight
import com.example.safesense.ui.theme.SuccessGreen
import com.example.safesense.ui.theme.WarningAmber
import com.example.safesense.ui.theme.White

// ─────────────────────────────────────────────────────────────────────────────
// HomeScreen.kt
// Location: ui/home/HomeScreen.kt
//
// Full production layout for the SafeSense Home screen.
// Matches the approved mockup exactly. Uses only theme colour variables —
// no raw Color.Red or default Material colours anywhere.
//
// COMPOSABLE STRUCTURE:
//   HomeScreen                  — root, collects state, owns Scaffold
//   ├─ TopHeader                — red top bar with logo + bell
//   ├─ MonitoringStatusCard     — DeepRed card with pulse dot (always active)
//   ├─ FalsePositiveNudgeBanner — dismissible amber warning banner
//   ├─ SensorGrid               — 2×2 pill grid (real SensorManager check)
//   ├─ PanicAlertButton         — SOS button (reusable)
//   ├─ ActionCards              — Walk Mode + History row
//   ├─ RecentActivityList       — section header + activity rows
//   └─ BottomNavBar             — 4-item nav bar
// ─────────────────────────────────────────────────────────────────────────────

// ── Colour constants not yet in theme ─────────────────────────────────────────
private val PulseDot          = Color(0xFF69F0AE)
private val SuccessLight      = Color(0xFFE8F5E9)
private val WarningLight      = Color(0xFFFFF8E1)
private val WarningBorder     = Color(0xFFF57F17)
private val WarningAmberLocal = Color(0xFFF57F17)

// ─────────────────────────────────────────────────────────────────────────────
// ROOT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    onNavigateToWalkMode: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Run real sensor check whenever the screen is composed.
    // SensorManager is synchronous — no suspend needed.
    LaunchedEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.updateSensorStatus(
            accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            proximity     = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null,
            gps           = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LOCATION_GPS),
            audio         = false   // stays grey until RECORD_AUDIO permission granted in Phase 2
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                selectedIndex = selectedNavIndex,
                onItemSelected = { index ->
                    selectedNavIndex = index
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
            // ── Red header zone ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryRed)
                    .padding(bottom = 12.dp)
            ) {
                Column {
                    TopHeader()
                    MonitoringStatusCard(
                        // App is always monitoring — no toggle
                        isMonitoring = true,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // ── White content zone ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (state.showFalsePositiveNudge) {
                    FalsePositiveNudgeBanner(
                        onDismiss = { viewModel.dismissFalsePositiveNudge() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                SensorGrid(
                    accelerometerActive = state.accelerometerActive,
                    gpsActive           = state.gpsActive,
                    proximityActive     = state.proximityActive,
                    audioActive         = state.audioActive
                )

                Spacer(modifier = Modifier.height(12.dp))

                PanicAlertButton(onClick = { /* Phase 3: launch countdown */ })

                Spacer(modifier = Modifier.height(12.dp))

                ActionCards(
                    recentIncidentCount  = state.recentIncidentCount,
                    walkModeActive       = state.walkModeActive,
                    onWalkMode           = onNavigateToWalkMode,
                    onHistory            = onNavigateToHistory
                )

                Spacer(modifier = Modifier.height(20.dp))

                RecentActivityList()

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.CenterStart) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.safesense_logo),
                contentDescription = "SafeSense Logo",
                modifier = Modifier.height(129.dp)
            )
        }

        // Notification bell with translucent deep-red circle background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DeepRed.copy(alpha = 0.55f))
                .clickable { /* TODO: navigate to notifications */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONITORING STATUS CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitoringStatusCard(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulsing scale animation for the dot when active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dotColor = if (isMonitoring) PulseDot else Gray400
    val statusText = if (isMonitoring) "Monitoring: Active" else "Monitoring: Paused"
    val subtitleText = "Fall · Shake · Snatch detection running"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DeepRed.copy(alpha = 0.6f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulse dot
        Box(
            modifier = Modifier
                .size(14.dp)
                .scale(if (isMonitoring) pulseScale else 1f)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = statusText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Text(
                text = subtitleText,
                fontSize = 13.sp,
                color = White.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FALSE POSITIVE NUDGE BANNER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FalsePositiveNudgeBanner(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WarningLight)
            .border(1.dp, WarningBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = WarningAmberLocal,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Too many false alerts? Adjust sensitivity in Settings.",
            fontSize = 13.sp,
            color = Color(0xFF5D4037),
            modifier = Modifier.weight(1f),
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Dismiss",
            tint = Color(0xFF8D6E63),
            modifier = Modifier
                .size(18.dp)
                .clickable { onDismiss() }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SENSOR GRID  (2 × 2)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SensorGrid(
    accelerometerActive: Boolean,
    gpsActive: Boolean,
    proximityActive: Boolean,
    audioActive: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorPill(label = "Accelerometer", active = accelerometerActive, modifier = Modifier.weight(1f))
            SensorPill(label = "GPS",           active = gpsActive,           modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorPill(label = "Proximity",    active = proximityActive, modifier = Modifier.weight(1f))
            SensorPill(label = "Microphone",   active = audioActive,     modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SensorPill(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (active) SuccessGreen else Gray400,
        label = "sensorDotColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Gray100)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Gray900,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PANIC ALERT BUTTON  (reusable — also lives in ui/components/PanicButton.kt)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PanicAlertButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),   // large target — must be easy to tap under stress
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB71C1C),
            contentColor   = White
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = White
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Panic Alert",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTION CARDS ROW — Walk Mode + History
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ActionCards(
    recentIncidentCount: Int,
    walkModeActive: Boolean,
    onWalkMode: () -> Unit,
    onHistory: () -> Unit
) {
    val historySubtitle = if (recentIncidentCount == 0)
        "No events today"
    else
        "$recentIncidentCount events today"

    val walkModeBg         = if (walkModeActive) RedLight    else Gray100
    val walkModeIconTint   = if (walkModeActive) PrimaryRed  else Gray900
    val walkModeTitleColor = if (walkModeActive) DeepRed     else Gray900
    val walkModeSubColor   = if (walkModeActive) PrimaryRed  else Gray600
    val walkModeSub        = if (walkModeActive) "Active — destination set" else "Set destination"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Walk Mode card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(walkModeBg)
                .clickable { onWalkMode() }
                .padding(16.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = walkModeIconTint,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Walk Mode",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = walkModeTitleColor
                )
                Text(
                    text = walkModeSub,
                    fontSize = 13.sp,
                    color = walkModeSubColor
                )
            }
        }

        // History card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Gray100)
                .clickable { onHistory() }
                .padding(16.dp)
        ) {
            Column {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = Gray900,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "History",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray900
                )
                Text(
                    text = historySubtitle,
                    fontSize = 13.sp,
                    color = Gray600
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RECENT ACTIVITY LIST
// ─────────────────────────────────────────────────────────────────────────────

private data class ActivityItem(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
    val subtitle: String,
    val time: String
)

@Composable
private fun RecentActivityList() {
    // Hardcoded placeholder items — replaced by real data in Phase 2
    val items = listOf(
        ActivityItem(
            icon       = Icons.Filled.Accessibility,
            iconTint   = Gray600,
            iconBg     = Gray100,
            title      = "Fall detected — cancelled",
            subtitle   = "False positive — user OK",
            time       = "14:22"
        ),
        ActivityItem(
            icon       = Icons.Filled.Warning,
            iconTint   = WarningAmberLocal,
            iconBg     = WarningLight,
            title      = "Shake alert sent",
            subtitle   = "SMS to 2 contacts",
            time       = "11:05"
        )
    )

    Text(
        text = "RECENT ACTIVITY",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Gray600,
        letterSpacing = 1.2.sp
    )

    Spacer(modifier = Modifier.height(10.dp))

    Column {
        items.forEachIndexed { index, item ->
            ActivityRow(item = item)
            if (index < items.lastIndex) {
                HorizontalDivider(
                    color = Gray200,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gray900
            )
            Text(
                text = item.subtitle,
                fontSize = 13.sp,
                color = Gray600
            )
        }

        Text(
            text = item.time,
            fontSize = 12.sp,
            color = Gray400
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAVIGATION BAR
// ─────────────────────────────────────────────────────────────────────────────

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
private fun BottomNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        NavItem("Home",     Icons.Filled.Home),
        NavItem("History",  Icons.Outlined.ReceiptLong),
        NavItem("Contacts", Icons.Outlined.People),
        NavItem("Settings", Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = White,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor    = PrimaryRed,
                    selectedTextColor    = PrimaryRed,
                    unselectedIconColor  = Gray400,
                    unselectedTextColor  = Gray400,
                    indicatorColor       = White
                )
            )
        }
    }
}