package com.example.ui.screens.recording

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.RecordedRouteEntity
import com.example.data.local.entity.RecordedRouteWithWaypoints
import com.example.service.GpsLocationData
import com.example.ui.components.map.MapUtils
import com.example.ui.theme.*
import com.example.ui.viewmodel.RouteRecordingState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    recordingState: RouteRecordingState,
    gpsLocation: GpsLocationData,
    recordedRoutes: List<RecordedRouteEntity>,
    onStartRecording: (title: String, travelMode: String) -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopAndSaveRecording: (title: String, description: String, aiSummary: String) -> Unit,
    onDiscardRecording: () -> Unit,
    onDeleteRecordedRoute: (Long) -> Unit,
    onToggleFavoriteRoute: (Long, Boolean) -> Unit,
    onViewRouteOnMap: (RecordedRouteEntity) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var saveDescInput by remember { mutableStateOf("") }
    var selectedTravelMode by remember { mutableStateOf("driving") }
    var selectedHistoricalRoute by remember { mutableStateOf<RecordedRouteEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    // Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Save Recorded Route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SleekZinc100
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Store ${recordingState.waypoints.size} waypoints and metadata to Room Database for historical retrieval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc400
                    )
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Route Title") },
                        placeholder = { Text("e.g., Morning Commute") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_save_route_title"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700,
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100
                        )
                    )
                    OutlinedTextField(
                        value = saveDescInput,
                        onValueChange = { saveDescInput = it },
                        label = { Text("Notes / Description (Optional)") },
                        placeholder = { Text("e.g., Smooth traffic, clear weather") },
                        modifier = Modifier.fillMaxWidth().testTag("input_save_route_desc"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700,
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = saveTitleInput.ifBlank { "Recorded Route #${recordedRoutes.size + 1}" }
                        onStopAndSaveRecording(finalTitle, saveDescInput, "")
                        showSaveDialog = false
                        saveTitleInput = ""
                        saveDescInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    modifier = Modifier.testTag("btn_confirm_save_route")
                ) {
                    Text("Save to Room DB", color = SleekBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            tonalElevation = 6.dp
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROUTE RECORDER & LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = if (recordingState.isRecording) "Active Route Recording" else "Record New Route",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SleekZinc100,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recordingState.isRecording) {
                    Surface(
                        color = if (recordingState.isPaused) SleekOrange.copy(alpha = 0.2f) else SleekEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(
                            if (recordingState.isPaused) SleekOrange else SleekEmerald,
                            Color.Transparent
                        )))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (recordingState.isPaused) SleekOrange else SleekEmerald)
                            )
                            Text(
                                text = if (recordingState.isPaused) "PAUSED" else "LOGGING",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (recordingState.isPaused) SleekOrange else SleekEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Main Recording HUD Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recording_hud_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
                    listOf(
                        if (recordingState.isRecording) SleekBlue.copy(alpha = 0.6f) else SleekZinc800,
                        SleekZinc800
                    )
                ))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!recordingState.isRecording) {
                        // Travel Mode Selector
                        Text(
                            text = "SELECT ACTIVITY MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc400,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "driving" to Icons.Default.DirectionsCar,
                                "cycling" to Icons.Default.DirectionsBike,
                                "walking" to Icons.Default.DirectionsWalk,
                                "running" to Icons.Default.DirectionsRun
                            ).forEach { (mode, icon) ->
                                val isSelected = selectedTravelMode == mode
                                Surface(
                                    onClick = { selectedTravelMode = mode },
                                    modifier = Modifier.weight(1f).testTag("btn_mode_$mode"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) SleekBlue.copy(alpha = 0.2f) else SleekZinc800,
                                    border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SleekBlue, SleekBlue))) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = mode,
                                            tint = if (isSelected) SleekBlue else SleekZinc400,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = mode.replaceFirstChar { it.uppercase() },
                                            fontSize = 11.sp,
                                            color = if (isSelected) SleekBlue else SleekZinc400,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Start Recording Button
                        Button(
                            onClick = {
                                onStartRecording("Route • ${selectedTravelMode.replaceFirstChar { it.uppercase() }}", selectedTravelMode)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_start_recording"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = SleekBlack, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Recording Route",
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Active Recording Telemetry Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "DURATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc500,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = MapUtils.formatDuration(recordingState.durationSeconds),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DISTANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc500,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = MapUtils.formatDistance(recordingState.totalDistanceMeters),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SleekBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = SleekZinc800)

                        // 4-Grid Live Telemetry
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = SleekZinc800
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CURRENT SPEED", fontSize = 10.sp, color = SleekZinc400, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${String.format("%.1f", recordingState.currentSpeedKmh)} km/h",
                                        fontSize = 16.sp,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = SleekZinc800
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("AVG SPEED", fontSize = 10.sp, color = SleekZinc400, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${String.format("%.1f", recordingState.averageSpeedKmh)} km/h",
                                        fontSize = 16.sp,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = SleekZinc800
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("WAYPOINTS LOGGED", fontSize = 10.sp, color = SleekZinc400, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${recordingState.waypoints.size} pts",
                                        fontSize = 16.sp,
                                        color = SleekEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = SleekZinc800
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("ELEVATION GAIN", fontSize = 10.sp, color = SleekZinc400, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "+${String.format("%.0f", recordingState.elevationGainMeters)} m",
                                        fontSize = 16.sp,
                                        color = SleekOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Controller Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (recordingState.isPaused) {
                                Button(
                                    onClick = onResumeRecording,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_resume_recording"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekEmerald)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SleekBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", color = SleekBlack, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onPauseRecording,
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_pause_recording"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekOrange)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = SleekBlack)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", color = SleekBlack, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_stop_and_save"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = SleekBlack)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finish & Save", color = SleekBlack, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = onDiscardRecording,
                            modifier = Modifier.fillMaxWidth().testTag("btn_discard_recording")
                        ) {
                            Text("Discard Recording", color = SleekSosRed, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // 3. Room Database Historical Recorded Routes Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL RECORDED ROUTES (${recordedRoutes.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        if (recordedRoutes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(SleekZinc800, SleekZinc900)))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Route,
                            contentDescription = null,
                            tint = SleekZinc600,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Recorded Routes in Room DB",
                            style = MaterialTheme.typography.titleMedium,
                            color = SleekZinc300,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap 'Start Recording Route' above to log your journey coordinates, speed, elevation, and waypoints with precise timestamps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc500,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recordedRoutes, key = { it.id }) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("historical_route_${route.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(SleekZinc800, SleekZinc850)))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SleekBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (route.travelMode.lowercase()) {
                                            "cycling" -> Icons.Default.DirectionsBike
                                            "walking" -> Icons.Default.DirectionsWalk
                                            "running" -> Icons.Default.DirectionsRun
                                            else -> Icons.Default.DirectionsCar
                                        },
                                        contentDescription = route.travelMode,
                                        tint = SleekBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = route.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateFormat.format(Date(route.startTime)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc400
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleFavoriteRoute(route.id, route.isFavorite) },
                                    modifier = Modifier.size(36.dp).testTag("btn_fav_${route.id}")
                                ) {
                                    Icon(
                                        if (route.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (route.isFavorite) SleekOrange else SleekZinc500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteRecordedRoute(route.id) },
                                    modifier = Modifier.size(36.dp).testTag("btn_delete_route_${route.id}")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = SleekZinc500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Metrics Pill Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SleekZinc800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DISTANCE", fontSize = 9.sp, color = SleekZinc500, fontWeight = FontWeight.Bold)
                                    Text(MapUtils.formatDistance(route.totalDistanceMeters), fontSize = 13.sp, color = SleekZinc100, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SleekZinc800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DURATION", fontSize = 9.sp, color = SleekZinc500, fontWeight = FontWeight.Bold)
                                    Text(MapUtils.formatDuration(route.durationSeconds), fontSize = 13.sp, color = SleekZinc100, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SleekZinc800,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("WAYPOINTS", fontSize = 9.sp, color = SleekZinc500, fontWeight = FontWeight.Bold)
                                    Text("${route.waypointsCount} pts", fontSize = 13.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // View on Map Button
                        FilledTonalButton(
                            onClick = { onViewRouteOnMap(route) },
                            modifier = Modifier.fillMaxWidth().testTag("btn_view_map_${route.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SleekZinc800,
                                contentColor = SleekBlue
                            )
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Route & Waypoints on Map", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
