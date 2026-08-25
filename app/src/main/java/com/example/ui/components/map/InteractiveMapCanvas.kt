package com.example.ui.components.map

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.map.MapTileManager
import com.example.ui.theme.*
import java.util.Calendar
import kotlin.math.*

enum class MapStyle {
    CYBER_DARK,
    STREET,
    SATELLITE
}

data class MapMarker(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String,
    val subtitle: String = "",
    val color: Color = ElectricCyan,
    val iconType: String = "pin" // "pin", "start", "destination", "sos", "device"
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveMapCanvas(
    modifier: Modifier = Modifier,
    userLatitude: Double,
    userLongitude: Double,
    userAccuracy: Float = 10f,
    userHeading: Float = 0f,
    isGpsActive: Boolean = true,
    routePoints: List<Pair<Double, Double>> = emptyList(), // [ (lat, lng), ... ]
    markers: List<MapMarker> = emptyList(),
    mapStyle: MapStyle = MapStyle.CYBER_DARK,
    isNavigating: Boolean = false,
    currentStepInstruction: String? = null,
    isVoiceMuted: Boolean = false,
    isSpeaking: Boolean = false,
    currentStepIndex: Int = 0,
    totalStepsCount: Int = 0,
    onToggleVoiceMute: () -> Unit = {},
    onRepeatVoiceInstruction: () -> Unit = {},
    onSpeakAiBriefing: () -> Unit = {},
    onNextStep: () -> Unit = {},
    onPreviousStep: () -> Unit = {},
    onMapTap: (Double, Double) -> Unit = { _, _ -> },
    onStyleChange: (MapStyle) -> Unit = {}
) {
    // Map Viewport state (Center Lat/Lng and Zoom)
    var centerLat by remember { mutableStateOf(if (userLatitude != 0.0) userLatitude else 0.3476) }
    var centerLng by remember { mutableStateOf(if (userLongitude != 0.0) userLongitude else 32.5825) }
    var zoom by remember { mutableStateOf(14.5f) }
    var isFollowingUser by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val mapTileManager = remember { MapTileManager.getInstance(context) }
    var tileVersion by remember { mutableStateOf(0L) }
    var showOfflineSheet by remember { mutableStateOf(false) }
    val isOfflineMode by mapTileManager.isOfflineOnly.collectAsState()

    // Listen to background tile arrivals to re-render the canvas
    LaunchedEffect(Unit) {
        mapTileManager.tileUpdateSignal.collect {
            tileVersion++
        }
    }

    // Keep map centered on user when following is enabled
    LaunchedEffect(userLatitude, userLongitude, isFollowingUser) {
        if (isFollowingUser && userLatitude != 0.0 && userLongitude != 0.0) {
            centerLat = userLatitude
            centerLng = userLongitude
        }
    }

    // Pulse animation for GPS marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxSize()) {
        // Main Interactive Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val centerPixel = MapUtils.latLngToPixel(centerLat, centerLng, zoom)
                        val viewCenter = Offset(size.width / 2f, size.height / 2f)
                        val tappedWorldPixel = centerPixel + (tapOffset - viewCenter)
                        val (lat, lng) = MapUtils.pixelToLatLng(tappedWorldPixel.x, tappedWorldPixel.y, zoom)
                        onMapTap(lat, lng)
                    }
                }
                .pointerInput(zoom) {
                    detectDragGestures(
                        onDragStart = { isFollowingUser = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val centerPixel = MapUtils.latLngToPixel(centerLat, centerLng, zoom)
                            val newCenterPixel = centerPixel - dragAmount
                            val (newLat, newLng) = MapUtils.pixelToLatLng(newCenterPixel.x, newCenterPixel.y, zoom)
                            centerLat = newLat
                            centerLng = newLng
                        }
                    )
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val viewCenter = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val centerPixel = MapUtils.latLngToPixel(centerLat, centerLng, zoom)

            // 1. Draw Real Geographic Raster Map Tiles & Dynamic Terrain
            drawRasterMapTiles(
                tileManager = mapTileManager,
                style = mapStyle,
                size = size,
                zoom = zoom,
                centerLat = centerLat,
                centerLng = centerLng,
                tileVersion = tileVersion
            )

            // 2. Draw Route Polylines
            if (routePoints.size > 1) {
                val path = Path()
                var first = true
                routePoints.forEach { (lat, lng) ->
                    val pixel = MapUtils.latLngToPixel(lat, lng, zoom)
                    val screenPos = viewCenter + (pixel - centerPixel)
                    if (first) {
                        path.moveTo(screenPos.x, screenPos.y)
                        first = false
                    } else {
                        path.lineTo(screenPos.x, screenPos.y)
                    }
                }

                // Route Glow
                drawPath(
                    path = path,
                    color = when (mapStyle) {
                        MapStyle.CYBER_DARK -> ElectricCyan.copy(alpha = 0.35f)
                        MapStyle.SATELLITE -> Color(0xFF00E5FF).copy(alpha = 0.4f)
                        MapStyle.STREET -> Color(0xFF0284C7).copy(alpha = 0.3f)
                    },
                    style = Stroke(
                        width = 16f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Route Core Line
                drawPath(
                    path = path,
                    color = when (mapStyle) {
                        MapStyle.CYBER_DARK -> ElectricCyan
                        MapStyle.SATELLITE -> Color(0xFF38BDF8)
                        MapStyle.STREET -> Color(0xFF0284C7)
                    },
                    style = Stroke(
                        width = 7f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 3. Draw Markers
            markers.forEach { marker ->
                val pixel = MapUtils.latLngToPixel(marker.latitude, marker.longitude, zoom)
                val screenPos = viewCenter + (pixel - centerPixel)

                // Draw marker pin
                drawCircle(
                    color = marker.color,
                    radius = 12f,
                    center = screenPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = screenPos
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = 13f,
                    center = screenPos,
                    style = Stroke(width = 2f)
                )

                // Title label above marker
                if (marker.title.isNotBlank()) {
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(marker.title),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            background = Color(0xCC090D16)
                        )
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            screenPos.x - textLayoutResult.size.width / 2f,
                            screenPos.y - 36f
                        )
                    )
                }
            }

            // 4. Draw Current User GPS Location
            if (userLatitude != 0.0 && userLongitude != 0.0) {
                val userPixel = MapUtils.latLngToPixel(userLatitude, userLongitude, zoom)
                val userScreenPos = viewCenter + (userPixel - centerPixel)

                // Accuracy aura
                val accuracyRadiusPixels = (userAccuracy / (MapUtils.EARTH_RADIUS_METERS * 2 * Math.PI) *
                        (MapUtils.TILE_SIZE * 2.0.pow(zoom.toDouble()))).toFloat().coerceIn(16f, 120f)

                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    radius = accuracyRadiusPixels,
                    center = userScreenPos
                )

                // Pulsing wave
                drawCircle(
                    color = NeonTeal.copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = userScreenPos
                )

                // Heading beam indicator
                if (userHeading != 0f) {
                    val headingRad = Math.toRadians((userHeading - 90).toDouble())
                    val arrowEnd = userScreenPos + Offset(
                        (cos(headingRad) * 26f).toFloat(),
                        (sin(headingRad) * 26f).toFloat()
                    )
                    drawLine(
                        color = NeonTeal,
                        start = userScreenPos,
                        end = arrowEnd,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }

                // Core GPS Disc
                drawCircle(
                    color = Color.White,
                    radius = 10f,
                    center = userScreenPos
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = 7.5f,
                    center = userScreenPos
                )
            }
        }

        // Navigation Instruction Banner (if navigating)
        if (isNavigating && currentStepInstruction != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
                    .testTag("nav_instruction_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TechCardDark.copy(alpha = 0.96f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ElectricCyan))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Dynamic Turn / Navigation Icon with speaking indicator
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) ElectricCyan.copy(alpha = 0.25f) else SleekZinc800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.SpatialAudio else Icons.Default.Navigation,
                                contentDescription = "Turn Instruction",
                                tint = if (isSpeaking) ElectricCyan else NeonTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (totalStepsCount > 0) "STEP ${currentStepIndex + 1}/$totalStepsCount" else "VOICE GUIDANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                if (isSpeaking) {
                                    Text(
                                        text = "• SPEAKING...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = currentStepInstruction,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                        }

                        // Voice Controls Action Cluster
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Replay instruction button
                            IconButton(
                                onClick = onRepeatVoiceInstruction,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_voice_guidance_speaker")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak Turn Instruction",
                                    tint = if (isSpeaking) ElectricCyan else SleekZinc300,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Mute/Unmute voice button
                            IconButton(
                                onClick = onToggleVoiceMute,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_toggle_voice_mute")
                            ) {
                                Icon(
                                    imageVector = if (isVoiceMuted) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                                    contentDescription = if (isVoiceMuted) "Unmute Voice Guidance" else "Mute Voice Guidance",
                                    tint = if (isVoiceMuted) SosRed else SleekZinc400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Navigation Sub-Bar: AI Voice Briefing & Step Shifter
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SleekZinc800.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Voice Briefing Button
                        Surface(
                            onClick = onSpeakAiBriefing,
                            shape = RoundedCornerShape(10.dp),
                            color = SleekZinc800.copy(alpha = 0.8f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(ElectricCyan.copy(alpha = 0.5f))),
                            modifier = Modifier.testTag("btn_ai_voice_briefing")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                Text("AI Voice Briefing", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Step Stepper Navigation
                        if (totalStepsCount > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = onPreviousStep,
                                    enabled = currentStepIndex > 0,
                                    modifier = Modifier.size(28.dp).testTag("btn_nav_prev_step")
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Previous Step",
                                        tint = if (currentStepIndex > 0) SleekZinc300 else SleekZinc700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${currentStepIndex + 1}/$totalStepsCount",
                                    fontSize = 11.sp,
                                    color = SleekZinc400,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = onNextStep,
                                    enabled = currentStepIndex < totalStepsCount - 1,
                                    modifier = Modifier.size(28.dp).testTag("btn_nav_next_step")
                                ) {
                                    Icon(
                                        Icons.Default.ArrowForward,
                                        contentDescription = "Next Step",
                                        tint = if (currentStepIndex < totalStepsCount - 1) SleekZinc300 else SleekZinc700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Map Overlays and Floating Controls
        val currentContext = LocalContext.current
        val isDarkMap = mapStyle != MapStyle.STREET

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day / Night Mode Toggle Button (Time-Aware)
            FloatingActionButton(
                onClick = {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val isNightTime = hour < 6 || hour >= 18
                    if (isDarkMap) {
                        onStyleChange(MapStyle.STREET)
                        Toast.makeText(
                            currentContext,
                            "Light Map Mode enabled (Daylight contrast)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onStyleChange(MapStyle.CYBER_DARK)
                        val msg = if (isNightTime) {
                            "Dark Map Mode enabled (Night time ${String.format("%02d:00", hour)} — Optimized visibility)"
                        } else {
                            "Dark Map Mode enabled"
                        }
                        Toast.makeText(currentContext, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_toggle_day_night"),
                containerColor = if (isDarkMap) SleekZinc800 else SleekAmber.copy(alpha = 0.25f),
                contentColor = if (isDarkMap) SleekAmber else Color(0xFFD97706),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isDarkMap) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = if (isDarkMap) "Switch to Light Mode" else "Switch to Dark Night Mode",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Offline Maps Manager Button
            FloatingActionButton(
                onClick = { showOfflineSheet = true },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("btn_offline_maps"),
                containerColor = if (isOfflineMode) SleekAmber else TechCardDark,
                contentColor = if (isOfflineMode) Color.Black else SleekCyan,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Offline Maps & Storage",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Map Style Button
            FloatingActionButton(
                onClick = {
                    val next = when (mapStyle) {
                        MapStyle.CYBER_DARK -> MapStyle.STREET
                        MapStyle.STREET -> MapStyle.SATELLITE
                        MapStyle.SATELLITE -> MapStyle.CYBER_DARK
                    }
                    onStyleChange(next)
                },
                modifier = Modifier.size(44.dp).testTag("btn_map_style"),
                containerColor = TechCardDark,
                contentColor = ElectricCyan,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = "Change Map Style",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom In
            FloatingActionButton(
                onClick = { if (zoom < 19f) zoom += 1f },
                modifier = Modifier.size(44.dp).testTag("btn_zoom_in"),
                containerColor = TechCardDark,
                contentColor = TextPrimaryDark,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(22.dp))
            }

            // Zoom Out
            FloatingActionButton(
                onClick = { if (zoom > 3f) zoom -= 1f },
                modifier = Modifier.size(44.dp).testTag("btn_zoom_out"),
                containerColor = TechCardDark,
                contentColor = TextPrimaryDark,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(22.dp))
            }

            // Center GPS Button
            FloatingActionButton(
                onClick = {
                    if (userLatitude != 0.0 && userLongitude != 0.0) {
                        centerLat = userLatitude
                        centerLng = userLongitude
                        isFollowingUser = true
                    }
                },
                modifier = Modifier.size(48.dp).testTag("btn_center_gps"),
                containerColor = if (isFollowingUser) ElectricCyan else TechCardDark,
                contentColor = if (isFollowingUser) Color.Black else ElectricCyan,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isFollowingUser) Icons.Filled.MyLocation else Icons.Outlined.LocationSearching,
                    contentDescription = "Center on my location",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Offline Maps Bottom Sheet Modal
        if (showOfflineSheet) {
            OfflineMapsBottomSheet(
                onDismissRequest = { showOfflineSheet = false },
                currentLatitude = centerLat,
                currentLongitude = centerLng,
                currentMapStyle = mapStyle
            )
        }

        // GPS HUD Badge (Bottom-Left)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .testTag("gps_hud_badge"),
            shape = RoundedCornerShape(12.dp),
            color = TechCardDark.copy(alpha = 0.92f),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(TechCardBorder))
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
                        .background(if (isGpsActive) NeonTeal else SosRed)
                )
                Text(
                    text = if (isGpsActive) "GPS ±${userAccuracy.roundToInt()}m" else "GPS OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isGpsActive) NeonTeal else SosRed,
                    fontWeight = FontWeight.Bold
                )
                if (isOfflineMode) {
                    Text(
                        text = "• OFFLINE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "• ${String.format("%.4f, %.4f", centerLat, centerLng)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

private fun DrawScope.drawRasterMapTiles(
    tileManager: MapTileManager,
    style: MapStyle,
    size: Size,
    zoom: Float,
    centerLat: Double,
    centerLng: Double,
    tileVersion: Long
) {
    // 1. Draw solid background color according to current style
    val bgColor = when (style) {
        MapStyle.CYBER_DARK -> Color(0xFF0F172A)
        MapStyle.STREET -> Color(0xFFF8FAFC)
        MapStyle.SATELLITE -> Color(0xFF020617)
    }
    drawRect(color = bgColor)

    // Base integer zoom level for 256x256 tile resolution
    val baseZ = zoom.toInt().coerceIn(0, 19)
    val scale = 2.0.pow((zoom - baseZ).toDouble()).toFloat()
    val scaledTileSize = (256.0 * scale).toFloat()

    val centerPixel = MapUtils.latLngToPixel(centerLat, centerLng, zoom)
    val halfW = size.width / 2f
    val halfH = size.height / 2f

    val minWorldX = centerPixel.x - halfW
    val maxWorldX = centerPixel.x + halfW
    val minWorldY = centerPixel.y - halfH
    val maxWorldY = centerPixel.y + halfH

    val minBaseX = minWorldX / scale
    val maxBaseX = maxWorldX / scale
    val minBaseY = minWorldY / scale
    val maxBaseY = maxWorldY / scale

    val tileCount = 1 shl baseZ
    val minTileX = floor(minBaseX / 256.0).toInt().coerceIn(0, (tileCount - 1).coerceAtLeast(0))
    val maxTileX = floor(maxBaseX / 256.0).toInt().coerceIn(0, (tileCount - 1).coerceAtLeast(0))
    val minTileY = floor(minBaseY / 256.0).toInt().coerceIn(0, (tileCount - 1).coerceAtLeast(0))
    val maxTileY = floor(maxBaseY / 256.0).toInt().coerceIn(0, (tileCount - 1).coerceAtLeast(0))

    val gridStrokeColor = when (style) {
        MapStyle.CYBER_DARK -> Color(0xFF1E293B)
        MapStyle.STREET -> Color(0xFFE2E8F0)
        MapStyle.SATELLITE -> Color(0xFF1E293B).copy(alpha = 0.5f)
    }

    // Loop through all visible tiles and render
    for (tx in minTileX..maxTileX) {
        for (ty in minTileY..maxTileY) {
            val tileWorldX = tx * 256.0f * scale
            val tileWorldY = ty * 256.0f * scale

            val screenX = tileWorldX - minWorldX
            val screenY = tileWorldY - minWorldY

            val bitmap = tileManager.getTile(baseZ, tx, ty, style)

            if (bitmap != null) {
                drawImage(
                    image = bitmap,
                    dstOffset = IntOffset(screenX.roundToInt(), screenY.roundToInt()),
                    dstSize = IntSize(ceil(scaledTileSize).toInt() + 1, ceil(scaledTileSize).toInt() + 1)
                )
            } else {
                // Placeholder grid tile with stylish loading aesthetic while fetching/decoding
                drawRect(
                    color = when (style) {
                        MapStyle.CYBER_DARK -> Color(0xFF111C33)
                        MapStyle.STREET -> Color(0xFFF1F5F9)
                        MapStyle.SATELLITE -> Color(0xFF0F172A)
                    },
                    topLeft = Offset(screenX, screenY),
                    size = Size(scaledTileSize, scaledTileSize)
                )
                drawRect(
                    color = gridStrokeColor,
                    topLeft = Offset(screenX, screenY),
                    size = Size(scaledTileSize, scaledTileSize),
                    style = Stroke(width = 1f)
                )
            }
        }
    }
}
