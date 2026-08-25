package com.example.data.location

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocationRecordEntity
import com.example.service.GpsLocationData
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LocationRepository provides real-time GPS tracking stream via FusedLocationProviderClient
 * and manages persistence of movement records to the Room database.
 */
class LocationRepository(
    context: Context,
    private val locationService: LocationService = DefaultLocationService(context)
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = AppDatabase.getInstance(context)
    private val locationDao = db.locationDao()

    private val _currentLocation = MutableStateFlow(GpsLocationData())
    val currentLocation: StateFlow<GpsLocationData> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    val recentLocations: Flow<List<LocationRecordEntity>> = locationDao.getRecentLocations()

    /**
     * Obtains real-time location updates from FusedLocationProviderClient.
     */
    fun getLocationUpdates(
        intervalMs: Long = 2000L,
        minDistanceMeters: Float = 1.0f,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<GpsLocationData> {
        return locationService.getLocationUpdates(intervalMs, minDistanceMeters, priority)
    }

    /**
     * Starts continuous tracking and automatically stores user movement breadcrumbs in Room DB.
     */
    fun startMovementTracking(
        tripId: Long? = null,
        intervalMs: Long = 2000L,
        minDistanceMeters: Float = 1.5f
    ): Flow<GpsLocationData> {
        _isTracking.value = true
        return locationService.trackAndPersistMovement(tripId, intervalMs, minDistanceMeters)
    }

    fun stopMovementTracking() {
        _isTracking.value = false
    }

    suspend fun saveLocationRecord(record: LocationRecordEntity): Long {
        return locationService.saveLocationRecord(record)
    }

    suspend fun getLastKnownLocation(): GpsLocationData? {
        return locationService.getLastKnownLocation()
    }

    fun getLocationsSince(sinceTimestamp: Long): Flow<List<LocationRecordEntity>> {
        return locationDao.getLocationsSince(sinceTimestamp)
    }

    suspend fun getLocationsForTrip(tripId: Long): List<LocationRecordEntity> {
        return locationService.getLocationRecordsForTrip(tripId)
    }

    suspend fun clearHistory() {
        locationService.clearAllLocationRecords()
    }
}
