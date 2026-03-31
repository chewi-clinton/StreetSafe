package com.example.safesense.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safesense.domain.model.AlertStatus
import com.example.safesense.domain.model.Incident
import com.example.safesense.domain.model.IncidentType
import com.example.safesense.ui.theme.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidentId: Long,
    onBack: () -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(incidentId) {
        viewModel.loadIncident(incidentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.deleteIncident()
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                uiState.incident?.let { incident ->
                    IncidentDetailContent(incident)
                }
            }
        }
    }
}

@Composable
private fun IncidentDetailContent(incident: Incident) {
    val scrollState = rememberScrollState()
    val sdfDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val date = Date(incident.timestampMillis)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Status and Type Header
        HeaderSection(incident)

        // Time and Date Section
        InfoCard(title = "Time & Date") {
            DetailRow(icon = Icons.Default.Event, label = "Date", value = sdfDate.format(date))
            DetailRow(icon = Icons.Default.Schedule, label = "Time", value = sdfTime.format(date))
        }

        // Location Section
        InfoCard(title = "Location") {
            if (incident.gpsAttached && incident.latitude != null && incident.longitude != null) {
                DetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Coordinates",
                    value = String.format(Locale.getDefault(), "%.5f, %.5f", incident.latitude, incident.longitude)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Real Map Previewer using osmdroid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    MapPreview(latitude = incident.latitude, longitude = incident.longitude)
                    
                    // Clickable overlay layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                val uri = Uri.parse("geo:${incident.latitude},${incident.longitude}?q=${incident.latitude},${incident.longitude}(Incident+Location)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                    )

                    // Overlay to indicate clickability
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew, 
                            contentDescription = "Open in Maps", 
                            tint = Color.White, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = "Tap map to open in external navigation app",
                    fontSize = 12.sp,
                    color = Gray600,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
            } else {
                Text("No location data available for this incident.", color = Gray600, fontSize = 14.sp)
            }
        }

        // Alert Details
        InfoCard(title = "Alert Status") {
            DetailRow(
                icon = Icons.Default.NotificationsActive,
                label = "Status",
                value = incident.alertStatus.name
            )
            DetailRow(
                icon = Icons.Default.People,
                label = "Contacts Alerted",
                value = "${incident.contactsAlerted} / ${incident.totalActiveContacts}"
            )
            DetailRow(
                icon = Icons.Default.BarChart,
                label = "Confidence",
                value = incident.confidence.name
            )
        }

        // Sensor Data (if applicable)
        if (incident.accelerometerPeakValue != null || incident.cancelledAfterSeconds != null) {
            InfoCard(title = "Technical Details") {
                incident.accelerometerPeakValue?.let {
                    DetailRow(
                        icon = Icons.Default.Speed,
                        label = "Peak Force",
                        value = String.format(Locale.getDefault(), "%.2f g", it)
                    )
                }
                incident.cancelledAfterSeconds?.let {
                    DetailRow(
                        icon = Icons.Default.TimerOff,
                        label = "Cancelled After",
                        value = "$it seconds"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MapPreview(latitude: Double, longitude: Double) {
    val geoPoint = remember { GeoPoint(latitude, longitude) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(false) // Disable interaction on the preview
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                controller.setZoom(17.0)
                controller.setCenter(geoPoint)
                
                // Add a marker at the incident location
                val marker = Marker(this)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Incident Location"
                overlays.add(marker)
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.controller.setCenter(geoPoint)
        }
    )
}

@Composable
private fun HeaderSection(incident: Incident) {
    val statusColor = when (incident.alertStatus) {
        AlertStatus.SENT, AlertStatus.COMPLETED -> SuccessGreen
        AlertStatus.CANCELLED -> Gray600
        AlertStatus.FAILED -> PrimaryRed
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = when (incident.type) {
                IncidentType.FALL -> Icons.Default.AccessibilityNew
                IncidentType.SHAKE -> Icons.Default.Warning
                IncidentType.WALK_MODE -> Icons.AutoMirrored.Filled.DirectionsWalk
                IncidentType.MANUAL -> Icons.Default.TouchApp
                else -> Icons.Default.ErrorOutline
            }
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(32.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = if (incident.type == IncidentType.WALK_MODE) "Walk Mode" else incident.type.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.1f),
                contentColor = statusColor
            ) {
                Text(
                    text = incident.alertStatus.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Gray600,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Gray100.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Gray600)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Gray600)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
