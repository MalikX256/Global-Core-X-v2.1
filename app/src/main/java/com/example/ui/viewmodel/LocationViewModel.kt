package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.RecordedRouteEntity
import com.example.data.local.entity.RecordedRouteWithWaypoints
import com.example.data.local.entity.RouteWaypointEntity
import com.example.data.repository.AppRepository
import com.example.service.GpsLocationData
import com.example.service.LocationHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class RouteRecordingState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val title: String = "",
    val travelMode: String = "driving",
    val startTime: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalDistanceMeters: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val maxSpeedKmh: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val waypoints: List<RouteWaypointEntity> = emptyList(),
    val startAddress: String = "",
    val endAddress: String = ""
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val repository = AppRepository(application)
    private val locationHelper = LocationHelper(application)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Permission state
    private val _hasLocationPermission = MutableStateFlow(checkLocationPermissions())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    // Real-time GPS Location
    private val _currentLocation = MutableStateFlow(GpsLocationData())
    val currentLocation: StateFlow<GpsLocationData> = _currentLocation.asStateFlow()

    private val _isGpsActive = MutableStateFlow(false)
    val isGpsActive: StateFlow<Boolean> = _isGpsActive.asStateFlow()

    // Live Tracking & Recording state
    private val _isLiveTracking = MutableStateFlow(false)
    val isLiveTracking: StateFlow<Boolean> = _isLiveTracking.asStateFlow()

    private val _recordingState = MutableStateFlow(RouteRecordingState())
    val recordingState: StateFlow<RouteRecordingState> = _recordingState.asStateFlow()

    // Historical Routes Retrieval from Room Database
    val allRecordedRoutes: StateFlow<List<RecordedRouteEntity>> = repository.allRecordedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordedRoutesWithWaypoints: StateFlow<List<RecordedRouteWithWaypoints>> = repository.allRecordedRoutesWithWaypoints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteRecordedRoutes: StateFlow<List<RecordedRouteEntity>> = repository.favoriteRecordedRoutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null
    private var lastRecordedLocation: Location? = null

    init {
        checkAndStartLocationUpdates()
    }

    fun checkLocationPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun onPermissionResult(isGranted: Boolean) {
        _hasLocationPermission.value = isGranted
        if (isGranted) {
            startRealTimeLocationUpdates()
        }
    }

    fun checkAndStartLocationUpdates() {
        val hasPerm = checkLocationPermissions()
        _hasLocationPermission.value = hasPerm
        if (hasPerm) {
            startRealTimeLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealTimeLocationUpdates(intervalMillis: Long = 2000L) {
        if (!checkLocationPermissions()) return

        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                    val heading = if (location.hasBearing()) location.bearing else 0f

                    val data = GpsLocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        altitude = location.altitude,
                        speedKmh = speedKmh,
                        heading = heading,
                        timestamp = location.time,
                        isGpsActive = true
                    )
                    _currentLocation.value = data
                    _isGpsActive.value = true

                    // If actively recording, log waypoint
                    if (_recordingState.value.isRecording && !_recordingState.value.isPaused) {
                        handleRecordedLocation(location, speedKmh, heading)
                    }
                }
            }

            override fun onLocationAvailability(avail: LocationAvailability) {
                _isGpsActive.value = avail.isLocationAvailable
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                    val heading = if (loc.hasBearing()) loc.bearing else 0f
                    _currentLocation.value = GpsLocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        altitude = loc.altitude,
                        speedKmh = speedKmh,
                        heading = heading,
                        timestamp = loc.time,
                        isGpsActive = true
                    )
                    _isGpsActive.value = true
                }
            }
        } catch (e: SecurityException) {
            _isGpsActive.value = false
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    // --- Live Tracking Controller ---
    fun toggleLiveTracking() {
        val newState = !_isLiveTracking.value
        _isLiveTracking.value = newState
        if (newState) {
            startRealTimeLocationUpdates(1500L)
        }
    }

    // --- Route Recording Controller ---
    fun startRecording(title: String = "Recorded Route", travelMode: String = "driving") {
        lastRecordedLocation = null
        val now = System.currentTimeMillis()
        _recordingState.value = RouteRecordingState(
            isRecording = true,
            isPaused = false,
            title = title,
            travelMode = travelMode,
            startTime = now,
            durationSeconds = 0L,
            totalDistanceMeters = 0.0,
            currentSpeedKmh = 0f,
            maxSpeedKmh = 0.0,
            averageSpeedKmh = 0.0,
            waypoints = emptyList()
        )

        // Capture initial waypoint if location is already known
        val cur = _currentLocation.value
        if (cur.latitude != 0.0 && cur.longitude != 0.0) {
            val firstWp = RouteWaypointEntity(
                routeId = 0,
                latitude = cur.latitude,
                longitude = cur.longitude,
                altitude = cur.altitude,
                speed = cur.speedKmh / 3.6f,
                speedKmh = cur.speedKmh,
                bearing = cur.heading,
                accuracy = cur.accuracy,
                timestamp = now,
                orderIndex = 0
            )
            _recordingState.update { it.copy(waypoints = listOf(firstWp)) }
        }

        startTimer()
    }

    fun pauseRecording() {
        _recordingState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
    }

    fun resumeRecording() {
        _recordingState.update { it.copy(isPaused = false) }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_recordingState.value.isRecording && !_recordingState.value.isPaused) {
                delay(1000L)
                _recordingState.update { state ->
                    val newDuration = state.durationSeconds + 1
                    val avgSpeed = if (newDuration > 0) {
                        (state.totalDistanceMeters / 1000.0) / (newDuration / 3600.0)
                    } else 0.0
                    state.copy(
                        durationSeconds = newDuration,
                        averageSpeedKmh = avgSpeed
                    )
                }
            }
        }
    }

    private fun handleRecordedLocation(location: Location, speedKmh: Float, heading: Float) {
        val currentWaypoints = _recordingState.value.waypoints.toMutableList()
        var distanceDelta = 0.0
        var elevationDelta = 0.0

        if (lastRecordedLocation != null) {
            val dist = location.distanceTo(lastRecordedLocation!!)
            if (dist < 1.5) return // filter tiny noise jitter
            distanceDelta = dist.toDouble()
            val altDiff = location.altitude - (lastRecordedLocation?.altitude ?: location.altitude)
            if (altDiff > 0) {
                elevationDelta = altDiff
            }
        }
        lastRecordedLocation = location

        val newWaypoint = RouteWaypointEntity(
            routeId = 0,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speed = location.speed,
            speedKmh = speedKmh,
            bearing = heading,
            accuracy = location.accuracy,
            timestamp = location.time,
            orderIndex = currentWaypoints.size
        )
        currentWaypoints.add(newWaypoint)

        _recordingState.update { state ->
            val newTotalDistance = state.totalDistanceMeters + distanceDelta
            val newMaxSpeed = maxOf(state.maxSpeedKmh, speedKmh.toDouble())
            val newElevation = state.elevationGainMeters + elevationDelta
            val avgSpeed = if (state.durationSeconds > 0) {
                (newTotalDistance / 1000.0) / (state.durationSeconds / 3600.0)
            } else speedKmh.toDouble()

            state.copy(
                totalDistanceMeters = newTotalDistance,
                currentSpeedKmh = speedKmh,
                maxSpeedKmh = newMaxSpeed,
                averageSpeedKmh = avgSpeed,
                elevationGainMeters = newElevation,
                waypoints = currentWaypoints
            )
        }
    }

    fun stopAndSaveRecording(
        customTitle: String? = null,
        description: String = "",
        aiSummary: String = "",
        onSaved: (Long) -> Unit = {}
    ) {
        timerJob?.cancel()
        val state = _recordingState.value
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            val startWp = state.waypoints.firstOrNull()
            val endWp = state.waypoints.lastOrNull()

            val title = customTitle?.ifBlank { null }
                ?: state.title.ifBlank { "Route ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(state.startTime))}" }

            val polylineCoords = state.waypoints.map { "[${it.longitude},${it.latitude}]" }
            val polylineGeoJson = """{"type":"LineString","coordinates":[${polylineCoords.joinToString(",")}]}"""

            val routeEntity = RecordedRouteEntity(
                title = title,
                description = description,
                startTime = if (state.startTime > 0) state.startTime else now,
                endTime = now,
                durationSeconds = state.durationSeconds,
                totalDistanceMeters = state.totalDistanceMeters,
                averageSpeedKmh = state.averageSpeedKmh,
                maxSpeedKmh = state.maxSpeedKmh,
                elevationGainMeters = state.elevationGainMeters,
                startAddress = state.startAddress,
                endAddress = state.endAddress,
                startLat = startWp?.latitude ?: _currentLocation.value.latitude,
                startLng = startWp?.longitude ?: _currentLocation.value.longitude,
                endLat = endWp?.latitude ?: _currentLocation.value.latitude,
                endLng = endWp?.longitude ?: _currentLocation.value.longitude,
                travelMode = state.travelMode,
                polylineGeoJson = polylineGeoJson,
                waypointsCount = state.waypoints.size,
                aiSummary = aiSummary,
                isFavorite = false,
                createdAt = now
            )

            val savedId = repository.saveRecordedRouteWithWaypoints(routeEntity, state.waypoints)

            // Reset recording state
            _recordingState.value = RouteRecordingState()
            lastRecordedLocation = null

            onSaved(savedId)
        }
    }

    fun discardRecording() {
        timerJob?.cancel()
        _recordingState.value = RouteRecordingState()
        lastRecordedLocation = null
    }

    fun deleteRecordedRoute(id: Long) {
        viewModelScope.launch {
            repository.deleteRecordedRoute(id)
        }
    }

    fun toggleFavoriteRecordedRoute(id: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.setFavoriteRecordedRoute(id, !currentFav)
        }
    }

    fun getRouteWithWaypoints(id: Long): Flow<RecordedRouteWithWaypoints?> {
        return repository.getRecordedRouteWithWaypoints(id)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        timerJob?.cancel()
    }
}
