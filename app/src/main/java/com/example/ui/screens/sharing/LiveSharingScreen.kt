package com.example.ui.screens.sharing

import android.content.Intent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SharedSessionEntity
import com.example.service.GpsLocationData
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSharingScreen(
    activeSessions: List<SharedSessionEntity>,
    gpsLocation: GpsLocationData,
    onCreateSession: (name: String, phone: String, durationMin: Int, msg: String) -> Unit,
    onStopSharing: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableStateOf(60) }
    var customMessage by remember { mutableStateOf("Tracking my travel route via GlobalCoreX") }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Location Sharing", fontWeight = FontWeight.Bold, color = SleekZinc100) },
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
            // Header card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("live_sharing_header"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(24.dp))
                            Text("TEMPORARY LIVE SHARING", style = MaterialTheme.typography.labelSmall, color = SleekZinc500, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate temporary encrypted tracking links for trusted contacts. Sessions expire automatically and can be revoked anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SleekZinc400
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SleekBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_create_share_link")
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Live Sharing Link", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Active Sessions
            item {
                Text(
                    text = "ACTIVE SESSIONS (${activeSessions.filter { it.isActive }.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (activeSessions.none { it.isActive }) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No active live sharing sessions.", color = SleekZinc500, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(activeSessions.filter { it.isActive }) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("share_session_${session.id}"),
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
                                    Box(
                                        modifier = Modifier.size(10.dp).clip(CircleShape).background(SleekGreen)
                                    )
                                    Text(
                                        text = "Shared with: ${session.contactName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SleekGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Expires ${timeFormat.format(Date(session.expiresAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (session.customMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"${session.customMessage}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekZinc400
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val shareUrl = "https://globalcorex.nav/live/${session.token}?lat=${gpsLocation.latitude}&lng=${gpsLocation.longitude}"
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(shareUrl))
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "🌍 GlobalCoreX Live Location Sharing:\nFollow my live movement here: $shareUrl")
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Tracking Link"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = SleekZinc100, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Link", color = SleekZinc100)
                                }

                                Button(
                                    onClick = { onStopSharing(session.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekSosRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Revoke Access")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Session Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Live Sharing Link", color = SleekZinc100, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Recipient Name", color = SleekZinc400) },
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
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Phone Number", color = SleekZinc400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("Duration:", color = SleekZinc400, style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15 to "15m", 60 to "1h", 480 to "8h", 1440 to "24h").forEach { (min, label) ->
                            val isSelected = selectedDurationMinutes == min
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDurationMinutes = min },
                                label = { Text(label) },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank()) {
                            onCreateSession(contactName, contactPhone, selectedDurationMinutes, customMessage)
                            showCreateDialog = false
                            contactName = ""
                            contactPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Generate Link", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
