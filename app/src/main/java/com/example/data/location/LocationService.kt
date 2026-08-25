package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LocationRecordEntity
import com.example.service.GpsLocationData
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume

/**
 * Interface defining location tracking operations using FusedLocationProviderClient
 * and persistence to the Room database.
 */
interface LocationService {
    /**
     * Emits continuous real-time location updates using FusedLocationProviderClient.
     */
    fun getLocationUpdates(
        intervalMs: Long = 2000L,
        minDistanceMeters: Float = 1.0f,
        priority: Int = Priority.PRIORITY_HIGH_ACCURACY
    ): Flow<GpsLocationData>

    /**
     * Tracks movement in real-time and automatically stores each location breadcrumb
     * into the local Room database (location_records table).
     */
    fun trackAndPersistMovement(
        tripId: Long? = null,
        intervalMs: Long = 2000L,
        minDistanceMeters: Float = 1.5f
    ): Flow<GpsLocationData>

    /**
     * Fetches the single last known location.
     */
    suspend fun getLastKnownLocation(): GpsLocationData?

    /**
     * Persists a location record explicitly into the Room database.
     */
    suspend fun saveLocationRecord(record: LocationRecordEntity): Long

    /**
     * Retrieves recent movement history from Room database.
     */
    fun getRecentLocationRecords(): Flow<List<LocationRecordEntity>>

    /**
     * Retrieves movement history for a specific trip from Room database.
     */
    suspend fun getLocationRecordsForTrip(tripId: Long): List<LocationRecordEntity>

    /**
     * Clears all stored location records from Room database.
     */
    suspend fun clearAllLocationRecords()
}

/**
 * Default implementation of LocationService using Google Play Services FusedLocationProviderClient
 * and Room Database persistence.
 */
class DefaultLocationService(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    private val database = AppDatabase.getInstance(context.applicationContext)
    private val locationDao = database.locationDao()

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(
        intervalMs: Long,
        minDistanceMeters: Float,
        priority: Int
    ): Flow<GpsLocationData> = callbackFlow {
        val minInterval = (intervalMs / 2).coerceAtLeast(500L)

        val locationRequest = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(minInterval)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f
                    val heading = if (loc.hasBearing()) loc.bearing else 0f

                    trySend(
                        GpsLocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            speedKmh = speedKmh,
                            heading = heading,
                            timestamp = loc.time,
                            isGpsActive = true
                        )
                    )
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    trySend(GpsLocationData(isGpsActive = false))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )

            // Emit immediate last known location if available
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f
                    val heading = if (loc.hasBearing()) loc.bearing else 0f
                    trySend(
                        GpsLocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            speedKmh = speedKmh,
                            heading = heading,
                            timestamp = loc.time,
                            isGpsActive = true
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            trySend(GpsLocationData(isGpsActive = false))
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    override fun trackAndPersistMovement(
        tripId: Long?,
        intervalMs: Long,
        minDistanceMeters: Float
    ): Flow<GpsLocationData> = flow {
        var lastSavedLat = 0.0
        var lastSavedLng = 0.0

        getLocationUpdates(intervalMs, minDistanceMeters, Priority.PRIORITY_HIGH_ACCURACY)
            .collect { gps ->
                emit(gps)

                if (gps.isGpsActive && (gps.latitude != 0.0 || gps.longitude != 0.0)) {
                    // Check if moved sufficiently to avoid redundant duplicate database entries
                    val distanceMoved = FloatArray(1)
                    if (lastSavedLat != 0.0 && lastSavedLng != 0.0) {
                        Location.distanceBetween(
                            lastSavedLat,
                            lastSavedLng,
                            gps.latitude,
                            gps.longitude,
                            distanceMoved
                        )
                    } else {
                        distanceMoved[0] = 100f
                    }

                    if (distanceMoved[0] >= minDistanceMeters) {
                        lastSavedLat = gps.latitude
                        lastSavedLng = gps.longitude

                        val record = LocationRecordEntity(
                            tripId = tripId,
                            latitude = gps.latitude,
                            longitude = gps.longitude,
                            accuracy = gps.accuracy,
                            speed = gps.speedKmh / 3.6f,
                            heading = gps.heading,
                            altitude = gps.altitude,
                            timestamp = gps.timestamp
                        )

                        scope.launch {
                            locationDao.insertLocation(record)
                        }
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): GpsLocationData? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f
                        val heading = if (loc.hasBearing()) loc.bearing else 0f
                        continuation.resume(
                            GpsLocationData(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                altitude = loc.altitude,
                                speedKmh = speedKmh,
                                heading = heading,
                                timestamp = loc.time,
                                isGpsActive = true
                            )
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }

    override suspend fun saveLocationRecord(record: LocationRecordEntity): Long = withContext(Dispatchers.IO) {
        locationDao.insertLocation(record)
    }

    override fun getRecentLocationRecords(): Flow<List<LocationRecordEntity>> {
        return locationDao.getRecentLocations()
    }

    override suspend fun getLocationRecordsForTrip(tripId: Long): List<LocationRecordEntity> = withContext(Dispatchers.IO) {
        locationDao.getLocationsForTrip(tripId)
    }

    override suspend fun clearAllLocationRecords() = withContext(Dispatchers.IO) {
        locationDao.deleteAllLocations()
    }
}
