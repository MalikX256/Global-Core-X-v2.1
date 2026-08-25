package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.ActiveTripState
import com.example.service.GpsLocationData
import com.example.ui.components.map.MapUtils
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    gpsLocation: GpsLocationData,
    batteryLevel: Int,
    isBatterySaverActive: Boolean = false,
    trackingState: ActiveTripState,
    todayTripsCount: Int,
    todayDistanceMeters: Double,
    userName: String,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToRoutes: () -> Unit,
    onNavigateToAiRoute: () -> Unit,
    onNavigateToAiSupport: () -> Unit = {},
    onNavigateToOfflineMaps: () -> Unit = {},
    onNavigateToShare: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    // Pulse animation for live GPS status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Welcome back"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SleekBlack)
            .padding(horizontal = 18.dp)
            .testTag("home_dashboard_screen"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Sleek Status & Telemetry Header Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPS Live Status Pill
                Surface(
                    shape = CircleShape,
                    color = SleekZinc900,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (gpsLocation.isGpsActive) SleekGreen.copy(alpha = 0.4f) else SleekZinc800
                        )
                    ),
                    modifier = Modifier.testTag("pill_gps_status")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (gpsLocation.isGpsActive) SleekGreen.copy(alpha = pulseAlpha) else SleekSosRed)
                        )
                        Text(
                            text = if (gpsLocation.isGpsActive) "GPS ONLINE" else "GPS STANDBY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (gpsLocation.isGpsActive) SleekZinc100 else SleekZinc400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Battery Optimization Pill
                Surface(
                    shape = CircleShape,
                    color = if (isBatterySaverActive) SleekAmber.copy(alpha = 0.15f) else SleekZinc900,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isBatterySaverActive) SleekAmber else SleekZinc800
                        )
                    ),
                    modifier = Modifier.testTag("pill_battery_status")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (batteryLevel > 50) Icons.Default.BatteryFull else if (batteryLevel > 20) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = if (isBatterySaverActive || batteryLevel <= 20) SleekAmber else SleekGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // GPS Fix Precision Pill
                Surface(
                    shape = CircleShape,
                    color = SleekZinc900,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "PRECISION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                        Text(
                            text = if (gpsLocation.isGpsActive) "±${gpsLocation.accuracy.toInt()}m" else "Searching...",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (gpsLocation.accuracy <= 10f && gpsLocation.isGpsActive) SleekCyan else SleekZinc200,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 2. Hero Navigation & Telemetry Dashboard Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_header_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative circular gradient halo
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(SleekBlue.copy(alpha = 0.12f), Color.Transparent)
                                )
                            )
                    )

                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$greetingText, ${userName.ifBlank { "Explorer" }}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (gpsLocation.latitude != 0.0)
                                        "${String.format("%.5f", gpsLocation.latitude)}° N • ${String.format("%.5f", gpsLocation.longitude)}° E"
                                    else "Acquiring high-accuracy constellation lock...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekZinc400,
                                    fontSize = 12.sp
                                )
                            }

                            // Quick Map View Link
                            IconButton(
                                onClick = onNavigateToMap,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SleekBlue.copy(alpha = 0.15f))
                                    .border(1.dp, SleekBlue.copy(alpha = 0.35f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Open Map",
                                    tint = SleekBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Inset Real-Time Telemetry Stats (Speed, Altitude, Distance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Speed Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SleekBlack)
                                    .border(1.dp, SleekZinc800, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "LIVE SPEED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc500,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (trackingState.isTracking)
                                                "${trackingState.currentSpeedKmh.toInt()}"
                                            else "${gpsLocation.speedKmh.toInt()}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = SleekZinc100,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "km/h",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekZinc500,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Altitude Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SleekBlack)
                                    .border(1.dp, SleekZinc800, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ALTITUDE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc500,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${gpsLocation.altitude.toInt()}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = SleekZinc100,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekZinc500,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Daily Distance Card
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SleekBlack)
                                    .border(1.dp, SleekZinc800, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (trackingState.isTracking) "ACTIVE TRIP" else "TODAY'S VOYAGE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (trackingState.isTracking) SleekGreen else SleekZinc500,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (trackingState.isTracking)
                                                MapUtils.formatDistance(trackingState.distanceMeters).replace(" km", "").replace(" m", "")
                                            else String.format("%.1f", todayDistanceMeters / 1000.0),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = SleekZinc100,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (trackingState.isTracking && trackingState.distanceMeters < 1000) "m" else "km",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SleekZinc500,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Primary Tracking Action & AI Help Spotlight Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tracking Trigger Card
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .clickable {
                            if (trackingState.isTracking) onStopTracking() else onStartTracking()
                        }
                        .testTag("btn_toggle_tracking"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (trackingState.isTracking) SleekSosRedMuted else SleekGreenMuted
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (trackingState.isTracking) SleekSosRed.copy(alpha = 0.4f) else SleekGreenBorder
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (trackingState.isTracking) SleekSosRed else SleekGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (trackingState.isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (trackingState.isTracking) "Stop Recording" else "Start Live Track",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (trackingState.isTracking) SleekSosRed else SleekGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (trackingState.isTracking)
                                MapUtils.formatDuration(trackingState.durationSeconds)
                            else "Foreground FusedLocation",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc400,
                            fontSize = 11.sp
                        )
                    }
                }

                // AI Help & Support Agent Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToAiSupport() }
                        .testTag("quick_action_ai_support"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekBlueMuted),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekBlueBorder)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(SleekBlue, SleekCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI Support Agent",
                            style = MaterialTheme.typography.titleSmall,
                            color = SleekBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "24/7 Gemini Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc400,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 4. Quick Navigation Grid (Map, Offline Maps, AI Routes, Live Beacon)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "EXPLORATION & TOOLS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardToolCard(
                        title = "Interactive Map",
                        subtitle = "Live Tile Canvas",
                        icon = Icons.Default.Map,
                        tint = SleekBlue,
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f).testTag("tool_map")
                    )
                    DashboardToolCard(
                        title = "Offline Maps",
                        subtitle = "Room DB Caching",
                        icon = Icons.Default.DownloadForOffline,
                        tint = SleekCyan,
                        onClick = onNavigateToOfflineMaps,
                        modifier = Modifier.weight(1f).testTag("tool_offline_maps")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardToolCard(
                        title = "Route Planner",
                        subtitle = "Smart Multi-Modal",
                        icon = Icons.Default.AltRoute,
                        tint = SleekEmerald,
                        onClick = onNavigateToRoutes,
                        modifier = Modifier.weight(1f).testTag("tool_routes")
                    )
                    DashboardToolCard(
                        title = "Live Beacon",
                        subtitle = "Real-Time Sharing",
                        icon = Icons.Default.ShareLocation,
                        tint = SleekOrange,
                        onClick = onNavigateToShare,
                        modifier = Modifier.weight(1f).testTag("tool_share")
                    )
                }
            }
        }

        // 5. Recent Activity & Logging Snapshot
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRoutes() }
                    .testTag("recent_trip_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekZinc800),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = SleekZinc300,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PERSISTED SESSIONS (ROOM DB)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (todayTripsCount > 0)
                                    "$todayTripsCount voyages logged today • ${String.format("%.1f", todayDistanceMeters / 1000.0)} km"
                                else "All routes safely preserved locally",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc100,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = SleekZinc500
                    )
                }
            }
        }

        // 6. Prominent Full-Width SOS Emergency Action Bar
        item {
            Button(
                onClick = onNavigateToSos,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekSosRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = SleekSosRedGlow)
                    .testTag("quick_action_sos")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "EMERGENCY SOS BEACON",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SleekZinc900),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc400,
                    fontSize = 11.sp
                )
            }
        }
    }
}
