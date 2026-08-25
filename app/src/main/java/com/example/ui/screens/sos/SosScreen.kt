package com.example.ui.screens.sos

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.SosEventEntity
import com.example.service.GpsLocationData
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    gpsLocation: GpsLocationData,
    batteryLevel: Int,
    isCountingDown: Boolean,
    countdownSeconds: Int,
    activeSosEvent: SosEventEntity?,
    contacts: List<EmergencyContactEntity>,
    onStartCountdown: () -> Unit,
    onCancelCountdown: () -> Unit,
    onTriggerImmediately: () -> Unit,
    onCancelActiveSos: (Long) -> Unit,
    onAddContact: (String, String, String, Boolean) -> Unit,
    onDeleteContact: (EmergencyContactEntity) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactRel by remember { mutableStateOf("Family") }
    var isPrimaryContact by remember { mutableStateOf(false) }

    // Pulsing animation for SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosScale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EMERGENCY ASSISTANCE SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekSosRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "GlobalCoreX SOS",
                    style = MaterialTheme.typography.titleLarge,
                    color = SleekZinc100,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "In danger? Press the SOS button. A confirmation countdown prevents accidental activation before emergency coordinates are broadcasted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SleekZinc400
                )
            }
        }

        // 2. SOS Button / Countdown / Active Emergency State
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sos_main_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeSosEvent != null) SleekSosRedMuted else SleekZinc900
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (activeSosEvent != null) SleekSosRed else SleekZinc800
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        // A. Countdown Active
                        isCountingDown -> {
                            Text(
                                text = "EMERGENCY ALERT TRIGGERING IN",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekSosRed,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(SleekSosRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$countdownSeconds",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = SleekSosRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCancelCountdown,
                                colors = ButtonDefaults.buttonColors(containerColor = SleekZinc100, contentColor = SleekZinc900),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_cancel_sos_countdown")
                            ) {
                                Text("CANCEL COUNTDOWN", fontWeight = FontWeight.Bold)
                            }
                        }

                        // B. Active SOS Broadcasted
                        activeSosEvent != null -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(SleekSosRed)
                                )
                                Text(
                                    text = "SOS EMERGENCY BROADCAST ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekSosRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Coordinates: ${String.format("%.5f, %.5f", activeSosEvent.latitude, activeSosEvent.longitude)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = SleekZinc100,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeSosEvent.address.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📍 ${activeSosEvent.address}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekZinc400,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Battery: ${activeSosEvent.batteryLevel}% • Status: ${activeSosEvent.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SleekGreen
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Share link button
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeSosEvent.shareUrl))
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "🚨 GLOBALCOREX EMERGENCY SOS ALERT!\nI require immediate assistance.\nLive coordinates: ${activeSosEvent.latitude}, ${activeSosEvent.longitude}\nTracking link: ${activeSosEvent.shareUrl}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Emergency SOS Alert"))
                                },
                                modifier = Modifier.fillMaxWidth().testTag("btn_share_sos_link"),
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = SleekZinc100, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Emergency Map Link", color = SleekZinc100)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onCancelActiveSos(activeSosEvent.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SleekZinc800, contentColor = SleekZinc100),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_resolve_sos")
                            ) {
                                Text("Cancel Emergency Alert", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // C. Idle Ready State
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(SleekSosRed, Color(0xFF990000))
                                        )
                                    )
                                    .clickable { onStartCountdown() }
                                    .testTag("btn_trigger_sos"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Sos,
                                        contentDescription = "SOS Button",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "HOLD / TAP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Current Position: ${if (gpsLocation.latitude != 0.0) "${String.format("%.4f, %.4f", gpsLocation.latitude, gpsLocation.longitude)} (±${gpsLocation.accuracy.toInt()}m)" else "Acquiring GPS fix..."}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SleekZinc400
                            )
                        }
                    }
                }
            }
        }

        // 3. Trusted Emergency Contacts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TRUSTED CONTACTS (${contacts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                FilledTonalButton(
                    onClick = { showAddContactDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = SleekZinc800, contentColor = SleekZinc100),
                    modifier = Modifier.testTag("btn_add_contact")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Contact")
                }
            }
        }

        if (contacts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No emergency contacts saved yet. Add trusted family/dispatch contacts above.", color = SleekZinc500, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(contacts) { contact ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("contact_card_${contact.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (contact.isPrimary) SleekSosRed.copy(alpha = 0.2f) else SleekBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (contact.isPrimary) Icons.Default.Shield else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (contact.isPrimary) SleekSosRed else SleekBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (contact.isPrimary) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SleekSosRed.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "PRIMARY",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SleekSosRed,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${contact.phone} • ${contact.relationship}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekZinc400
                                )
                            }
                        }

                        // Direct Communication Actions (Dialer & SMS)
                        Row {
                            IconButton(onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                context.startActivity(dialIntent)
                            }) {
                                Icon(Icons.Default.Phone, contentDescription = "Call Contact", tint = SleekGreen)
                            }
                            IconButton(onClick = {
                                val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${contact.phone}")).apply {
                                    putExtra("sms_body", "🚨 GLOBALCOREX EMERGENCY SOS ALERT: I require emergency assistance at coordinates ${gpsLocation.latitude}, ${gpsLocation.longitude}")
                                }
                                context.startActivity(smsIntent)
                            }) {
                                Icon(Icons.Default.Message, contentDescription = "SMS Contact", tint = SleekBlue)
                            }
                            IconButton(onClick = { onDeleteContact(contact) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Contact", tint = SleekZinc400)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add Emergency Contact", color = SleekZinc100, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Full Name", color = SleekZinc400) },
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
                        label = { Text("Phone / WhatsApp (with +country code)", color = SleekZinc400) },
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
                        value = contactRel,
                        onValueChange = { contactRel = it },
                        label = { Text("Relationship (Family, Police, HQ)", color = SleekZinc400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SleekZinc100,
                            unfocusedTextColor = SleekZinc100,
                            focusedBorderColor = SleekBlue,
                            unfocusedBorderColor = SleekZinc700
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isPrimaryContact,
                            onCheckedChange = { isPrimaryContact = it },
                            colors = CheckboxDefaults.colors(checkedColor = SleekSosRed)
                        )
                        Text("Set as Primary Emergency Responder", color = SleekZinc400, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                            onAddContact(contactName, contactPhone, contactRel, isPrimaryContact)
                            showAddContactDialog = false
                            contactName = ""
                            contactPhone = ""
                            isPrimaryContact = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Contact", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
