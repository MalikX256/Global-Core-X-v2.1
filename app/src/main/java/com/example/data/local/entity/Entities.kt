package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recorded_routes")
data class RecordedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val totalDistanceMeters: Double,
    val averageSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val elevationGainMeters: Double = 0.0,
    val startAddress: String = "",
    val endAddress: String = "",
    val startLat: Double = 0.0,
    val startLng: Double = 0.0,
    val endLat: Double = 0.0,
    val endLng: Double = 0.0,
    val travelMode: String = "driving", // driving, walking, cycling, running
    val polylineGeoJson: String = "",
    val waypointsCount: Int = 0,
    val aiSummary: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "route_waypoints",
    indices = [Index("routeId")]
)
data class RouteWaypointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speed: Float = 0f, // m/s
    val speedKmh: Float = 0f,
    val bearing: Float = 0f, // degrees
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)

data class RecordedRouteWithWaypoints(
    @Embedded val route: RecordedRouteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId"
    )
    val waypoints: List<RouteWaypointEntity>
)

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String, // unique internal user ID e.g. "user_gcx_9a8b7c"
    val username: String = "",
    val email: String,
    val displayName: String,
    val phone: String = "",
    val country: String = "",
    val avatarUrl: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val isEmailVerified: Boolean = false,
    val verificationCode: String = "",
    val authToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isCurrentLoggedIn: Boolean = false,
    val rememberMe: Boolean = true
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val deviceName: String,
    val model: String,
    val isThisDevice: Boolean = false,
    val isOnline: Boolean = true,
    val lastLatitude: Double = 0.0,
    val lastLongitude: Double = 0.0,
    val lastBatteryLevel: Int = 100,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "location_records")
data class LocationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val speed: Float = 0f, // in m/s
    val heading: Float = 0f, // in degrees
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val totalDistanceMeters: Double,
    val durationSeconds: Long,
    val averageSpeedKmh: Double,
    val maxSpeedKmh: Double,
    val startAddress: String = "",
    val endAddress: String = "",
    val travelMode: String = "driving", // driving, walking, cycling
    val isFavorite: Boolean = false
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originName: String,
    val originLat: Double,
    val originLng: Double,
    val destName: String,
    val destLat: Double,
    val destLng: Double,
    val distanceKm: Double,
    val durationMin: Int,
    val travelMode: String = "driving",
    val polylineGeoJson: String = "",
    val aiRecommendation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val relationship: String = "Family",
    val isPrimary: Boolean = false,
    val email: String = ""
)

@Entity(tableName = "sos_events")
data class SosEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val status: String = "ACTIVE", // ACTIVE, RESOLVED, CANCELLED
    val batteryLevel: Int = 100,
    val shareUrl: String = "",
    val cancelledAt: Long? = null
)

@Entity(tableName = "shared_sessions")
data class SharedSessionEntity(
    @PrimaryKey val id: String,
    val token: String,
    val contactName: String,
    val contactPhone: String,
    val durationMinutes: Int,
    val startTime: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isActive: Boolean = true,
    val customMessage: String = "",
    val lastLat: Double = 0.0,
    val lastLng: Double = 0.0
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkTheme: Boolean = true,
    val distanceUnit: String = "km", // km or miles
    val mapStyle: String = "CYBER_DARK", // CYBER_DARK, STREET, SATELLITE
    val avoidTolls: Boolean = false,
    val avoidHighways: Boolean = false,
    val emergencyCountdownSec: Int = 5,
    val liveTrackingIntervalSec: Int = 3,
    val batterySaverEnabled: Boolean = true,
    val voiceGuidanceEnabled: Boolean = true,
    // Location Privacy & Control
    val locationTrackingEnabled: Boolean = true,
    val locationHistoryEnabled: Boolean = true,
    val liveSharingEnabled: Boolean = true,
    val tripRecordingEnabled: Boolean = true,
    val sosLocationSharingEnabled: Boolean = true
)

@Entity(
    tableName = "map_tiles",
    primaryKeys = ["tileKey", "style"]
)
data class MapTileEntity(
    val tileKey: String, // e.g. "14/9678/8123" (z/x/y)
    val style: String,   // CYBER_DARK, STREET, SATELLITE
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
    val tileData: ByteArray,
    val regionId: Long? = null,
    val sizeBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MapTileEntity
        return tileKey == other.tileKey && style == other.style
    }

    override fun hashCode(): Int {
        var result = tileKey.hashCode()
        result = 31 * result + style.hashCode()
        return result
    }
}

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
    val minZoom: Int = 12,
    val maxZoom: Int = 15,
    val style: String = "STREET",
    val totalTiles: Int = 0,
    val downloadedTiles: Int = 0,
    val sizeBytes: Long = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

