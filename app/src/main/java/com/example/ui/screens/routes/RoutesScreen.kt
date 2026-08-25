package com.example.ui.screens.routes

import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.local.entity.SavedRouteEntity
import com.example.data.local.entity.UserPreferencesEntity
import com.example.ui.components.map.MapUtils
import com.example.ui.theme.*
import com.example.ui.viewmodel.RoutePlannerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    routeState: RoutePlannerState,
    savedRoutes: List<SavedRouteEntity>,
    userPreferences: UserPreferencesEntity,
    onOriginChange: (String, Double, Double) -> Unit,
    onDestinationChange: (String, Double, Double) -> Unit,
    onSearchDest: (String) -> Unit,
    onTravelModeChange: (String) -> Unit,
    onCalculateRoute: () -> Unit,
    onAiOptimizeRoute: () -> Unit,
    onSpeakAiBriefing: () -> Unit = {},
    onSaveRoute: (String) -> Unit,
    onDeleteSavedRoute: (SavedRouteEntity) -> Unit,
    onStartNavigation: () -> Unit,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit
) {
    var destInput by remember { mutableStateOf(routeState.destQuery) }
    var routeNameInput by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Title
        item {
            Column {
                Text(
                    text = "GLOBAL ROUTE PLANNER",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Plan & AI Optimization",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekZinc100,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Route Origin & Destination Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("route_inputs_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Origin row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.TripOrigin, contentDescription = null, tint = SleekGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Starting Point", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                            Text(
                                text = routeState.originQuery.ifBlank { "Current GPS Fix" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc100,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SleekZinc800)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Destination row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Place, contentDescription = null, tint = SleekSosRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Destination Address / City", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                            OutlinedTextField(
                                value = destInput,
                                onValueChange = {
                                    destInput = it
                                    if (it.length >= 2) onSearchDest(it)
                                },
                                placeholder = { Text("e.g. Entebbe, London, Paris, Tokyo...", color = SleekZinc500, fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("dest_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SleekBlue,
                                    unfocusedBorderColor = SleekZinc800,
                                    focusedTextColor = SleekZinc100,
                                    unfocusedTextColor = SleekZinc100
                                ),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Search Results
                    if (routeState.searchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SleekZinc950),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                routeState.searchResults.take(3).forEach { res ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val lat = res.lat.toDoubleOrNull() ?: 0.0
                                                val lon = res.lon.toDoubleOrNull() ?: 0.0
                                                destInput = res.displayName
                                                onDestinationChange(res.displayName, lat, lon)
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = res.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SleekZinc100,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Travel Mode Chips
                    Text("TRAVEL MODE", style = MaterialTheme.typography.labelSmall, color = SleekZinc500, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("driving" to "Drive", "walking" to "Walk", "cycling" to "Cycle")
                        modes.forEach { (modeKey, modeLabel) ->
                            val isSelected = routeState.travelMode.equals(modeKey, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onTravelModeChange(modeKey) },
                                label = { Text(modeLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (modeKey) {
                                            "walking" -> Icons.Default.DirectionsWalk
                                            "cycling" -> Icons.Default.DirectionsBike
                                            else -> Icons.Default.DirectionsCar
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SleekZinc100,
                                    selectedLabelColor = SleekZinc900,
                                    selectedLeadingIconColor = SleekZinc900,
                                    containerColor = SleekZinc800,
                                    labelColor = SleekZinc300,
                                    iconColor = SleekZinc400
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) SleekZinc100 else SleekZinc700
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Route Preferences Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = userPreferences.avoidTolls,
                                onCheckedChange = { onUpdatePreferences(userPreferences.copy(avoidTolls = it)) },
                                colors = CheckboxDefaults.colors(checkedColor = SleekBlue)
                            )
                            Text("Avoid Tolls", style = MaterialTheme.typography.bodyMedium, color = SleekZinc400)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = userPreferences.avoidHighways,
                                onCheckedChange = { onUpdatePreferences(userPreferences.copy(avoidHighways = it)) },
                                colors = CheckboxDefaults.colors(checkedColor = SleekBlue)
                            )
                            Text("Avoid Highways", style = MaterialTheme.typography.bodyMedium, color = SleekZinc400)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Calculate Route Button
                    Button(
                        onClick = onCalculateRoute,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_calc_route"),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !routeState.isLoading
                    ) {
                        if (routeState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Calculate Real Route (OSRM)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Calculated Route Metrics & AI Route Suggestions
        if (routeState.routeCoordinates.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("route_details_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ESTIMATED DISTANCE", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                Text(
                                    text = MapUtils.formatDistance(routeState.distanceMeters),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SleekGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ESTIMATED DURATION", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                Text(
                                    text = MapUtils.formatDuration(routeState.durationSeconds.toLong()),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SleekBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekZinc800)
                        Spacer(modifier = Modifier.height(16.dp))

                        // AI ROUTE SUGGESTION BADGE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SleekZinc950),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "AI ROUTE OPTIMIZATION (GEMINI)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SleekBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = onSpeakAiBriefing,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SleekBlue.copy(alpha = 0.2f),
                                            contentColor = SleekBlue
                                        ),
                                        modifier = Modifier.testTag("btn_speak_ai_route_briefing")
                                    ) {
                                        Icon(Icons.Default.SpatialAudio, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Listen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (routeState.isAiLoading) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SleekBlue, strokeWidth = 2.dp)
                                        Text("Analyzing real route safety, traffic & efficiency...", color = SleekZinc400, style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text(
                                        text = routeState.aiRecommendation.ifBlank {
                                            "Recommended Route — optimal corridor selected with minimal elevation changes."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekZinc100,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f).testTag("btn_save_route"),
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700))
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = SleekZinc100, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Route", color = SleekZinc100)
                            }

                            Button(
                                onClick = onStartNavigation,
                                modifier = Modifier.weight(1f).testTag("btn_start_nav_direct"),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Nav", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 4. Saved Routes List
        item {
            Text(
                text = "SAVED ROUTES (${savedRoutes.size})",
                style = MaterialTheme.typography.labelSmall,
                color = SleekZinc500,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (savedRoutes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No saved routes yet. Plan a trip above and save it for quick access.", color = SleekZinc500, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(savedRoutes) { route ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = route.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekZinc100,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.1f", route.distanceKm)} km • ${route.durationMin} min • ${route.travelMode.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekGreen
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                onDestinationChange(route.destName, route.destLat, route.destLng)
                                onCalculateRoute()
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Load Route", tint = SleekBlue)
                            }
                            IconButton(onClick = { onDeleteSavedRoute(route) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = SleekZinc400)
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Route Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Planned Route", color = SleekZinc100, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = routeNameInput,
                    onValueChange = { routeNameInput = it },
                    placeholder = { Text("e.g. Home to Office, Airport Run...", color = SleekZinc500) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SleekZinc100,
                        unfocusedTextColor = SleekZinc100,
                        focusedBorderColor = SleekBlue,
                        unfocusedBorderColor = SleekZinc700
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveRoute(routeNameInput)
                        showSaveDialog = false
                        routeNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
