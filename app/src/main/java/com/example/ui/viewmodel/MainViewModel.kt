package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.remote.model.NominatimSearchResult
import com.example.data.remote.model.OsrmRouteResponse
import com.example.data.repository.AppRepository
import com.example.service.GpsLocationData
import com.example.service.LocationHelper
import com.example.service.NavigationVoiceManager
import com.example.service.TrackingService
import com.example.ui.components.map.MapMarker
import com.example.ui.components.map.MapStyle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SosRed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class RoutePlannerState(
    val originQuery: String = "Current GPS Location",
    val originLat: Double = 0.0,
    val originLng: Double = 0.0,
    val destQuery: String = "",
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val travelMode: String = "driving", // "driving", "walking", "cycling"
    val isLoading: Boolean = false,
    val searchResults: List<NominatimSearchResult> = emptyList(),
    val routeResponse: OsrmRouteResponse? = null,
    val routeCoordinates: List<Pair<Double, Double>> = emptyList(),
    val distanceMeters: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val aiRecommendation: String = "",
    val isAiLoading: Boolean = false,
    val errorMessage: String? = null,
    val isNavigating: Boolean = false,
    val currentStepIndex: Int = 0
) {
    val steps: List<com.example.data.remote.model.OsrmStep>
        get() = routeResponse?.routes?.firstOrNull()?.legs?.flatMap { it.steps } ?: emptyList()

    val currentStep: com.example.data.remote.model.OsrmStep?
        get() = steps.getOrNull(currentStepIndex) ?: steps.firstOrNull()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val locationHelper = LocationHelper(application)
    val voiceManager = NavigationVoiceManager(application)

    // Voice Guidance State
    val isVoiceMuted = voiceManager.isMuted
    val isSpeaking = voiceManager.isSpeaking
    val isTtsReady = voiceManager.isTtsReady

    // User & Preferences
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPreferences: StateFlow<UserPreferencesEntity> = repository.userPreferences
        .map { it ?: UserPreferencesEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesEntity())

    // GPS & Battery
    private val _gpsLocation = MutableStateFlow(GpsLocationData())
    val gpsLocation = _gpsLocation.asStateFlow()

    private val _batteryLevel = MutableStateFlow(locationHelper.getBatteryLevel())
    val batteryLevel = _batteryLevel.asStateFlow()

    val isBatterySaverActive: StateFlow<Boolean> = combine(_batteryLevel, userPreferences) { battery, prefs ->
        prefs.batterySaverEnabled && battery <= 20
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Live Tracking Service State
    val trackingState = TrackingService.trackingState

    // Route Planner State
    private val _routeState = MutableStateFlow(RoutePlannerState())
    val routeState = _routeState.asStateFlow()

    // Map View Settings (Initialized based on system time: Dark mode at night 18:00-06:00, Light mode at day)
    private val _mapStyle = MutableStateFlow(
        run {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour < 6 || hour >= 18) MapStyle.CYBER_DARK else MapStyle.STREET
        }
    )
    val mapStyle = _mapStyle.asStateFlow()

    // POIs & Place Exploration (Hospitals, Schools, Hotels, Police Stations, Gas Stations, etc.)
    private val _poiItems = MutableStateFlow<List<com.example.ui.components.map.MapPoiItem>>(emptyList())
    val poiItems = _poiItems.asStateFlow()

    private val _selectedPoiCategory = MutableStateFlow(com.example.ui.components.map.PoiCategory.ALL)
    val selectedPoiCategory = _selectedPoiCategory.asStateFlow()

    private val _selectedPoi = MutableStateFlow<com.example.ui.components.map.MapPoiItem?>(null)
    val selectedPoi = _selectedPoi.asStateFlow()

    private val _isPoiLoading = MutableStateFlow(false)
    val isPoiLoading = _isPoiLoading.asStateFlow()

    // Engine switch: Google Maps vs Custom Vector / Offline Tile Canvas
    private val _useGoogleMaps = MutableStateFlow(true)
    val useGoogleMaps = _useGoogleMaps.asStateFlow()

    // History & Stats
    val allTrips = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTrips = repository.allTrips.map { trips ->
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        trips.filter { it.startTime >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRoutes = repository.savedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordedRoutes = repository.allRecordedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordedRoutesWithWaypoints = repository.allRecordedRoutesWithWaypoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Emergency SOS State
    val emergencyContacts = repository.emergencyContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSosEvent = repository.activeSosEvent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSosCountdownActive = MutableStateFlow(false)
    val isSosCountdownActive = _isSosCountdownActive.asStateFlow()

    private val _sosCountdown = MutableStateFlow(5)
    val sosCountdown = _sosCountdown.asStateFlow()

    private var sosCountdownJob: Job? = null

    // Live Location Sharing
    val activeSharedSessions = repository.activeSharedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Devices
    val allDevices = repository.allDevices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // General Notifications / Toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    init {
        // Continuous battery monitoring
        viewModelScope.launch {
            while (true) {
                val current = locationHelper.getBatteryLevel()
                if (_batteryLevel.value != current) {
                    _batteryLevel.value = current
                }
                delay(4000)
            }
        }

        // Collect live GPS updates with dynamic battery-saver adaptive interval
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            combine(userPreferences, _batteryLevel) { prefs, battery ->
                Pair(prefs, battery)
            }.flatMapLatest { (prefs, battery) ->
                val intervalMs = locationHelper.getEffectiveIntervalMillis(
                    prefs.liveTrackingIntervalSec,
                    prefs.batterySaverEnabled
                )
                locationHelper.getLocationFlow(intervalMs, prefs.batterySaverEnabled)
            }.collect { gps ->
                _gpsLocation.value = gps
                if (_routeState.value.originLat == 0.0 && gps.latitude != 0.0) {
                    _routeState.value = _routeState.value.copy(
                        originLat = gps.latitude,
                        originLng = gps.longitude
                    )
                }
            }
        }

        // Initialize defaults
        viewModelScope.launch {
            repository.ensureDefaultContacts()
            repository.ensureDefaultDevice()
            repository.ensureDefaultPreferences()
            // Auto login guest if none
            repository.currentUser.firstOrNull() ?: run {
                repository.login("donmalik.pro1@gmail.com", "Malik-X (Commander)")
            }
        }
    }

    fun setMapStyle(style: MapStyle) {
        _mapStyle.value = style
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun triggerUserMessage(msg: String) {
        _userMessage.value = msg
    }

    // --- Auth ---
    fun login(email: String, displayName: String) {
        viewModelScope.launch {
            try {
                repository.login(email, displayName)
                _userMessage.value = "Welcome to GlobalCoreX, $displayName"
            } catch (e: Exception) {
                _userMessage.value = "Login error: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _userMessage.value = "Logged out successfully"
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
            _userMessage.value = "Account and data purged"
        }
    }

    // --- Tracking Service ---
    fun startTracking() {
        val app = getApplication<Application>()
        val intent = Intent(app, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START_TRACKING
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
        _userMessage.value = "Tracking activated. Recording GPS path..."
    }

    fun stopTracking() {
        val app = getApplication<Application>()
        val intent = Intent(app, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP_TRACKING
        }
        app.startService(intent)
        _userMessage.value = "Route saved to History."
    }

    // --- Route Planning ---
    fun setOrigin(name: String, lat: Double, lng: Double) {
        _routeState.value = _routeState.value.copy(
            originQuery = name,
            originLat = lat,
            originLng = lng,
            searchResults = emptyList()
        )
    }

    fun setDestination(name: String, lat: Double, lng: Double) {
        _routeState.value = _routeState.value.copy(
            destQuery = name,
            destLat = lat,
            destLng = lng,
            searchResults = emptyList()
        )
    }

    fun selectPoi(poi: com.example.ui.components.map.MapPoiItem?) {
        _selectedPoi.value = poi
    }

    fun toggleMapEngine() {
        _useGoogleMaps.value = !_useGoogleMaps.value
        _userMessage.value = if (_useGoogleMaps.value) "Switched to Google Maps Engine" else "Switched to High-Performance Offline Map Canvas"
    }

    fun setMapEngine(useGoogle: Boolean) {
        _useGoogleMaps.value = useGoogle
    }

    fun fetchNearbyPois(category: com.example.ui.components.map.PoiCategory) {
        _selectedPoiCategory.value = category
        _isPoiLoading.value = true
        viewModelScope.launch {
            val userLat = if (_gpsLocation.value.latitude != 0.0) _gpsLocation.value.latitude else 0.3476
            val userLng = if (_gpsLocation.value.longitude != 0.0) _gpsLocation.value.longitude else 32.5825
            val items = repository.searchNearbyPois(category, userLat, userLng)
            _poiItems.value = items
            _isPoiLoading.value = false
            _userMessage.value = if (items.isNotEmpty()) {
                "Found ${items.size} nearby ${category.title}."
            } else {
                "No ${category.title} found within immediate search radius."
            }
        }
    }

    fun setTravelMode(mode: String) {
        _routeState.value = _routeState.value.copy(travelMode = mode)
        if (_routeState.value.destLat != 0.0) {
            calculateRoute()
        }
    }

    fun searchDestination(query: String) {
        _routeState.value = _routeState.value.copy(destQuery = query, isLoading = true)
        viewModelScope.launch {
            val results = repository.searchPlace(query)
            _routeState.value = _routeState.value.copy(
                searchResults = results,
                isLoading = false
            )
        }
    }

    fun calculateRoute() {
        val st = _routeState.value
        val origLat = if (st.originLat != 0.0) st.originLat else _gpsLocation.value.latitude
        val origLng = if (st.originLng != 0.0) st.originLng else _gpsLocation.value.longitude

        if (origLat == 0.0 || origLng == 0.0) {
            _userMessage.value = "Awaiting GPS coordinates for origin..."
            return
        }
        if (st.destLat == 0.0 || st.destLng == 0.0) {
            _userMessage.value = "Please select a destination first"
            return
        }

        _routeState.value = _routeState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.fetchRoute(origLat, origLng, st.destLat, st.destLng, st.travelMode)
            result.onSuccess { osrm ->
                val primaryRoute = osrm.routes.firstOrNull()
                val coordinates = primaryRoute?.geometry?.coordinates?.map {
                    Pair(it[1], it[0]) // [lng, lat] -> (lat, lng)
                } ?: emptyList()

                _routeState.value = _routeState.value.copy(
                    isLoading = false,
                    routeResponse = osrm,
                    routeCoordinates = coordinates,
                    distanceMeters = primaryRoute?.distance ?: 0.0,
                    durationSeconds = primaryRoute?.duration ?: 0.0
                )
                // Proactively trigger AI Route Analysis
                fetchAiRouteAnalysis()
            }.onFailure { err ->
                _routeState.value = _routeState.value.copy(
                    isLoading = false,
                    errorMessage = "Route calculation error: ${err.localizedMessage}"
                )
            }
        }
    }

    fun fetchAiRouteAnalysis() {
        val st = _routeState.value
        if (st.routeCoordinates.isEmpty()) return

        _routeState.value = _routeState.value.copy(isAiLoading = true)
        viewModelScope.launch {
            val prefs = userPreferences.value
            val recommendation = repository.getAiRouteAnalysis(
                originName = st.originQuery,
                destName = st.destQuery.ifBlank { "Selected Destination" },
                mode = st.travelMode,
                distanceKm = st.distanceMeters / 1000.0,
                durationMin = (st.durationSeconds / 60.0).toInt(),
                avoidTolls = prefs.avoidTolls,
                avoidHighways = prefs.avoidHighways
            )
            _routeState.value = _routeState.value.copy(
                isAiLoading = false,
                aiRecommendation = recommendation
            )
        }
    }

    fun saveCurrentRoute(name: String) {
        val st = _routeState.value
        if (st.routeCoordinates.isEmpty()) return

        viewModelScope.launch {
            repository.saveRoute(
                SavedRouteEntity(
                    name = name.ifBlank { "Route to ${st.destQuery.take(20)}" },
                    originName = st.originQuery,
                    originLat = st.originLat,
                    originLng = st.originLng,
                    destName = st.destQuery,
                    destLat = st.destLat,
                    destLng = st.destLng,
                    distanceKm = st.distanceMeters / 1000.0,
                    durationMin = (st.durationSeconds / 60.0).toInt(),
                    travelMode = st.travelMode,
                    aiRecommendation = st.aiRecommendation
                )
            )
            _userMessage.value = "Route saved to Saved Routes!"
        }
    }

    fun deleteSavedRoute(route: SavedRouteEntity) {
        viewModelScope.launch {
            repository.deleteSavedRoute(route)
            _userMessage.value = "Saved route removed."
        }
    }

    fun deleteRecordedRoute(id: Long) {
        viewModelScope.launch {
            repository.deleteRecordedRoute(id)
            _userMessage.value = "Recorded route deleted from Room DB."
        }
    }

    fun toggleFavoriteRecordedRoute(id: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.setFavoriteRecordedRoute(id, !currentFav)
        }
    }

    fun saveRecordedRoute(route: RecordedRouteEntity, waypoints: List<RouteWaypointEntity>) {
        viewModelScope.launch {
            repository.saveRecordedRouteWithWaypoints(route, waypoints)
            _userMessage.value = "Route saved to Room DB with ${waypoints.size} waypoints."
        }
    }

    fun toggleDayNightMapMode() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightTime = hour < 6 || hour >= 18
        if (_mapStyle.value == MapStyle.STREET) {
            _mapStyle.value = MapStyle.CYBER_DARK
            _userMessage.value = if (isNightTime) "Night Mode active (${hour}:00) — Low-glare dark map" else "Dark Mode active"
        } else {
            _mapStyle.value = MapStyle.STREET
            _userMessage.value = if (!isNightTime) "Day Mode active (${hour}:00) — High-contrast street map" else "Light Mode active"
        }
    }

    fun applySystemTimeMapMode() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNightTime = hour < 6 || hour >= 18
        _mapStyle.value = if (isNightTime) MapStyle.CYBER_DARK else MapStyle.STREET
        _userMessage.value = if (isNightTime) "Auto Night Mode set based on current time (${hour}:00)" else "Auto Day Mode set based on current time (${hour}:00)"
    }

    fun startNavigation() {
        val st = _routeState.value
        if (st.routeCoordinates.isEmpty()) return
        _routeState.value = st.copy(isNavigating = true, currentStepIndex = 0)
        startTracking()
        _userMessage.value = "Voice-guided navigation active."

        if (userPreferences.value.voiceGuidanceEnabled) {
            viewModelScope.launch {
                delay(300)
                val currentSt = _routeState.value
                voiceManager.speakAiRouteBriefing(
                    destName = currentSt.destQuery,
                    distanceKm = currentSt.distanceMeters / 1000.0,
                    durationMin = (currentSt.durationSeconds / 60.0).toInt().coerceAtLeast(1),
                    travelMode = currentSt.travelMode,
                    aiRecommendation = currentSt.aiRecommendation
                )
            }
        }
    }

    fun stopNavigation() {
        _routeState.value = _routeState.value.copy(isNavigating = false)
        stopTracking()
        voiceManager.stopSpeaking()
        _userMessage.value = "Navigation ended."
    }

    // --- Voice Guidance Controls ---
    fun toggleVoiceMute() {
        val isMuted = voiceManager.toggleMute()
        _userMessage.value = if (isMuted) "Voice guidance muted" else "Voice guidance unmuted"
    }

    fun repeatCurrentVoiceInstruction() {
        val st = _routeState.value
        val step = st.currentStep
        if (step != null) {
            val spoken = voiceManager.formatManeuverToSpokenText(step, step.distance)
            voiceManager.speak(spoken)
            _userMessage.value = "Voice: $spoken"
        } else if (st.aiRecommendation.isNotBlank()) {
            voiceManager.speakAiRouteBriefing(
                destName = st.destQuery,
                distanceKm = st.distanceMeters / 1000.0,
                durationMin = (st.durationSeconds / 60.0).toInt().coerceAtLeast(1),
                travelMode = st.travelMode,
                aiRecommendation = st.aiRecommendation
            )
        } else {
            voiceManager.speak("Continue along designated route corridor.")
        }
    }

    fun speakAiRouteBriefing() {
        val st = _routeState.value
        voiceManager.speakAiRouteBriefing(
            destName = st.destQuery,
            distanceKm = st.distanceMeters / 1000.0,
            durationMin = (st.durationSeconds / 60.0).toInt().coerceAtLeast(1),
            travelMode = st.travelMode,
            aiRecommendation = st.aiRecommendation
        )
        _userMessage.value = "Playing AI route voice briefing..."
    }

    fun nextNavigationStep() {
        val st = _routeState.value
        val steps = st.steps
        if (steps.isNotEmpty() && st.currentStepIndex < steps.size - 1) {
            val nextIndex = st.currentStepIndex + 1
            _routeState.value = st.copy(currentStepIndex = nextIndex)
            val nextStep = steps[nextIndex]
            val spoken = voiceManager.formatManeuverToSpokenText(nextStep, nextStep.distance)
            voiceManager.speak(spoken)
        } else if (steps.isNotEmpty() && st.currentStepIndex >= steps.size - 1) {
            voiceManager.speakArrival(st.destQuery)
        }
    }

    fun previousNavigationStep() {
        val st = _routeState.value
        val steps = st.steps
        if (steps.isNotEmpty() && st.currentStepIndex > 0) {
            val prevIndex = st.currentStepIndex - 1
            _routeState.value = st.copy(currentStepIndex = prevIndex)
            val prevStep = steps[prevIndex]
            val spoken = voiceManager.formatManeuverToSpokenText(prevStep, prevStep.distance)
            voiceManager.speak(spoken)
        }
    }

    fun testVoiceGuidance() {
        voiceManager.speak("GlobalCoreX Voice Navigation with AI Suggestions Engine is fully operational.")
        _userMessage.value = "Testing voice synthesizer..."
    }

    // --- SOS Emergency System ---
    fun initiateSosCountdown() {
        sosCountdownJob?.cancel()
        _isSosCountdownActive.value = true
        _sosCountdown.value = userPreferences.value.emergencyCountdownSec

        sosCountdownJob = viewModelScope.launch {
            while (_sosCountdown.value > 0) {
                delay(1000)
                _sosCountdown.value -= 1
            }
            _isSosCountdownActive.value = false
            triggerEmergencySos()
        }
    }

    fun cancelSosCountdown() {
        sosCountdownJob?.cancel()
        _isSosCountdownActive.value = false
        _userMessage.value = "SOS countdown cancelled."
    }

    fun triggerEmergencySos() {
        viewModelScope.launch {
            val lat = _gpsLocation.value.latitude
            val lng = _gpsLocation.value.longitude
            val address = repository.reverseGeocode(lat, lng)
            val event = repository.triggerSos(lat, lng, address, locationHelper.getBatteryLevel())
            _userMessage.value = "🚨 EMERGENCY SOS BROADCASTED! Contacts notified."
        }
    }

    fun cancelActiveSos(id: Long) {
        viewModelScope.launch {
            repository.cancelSos(id)
            _userMessage.value = "Emergency alert cancelled."
        }
    }

    fun addEmergencyContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        viewModelScope.launch {
            repository.addEmergencyContact(
                EmergencyContactEntity(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isPrimary = isPrimary
                )
            )
            _userMessage.value = "Emergency contact added: $name"
        }
    }

    fun deleteEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contact)
            _userMessage.value = "Contact removed."
        }
    }

    fun deleteEmergencyContactById(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyContactById(id)
            _userMessage.value = "Contact removed."
        }
    }

    // --- Live Location Sharing ---
    fun createShareSession(name: String, phone: String, durationMinutes: Int, message: String) {
        viewModelScope.launch {
            val lat = _gpsLocation.value.latitude
            val lng = _gpsLocation.value.longitude
            val session = repository.createShareSession(name, phone, durationMinutes, message, lat, lng)
            _userMessage.value = "Secure live sharing link created for $name (${durationMinutes}m)"
        }
    }

    fun stopSharing(id: String) {
        viewModelScope.launch {
            repository.stopSharingSession(id)
            _userMessage.value = "Live sharing session revoked."
        }
    }

    // --- Devices ---
    fun registerDevice(name: String, model: String) {
        viewModelScope.launch {
            repository.registerPairedDevice(name, model)
            _userMessage.value = "New device paired: $name"
        }
    }

    fun deleteDevice(device: DeviceEntity) {
        viewModelScope.launch {
            repository.deleteDevice(device)
            _userMessage.value = "Device unlinked."
        }
    }

    // --- History & Preferences ---
    fun deleteTrip(id: Long) {
        viewModelScope.launch {
            repository.deleteTrip(id)
            _userMessage.value = "Trip deleted."
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
            _userMessage.value = "Location and trip history cleared."
        }
    }

    fun updatePreferences(prefs: UserPreferencesEntity) {
        viewModelScope.launch {
            repository.updatePreferences(prefs)
            _userMessage.value = "Preferences saved."
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
