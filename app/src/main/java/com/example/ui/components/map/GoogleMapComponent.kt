package com.example.ui.components.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

enum class GoogleMapMode {
    CYBER_DARK,
    CLEAN_STREET,
    SATELLITE_HYBRID,
    TERRAIN
}

@Composable
fun GoogleMapComponent(
    modifier: Modifier = Modifier,
    userLatitude: Double,
    userLongitude: Double,
    userAccuracy: Float = 10f,
    userHeading: Float = 0f,
    isGpsActive: Boolean = true,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    poiItems: List<MapPoiItem> = emptyList(),
    destinationLat: Double = 0.0,
    destinationLng: Double = 0.0,
    destinationTitle: String = "",
    selectedPoi: MapPoiItem? = null,
    onSelectPoi: (MapPoiItem?) -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onSelectDestination: (title: String, lat: Double, lng: Double) -> Unit = { _, _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    var currentMapMode by remember { mutableStateOf(GoogleMapMode.CYBER_DARK) }
    var isTrafficEnabled by remember { mutableStateOf(true) }
    var is3dPerspective by remember { mutableStateOf(false) }
    var isFollowingUser by remember { mutableStateOf(true) }
    var showMapControlsMenu by remember { mutableStateOf(false) }

    val initialPosition = remember {
        val lat = if (userLatitude != 0.0) userLatitude else 0.3476
        val lng = if (userLongitude != 0.0) userLongitude else 32.5825
        CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = initialPosition
    }

    // Automatically center map on user when tracking starts or location updates if following is active
    LaunchedEffect(userLatitude, userLongitude, isFollowingUser) {
        if (isFollowingUser && userLatitude != 0.0 && userLongitude != 0.0) {
            val target = LatLng(userLatitude, userLongitude)
            val currentZoom = cameraPositionState.position.zoom.coerceAtLeast(14.5f)
            val tilt = if (is3dPerspective) 45f else 0f
            val bearing = if (is3dPerspective && userHeading > 0f) userHeading else cameraPositionState.position.bearing

            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(target)
                        .zoom(currentZoom)
                        .tilt(tilt)
                        .bearing(bearing)
                        .build()
                ),
                durationMs = 600
            )
        }
    }

    // Stop auto-following if the user manually drags the map
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isFollowingUser = false
        }
    }

    // Determine MapProperties based on mode
    val mapProperties = remember(currentMapMode, isTrafficEnabled) {
        when (currentMapMode) {
            GoogleMapMode.CYBER_DARK -> MapProperties(
                isTrafficEnabled = isTrafficEnabled,
                mapType = MapType.NORMAL,
                mapStyleOptions = MapStyleOptions(GoogleMapStyles.CYBER_DARK_JSON)
            )
            GoogleMapMode.CLEAN_STREET -> MapProperties(
                isTrafficEnabled = isTrafficEnabled,
                mapType = MapType.NORMAL,
                mapStyleOptions = MapStyleOptions(GoogleMapStyles.CLEAN_STREET_JSON)
            )
            GoogleMapMode.SATELLITE_HYBRID -> MapProperties(
                isTrafficEnabled = isTrafficEnabled,
                mapType = MapType.HYBRID
            )
            GoogleMapMode.TERRAIN -> MapProperties(
                isTrafficEnabled = isTrafficEnabled,
                mapType = MapType.TERRAIN
            )
        }
    }

    val uiSettings = remember {
        MapUiSettings(
            compassEnabled = true,
            myLocationButtonEnabled = false,
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            indoorLevelPickerEnabled = true,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true
        )
    }

    // Prepare LatLng route polyline
    val routeLatLngs = remember(routePoints) {
        routePoints.map { LatLng(it.first, it.second) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            onMapClick = { latLng ->
                isFollowingUser = false
                onSelectPoi(null)
                onMapClick(latLng)
            },
            onMapLongClick = { latLng ->
                isFollowingUser = false
                onMapLongClick(latLng)
                onSelectDestination("Pinned Location (${String.format("%.4f, %.4f", latLng.latitude, latLng.longitude)})", latLng.latitude, latLng.longitude)
            },
            onPOIClick = { poi: PointOfInterest ->
                isFollowingUser = false
                val newPoi = MapPoiItem(
                    id = poi.placeId ?: "poi_${poi.latLng.latitude}_${poi.latLng.longitude}",
                    name = poi.name,
                    category = PoiCategory.ALL,
                    latitude = poi.latLng.latitude,
                    longitude = poi.latLng.longitude,
                    address = "Google Maps Verified Place",
                    distanceMeters = if (userLatitude != 0.0) {
                        MapUtils.calculateDistanceMeters(userLatitude, userLongitude, poi.latLng.latitude, poi.latLng.longitude)
                    } else 0.0
                )
                onSelectPoi(newPoi)
            }
        ) {
            // 1. User Live Location Marker & Accuracy Halo
            if (userLatitude != 0.0 && userLongitude != 0.0) {
                val userLocation = LatLng(userLatitude, userLongitude)

                // High-precision accuracy circle
                Circle(
                    center = userLocation,
                    radius = userAccuracy.toDouble().coerceIn(15.0, 150.0),
                    fillColor = SleekBlue.copy(alpha = 0.18f),
                    strokeColor = SleekBlue.copy(alpha = 0.65f),
                    strokeWidth = 2f
                )

                // User Location Dot Marker
                Marker(
                    state = rememberMarkerState(position = userLocation),
                    title = "My Current Location",
                    snippet = "Lat: ${String.format("%.5f", userLatitude)}, Lng: ${String.format("%.5f", userLongitude)}",
                    icon = remember(userHeading) {
                        createCustomLocationIcon(heading = userHeading, color = SleekBlue.toArgb())
                    },
                    anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f)
                )
            }

            // 2. Active Route Polyline
            if (routeLatLngs.isNotEmpty()) {
                // Background shadow glow casing
                Polyline(
                    points = routeLatLngs,
                    color = SleekZinc900.copy(alpha = 0.8f),
                    width = 16f
                )
                // Main route line
                Polyline(
                    points = routeLatLngs,
                    color = SleekCyan,
                    width = 10f
                )
            }

            // 3. Destination Pin Marker
            if (destinationLat != 0.0 && destinationLng != 0.0) {
                val destLocation = LatLng(destinationLat, destinationLng)
                Marker(
                    state = rememberMarkerState(position = destLocation),
                    title = destinationTitle.ifBlank { "Destination" },
                    snippet = "Coordinates: ${String.format("%.4f, %.4f", destinationLat, destinationLng)}",
                    icon = remember {
                        createCustomPinIcon(emoji = "🎯", bgColor = SleekSosRed.toArgb())
                    }
                )
            }

            // 4. Categorized POI Markers (Hospitals, Schools, Hotels, Police, Gas Stations, etc.)
            poiItems.forEach { poi ->
                val poiLatLng = LatLng(poi.latitude, poi.longitude)
                val isSelected = selectedPoi?.id == poi.id

                Marker(
                    state = rememberMarkerState(position = poiLatLng),
                    title = "${poi.category.emoji} ${poi.name}",
                    snippet = if (poi.distanceMeters > 0) "${MapUtils.formatDistance(poi.distanceMeters)} away • ${poi.category.title}" else poi.category.title,
                    icon = remember(poi.category, isSelected) {
                        createCustomPoiIcon(
                            emoji = poi.category.emoji,
                            bgColor = poi.category.accentColor.toArgb(),
                            isEnlarged = isSelected
                        )
                    },
                    onClick = {
                        onSelectPoi(poi)
                        true
                    }
                )
            }
        }

        // ====================================================
        // Floating Google Maps Control Buttons (Right Side)
        // ====================================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Layer / Mode Switcher Button
            IconButton(
                onClick = { showMapControlsMenu = !showMapControlsMenu },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SleekZinc900.copy(alpha = 0.92f))
                    .border(1.dp, SleekZinc700, CircleShape)
                    .testTag("btn_map_layers")
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Map Layers",
                    tint = if (showMapControlsMenu) SleekCyan else SleekZinc200,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3D Perspective Tilt Button
            IconButton(
                onClick = {
                    is3dPerspective = !is3dPerspective
                    coroutineScope.launch {
                        val currentTarget = cameraPositionState.position.target
                        val currentZoom = cameraPositionState.position.zoom
                        val tilt = if (is3dPerspective) 50f else 0f
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(currentTarget)
                                    .zoom(currentZoom)
                                    .tilt(tilt)
                                    .bearing(if (is3dPerspective) userHeading else 0f)
                                    .build()
                            )
                        )
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (is3dPerspective) SleekBlue.copy(alpha = 0.3f) else SleekZinc900.copy(alpha = 0.92f))
                    .border(1.dp, if (is3dPerspective) SleekBlue else SleekZinc700, CircleShape)
                    .testTag("btn_3d_tilt")
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = "Toggle 3D View",
                    tint = if (is3dPerspective) SleekCyan else SleekZinc200,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Traffic Overlay Toggle Button
            IconButton(
                onClick = { isTrafficEnabled = !isTrafficEnabled },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isTrafficEnabled) SleekAmber.copy(alpha = 0.25f) else SleekZinc900.copy(alpha = 0.92f))
                    .border(1.dp, if (isTrafficEnabled) SleekAmber else SleekZinc700, CircleShape)
                    .testTag("btn_traffic_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Traffic,
                    contentDescription = "Toggle Traffic",
                    tint = if (isTrafficEnabled) SleekAmber else SleekZinc400,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Zoom In Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SleekZinc900.copy(alpha = 0.92f))
                    .border(1.dp, SleekZinc700, CircleShape)
                    .testTag("btn_zoom_in")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = SleekZinc200,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Zoom Out Button
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SleekZinc900.copy(alpha = 0.92f))
                    .border(1.dp, SleekZinc700, CircleShape)
                    .testTag("btn_zoom_out")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    tint = SleekZinc200,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Recenter / GPS Follow Button
            IconButton(
                onClick = {
                    isFollowingUser = true
                    if (userLatitude != 0.0 && userLongitude != 0.0) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(userLatitude, userLongitude),
                                    16f
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isFollowingUser) SleekBlue else SleekZinc900.copy(alpha = 0.95f))
                    .border(1.5.dp, if (isFollowingUser) SleekCyan else SleekZinc700, CircleShape)
                    .testTag("btn_recenter_user")
            ) {
                Icon(
                    imageVector = if (isFollowingUser) Icons.Default.MyLocation else Icons.Default.LocationSearching,
                    contentDescription = "Center Location",
                    tint = if (isFollowingUser) Color.White else SleekCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ====================================================
        // Map Styles Popup Card
        // ====================================================
        AnimatedVisibility(
            visible = showMapControlsMenu,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900.copy(alpha = 0.98f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700)),
                modifier = Modifier.width(220.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "MAP VIEWPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    MapStyleOptionRow(
                        title = "Cyber Dark Nav",
                        subtitle = "High contrast night styling",
                        isSelected = currentMapMode == GoogleMapMode.CYBER_DARK,
                        onClick = {
                            currentMapMode = GoogleMapMode.CYBER_DARK
                            showMapControlsMenu = false
                        }
                    )

                    MapStyleOptionRow(
                        title = "Clean Street",
                        subtitle = "Crisp daytime clarity",
                        isSelected = currentMapMode == GoogleMapMode.CLEAN_STREET,
                        onClick = {
                            currentMapMode = GoogleMapMode.CLEAN_STREET
                            showMapControlsMenu = false
                        }
                    )

                    MapStyleOptionRow(
                        title = "Satellite Hybrid",
                        subtitle = "High-res earth imagery",
                        isSelected = currentMapMode == GoogleMapMode.SATELLITE_HYBRID,
                        onClick = {
                            currentMapMode = GoogleMapMode.SATELLITE_HYBRID
                            showMapControlsMenu = false
                        }
                    )

                    MapStyleOptionRow(
                        title = "Terrain 3D",
                        subtitle = "Elevation & contours",
                        isSelected = currentMapMode == GoogleMapMode.TERRAIN,
                        onClick = {
                            currentMapMode = GoogleMapMode.TERRAIN
                            showMapControlsMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapStyleOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) SleekBlue.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) SleekCyan else SleekZinc200,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SleekZinc500,
                fontSize = 10.sp
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SleekCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Generates an Android BitmapDescriptor icon for user's GPS marker with an orientation indicator.
 */
private fun createCustomLocationIcon(heading: Float, color: Int): BitmapDescriptor {
    val size = 72
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    // Outer glow
    paint.color = color
    paint.alpha = 80
    canvas.drawCircle(cx, cy, 32f, paint)

    // White border ring
    paint.alpha = 255
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, cy, 20f, paint)

    // Inner filled color core
    paint.color = color
    canvas.drawCircle(cx, cy, 14f, paint)

    // Heading pointer if moving
    if (heading > 0) {
        paint.color = android.graphics.Color.WHITE
        paint.strokeWidth = 4f
        val rad = Math.toRadians((heading - 90).toDouble())
        val px = cx + (26 * Math.cos(rad)).toFloat()
        val py = cy + (26 * Math.sin(rad)).toFloat()
        canvas.drawLine(cx, cy, px, py, paint)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Generates an Android BitmapDescriptor icon for categorized POIs (Hospitals, Police, Schools, Hotels, Gas Stations, etc.).
 */
private fun createCustomPoiIcon(emoji: String, bgColor: Int, isEnlarged: Boolean): BitmapDescriptor {
    val size = if (isEnlarged) 88 else 74
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    // Drop shadow
    paint.color = android.graphics.Color.BLACK
    paint.alpha = 100
    canvas.drawCircle(cx, cy + 3, (size / 2.2f), paint)

    // Background circle
    paint.alpha = 255
    paint.color = bgColor
    canvas.drawCircle(cx, cy, (size / 2.3f), paint)

    // Outer border ring
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = if (isEnlarged) 4.5f else 3f
    canvas.drawCircle(cx, cy, (size / 2.3f), paint)

    // Emoji text
    paint.style = Paint.Style.FILL
    paint.textSize = if (isEnlarged) 36f else 28f
    paint.textAlign = Paint.Align.CENTER
    val yPos = cy - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(emoji, cx, yPos, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Generates an Android BitmapDescriptor icon for destination and custom pins.
 */
private fun createCustomPinIcon(emoji: String, bgColor: Int): BitmapDescriptor {
    val size = 80
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    paint.color = bgColor
    canvas.drawCircle(cx, cy, 32f, paint)

    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 3.5f
    canvas.drawCircle(cx, cy, 32f, paint)

    paint.style = Paint.Style.FILL
    paint.textSize = 30f
    paint.textAlign = Paint.Align.CENTER
    val yPos = cy - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(emoji, cx, yPos, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
