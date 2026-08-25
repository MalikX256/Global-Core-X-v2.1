package com.example.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TripEntity
import com.example.ui.components.map.MapUtils
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

@Composable
fun HistoryScreen(
    trips: List<TripEntity>,
    onDeleteTrip: (Long) -> Unit,
    onDeleteAllHistory: () -> Unit,
    onViewTripOnMap: (TripEntity) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.TODAY) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val filteredTrips = remember(trips, selectedFilter) {
        val now = Calendar.getInstance()
        when (selectedFilter) {
            HistoryFilter.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                trips.filter { it.startTime >= cal.timeInMillis }
            }
            HistoryFilter.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                trips.filter { it.startTime >= cal.timeInMillis }
            }
            HistoryFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                }
                trips.filter { it.startTime >= cal.timeInMillis }
            }
        }
    }

    val totalDistanceMeters = remember(filteredTrips) { filteredTrips.sumOf { it.totalDistanceMeters } }
    val totalDurationSeconds = remember(filteredTrips) { filteredTrips.sumOf { it.durationSeconds } }
    val avgSpeed = remember(filteredTrips) {
        if (filteredTrips.isNotEmpty()) filteredTrips.map { it.averageSpeedKmh }.average() else 0.0
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Title & Clear Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LOCATION & ROUTE LOGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Trip History",
                        style = MaterialTheme.typography.titleLarge,
                        color = SleekZinc100,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (trips.isNotEmpty()) {
                    TextButton(
                        onClick = { showDeleteAllDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = SleekSosRed)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }

        // 2. Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.values().forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = when (filter) {
                                    HistoryFilter.TODAY -> "Today"
                                    HistoryFilter.THIS_WEEK -> "This Week"
                                    HistoryFilter.THIS_MONTH -> "This Month"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SleekZinc100,
                            selectedLabelColor = SleekZinc900,
                            containerColor = SleekZinc800,
                            labelColor = SleekZinc300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) SleekZinc100 else SleekZinc700
                        )
                    )
                }
            }
        }

        // 3. Aggregate Metrics Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("history_summary_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "PERIOD SUMMARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Distance", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                            Text(
                                text = MapUtils.formatDistance(totalDistanceMeters),
                                style = MaterialTheme.typography.titleLarge,
                                color = SleekGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Trips", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                            Text(
                                text = "${filteredTrips.size}",
                                style = MaterialTheme.typography.titleLarge,
                                color = SleekZinc100,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Active Time", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                            Text(
                                text = MapUtils.formatDuration(totalDurationSeconds),
                                style = MaterialTheme.typography.titleLarge,
                                color = SleekBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Trip Items List
        if (filteredTrips.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = SleekZinc500,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No recorded trips in this time frame.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc500
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredTrips) { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("trip_card_${trip.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = when (trip.travelMode) {
                                        "walking" -> Icons.Default.DirectionsWalk
                                        "cycling" -> Icons.Default.DirectionsBike
                                        else -> Icons.Default.DirectionsCar
                                    },
                                    contentDescription = null,
                                    tint = SleekBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = trip.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = dateFormat.format(Date(trip.startTime)),
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc400
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (trip.startAddress.isNotBlank()) {
                            Text(
                                text = "📍 ${trip.startAddress}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc400,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Distance", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                    Text(
                                        text = MapUtils.formatDistance(trip.totalDistanceMeters),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                    Text(
                                        text = MapUtils.formatDuration(trip.durationSeconds),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column {
                                    Text("Avg Speed", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                    Text(
                                        text = "${trip.averageSpeedKmh.toInt()} km/h",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { onViewTripOnMap(trip) }) {
                                    Icon(Icons.Default.Map, contentDescription = "View on Map", tint = SleekBlue)
                                }
                                IconButton(onClick = { onDeleteTrip(trip.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SleekZinc400)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Purge Trip History", color = SleekZinc100, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all recorded trip logs and GPS history?", color = SleekZinc400) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllHistory()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSosRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
