package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.entity.LocationRecordEntity
import com.example.data.local.entity.TripEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

data class ActiveTripState(
    val isTracking: Boolean = false,
    val tripId: Long = 0,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val currentSpeedKmh: Float = 0f,
    val averageSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val pointsCount: Int = 0,
    val startTime: Long = 0
)

class TrackingService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationHelper: LocationHelper
    private lateinit var repository: AppRepository

    private var currentTripId: Long = 0
    private var startTime: Long = 0
    private var lastLocation: Location? = null
    private var accumulatedDistanceMeters: Double = 0.0
    private var maxSpeed: Float = 0f
    private var locationCount: Int = 0
    private var timerJob: Job? = null

    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val CHANNEL_ID = "globalcorex_tracking_channel"
        const val NOTIFICATION_ID = 9001

        private val _trackingState = MutableStateFlow(ActiveTripState())
        val trackingState = _trackingState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        locationHelper = LocationHelper(this)
        repository = AppRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_trackingState.value.isTracking) return

        startTime = System.currentTimeMillis()
        currentTripId = System.currentTimeMillis()
        accumulatedDistanceMeters = 0.0
        maxSpeed = 0f
        locationCount = 0
        lastLocation = null

        startForeground(NOTIFICATION_ID, buildNotification("Recording route... 0.0 km"))

        _trackingState.value = ActiveTripState(
            isTracking = true,
            tripId = currentTripId,
            startTime = startTime
        )

        // Timer job to update duration
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val durationSec = (System.currentTimeMillis() - startTime) / 1000
                val avgSpeed = if (durationSec > 0) {
                    (accumulatedDistanceMeters / durationSec * 3.6).toFloat()
                } else 0f

                _trackingState.value = _trackingState.value.copy(
                    durationSeconds = durationSec,
                    averageSpeedKmh = avgSpeed,
                    distanceMeters = accumulatedDistanceMeters,
                    maxSpeedKmh = maxSpeed,
                    pointsCount = locationCount
                )

                updateNotification(
                    "${String.format("%.2f", accumulatedDistanceMeters / 1000.0)} km • ${String.format("%02d:%02d", durationSec / 60, durationSec % 60)}"
                )
            }
        }

        // Location listener job
        serviceScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            repository.userPreferences.flatMapLatest { prefs ->
                val saverEnabled = prefs?.batterySaverEnabled ?: true
                val baseIntervalSec = prefs?.liveTrackingIntervalSec ?: 2
                val intervalMs = locationHelper.getEffectiveIntervalMillis(baseIntervalSec, saverEnabled)
                locationHelper.getLocationFlow(intervalMs, saverEnabled)
            }.collect { gps ->
                if (gps.isGpsActive && (gps.latitude != 0.0 || gps.longitude != 0.0)) {
                    val loc = Location("gps").apply {
                        latitude = gps.latitude
                        longitude = gps.longitude
                        accuracy = gps.accuracy
                        speed = gps.speedKmh / 3.6f
                        bearing = gps.heading
                        time = gps.timestamp
                    }

                    if (lastLocation != null) {
                        val distance = lastLocation!!.distanceTo(loc)
                        if (distance > 1.0f) { // filter micro GPS jitter
                            accumulatedDistanceMeters += distance
                        }
                    }
                    lastLocation = loc
                    locationCount++
                    if (gps.speedKmh > maxSpeed) {
                        maxSpeed = gps.speedKmh
                    }

                    // Save location record to Room
                    repository.saveLocationRecord(
                        LocationRecordEntity(
                            tripId = currentTripId,
                            latitude = gps.latitude,
                            longitude = gps.longitude,
                            accuracy = gps.accuracy,
                            speed = gps.speedKmh / 3.6f,
                            heading = gps.heading,
                            altitude = gps.altitude,
                            timestamp = gps.timestamp
                        )
                    )

                    // Update device location
                    repository.updateThisDeviceLocation(
                        lat = gps.latitude,
                        lng = gps.longitude,
                        battery = locationHelper.getBatteryLevel()
                    )

                    _trackingState.value = _trackingState.value.copy(
                        currentSpeedKmh = gps.speedKmh,
                        maxSpeedKmh = maxSpeed,
                        distanceMeters = accumulatedDistanceMeters,
                        pointsCount = locationCount
                    )
                }
            }
        }
    }

    private fun stopTracking() {
        timerJob?.cancel()
        val endTime = System.currentTimeMillis()
        val durationSec = (endTime - startTime) / 1000
        val avgSpeed = if (durationSec > 0) (accumulatedDistanceMeters / durationSec * 3.6) else 0.0

        serviceScope.launch {
            if (accumulatedDistanceMeters > 10.0 || durationSec > 10) {
                val startAddr = if (lastLocation != null) repository.reverseGeocode(lastLocation!!.latitude, lastLocation!!.longitude) else "Route Track"
                repository.saveTrip(
                    TripEntity(
                        title = "Recorded Trip #${(1000..9999).random()}",
                        startTime = startTime,
                        endTime = endTime,
                        totalDistanceMeters = accumulatedDistanceMeters,
                        durationSeconds = durationSec,
                        averageSpeedKmh = avgSpeed,
                        maxSpeedKmh = maxSpeed.toDouble(),
                        startAddress = startAddr,
                        endAddress = startAddr,
                        travelMode = "driving"
                    )
                )
            }
        }

        _trackingState.value = ActiveTripState(isTracking = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GlobalCoreX Live Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time distance and navigation recording status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GlobalCoreX • Live Navigation Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
