package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.UserPreferencesEntity
import com.example.data.map.MapTileManager
import com.example.data.map.DownloadProgress
import com.example.security.SecurityUtils
import com.example.ui.components.map.MapStyle
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsProfileScreen(
    currentUser: UserEntity?,
    preferences: UserPreferencesEntity,
    emergencyContacts: List<EmergencyContactEntity> = emptyList(),
    batteryLevel: Int = 100,
    onUpdateProfile: (fullName: String, username: String, phone: String, country: String, avatarUrl: String) -> Unit,
    onChangePassword: (currentPass: String, newPass: String, onComplete: (Boolean, String?) -> Unit) -> Unit,
    onAddEmergencyContact: (name: String, phone: String, email: String, relationship: String, isPrimary: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteEmergencyContact: (contactId: Long) -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdatePreferences: (UserPreferencesEntity) -> Unit,
    onTestVoiceGuidance: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mapTileManager = remember { MapTileManager.getInstance(context) }
    val isOfflineOnly by mapTileManager.isOfflineOnly.collectAsState()
    val activeDownload by mapTileManager.activeDownload.collectAsState()
    val totalCachedTiles by mapTileManager.getTotalCachedTileCount().collectAsState(initial = 0)
    val totalCachedBytes by mapTileManager.getTotalCachedBytes().collectAsState(initial = 0L)
    val offlineRegions by mapTileManager.getAllOfflineRegions().collectAsState(initial = emptyList())

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showDownloadRegionDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var customRegionName by remember { mutableStateOf("Global Region") }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }

    val formattedDate = remember(currentUser?.createdAt) {
        val time = currentUser?.createdAt ?: System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(time))
    }

    val avatarEmoji = when (currentUser?.avatarUrl) {
        "avatar_2" -> "⚡"
        "avatar_3" -> "🛰️"
        "avatar_4" -> "🛡️"
        "avatar_5" -> "🧭"
        "avatar_6" -> "🌐"
        else -> "🔷"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Settings",
                        fontWeight = FontWeight.Bold,
                        color = SleekZinc100
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Profile Details Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_profile_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SleekBlue.copy(alpha = 0.2f))
                                    .border(1.5.dp, SleekBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarEmoji,
                                    fontSize = 28.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = currentUser?.displayName?.ifBlank { "Malik-X Explorer" } ?: "Malik-X Explorer",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentUser?.isEmailVerified == true) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SleekGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "VERIFIED",
                                                color = SleekGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "@${currentUser?.username?.ifBlank { "malik_x" } ?: "malik_x"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekBlue,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = currentUser?.email ?: "explorer@globalcore.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SleekZinc400
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekZinc800)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Metadata Grid: Phone, Country, Member Since
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ProfileDetailRow(
                                icon = Icons.Outlined.Phone,
                                label = "Phone",
                                value = currentUser?.phone?.ifBlank { "+256 750 985651" } ?: "+256 750 985651"
                            )
                            ProfileDetailRow(
                                icon = Icons.Outlined.Public,
                                label = "Country / Region",
                                value = currentUser?.country?.ifBlank { "Global" } ?: "Global"
                            )
                            ProfileDetailRow(
                                icon = Icons.Outlined.CalendarMonth,
                                label = "Member Since",
                                value = formattedDate
                            )
                            ProfileDetailRow(
                                icon = Icons.Outlined.Fingerprint,
                                label = "User ID",
                                value = currentUser?.id ?: "user_gcx_default",
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("User ID", currentUser?.id ?: ""))
                                    Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = SleekZinc800)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Account Actions: Edit Profile, Change Password
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_edit_profile"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SleekZinc800,
                                    contentColor = SleekZinc100
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showChangePasswordDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_change_password"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SleekZinc800,
                                    contentColor = SleekZinc100
                                )
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sign Out & Delete Account
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sign_out"),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700)
                                )
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = SleekZinc300, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", color = SleekZinc200, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showDeleteAccountDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_delete_account"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SleekSosRedMuted,
                                    contentColor = SleekSosRed
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Location Privacy Controls (CRITICAL REQUIREMENT)
            item {
                Text(
                    text = "LOCATION PRIVACY & PERMISSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_privacy_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PrivacyToggleRow(
                            icon = Icons.Default.GpsFixed,
                            iconTint = SleekBlue,
                            title = "Location Tracking",
                            description = "Enable GPS positioning for active navigation & telemetry",
                            checked = preferences.locationTrackingEnabled,
                            onCheckedChange = { onUpdatePreferences(preferences.copy(locationTrackingEnabled = it)) },
                            tag = "toggle_location_tracking"
                        )

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        PrivacyToggleRow(
                            icon = Icons.Default.History,
                            iconTint = SleekPurple,
                            title = "Location History",
                            description = "Keep encrypted on-device log of traveled routes and trips",
                            checked = preferences.locationHistoryEnabled,
                            onCheckedChange = { onUpdatePreferences(preferences.copy(locationHistoryEnabled = it)) },
                            tag = "toggle_location_history"
                        )

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        PrivacyToggleRow(
                            icon = Icons.Default.ShareLocation,
                            iconTint = SleekCyan,
                            title = "Live Location Sharing",
                            description = "Generate tokenized peer links for live trip monitoring",
                            checked = preferences.liveSharingEnabled,
                            onCheckedChange = { onUpdatePreferences(preferences.copy(liveSharingEnabled = it)) },
                            tag = "toggle_live_sharing"
                        )

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        PrivacyToggleRow(
                            icon = Icons.Default.Route,
                            iconTint = SleekGreen,
                            title = "Trip Recording",
                            description = "Record breadcrumbs and elevation metrics during active journeys",
                            checked = preferences.tripRecordingEnabled,
                            onCheckedChange = { onUpdatePreferences(preferences.copy(tripRecordingEnabled = it)) },
                            tag = "toggle_trip_recording"
                        )

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        PrivacyToggleRow(
                            icon = Icons.Default.EmergencyShare,
                            iconTint = SleekSosRed,
                            title = "SOS Location Sharing",
                            description = "Transmit exact coordinates to emergency contacts upon SOS activation",
                            checked = preferences.sosLocationSharingEnabled,
                            onCheckedChange = { onUpdatePreferences(preferences.copy(sosLocationSharingEnabled = it)) },
                            tag = "toggle_sos_sharing"
                        )
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
                    Text(
                        text = "TRUSTED EMERGENCY CONTACTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = { showAddContactDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("btn_add_emergency_contact")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Contact", color = SleekBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (emergencyContacts.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SleekZinc950)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ContactPhone, contentDescription = null, tint = SleekZinc500)
                                Column {
                                    Text("No custom contacts added", style = MaterialTheme.typography.bodySmall, color = SleekZinc300, fontWeight = FontWeight.Bold)
                                    Text("GlobalCore-X SOS will alert Malik-X default responder (+256750985651)", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                }
                            }
                        } else {
                            emergencyContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SleekZinc950)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (contact.isPrimary) SleekSosRed.copy(alpha = 0.2f) else SleekZinc800),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (contact.isPrimary) SleekSosRed else SleekZinc400,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(contact.name, style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.Bold)
                                                if (contact.isPrimary) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = SleekSosRed.copy(alpha = 0.2f)) {
                                                        Text("PRIMARY", color = SleekSosRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            Text("${contact.relationship} • ${contact.phone}", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                                        }
                                    }

                                    IconButton(onClick = { onDeleteEmergencyContact(contact.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete contact", tint = SleekZinc500)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Navigation & Display Preferences
            item {
                Text(
                    text = "NAVIGATION & DISPLAY PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Distance Unit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Distance Units", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                Text("Kilometers (km) or Miles (mi)", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = preferences.distanceUnit == "km",
                                    onClick = { onUpdatePreferences(preferences.copy(distanceUnit = "km")) },
                                    label = { Text("KM") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekZinc100,
                                        selectedLabelColor = SleekZinc900,
                                        containerColor = SleekZinc800,
                                        labelColor = SleekZinc300
                                    )
                                )
                                FilterChip(
                                    selected = preferences.distanceUnit == "miles",
                                    onClick = { onUpdatePreferences(preferences.copy(distanceUnit = "miles")) },
                                    label = { Text("Miles") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SleekZinc100,
                                        selectedLabelColor = SleekZinc900,
                                        containerColor = SleekZinc800,
                                        labelColor = SleekZinc300
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        // SOS Countdown Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SOS Safety Countdown", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                Text("Buffer before emergency broadcast", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 5, 10).forEach { sec ->
                                    val isSelected = preferences.emergencyCountdownSec == sec
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdatePreferences(preferences.copy(emergencyCountdownSec = sec)) },
                                        label = { Text("${sec}s") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SleekSosRed,
                                            selectedLabelColor = Color.White,
                                            containerColor = SleekZinc800,
                                            labelColor = SleekZinc300
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        // Live GPS Refresh Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GPS Refresh Rate", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                Text("Update frequency during live tracking", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1, 3, 5).forEach { rate ->
                                    val isSelected = preferences.liveTrackingIntervalSec == rate
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdatePreferences(preferences.copy(liveTrackingIntervalSec = rate)) },
                                        label = { Text("${rate}s") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SleekGreen,
                                            selectedLabelColor = Color.Black,
                                            containerColor = SleekZinc800,
                                            labelColor = SleekZinc300
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Battery & Power Management
            item {
                Text(
                    text = "BATTERY & POWER MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                val isSaverActive = preferences.batterySaverEnabled && batteryLevel <= 20
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("battery_saver_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isSaverActive) SleekAmber else SleekZinc800
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSaverActive) SleekAmber.copy(alpha = 0.15f) else SleekZinc800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (batteryLevel <= 20) Icons.Default.BatteryAlert else Icons.Default.BatterySaver,
                                        contentDescription = "Battery Saver",
                                        tint = if (isSaverActive) SleekAmber else SleekZinc300,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text("Battery Saver", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                    Text("Reduce GPS frequency below 20% battery", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                                }
                            }

                            Switch(
                                checked = preferences.batterySaverEnabled,
                                onCheckedChange = { isChecked ->
                                    onUpdatePreferences(preferences.copy(batterySaverEnabled = isChecked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SleekBlack,
                                    checkedTrackColor = SleekGreen,
                                    uncheckedThumbColor = SleekZinc400,
                                    uncheckedTrackColor = SleekZinc800
                                ),
                                modifier = Modifier.testTag("battery_saver_toggle")
                            )
                        }

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        // Status Info Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSaverActive) SleekAmber.copy(alpha = 0.1f) else SleekZinc950)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!preferences.batterySaverEnabled) {
                                        "Battery Saver is Disabled"
                                    } else if (isSaverActive) {
                                        "⚡ Active — GPS polling reduced to 12s"
                                    } else {
                                        "Standby — Triggers automatically at ≤20%"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSaverActive) SleekAmber else if (preferences.batterySaverEnabled) SleekGreen else SleekZinc400,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current battery: $batteryLevel%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekZinc400,
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isSaverActive) SleekAmber.copy(alpha = 0.2f) else SleekZinc800
                            ) {
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (batteryLevel <= 20) SleekAmber else SleekZinc100,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Voice Guidance (TTS)
            item {
                Text(
                    text = "VOICE-GUIDED NAVIGATION & TTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("voice_guidance_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (preferences.voiceGuidanceEnabled) SleekBlue.copy(alpha = 0.15f) else SleekZinc800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SpatialAudio,
                                        contentDescription = "Voice Guidance",
                                        tint = if (preferences.voiceGuidanceEnabled) SleekBlue else SleekZinc400,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text("Voice Guidance (TTS)", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                    Text("Spoken turn-by-turn prompts & AI briefings", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                                }
                            }

                            Switch(
                                checked = preferences.voiceGuidanceEnabled,
                                onCheckedChange = { isChecked ->
                                    onUpdatePreferences(preferences.copy(voiceGuidanceEnabled = isChecked))
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SleekBlack,
                                    checkedTrackColor = SleekBlue,
                                    uncheckedThumbColor = SleekZinc400,
                                    uncheckedTrackColor = SleekZinc800
                                ),
                                modifier = Modifier.testTag("voice_guidance_toggle")
                            )
                        }

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("AI Voice Engine Status", style = MaterialTheme.typography.labelMedium, color = SleekZinc200, fontWeight = FontWeight.SemiBold)
                                Text("Android Text-to-Speech Engine Ready", style = MaterialTheme.typography.labelSmall, color = SleekZinc400, fontSize = 11.sp)
                            }

                            FilledTonalButton(
                                onClick = onTestVoiceGuidance,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SleekZinc800, contentColor = SleekBlue),
                                modifier = Modifier.testTag("btn_test_voice_guidance")
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. Offline Maps & Tile Storage (Room Database)
            item {
                Text(
                    text = "OFFLINE MAPS & TILE STORAGE (ROOM DB)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("offline_maps_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Offline Only Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isOfflineOnly) SleekCyan.copy(alpha = 0.2f) else SleekZinc800),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isOfflineOnly) SleekCyan else SleekZinc400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Offline Only Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleekZinc100,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isOfflineOnly) "Strict Room DB reading (zero mobile data)" else "Auto-cache tiles & fetch online",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOfflineOnly) SleekCyan else SleekZinc400,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isOfflineOnly,
                                onCheckedChange = { mapTileManager.setOfflineMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SleekBlack,
                                    checkedTrackColor = SleekCyan,
                                    uncheckedThumbColor = SleekZinc400,
                                    uncheckedTrackColor = SleekZinc800
                                ),
                                modifier = Modifier.testTag("toggle_offline_only")
                            )
                        }

                        HorizontalDivider(color = SleekZinc800.copy(alpha = 0.5f))

                        // Cache Storage Statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Room SQLite Cached Tiles",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SleekZinc100,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$totalCachedTiles tiles stored (${String.format("%.2f", totalCachedBytes / (1024.0 * 1024.0))} MB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SleekGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = { showClearCacheDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SleekZinc800,
                                    contentColor = SleekSosRed
                                ),
                                modifier = Modifier.testTag("btn_clear_tile_cache")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Active Download Progress if running
                        activeDownload?.let { progress ->
                            val progressFraction = if (progress.totalTiles > 0) progress.downloadedTiles.toFloat() / progress.totalTiles.toFloat() else 0f
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SleekZinc800.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading Region: ${progress.regionName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc200,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${(progressFraction * 100).toInt()}% (${progress.downloadedTiles}/${progress.totalTiles})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = SleekCyan,
                                    trackColor = SleekZinc700
                                )
                            }
                        }

                        // Download Geographical Region Button
                        Button(
                            onClick = { showDownloadRegionDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_download_region"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SleekCyan,
                                contentColor = SleekBlack
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Geographical Region for Offline Use",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }

            // 8. Brand & Owner Info
            item {
                Text(
                    text = "GLOBALCORE-X BRAND & SUPPORT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SleekZinc500,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("brand_owner_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekZinc900),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("GlobalCoreX Navigation System", style = MaterialTheme.typography.titleMedium, color = SleekZinc100, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Version 1.0.0 Production • Worldwide GPS & Telemetry", style = MaterialTheme.typography.labelSmall, color = SleekGreen)

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = SleekZinc800)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Owner & Developer", style = MaterialTheme.typography.labelSmall, color = SleekZinc500)
                                Text("Malik-X", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekGreen.copy(alpha = 0.12f))
                                .clickable {
                                    val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/256750985651?text=Hello%20Malik-X%2C%20I%20am%20using%20GlobalCoreX%20Navigation."))
                                    context.startActivity(waIntent)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = SleekGreen, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("WhatsApp Support", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                    Text("+256750985651", style = MaterialTheme.typography.labelSmall, color = SleekGreen)
                                }
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SleekGreen, modifier = Modifier.size(16.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SleekBlue.copy(alpha = 0.12f))
                                .clickable {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:donmalik.pro1@gmail.com")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "GlobalCoreX Navigation Inquiry")
                                    }
                                    context.startActivity(emailIntent)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Official Email", style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                                    Text("donmalik.pro1@gmail.com", style = MaterialTheme.typography.labelSmall, color = SleekBlue)
                                }
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SleekBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Edit Profile
    if (showEditProfileDialog) {
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, uname, phone, country, avatar ->
                onUpdateProfile(name, uname, phone, country, avatar)
                showEditProfileDialog = false
            }
        )
    }

    // Dialog 2: Change Password
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = { currentPass, newPass, onComplete ->
                onChangePassword(currentPass, newPass) { success, msg ->
                    onComplete(success, msg)
                    if (success) {
                        showChangePasswordDialog = false
                    }
                }
            }
        )
    }

    // Dialog 3: Delete Account
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Permanently Delete Account?",
                    color = SleekZinc100,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently purge your user profile, cryptographic credentials, all recorded GPS trips, waypoints, telemetry logs, and emergency contacts from this device. This action cannot be undone.",
                    color = SleekZinc400,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSosRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Forever", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog 4: Add Emergency Contact
    if (showAddContactDialog) {
        AddEmergencyContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAdd = { name, phone, email, relationship, isPrimary ->
                onAddEmergencyContact(name, phone, email, relationship, isPrimary)
                showAddContactDialog = false
            }
        )
    }

    // Dialog 5: Download Geographical Region for Offline Use
    if (showDownloadRegionDialog) {
        val regionPresets = listOf(
            Triple("Kampala & Entebbe Region", Pair(0.20, 0.45), Pair(32.45, 32.70)),
            Triple("London Central Metro", Pair(51.45, 51.58), Pair(-0.20, 0.05)),
            Triple("New York City & Manhattan", Pair(40.65, 40.85), Pair(-74.05, -73.85)),
            Triple("Paris Metropolitan", Pair(48.80, 48.92), Pair(2.25, 2.45)),
            Triple("Nairobi Explorer Zone", Pair(-1.35, -1.22), Pair(36.75, 36.92)),
            Triple("Tokyo Shinjuku & Shibuya", Pair(35.63, 35.73), Pair(139.65, 139.78))
        )

        AlertDialog(
            onDismissRequest = { showDownloadRegionDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = SleekCyan)
                    Text("Download Offline Map Region", color = SleekZinc100, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select a geographical region to download and persist tile assets directly into your device's local Room database for full zero-data offline navigation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SleekZinc400,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "PRESET REGIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleekZinc500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )

                    regionPresets.forEachIndexed { index, (name, latRange, lngRange) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPresetIndex == index) SleekCyan.copy(alpha = 0.15f) else SleekZinc950,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (selectedPresetIndex == index) SleekCyan else SleekZinc800
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPresetIndex = index }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedPresetIndex == index) SleekCyan else SleekZinc200,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${String.format("%.2f", latRange.first)}°N to ${String.format("%.2f", latRange.second)}°N",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SleekZinc500,
                                        fontSize = 10.sp
                                    )
                                }
                                if (selectedPresetIndex == index) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SleekCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = regionPresets[selectedPresetIndex]
                        showDownloadRegionDialog = false
                        coroutineScope.launch {
                            mapTileManager.startOfflineRegionDownload(
                                name = selected.first,
                                minLat = selected.second.first,
                                maxLat = selected.second.second,
                                minLng = selected.third.first,
                                maxLng = selected.third.second,
                                minZoom = 12,
                                maxZoom = 15,
                                style = MapStyle.CYBER_DARK
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekCyan, contentColor = SleekBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Start Download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadRegionDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog 6: Clear Cache Confirmation
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = {
                Text("Clear Map Tile Cache?", color = SleekZinc100, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This will purge all $totalCachedTiles offline cached tiles stored in the Room SQLite database. Future map rendering will re-download tiles on demand when online.",
                    color = SleekZinc400,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        coroutineScope.launch {
                            mapTileManager.clearAllTileCache()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SleekSosRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear All Tiles", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = SleekZinc400)
                }
            },
            containerColor = SleekZinc900,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onCopy != null) Modifier.clickable { onCopy() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = SleekZinc500, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = SleekZinc400)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = SleekZinc200,
                fontWeight = FontWeight.SemiBold
            )
            if (onCopy != null) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SleekBlue, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun PrivacyToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (checked) iconTint.copy(alpha = 0.15f) else SleekZinc800),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (checked) iconTint else SleekZinc500, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = SleekZinc100, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SleekBlack,
                checkedTrackColor = iconTint,
                uncheckedThumbColor = SleekZinc400,
                uncheckedTrackColor = SleekZinc800
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun EditProfileDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, username: String, phone: String, country: String, avatarUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var country by remember { mutableStateOf(user?.country ?: "Global") }
    var avatar by remember { mutableStateOf(user?.avatarUrl ?: "avatar_1") }
    var err by remember { mutableStateOf<String?>(null) }

    val avatarPresets = listOf(
        Pair("avatar_1", "🔷"),
        Pair("avatar_2", "⚡"),
        Pair("avatar_3", "🛰️"),
        Pair("avatar_4", "🛡️"),
        Pair("avatar_5", "🧭"),
        Pair("avatar_6", "🌐")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User Profile", color = SleekZinc100, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!err.isNullOrBlank()) {
                    Text(err ?: "", color = SleekSosRed, fontSize = 12.sp)
                }

                Text("Choose Avatar Preset", style = MaterialTheme.typography.labelSmall, color = SleekZinc400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(avatarPresets) { (id, emoji) ->
                        val isSelected = avatar == id
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SleekBlue.copy(alpha = 0.25f) else SleekZinc800)
                                .border(if (isSelected) 2.dp else 1.dp, if (isSelected) SleekBlue else SleekZinc700, CircleShape)
                                .clickable { avatar = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 18.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country / Region") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nameVal = SecurityUtils.validateFullName(name)
                    val userVal = SecurityUtils.validateUsername(username)
                    if (!nameVal.isValid) {
                        err = (nameVal as SecurityUtils.ValidationResult.Invalid).errorMessage
                    } else if (!userVal.isValid) {
                        err = (userVal as SecurityUtils.ValidationResult.Invalid).errorMessage
                    } else {
                        onSave(name.trim(), username.trim(), phone.trim(), country.trim(), avatar)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SleekZinc400)
            }
        },
        containerColor = SleekZinc900,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (currentPass: String, newPass: String, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", color = SleekZinc100, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!err.isNullOrBlank()) {
                    Text(err ?: "", color = SleekSosRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it; err = null },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                            Icon(if (showCurrentPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, tint = SleekZinc400)
                        }
                    },
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; err = null },
                    label = { Text("New Password (8+ chars)") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPass = !showNewPass }) {
                            Icon(if (showNewPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, tint = SleekZinc400)
                        }
                    },
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; err = null },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = dialogTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val passVal = SecurityUtils.validatePassword(newPass)
                    val matchVal = SecurityUtils.validateConfirmPassword(newPass, confirmPass)
                    when {
                        currentPass.isBlank() -> err = "Please enter current password."
                        !passVal.isValid -> err = (passVal as SecurityUtils.ValidationResult.Invalid).errorMessage
                        !matchVal.isValid -> err = (matchVal as SecurityUtils.ValidationResult.Invalid).errorMessage
                        else -> {
                            isSubmitting = true
                            onChangePassword(currentPass, newPass) { success, msg ->
                                isSubmitting = false
                                if (!success) {
                                    err = msg ?: "Failed to update password."
                                }
                            }
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
            ) {
                Text("Update Password", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel", color = SleekZinc400)
            }
        },
        containerColor = SleekZinc900,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun AddEmergencyContactDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, email: String, relationship: String, isPrimary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }
    var isPrimary by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Emergency Contact", color = SleekZinc100, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!err.isNullOrBlank()) {
                    Text(err ?: "", color = SleekSosRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name *") },
                    placeholder = { Text("e.g. Sarah Kasoma") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (International) *") },
                    placeholder = { Text("e.g. +256 750 985651") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. contact@gmail.com") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship") },
                    placeholder = { Text("e.g. Spouse, Partner, Doctor, Colleague") },
                    singleLine = true,
                    colors = dialogTextFieldColors()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isPrimary = !isPrimary }
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        colors = CheckboxDefaults.colors(checkedColor = SleekSosRed, checkmarkColor = Color.White)
                    )
                    Text("Set as Primary Emergency Responder", style = MaterialTheme.typography.bodySmall, color = SleekZinc200)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        err = "Name and phone number are required."
                    } else {
                        onAdd(name.trim(), phone.trim(), email.trim(), relationship.trim(), isPrimary)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SleekBlue)
            ) {
                Text("Add Contact", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SleekZinc400)
            }
        },
        containerColor = SleekZinc900,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SleekZinc100,
    unfocusedTextColor = SleekZinc100,
    focusedBorderColor = SleekBlue,
    unfocusedBorderColor = SleekZinc700,
    focusedLabelColor = SleekBlue,
    unfocusedLabelColor = SleekZinc400,
    focusedContainerColor = SleekZinc950,
    unfocusedContainerColor = SleekZinc950
)
