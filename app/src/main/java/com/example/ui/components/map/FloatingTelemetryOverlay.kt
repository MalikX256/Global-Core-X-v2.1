package com.example.ui.components.map

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.GpsLocationData
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class SpeedUnit(val label: String, val factor: Float) {
    KMH("KM/H", 1.0f),
    MPH("MPH", 0.621371f)
}

enum class AltitudeUnit(val label: String, val factor: Double) {
    METERS("m", 1.0),
    FEET("ft", 3.28084)
}

/**
 * Floating UI overlay on the Map screen displaying real-time speed and altitude
 * sourced directly from the FusedLocationProviderClient live updates stream.
 */
@Composable
fun FloatingTelemetryOverlay(
    gpsLocation: GpsLocationData,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var selectedSpeedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var selectedAltitudeUnit by remember { mutableStateOf(AltitudeUnit.METERS) }

    val rawSpeedKmh = gpsLocation.speedKmh
    val convertedSpeed = (rawSpeedKmh * selectedSpeedUnit.factor).coerceAtLeast(0f)
    val formattedSpeed = if (convertedSpeed < 10) String.format("%.1f", convertedSpeed) else convertedSpeed.roundToInt().toString()

    val rawAltitudeMeters = gpsLocation.altitude
    val convertedAltitude = rawAltitudeMeters * selectedAltitudeUnit.factor
    val formattedAltitude = if (rawAltitudeMeters != 0.0) {
        "${String.format("%,.0f", convertedAltitude)} ${selectedAltitudeUnit.label}"
    } else {
        "-- ${selectedAltitudeUnit.label}"
    }

    // Dynamic speed category color & label
    val (speedColor, speedCategory) = remember(rawSpeedKmh) {
        when {
            rawSpeedKmh < 1.0f -> Pair(SleekZinc400, "STOPPED")
            rawSpeedKmh < 7.0f -> Pair(SleekEmerald, "WALKING")
            rawSpeedKmh < 25.0f -> Pair(SleekCyan, "CYCLING")
            rawSpeedKmh < 60.0f -> Pair(SleekBlue, "URBAN")
            rawSpeedKmh < 100.0f -> Pair(SleekAmber, "CRUISING")
            else -> Pair(SleekSosRed, "HIGH SPEED")
        }
    }

    // Cardinal Heading string (N, NE, E, SE, S, SW, W, NW)
    val cardinalHeading = remember(gpsLocation.heading) {
        val deg = ((gpsLocation.heading % 360) + 360) % 360
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((deg + 22.5) / 45.0).toInt() % 8
        "${directions[index]} ${deg.roundToInt()}°"
    }

    // Pulsing indicator for active live GPS data stream
    val infiniteTransition = rememberInfiniteTransition(label = "gpsLivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveGpsAlpha"
    )

    Surface(
        modifier = modifier
            .padding(top = 80.dp, end = 14.dp)
            .widthIn(min = 160.dp, max = 220.dp)
            .testTag("floating_telemetry_overlay"),
        shape = RoundedCornerShape(20.dp),
        color = SleekZinc900.copy(alpha = 0.92f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    SleekZinc700.copy(alpha = 0.8f),
                    SleekBlue.copy(alpha = 0.35f),
                    SleekZinc800.copy(alpha = 0.6f)
                )
            )
        ),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header: Live GPS badge + Minimize/Expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (gpsLocation.isGpsActive) SleekEmerald.copy(alpha = pulseAlpha) else SleekSosRed
                            )
                    )
                    Text(
                        text = if (gpsLocation.isGpsActive) "LIVE TELEMETRY" else "NO GPS FIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gpsLocation.isGpsActive) SleekZinc300 else SleekSosRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse Overlay" else "Expand Overlay",
                    tint = SleekZinc400,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("btn_toggle_telemetry_expand")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Primary Metric: Real-Time SPEED Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = speedColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "SPEED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = formattedSpeed,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            modifier = Modifier.testTag("speed_overlay_value")
                        )
                        Surface(
                            onClick = {
                                selectedSpeedUnit = if (selectedSpeedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = SleekZinc800,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .testTag("telemetry_unit_toggle")
                        ) {
                            Text(
                                text = selectedSpeedUnit.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Speed Category Tag Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = speedColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(speedColor.copy(alpha = 0.4f))),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = speedCategory,
                        color = speedColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Expanded Details Section: Altitude & Heading & Accuracy
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = SleekZinc800.copy(alpha = 0.7f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Secondary Metric: Real-Time ALTITUDE Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SleekEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terrain,
                                    contentDescription = "Altitude",
                                    tint = SleekEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ALTITUDE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc400,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formattedAltitude,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("altitude_overlay_value")
                                )
                            }
                        }

                        // Altitude unit toggle button
                        Surface(
                            onClick = {
                                selectedAltitudeUnit = if (selectedAltitudeUnit == AltitudeUnit.METERS) AltitudeUnit.FEET else AltitudeUnit.METERS
                            },
                            shape = RoundedCornerShape(4.dp),
                            color = SleekZinc800
                        ) {
                            Text(
                                text = selectedAltitudeUnit.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Compact Compass Heading & GPS Accuracy Sub-row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Heading
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Heading",
                                tint = SleekCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = cardinalHeading,
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc300,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Accuracy
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = "Accuracy",
                                tint = if (gpsLocation.accuracy <= 10f) SleekEmerald else SleekAmber,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "±${gpsLocation.accuracy.roundToInt()}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc400,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
