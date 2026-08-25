package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.auth.AuthScreenState
import com.example.ui.screens.auth.*
import com.example.ui.screens.devices.DevicesScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.map.MapScreen
import com.example.ui.screens.profile.SettingsProfileScreen
import com.example.ui.screens.recording.RecordingScreen
import com.example.ui.screens.routes.RoutesScreen
import com.example.ui.screens.sharing.LiveSharingScreen
import com.example.ui.screens.sos.SosScreen
import com.example.ui.screens.support.AiSupportScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LocationViewModel
import com.example.ui.viewmodel.MainViewModel

enum class MainTab {
    TRACKING,
    RECORDING,
    AI_ASSISTANT,
    HISTORY,
    SOS
}

enum class SubScreen {
    NONE,
    HOME_DASHBOARD,
    AI_SUPPORT,
    LIVE_SHARING,
    DEVICES,
    SETTINGS_PROFILE
}

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userPrefs by mainViewModel.userPreferences.collectAsState()
            GlobalCoreXTheme(darkTheme = userPrefs.isDarkTheme) {
                GlobalCoreXApp(
                    mainViewModel = mainViewModel,
                    locationViewModel = locationViewModel,
                    authViewModel = authViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalCoreXApp(
    mainViewModel: MainViewModel,
    locationViewModel: LocationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainTab.TRACKING) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Authentication States
    val authScreenState by authViewModel.screenState.collectAsState()
    val isAuthLoading by authViewModel.isLoading.collectAsState()
    val authUser by authViewModel.currentUser.collectAsState()
    val authErrorMessage by authViewModel.errorMessage.collectAsState()
    val authSuccessMessage by authViewModel.successMessage.collectAsState()
    val savedIdentifier by authViewModel.savedIdentifier.collectAsState()

    // MainViewModel States
    val userPrefs by mainViewModel.userPreferences.collectAsState()
    val gpsLocation by mainViewModel.gpsLocation.collectAsState()
    val batteryLevel by mainViewModel.batteryLevel.collectAsState()
    val isBatterySaverActive by mainViewModel.isBatterySaverActive.collectAsState()
    val trackingState by mainViewModel.trackingState.collectAsState()
    val routeState by mainViewModel.routeState.collectAsState()
    val mapStyle by mainViewModel.mapStyle.collectAsState()
    val allTrips by mainViewModel.allTrips.collectAsState()
    val todayTrips by mainViewModel.todayTrips.collectAsState()
    val savedRoutes by mainViewModel.savedRoutes.collectAsState()
    val allRecordedRoutes by mainViewModel.allRecordedRoutes.collectAsState()
    val emergencyContacts by mainViewModel.emergencyContacts.collectAsState()
    val activeSosEvent by mainViewModel.activeSosEvent.collectAsState()
    val isSosCountingDown by mainViewModel.isSosCountdownActive.collectAsState()
    val sosCountdown by mainViewModel.sosCountdown.collectAsState()
    val activeSharedSessions by mainViewModel.activeSharedSessions.collectAsState()
    val allDevices by mainViewModel.allDevices.collectAsState()
    val userMessage by mainViewModel.userMessage.collectAsState()
    val isVoiceMuted by mainViewModel.isVoiceMuted.collectAsState()
    val isSpeaking by mainViewModel.isSpeaking.collectAsState()
    val poiItems by mainViewModel.poiItems.collectAsState()
    val selectedPoiCategory by mainViewModel.selectedPoiCategory.collectAsState()
    val selectedPoi by mainViewModel.selectedPoi.collectAsState()
    val isPoiLoading by mainViewModel.isPoiLoading.collectAsState()
    val useGoogleMaps by mainViewModel.useGoogleMaps.collectAsState()

    // LocationViewModel States
    val recordingState by locationViewModel.recordingState.collectAsState()
    val hasLocationPermission by locationViewModel.hasLocationPermission.collectAsState()

    // Show Snackbars on user messages from MainViewModel
    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearUserMessage()
        }
    }

    // Permission launcher
    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(authScreenState) {
        if (authScreenState == AuthScreenState.Authenticated) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine) {
                permissionLauncher.launch(permissionsToRequest)
            }
        }
    }

    // ==========================================
    // 1. AUTHENTICATION FLOW & AUTH GUARD
    // ==========================================
    AnimatedContent(
        targetState = authScreenState,
        label = "AuthNavTransition"
    ) { state ->
        when (state) {
            AuthScreenState.Welcome -> {
                WelcomeScreen(
                    onNavigateToSignIn = { authViewModel.navigateTo(AuthScreenState.SignIn) },
                    onNavigateToSignUp = { authViewModel.navigateTo(AuthScreenState.SignUp) }
                )
            }

            AuthScreenState.SignIn -> {
                SignInScreen(
                    isLoading = isAuthLoading,
                    errorMessage = authErrorMessage,
                    savedIdentifier = savedIdentifier,
                    onSignIn = { id, pass, remember ->
                        authViewModel.signIn(id, pass, remember)
                    },
                    onNavigateToSignUp = { authViewModel.navigateTo(AuthScreenState.SignUp) },
                    onNavigateToForgotPassword = { authViewModel.navigateTo(AuthScreenState.ForgotPassword) },
                    onNavigateBackToWelcome = { authViewModel.navigateTo(AuthScreenState.Welcome) }
                )
            }

            AuthScreenState.SignUp -> {
                SignUpScreen(
                    isLoading = isAuthLoading,
                    errorMessage = authErrorMessage,
                    onSignUp = { fullName, username, email, phone, country, avatarUrl, password, terms, privacy ->
                        authViewModel.signUp(
                            fullName = fullName,
                            username = username,
                            email = email,
                            phone = phone,
                            country = country,
                            avatarUrl = avatarUrl,
                            password = password,
                            agreedToTerms = terms,
                            agreedToPrivacy = privacy
                        )
                    },
                    onNavigateToSignIn = { authViewModel.navigateTo(AuthScreenState.SignIn) },
                    onNavigateBackToWelcome = { authViewModel.navigateTo(AuthScreenState.Welcome) }
                )
            }

            is AuthScreenState.EmailVerification -> {
                EmailVerificationScreen(
                    email = state.email,
                    generatedCode = state.generatedCode,
                    isLoading = isAuthLoading,
                    errorMessage = authErrorMessage,
                    successMessage = authSuccessMessage,
                    onVerifyCode = { code ->
                        authViewModel.verifyEmail(state.userId, code)
                    },
                    onResendCode = {
                        authViewModel.resendVerificationCode(state.userId)
                    },
                    onContinueToApp = {
                        authViewModel.navigateTo(AuthScreenState.Authenticated)
                    },
                    onBackToSignIn = {
                        authViewModel.navigateTo(AuthScreenState.SignIn)
                    }
                )
            }

            AuthScreenState.ForgotPassword -> {
                ForgotPasswordScreen(
                    isLoading = isAuthLoading,
                    errorMessage = authErrorMessage,
                    onRequestResetCode = { email, onCodeSent ->
                        authViewModel.requestPasswordResetCode(email, onCodeSent)
                    },
                    onResetPassword = { email, code, newPass, onSuccess ->
                        authViewModel.resetPassword(email, code, newPass, onSuccess)
                    },
                    onNavigateBackToSignIn = {
                        authViewModel.navigateTo(AuthScreenState.SignIn)
                    }
                )
            }

            AuthScreenState.Authenticated -> {
                // ==========================================
                // 2. MAIN AUTHENTICATED APPLICATION
                // ==========================================
                // Handle Sub-Screens
                when (currentSubScreen) {
                    SubScreen.HOME_DASHBOARD -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = SleekBlack,
                            topBar = {
                                Surface(
                                    color = SleekBlack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "GLOBALCORE-X",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SleekZinc500,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                letterSpacing = 1.5.sp
                                            )
                                            Text(
                                                text = "Command Dashboard",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = SleekZinc100,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp
                                            )
                                        }

                                        IconButton(
                                            onClick = { currentSubScreen = SubScreen.NONE },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(SleekZinc900)
                                                .border(1.dp, SleekZinc800, CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Close Dashboard",
                                                tint = SleekZinc300,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                HomeScreen(
                                    gpsLocation = gpsLocation,
                                    batteryLevel = batteryLevel,
                                    isBatterySaverActive = isBatterySaverActive,
                                    trackingState = trackingState,
                                    todayTripsCount = todayTrips.size,
                                    todayDistanceMeters = todayTrips.sumOf { it.totalDistanceMeters },
                                    userName = authUser?.displayName ?: "Explorer",
                                    onStartTracking = {
                                        mainViewModel.startTracking()
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.TRACKING
                                    },
                                    onStopTracking = { mainViewModel.stopTracking() },
                                    onNavigateToMap = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.TRACKING
                                    },
                                    onNavigateToRoutes = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.AI_ASSISTANT
                                    },
                                    onNavigateToAiRoute = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.AI_ASSISTANT
                                    },
                                    onNavigateToAiSupport = {
                                        currentSubScreen = SubScreen.AI_SUPPORT
                                    },
                                    onNavigateToOfflineMaps = {
                                        currentSubScreen = SubScreen.SETTINGS_PROFILE
                                    },
                                    onNavigateToShare = {
                                        currentSubScreen = SubScreen.LIVE_SHARING
                                    },
                                    onNavigateToSos = {
                                        currentSubScreen = SubScreen.NONE
                                        currentTab = MainTab.SOS
                                    },
                                    onNavigateToDevices = {
                                        currentSubScreen = SubScreen.DEVICES
                                    }
                                )
                            }
                        }
                    }
                    SubScreen.AI_SUPPORT -> {
                        AiSupportScreen(
                            gpsLocation = gpsLocation,
                            batteryLevel = batteryLevel,
                            onBack = { currentSubScreen = SubScreen.NONE }
                        )
                    }
                    SubScreen.LIVE_SHARING -> {
                        LiveSharingScreen(
                            activeSessions = activeSharedSessions,
                            gpsLocation = gpsLocation,
                            onCreateSession = { name, phone, duration, msg ->
                                mainViewModel.createShareSession(name, phone, duration, msg)
                            },
                            onStopSharing = { mainViewModel.stopSharing(it) },
                            onBack = { currentSubScreen = SubScreen.NONE }
                        )
                    }
                    SubScreen.DEVICES -> {
                        DevicesScreen(
                            devices = allDevices,
                            onAddDevice = { name, model -> mainViewModel.registerDevice(name, model) },
                            onDeleteDevice = { mainViewModel.deleteDevice(it) },
                            onBack = { currentSubScreen = SubScreen.NONE }
                        )
                    }
                    SubScreen.SETTINGS_PROFILE -> {
                        SettingsProfileScreen(
                            currentUser = authUser,
                            preferences = userPrefs,
                            emergencyContacts = emergencyContacts,
                            batteryLevel = batteryLevel,
                            onUpdateProfile = { fullName, username, phone, country, avatarUrl ->
                                val uid = authUser?.id ?: ""
                                authViewModel.updateProfile(uid, fullName, username, phone, country, avatarUrl) { success, msg ->
                                    if (success) {
                                        mainViewModel.triggerUserMessage("Profile updated successfully.")
                                    } else {
                                        mainViewModel.triggerUserMessage(msg ?: "Failed to update profile.")
                                    }
                                }
                            },
                            onChangePassword = { currentPass, newPass, onComplete ->
                                val uid = authUser?.id ?: ""
                                authViewModel.changePassword(uid, currentPass, newPass) { success, msg ->
                                    onComplete(success, msg)
                                }
                            },
                            onAddEmergencyContact = { name, phone, email, rel, primary ->
                                mainViewModel.addEmergencyContact(name, phone, rel, primary)
                            },
                            onDeleteEmergencyContact = { contactId ->
                                mainViewModel.deleteEmergencyContactById(contactId)
                            },
                            onLogout = {
                                authViewModel.logout()
                                currentSubScreen = SubScreen.NONE
                            },
                            onDeleteAccount = {
                                val uid = authUser?.id ?: ""
                                authViewModel.deleteAccount(uid) {
                                    mainViewModel.deleteAllHistory()
                                }
                                currentSubScreen = SubScreen.NONE
                            },
                            onUpdatePreferences = { mainViewModel.updatePreferences(it) },
                            onTestVoiceGuidance = { mainViewModel.testVoiceGuidance() },
                            onBack = { currentSubScreen = SubScreen.NONE }
                        )
                    }
                    SubScreen.NONE -> {
                        // Main Tab Navigation Layout
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = SleekBlack,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                Surface(
                                    color = SleekBlack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "GLOBALCORE-X",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SleekZinc500,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                letterSpacing = 1.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Welcome, ${authUser?.displayName?.ifBlank { "Explorer" } ?: "Explorer"}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = SleekZinc100,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            // Dashboard button
                                            IconButton(
                                                onClick = { currentSubScreen = SubScreen.HOME_DASHBOARD },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekZinc900)
                                                    .border(1.dp, SleekZinc800, CircleShape)
                                                    .testTag("top_btn_dashboard")
                                            ) {
                                                Icon(
                                                    Icons.Default.Dashboard,
                                                    contentDescription = "Dashboard",
                                                    tint = SleekZinc300,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // AI Support Agent Quick Button
                                            IconButton(
                                                onClick = { currentSubScreen = SubScreen.AI_SUPPORT },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekBlue.copy(alpha = 0.15f))
                                                    .border(1.dp, SleekBlue.copy(alpha = 0.35f), CircleShape)
                                                    .testTag("top_btn_ai_support")
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = "AI Help & Support",
                                                    tint = SleekCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Devices icon button
                                            IconButton(
                                                onClick = { currentSubScreen = SubScreen.DEVICES },
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(SleekZinc900)
                                                    .border(1.dp, SleekZinc800, CircleShape)
                                                    .testTag("top_btn_devices")
                                            ) {
                                                Icon(
                                                    Icons.Default.Devices,
                                                    contentDescription = "My Devices",
                                                    tint = SleekZinc400,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Sleek Profile / Avatar button
                                            Surface(
                                                onClick = { currentSubScreen = SubScreen.SETTINGS_PROFILE },
                                                shape = CircleShape,
                                                color = SleekZinc800,
                                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc700)),
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .testTag("top_btn_profile")
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    val avatarEmoji = when (authUser?.avatarUrl) {
                                                        "avatar_2" -> "⚡"
                                                        "avatar_3" -> "🛰️"
                                                        "avatar_4" -> "🛡️"
                                                        "avatar_5" -> "🧭"
                                                        "avatar_6" -> "🌐"
                                                        else -> (authUser?.displayName?.take(1) ?: "M").uppercase()
                                                    }
                                                    Text(
                                                        text = avatarEmoji,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = SleekZinc100,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            bottomBar = {
                                Surface(
                                    color = SleekZinc900,
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(SleekZinc800)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    NavigationBar(
                                        modifier = Modifier.testTag("bottom_nav_bar"),
                                        containerColor = SleekZinc900,
                                        contentColor = SleekZinc100,
                                        tonalElevation = 0.dp
                                    ) {
                                        // 1. Tracking
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.TRACKING,
                                            onClick = { currentTab = MainTab.TRACKING },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentTab == MainTab.TRACKING) Icons.Filled.Explore else Icons.Outlined.Explore,
                                                    contentDescription = "Tracking"
                                                )
                                            },
                                            label = { Text("Tracking", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.TRACKING) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = SleekBlue,
                                                selectedTextColor = SleekBlue,
                                                indicatorColor = SleekBlue.copy(alpha = 0.2f),
                                                unselectedIconColor = SleekZinc500,
                                                unselectedTextColor = SleekZinc500
                                            )
                                        )

                                        // 2. Recording
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.RECORDING,
                                            onClick = { currentTab = MainTab.RECORDING },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (recordingState.isRecording) {
                                                            Badge(containerColor = if (recordingState.isPaused) SleekOrange else SleekEmerald)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (currentTab == MainTab.RECORDING) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                                        contentDescription = "Recording"
                                                    )
                                                }
                                            },
                                            label = { Text("Recording", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.RECORDING) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = SleekEmerald,
                                                selectedTextColor = SleekEmerald,
                                                indicatorColor = SleekEmerald.copy(alpha = 0.2f),
                                                unselectedIconColor = SleekZinc500,
                                                unselectedTextColor = SleekZinc500
                                            )
                                        )

                                        // 3. AI Assistant
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.AI_ASSISTANT,
                                            onClick = { currentTab = MainTab.AI_ASSISTANT },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentTab == MainTab.AI_ASSISTANT) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                                    contentDescription = "AI Assistant"
                                                )
                                            },
                                            label = { Text("AI Assistant", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.AI_ASSISTANT) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = SleekBlue,
                                                selectedTextColor = SleekBlue,
                                                indicatorColor = SleekBlue.copy(alpha = 0.2f),
                                                unselectedIconColor = SleekZinc500,
                                                unselectedTextColor = SleekZinc500
                                            )
                                        )

                                        // 4. History
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.HISTORY,
                                            onClick = { currentTab = MainTab.HISTORY },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentTab == MainTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                                    contentDescription = "History"
                                                )
                                            },
                                            label = { Text("History", fontSize = 11.sp, fontWeight = if (currentTab == MainTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = SleekZinc100,
                                                selectedTextColor = SleekZinc100,
                                                indicatorColor = SleekZinc800,
                                                unselectedIconColor = SleekZinc500,
                                                unselectedTextColor = SleekZinc500
                                            )
                                        )

                                        // 5. SOS
                                        NavigationBarItem(
                                            selected = currentTab == MainTab.SOS,
                                            onClick = { currentTab = MainTab.SOS },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (activeSosEvent != null) {
                                                            Badge(containerColor = SleekSosRed)
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (currentTab == MainTab.SOS) Icons.Filled.Sos else Icons.Outlined.Sos,
                                                        contentDescription = "SOS"
                                                    )
                                                }
                                            },
                                            label = { Text("SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (currentTab == MainTab.SOS) SleekSosRed else SleekZinc500) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = SleekSosRed,
                                                selectedTextColor = SleekSosRed,
                                                indicatorColor = SleekSosRedMuted,
                                                unselectedIconColor = SleekZinc500,
                                                unselectedTextColor = SleekZinc500
                                            )
                                        )
                                    }
                                }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (currentTab) {
                                    MainTab.TRACKING -> {
                                        MapScreen(
                                            gpsLocation = gpsLocation,
                                            routeState = routeState,
                                            mapStyle = mapStyle,
                                            poiItems = poiItems,
                                            selectedPoiCategory = selectedPoiCategory,
                                            selectedPoi = selectedPoi,
                                            isPoiLoading = isPoiLoading,
                                            useGoogleMaps = useGoogleMaps,
                                            isVoiceMuted = isVoiceMuted,
                                            isSpeaking = isSpeaking,
                                            onStyleChange = { mainViewModel.setMapStyle(it) },
                                            onSelectPoiCategory = { mainViewModel.fetchNearbyPois(it) },
                                            onSelectPoi = { mainViewModel.selectPoi(it) },
                                            onToggleMapEngine = { mainViewModel.toggleMapEngine() },
                                            onSearchDestination = { mainViewModel.searchDestination(it) },
                                            onSelectDestination = { name, lat, lng ->
                                                mainViewModel.setDestination(name, lat, lng)
                                                mainViewModel.calculateRoute()
                                            },
                                            onStartNavigation = { mainViewModel.startNavigation() },
                                            onStopNavigation = { mainViewModel.stopNavigation() },
                                            onToggleVoiceMute = { mainViewModel.toggleVoiceMute() },
                                            onRepeatVoiceInstruction = { mainViewModel.repeatCurrentVoiceInstruction() },
                                            onSpeakAiBriefing = { mainViewModel.speakAiRouteBriefing() },
                                            onNextStep = { mainViewModel.nextNavigationStep() },
                                            onPreviousStep = { mainViewModel.previousNavigationStep() },
                                            onGoToRoutePlanner = { currentTab = MainTab.AI_ASSISTANT }
                                        )
                                    }

                                    MainTab.RECORDING -> {
                                        RecordingScreen(
                                            recordingState = recordingState,
                                            gpsLocation = gpsLocation,
                                            recordedRoutes = allRecordedRoutes,
                                            onStartRecording = { title, mode -> locationViewModel.startRecording(title, mode) },
                                            onPauseRecording = { locationViewModel.pauseRecording() },
                                            onResumeRecording = { locationViewModel.resumeRecording() },
                                            onStopAndSaveRecording = { title, desc, aiSummary ->
                                                locationViewModel.stopAndSaveRecording(title, desc, aiSummary) {
                                                    mainViewModel.triggerUserMessage("Route successfully stored in Room Database.")
                                                }
                                            },
                                            onDiscardRecording = { locationViewModel.discardRecording() },
                                            onDeleteRecordedRoute = { locationViewModel.deleteRecordedRoute(it) },
                                            onToggleFavoriteRoute = { id, fav -> locationViewModel.toggleFavoriteRecordedRoute(id, fav) },
                                            onViewRouteOnMap = { route ->
                                                mainViewModel.setDestination(route.title, route.endLat, route.endLng)
                                                currentTab = MainTab.TRACKING
                                            }
                                        )
                                    }

                                    MainTab.AI_ASSISTANT -> {
                                        RoutesScreen(
                                            routeState = routeState,
                                            savedRoutes = savedRoutes,
                                            userPreferences = userPrefs,
                                            onOriginChange = { name, lat, lng -> mainViewModel.setOrigin(name, lat, lng) },
                                            onDestinationChange = { name, lat, lng -> mainViewModel.setDestination(name, lat, lng) },
                                            onSearchDest = { mainViewModel.searchDestination(it) },
                                            onTravelModeChange = { mainViewModel.setTravelMode(it) },
                                            onCalculateRoute = { mainViewModel.calculateRoute() },
                                            onAiOptimizeRoute = { mainViewModel.fetchAiRouteAnalysis() },
                                            onSpeakAiBriefing = { mainViewModel.speakAiRouteBriefing() },
                                            onSaveRoute = { mainViewModel.saveCurrentRoute(it) },
                                            onDeleteSavedRoute = { mainViewModel.deleteSavedRoute(it) },
                                            onStartNavigation = {
                                                mainViewModel.startNavigation()
                                                currentTab = MainTab.TRACKING
                                            },
                                            onUpdatePreferences = { mainViewModel.updatePreferences(it) }
                                        )
                                    }

                                    MainTab.HISTORY -> {
                                        HistoryScreen(
                                            trips = allTrips,
                                            onDeleteTrip = { mainViewModel.deleteTrip(it) },
                                            onDeleteAllHistory = { mainViewModel.deleteAllHistory() },
                                            onViewTripOnMap = { trip ->
                                                mainViewModel.setDestination(trip.endAddress.ifBlank { "Historic Trip" }, 0.0, 0.0)
                                                currentTab = MainTab.TRACKING
                                            }
                                        )
                                    }

                                    MainTab.SOS -> {
                                        SosScreen(
                                            gpsLocation = gpsLocation,
                                            batteryLevel = batteryLevel,
                                            isCountingDown = isSosCountingDown,
                                            countdownSeconds = sosCountdown,
                                            activeSosEvent = activeSosEvent,
                                            contacts = emergencyContacts,
                                            onStartCountdown = { mainViewModel.initiateSosCountdown() },
                                            onCancelCountdown = { mainViewModel.cancelSosCountdown() },
                                            onTriggerImmediately = { mainViewModel.triggerEmergencySos() },
                                            onCancelActiveSos = { mainViewModel.cancelActiveSos(it) },
                                            onAddContact = { name, phone, rel, primary ->
                                                mainViewModel.addEmergencyContact(name, phone, rel, primary)
                                            },
                                            onDeleteContact = { mainViewModel.deleteEmergencyContact(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
