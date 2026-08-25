package com.example.ui.components.map

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.OfflineRegionEntity
import com.example.data.map.DownloadProgress
import com.example.data.map.MapTileManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class PresetCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lng: Double,
    val radiusKm: Double = 8.0
)

val PRESET_CITIES = listOf(
    PresetCity("Kampala Central", "Uganda", 0.3476, 32.5825, 10.0),
    PresetCity("Nairobi Metropolitan", "Kenya", -1.286389, 36.817223, 12.0),
    PresetCity("London Greater Area", "United Kingdom", 51.5074, -0.1278, 12.0),
    PresetCity("New York City", "USA", 40.7128, -74.0060, 12.0),
    PresetCity("Tokyo Core", "Japan", 35.6762, 139.6503, 12.0),
    PresetCity("Paris Central", "France", 48.8566, 2.3522, 10.0),
    PresetCity("San Francisco Bay", "USA", 37.7749, -122.4194, 12.0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapsBottomSheet(
    onDismissRequest: () -> Unit,
    currentLatitude: Double,
    currentLongitude: Double,
    currentMapStyle: MapStyle = MapStyle.STREET,
    onNavigateToLocation: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val tileManager = remember { MapTileManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val offlineRegions by tileManager.getAllOfflineRegions().collectAsState(initial = emptyList())
    val totalCachedTiles by tileManager.getTotalCachedTileCount().collectAsState(initial = 0)
    val totalCachedBytes by tileManager.getTotalCachedBytes().collectAsState(initial = 0L)
    val isOfflineMode by tileManager.isOfflineOnly.collectAsState()
    val activeDownload by tileManager.activeDownload.collectAsState()

    var selectedRadiusKm by remember { mutableStateOf(10.0) }
    var customRegionName by remember { mutableStateOf("Current Area (${selectedRadiusKm.toInt()}km)") }
    var selectedStyle by remember { mutableStateOf(currentMapStyle) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = SleekZinc900,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp),
                shape = CircleShape,
                color = SleekZinc700
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
                .testTag("offline_maps_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SleekBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = SleekBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Offline Maps & Cache",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Download regions for zero-data GPS navigation",
                            style = MaterialTheme.typography.bodySmall,
                            color = SleekZinc400
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("btn_close_offline_sheet")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SleekZinc400)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage & Offline Mode Switch Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SleekZinc800.copy(alpha = 0.7f),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SleekZinc700))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CACHED TILES IN ROOM DB",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekZinc400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp
                        )
                        val sizeMb = totalCachedBytes / (1024.0 * 1024.0)
                        Text(
                            text = "${String.format("%.1f", sizeMb)} MB • $totalCachedTiles tiles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isOfflineMode) "Offline Only" else "Live + Cache",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOfflineMode) SleekAmber else SleekZinc300,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isOfflineMode,
                            onCheckedChange = { tileManager.setOfflineMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SleekAmber,
                                uncheckedThumbColor = SleekZinc400,
                                uncheckedTrackColor = SleekZinc700
                            ),
                            modifier = Modifier.testTag("switch_offline_mode")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Download Progress Card (if any download is in progress)
            if (activeDownload != null) {
                val download = activeDownload!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("active_download_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekBlue.copy(alpha = 0.15f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SleekBlue.copy(alpha = 0.5f)))
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = SleekBlue
                                )
                                Text(
                                    text = "Downloading: ${download.regionName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { tileManager.cancelActiveDownload() },
                                modifier = Modifier.size(24.dp).testTag("btn_cancel_download")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = SleekZinc400, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val progressFraction = if (download.totalTiles > 0) {
                            (download.downloadedTiles.toFloat() / download.totalTiles).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = SleekBlue,
                            trackColor = SleekZinc700
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val mbDownloaded = download.totalBytes / (1024.0 * 1024.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progressFraction * 100).roundToInt()}% (${download.downloadedTiles}/${download.totalTiles} tiles)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc300
                            )
                            Text(
                                text = "${String.format("%.2f", mbDownloaded)} MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Quick Download Current Location
                item {
                    Text(
                        text = "DOWNLOAD CURRENT REGION",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekBlue,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SleekZinc800.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SleekZinc700))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val activeLat = if (currentLatitude != 0.0) currentLatitude else 0.3476
                            val activeLng = if (currentLongitude != 0.0) currentLongitude else 32.5825

                            Text(
                                text = "Center: ${String.format("%.4f, %.4f", activeLat, activeLng)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc100,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Select Coverage Radius:",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc400
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5.0 to "5 km", 10.0 to "10 km", 20.0 to "20 km", 35.0 to "35 km").forEach { (rad, label) ->
                                    val isSelected = selectedRadiusKm == rad
                                    Surface(
                                        onClick = {
                                            selectedRadiusKm = rad
                                            customRegionName = "Current Area (${rad.toInt()}km)"
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) SleekBlue else SleekZinc700,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) Color.White else SleekZinc300,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val latOffset = selectedRadiusKm / 111.0
                                    val lngOffset = selectedRadiusKm / (111.0 * Math.cos(Math.toRadians(activeLat)).coerceAtLeast(0.1))

                                    tileManager.startOfflineRegionDownload(
                                        name = customRegionName,
                                        minLat = activeLat - latOffset,
                                        minLng = activeLng - lngOffset,
                                        maxLat = activeLat + latOffset,
                                        maxLng = activeLng + lngOffset,
                                        minZoom = 11,
                                        maxZoom = 15,
                                        style = selectedStyle
                                    )
                                    Toast.makeText(context, "Started downloading $customRegionName", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_download_current_region"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Region (~11-15 Zoom)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 2: Preset Global Regions
                item {
                    Text(
                        text = "POPULAR METROPOLITAN PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PRESET_CITIES.forEach { city ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SleekZinc800.copy(alpha = 0.5f),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SleekZinc700))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationCity,
                                            contentDescription = null,
                                            tint = SleekCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = city.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${city.country} • ${city.radiusKm.toInt()}km core",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SleekZinc400
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val latOffset = city.radiusKm / 111.0
                                            val lngOffset = city.radiusKm / (111.0 * Math.cos(Math.toRadians(city.lat)).coerceAtLeast(0.1))

                                            tileManager.startOfflineRegionDownload(
                                                name = "${city.name} (${city.country})",
                                                minLat = city.lat - latOffset,
                                                minLng = city.lng - lngOffset,
                                                maxLat = city.lat + latOffset,
                                                maxLng = city.lng + lngOffset,
                                                minZoom = 11,
                                                maxZoom = 15,
                                                style = selectedStyle
                                            )
                                            Toast.makeText(context, "Started downloading ${city.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekZinc700)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save", fontSize = 12.sp, color = SleekZinc100)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Saved Offline Regions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SAVED OFFLINE REGIONS (${offlineRegions.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SleekEmerald,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (offlineRegions.isNotEmpty() || totalCachedTiles > 0) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        tileManager.clearAllTileCache()
                                        Toast.makeText(context, "All offline cache cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Clear Cache", color = SleekSosRed, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (offlineRegions.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SleekZinc800.copy(alpha = 0.3f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(SleekZinc800))
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = SleekZinc500,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "No saved offline map regions yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekZinc400
                                    )
                                    Text(
                                        text = "Downloaded maps will be stored locally in Room database for offline use.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc500
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(offlineRegions) { region ->
                        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                        val formattedDate = remember(region.createdAt) { dateFormat.format(Date(region.createdAt)) }
                        val regionMb = region.sizeBytes / (1024.0 * 1024.0)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SleekZinc800.copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = SolidColor(if (region.isCompleted) SleekEmerald.copy(alpha = 0.4f) else SleekAmber.copy(alpha = 0.4f))
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (region.isCompleted) Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = if (region.isCompleted) SleekEmerald else SleekAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = region.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${region.downloadedTiles}/${region.totalTiles} tiles • ${String.format("%.1f", regionMb)} MB • $formattedDate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SleekZinc400
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            tileManager.deleteOfflineRegion(region.id)
                                            Toast.makeText(context, "Deleted ${region.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_delete_region_${region.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Region", tint = SleekSosRed)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
