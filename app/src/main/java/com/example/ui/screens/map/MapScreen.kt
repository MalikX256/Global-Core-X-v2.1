package com.example.ui.screens.map

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.model.NominatimSearchResult
import com.example.service.GpsLocationData
import com.example.ui.components.map.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.RoutePlannerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    gpsLocation: GpsLocationData,
    routeState: RoutePlannerState,
    mapStyle: MapStyle = MapStyle.CYBER_DARK,
    poiItems: List<MapPoiItem> = emptyList(),
    selectedPoiCategory: PoiCategory = PoiCategory.ALL,
    selectedPoi: MapPoiItem? = null,
    isPoiLoading: Boolean = false,
    useGoogleMaps: Boolean = true,
    isVoiceMuted: Boolean = false,
    isSpeaking: Boolean = false,
    onStyleChange: (MapStyle) -> Unit = {},
    onSelectPoiCategory: (PoiCategory) -> Unit = {},
    onSelectPoi: (MapPoiItem?) -> Unit = {},
    onToggleMapEngine: () -> Unit = {},
    onSearchDestination: (String) -> Unit,
    onSelectDestination: (String, Double, Double) -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onToggleVoiceMute: () -> Unit = {},
    onRepeatVoiceInstruction: () -> Unit = {},
    onSpeakAiBriefing: () -> Unit = {},
    onNextStep: () -> Unit = {},
    onPreviousStep: () -> Unit = {},
    onGoToRoutePlanner: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val currentStep = routeState.currentStep
    val currentInstruction = remember(routeState.isNavigating, routeState.currentStepIndex, currentStep) {
        if (!routeState.isNavigating) null
        else if (currentStep != null) {
            val type = currentStep.maneuver?.type?.replaceFirstChar { it.uppercase() } ?: "Turn"
            val modifier = currentStep.maneuver?.modifier ?: ""
            val road = currentStep.name.ifBlank { "corridor" }
            val dist = if (currentStep.distance > 1000) String.format("%.1f km", currentStep.distance / 1000.0) else "${currentStep.distance.toInt()}m"
            if (!currentStep.maneuver?.instruction.isNullOrBlank()) {
                "${currentStep.maneuver?.instruction} ($dist)"
            } else {
                "$type $modifier onto $road ($dist)".trim()
            }
        } else {
            "Continue along designated navigation corridor"
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // 1. Primary Map Engine: Google Maps Compose
        // ==========================================
        if (useGoogleMaps) {
            GoogleMapComponent(
                modifier = Modifier.fillMaxSize(),
                userLatitude = gpsLocation.latitude,
                userLongitude = gpsLocation.longitude,
                userAccuracy = gpsLocation.accuracy,
                userHeading = gpsLocation.heading,
                isGpsActive = gpsLocation.isGpsActive,
                routePoints = routeState.routeCoordinates,
                poiItems = poiItems,
                destinationLat = routeState.destLat,
                destinationLng = routeState.destLng,
                destinationTitle = routeState.destQuery,
                selectedPoi = selectedPoi,
                onSelectPoi = onSelectPoi,
                onMapClick = { latLng ->
                    onSelectPoi(null)
                },
                onMapLongClick = { latLng ->
                    onSelectDestination(
                        "Pinned Location (${String.format("%.4f, %.4f", latLng.latitude, latLng.longitude)})",
                        latLng.latitude,
                        latLng.longitude
                    )
                },
                onSelectDestination = onSelectDestination
            )
        } else {
            // High-Performance Fallback Custom Tile Canvas
            val canvasMarkers = remember(gpsLocation, routeState, poiItems) {
                val list = mutableListOf<MapMarker>()
                if (routeState.destLat != 0.0 && routeState.destLng != 0.0) {
                    list.add(
                        MapMarker(
                            id = "dest_marker",
                            latitude = routeState.destLat,
                            longitude = routeState.destLng,
                            title = routeState.destQuery.ifBlank { "Destination" },
                            color = SleekSosRed,
                            iconType = "destination"
                        )
                    )
                }
                poiItems.forEach { poi ->
                    list.add(
                        MapMarker(
                            id = poi.id,
                            latitude = poi.latitude,
                            longitude = poi.longitude,
                            title = "${poi.category.emoji} ${poi.name}",
                            subtitle = poi.address,
                            color = poi.category.accentColor,
                            iconType = "pin"
                        )
                    )
                }
                list
            }

            InteractiveMapCanvas(
                modifier = Modifier.fillMaxSize(),
                userLatitude = gpsLocation.latitude,
                userLongitude = gpsLocation.longitude,
                userAccuracy = gpsLocation.accuracy,
                userHeading = gpsLocation.heading,
                isGpsActive = gpsLocation.isGpsActive,
                routePoints = routeState.routeCoordinates,
                markers = canvasMarkers,
                mapStyle = mapStyle,
                isNavigating = routeState.isNavigating,
                currentStepInstruction = currentInstruction,
                isVoiceMuted = isVoiceMuted,
                isSpeaking = isSpeaking,
                currentStepIndex = routeState.currentStepIndex,
                totalStepsCount = routeState.steps.size,
                onToggleVoiceMute = onToggleVoiceMute,
                onRepeatVoiceInstruction = onRepeatVoiceInstruction,
                onSpeakAiBriefing = onSpeakAiBriefing,
                onNextStep = onNextStep,
                onPreviousStep = onPreviousStep,
                onMapTap = { lat, lng ->
                    onSelectDestination("Tapped Location (${String.format("%.4f, %.4f", lat, lng)})", lat, lng)
                },
                onStyleChange = onStyleChange
            )
        }

        // ==========================================
        // 2. Top Bar: Search + POI Category Chips
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
        ) {
            // Search Bar Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_search_bar"),
                shape = RoundedCornerShape(18.dp),
                color = SleekZinc900.copy(alpha = 0.95f),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Places",
                        tint = SleekBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.length >= 2) {
                                onSearchDestination(it)
                                isSearchExpanded = true
                            } else {
                                isSearchExpanded = false
                            }
                        },
                        placeholder = {
                            Text("Search hospital, school, hotel, road...", color = SleekZinc400, fontSize = 13.5.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchExpanded = false
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = SleekZinc400)
                        }
                    }

                    // Map Engine Switch Indicator Chip
                    Surface(
                        onClick = onToggleMapEngine,
                        shape = RoundedCornerShape(12.dp),
                        color = if (useGoogleMaps) SleekBlue.copy(alpha = 0.2f) else SleekEmerald.copy(alpha = 0.2f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (useGoogleMaps) SleekBlue else SleekEmerald)
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (useGoogleMaps) Icons.Default.Public else Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = if (useGoogleMaps) SleekCyan else SleekEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (useGoogleMaps) "Google" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (useGoogleMaps) SleekCyan else SleekEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Search Autocomplete Results List
            AnimatedVisibility(
                visible = isSearchExpanded && routeState.searchResults.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .heightIn(max = 250.dp)
                        .testTag("search_results_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900.copy(alpha = 0.98f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    LazyColumn(modifier = Modifier.padding(6.dp)) {
                        items(routeState.searchResults) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val lat = result.lat.toDoubleOrNull() ?: 0.0
                                        val lon = result.lon.toDoubleOrNull() ?: 0.0
                                        onSelectDestination(result.displayName, lat, lon)
                                        searchQuery = result.displayName
                                        isSearchExpanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = SleekBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = result.displayName.substringBefore(","),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = result.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc400,
                                        maxLines = 1
                                    )
                                }
                            }
                            HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // POI Categories Horizontal Scroll Filter
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PoiCategoryChip(
                    category = PoiCategory.HOSPITAL,
                    isSelected = selectedPoiCategory == PoiCategory.HOSPITAL,
                    onClick = { onSelectPoiCategory(PoiCategory.HOSPITAL) }
                )
                PoiCategoryChip(
                    category = PoiCategory.POLICE,
                    isSelected = selectedPoiCategory == PoiCategory.POLICE,
                    onClick = { onSelectPoiCategory(PoiCategory.POLICE) }
                )
                PoiCategoryChip(
                    category = PoiCategory.GAS_STATION,
                    isSelected = selectedPoiCategory == PoiCategory.GAS_STATION,
                    onClick = { onSelectPoiCategory(PoiCategory.GAS_STATION) }
                )
                PoiCategoryChip(
                    category = PoiCategory.HOTEL,
                    isSelected = selectedPoiCategory == PoiCategory.HOTEL,
                    onClick = { onSelectPoiCategory(PoiCategory.HOTEL) }
                )
                PoiCategoryChip(
                    category = PoiCategory.SCHOOL,
                    isSelected = selectedPoiCategory == PoiCategory.SCHOOL,
                    onClick = { onSelectPoiCategory(PoiCategory.SCHOOL) }
                )
                PoiCategoryChip(
                    category = PoiCategory.RESTAURANT,
                    isSelected = selectedPoiCategory == PoiCategory.RESTAURANT,
                    onClick = { onSelectPoiCategory(PoiCategory.RESTAURANT) }
                )
                PoiCategoryChip(
                    category = PoiCategory.PHARMACY,
                    isSelected = selectedPoiCategory == PoiCategory.PHARMACY,
                    onClick = { onSelectPoiCategory(PoiCategory.PHARMACY) }
                )
                PoiCategoryChip(
                    category = PoiCategory.BANK_ATM,
                    isSelected = selectedPoiCategory == PoiCategory.BANK_ATM,
                    onClick = { onSelectPoiCategory(PoiCategory.BANK_ATM) }
                )
            }
        }

        // ==========================================
        // 3. Floating Speed & Altitude Telemetry HUD
        // ==========================================
        FloatingTelemetryOverlay(
            gpsLocation = gpsLocation,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 110.dp)
        )

        // ==========================================
        // 4. Loading indicator for POIs
        // ==========================================
        if (isPoiLoading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp)),
                color = SleekZinc900.copy(alpha = 0.9f),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekCyan))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = SleekCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Locating nearby places...",
                        style = MaterialTheme.typography.labelMedium,
                        color = SleekZinc200,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ==========================================
        // 5. Selected POI Details Card
        // ==========================================
        selectedPoi?.let { poi ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 85.dp)
                    .align(Alignment.BottomCenter)
                    .testTag("selected_poi_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900.copy(alpha = 0.97f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(poi.category.accentColor))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = poi.category.accentColor.copy(alpha = 0.2f),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(poi.category.accentColor)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = poi.category.emoji, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = poi.category.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = poi.category.accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = poi.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { onSelectPoi(null) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = SleekZinc400)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = poi.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc300,
                        maxLines = 2
                    )

                    if (poi.distanceMeters > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📏 Distance: ${MapUtils.formatDistance(poi.distanceMeters)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SleekCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "📍 ${String.format("%.4f, %.4f", poi.latitude, poi.longitude)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSelectDestination(poi.name, poi.latitude, poi.longitude)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700))
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = SleekZinc200, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Pin", color = SleekZinc200)
                        }

                        Button(
                            onClick = {
                                onSelectDestination(poi.name, poi.latitude, poi.longitude)
                                onStartNavigation()
                                onSelectPoi(null)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigate Here", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. Bottom Route Info & Navigation HUD Card
        // ==========================================
        if (selectedPoi == null && routeState.destLat != 0.0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 85.dp)
                    .align(Alignment.BottomCenter)
                    .testTag("route_summary_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900.copy(alpha = 0.97f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DESTINATION PIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = routeState.destQuery.ifBlank { "Selected Pin" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { onSelectDestination("", 0.0, 0.0) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Route", tint = SleekZinc400)
                        }
                    }

                    if (routeState.distanceMeters > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Distance: ${MapUtils.formatDistance(routeState.distanceMeters)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "ETA: ${MapUtils.formatDuration(routeState.durationSeconds.toLong())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGoToRoutePlanner,
                            modifier = Modifier.weight(1f).testTag("btn_plan_details"),
                            shape = RoundedCornerShape(14.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700))
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = SleekZinc100, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Route & AI", color = SleekZinc100)
                        }

                        Button(
                            onClick = {
                                if (routeState.isNavigating) onStopNavigation() else onStartNavigation()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_start_nav"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (routeState.isNavigating) SleekSosRed else SleekBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (routeState.isNavigating) Icons.Default.Stop else Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (routeState.isNavigating) "End Nav" else "Start Nav",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoiCategoryChip(
    category: PoiCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) category.accentColor.copy(alpha = 0.25f) else SleekZinc900.copy(alpha = 0.92f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) category.accentColor else SleekZinc800)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White else SleekZinc300,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}
