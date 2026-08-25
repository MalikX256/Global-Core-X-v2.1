package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GpsLocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speedKmh: Float = 0f,
    val heading: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val isGpsActive: Boolean = false
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            85
        }
    }

    fun isLowBattery(): Boolean = getBatteryLevel() <= 20

    fun isBatterySaverActive(batterySaverEnabled: Boolean): Boolean {
        return batterySaverEnabled && isLowBattery()
    }

    fun getEffectiveIntervalMillis(baseIntervalSec: Int, batterySaverEnabled: Boolean): Long {
        return if (isBatterySaverActive(batterySaverEnabled)) {
            // Automatically reduce frequency to 12s - 15s to conserve power when below 20% battery
            maxOf(baseIntervalSec * 4, 12) * 1000L
        } else {
            (baseIntervalSec * 1000L).coerceAtLeast(1000L)
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(
        intervalMillis: Long = 3000L,
        batterySaverEnabled: Boolean = false
    ): Flow<GpsLocationData> = callbackFlow {
        val currentBattery = getBatteryLevel()
        val isSaverActive = batterySaverEnabled && currentBattery <= 20
        val effectiveInterval = if (isSaverActive) {
            maxOf(intervalMillis * 4, 12000L)
        } else {
            intervalMillis
        }
        val minInterval = if (isSaverActive) 6000L else (effectiveInterval / 2).coerceAtLeast(1000L)
        val priority = if (isSaverActive) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY
        val minDistance = if (isSaverActive) 5.0f else 1.0f

        val locationRequest = LocationRequest.Builder(
            priority,
            effectiveInterval
        ).setMinUpdateIntervalMillis(minInterval)
            .setMinUpdateDistanceMeters(minDistance)
            .build()

        val locationCallback = object : LocationCallback() {
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

            override fun onLocationAvailability(avail: LocationAvailability) {
                if (!avail.isLocationAvailable) {
                    trySend(GpsLocationData(isGpsActive = false))
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            // Also try to get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    trySend(
                        GpsLocationData(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy,
                            altitude = loc.altitude,
                            speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f) else 0f,
                            heading = if (loc.hasBearing()) loc.bearing else 0f,
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
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
