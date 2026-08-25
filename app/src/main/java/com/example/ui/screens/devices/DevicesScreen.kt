package com.example.ui.screens.devices

import androidx.compose.foundation.background
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
import com.example.data.local.entity.DeviceEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    devices: List<DeviceEntity>,
    onAddDevice: (name: String, model: String) -> Unit,
    onDeleteDevice: (DeviceEntity) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var devName by remember { mutableStateOf("") }
    var devModel by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Devices", fontWeight = FontWeight.Bold, color = SleekZinc100) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SleekZinc100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekZinc950)
            )
        },
        containerColor = SleekZinc950
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("devices_header_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(24.dp))
                            Text("AUTHORIZED TRACKING HARDWARE", style = MaterialTheme.typography.labelSmall, color = SleekZinc500, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Manage your active phone and paired GPS hardware modules registered under your GlobalCoreX account.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekZinc400
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_pair_device")
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pair New Tracker / Companion", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "REGISTERED HARDWARE (${devices.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(devices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("device_item_${device.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (device.isThisDevice) SleekBlue else SleekZinc800
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (device.isThisDevice) SleekBlue.copy(alpha = 0.2f) else SleekZinc800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (device.isThisDevice) Icons.Default.Smartphone else Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = if (device.isThisDevice) SleekBlue else SleekZinc400,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = device.deviceName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SleekZinc100,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (device.isThisDevice) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = SleekBlue.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "THIS PHONE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SleekBlue,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = device.model,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekZinc400
                                    )
                                }
                            }

                            if (!device.isThisDevice) {
                                IconButton(onClick = { onDeleteDevice(device) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Unlink", tint = SleekZinc400)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(if (device.isOnline) SleekGreen else SleekZinc600)
                                )
                                Text(
                                    text = if (device.isOnline) "ONLINE" else "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (device.isOnline) SleekGreen else SleekZinc500,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Battery: ${device.lastBatteryLevel}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (device.lastBatteryLevel > 20) SleekBlue else SleekSosRed
                            )
                            Text(
                                text = "Updated ${dateFormat.format(Date(device.lastUpdateTime))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekZinc500
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Device Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Pair GPS Hardware Tracker", color = SleekZinc100, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = devName,
                        onValueChange = { devName = it },
                        label = { Text("Tracker / Device Alias", color = SleekZinc400) },
                        placeholder = { Text("e.g. Vehicle OBD GPS, Bike Beacon", color = SleekZinc600) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = devModel,
                        onValueChange = { devModel = it },
                        label = { Text("Hardware Model / ID", color = SleekZinc400) },
                        placeholder = { Text("e.g. GX-Tracker-4G #829", color = SleekZinc600) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (devName.isNotBlank()) {
                            onAddDevice(devName, devModel.ifBlank { "GX-GPS-Companion" })
                            showAddDialog = false
                            devName = ""
                            devModel = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Pair Device", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
