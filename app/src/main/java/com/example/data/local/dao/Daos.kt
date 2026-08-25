package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentLoggedIn = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentLoggedIn = 1 LIMIT 1")
    suspend fun getCurrentUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByEmailOrUsername(identifier: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE users SET isCurrentLoggedIn = 1, authToken = :token WHERE id = :id")
    suspend fun setLoggedInUser(id: String, token: String)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isThisDevice DESC, lastUpdateTime DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity)

    @Delete
    suspend fun deleteDevice(device: DeviceEntity)

    @Query("UPDATE devices SET lastLatitude = :lat, lastLongitude = :lng, lastBatteryLevel = :battery, lastUpdateTime = :time WHERE id = :deviceId")
    suspend fun updateDeviceLocation(deviceId: String, lat: Double, lng: Double, battery: Int, time: Long)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_records ORDER BY timestamp DESC LIMIT 500")
    fun getRecentLocations(): Flow<List<LocationRecordEntity>>

    @Query("SELECT * FROM location_records WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLocationsForTrip(tripId: Long): List<LocationRecordEntity>

    @Query("SELECT * FROM location_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getLocationsSince(sinceTimestamp: Long): Flow<List<LocationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(record: LocationRecordEntity): Long

    @Query("DELETE FROM location_records WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Query("DELETE FROM location_records")
    suspend fun deleteAllLocations()
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE startTime >= :sinceTimestamp ORDER BY startTime DESC")
    fun getTripsSince(sinceTimestamp: Long): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getTripById(id: Long): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)

    @Query("DELETE FROM trips")
    suspend fun deleteAllTrips()
}

@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY timestamp DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity): Long

    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRouteById(id: Long)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, name ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("UPDATE emergency_contacts SET isPrimary = 0")
    suspend fun clearPrimaryContact()
}

@Dao
interface SosEventDao {
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllSosEvents(): Flow<List<SosEventEntity>>

    @Query("SELECT * FROM sos_events WHERE status = 'ACTIVE' ORDER BY timestamp DESC LIMIT 1")
    fun getActiveSosEvent(): Flow<SosEventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSosEvent(event: SosEventEntity): Long

    @Update
    suspend fun updateSosEvent(event: SosEventEntity)

    @Query("UPDATE sos_events SET status = 'CANCELLED', cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelSosEvent(id: Long, cancelledAt: Long)
}

@Dao
interface SharedSessionDao {
    @Query("SELECT * FROM shared_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SharedSessionEntity>>

    @Query("SELECT * FROM shared_sessions WHERE isActive = 1 AND expiresAt > :currentTime ORDER BY startTime DESC")
    fun getActiveSessions(currentTime: Long): Flow<List<SharedSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SharedSessionEntity)

    @Query("UPDATE shared_sessions SET isActive = 0 WHERE id = :id")
    suspend fun deactivateSession(id: String)

    @Query("UPDATE shared_sessions SET lastLat = :lat, lastLng = :lng WHERE id = :id")
    suspend fun updateSessionLocation(id: String, lat: Double, lng: Double)
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: UserPreferencesEntity)
}

@Dao
interface RecordedRouteDao {
    @Query("SELECT * FROM recorded_routes ORDER BY startTime DESC")
    fun getAllRecordedRoutes(): Flow<List<RecordedRouteEntity>>

    @Query("SELECT * FROM recorded_routes WHERE id = :id")
    fun getRecordedRouteById(id: Long): Flow<RecordedRouteEntity?>

    @Transaction
    @Query("SELECT * FROM recorded_routes WHERE id = :id")
    fun getRecordedRouteWithWaypoints(id: Long): Flow<RecordedRouteWithWaypoints?>

    @Transaction
    @Query("SELECT * FROM recorded_routes ORDER BY startTime DESC")
    fun getAllRecordedRoutesWithWaypoints(): Flow<List<RecordedRouteWithWaypoints>>

    @Query("SELECT * FROM recorded_routes WHERE isFavorite = 1 ORDER BY startTime DESC")
    fun getFavoriteRoutes(): Flow<List<RecordedRouteEntity>>

    @Query("SELECT * FROM recorded_routes WHERE startTime >= :sinceTimestamp ORDER BY startTime DESC")
    fun getRecordedRoutesSince(sinceTimestamp: Long): Flow<List<RecordedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordedRoute(route: RecordedRouteEntity): Long

    @Update
    suspend fun updateRecordedRoute(route: RecordedRouteEntity)

    @Query("UPDATE recorded_routes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteRecordedRoute(route: RecordedRouteEntity)

    @Query("DELETE FROM recorded_routes WHERE id = :id")
    suspend fun deleteRecordedRouteById(id: Long)

    @Query("DELETE FROM recorded_routes")
    suspend fun deleteAllRecordedRoutes()
}

@Dao
interface RouteWaypointDao {
    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY orderIndex ASC, timestamp ASC")
    fun getWaypointsForRoute(routeId: Long): Flow<List<RouteWaypointEntity>>

    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY orderIndex ASC, timestamp ASC")
    suspend fun getWaypointsListForRoute(routeId: Long): List<RouteWaypointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoint(waypoint: RouteWaypointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<RouteWaypointEntity>)

    @Query("DELETE FROM route_waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypointsForRoute(routeId: Long)

    @Query("DELETE FROM route_waypoints")
    suspend fun deleteAllWaypoints()
}

@Dao
interface MapTileDao {
    @Query("SELECT * FROM map_tiles WHERE tileKey = :tileKey AND style = :style LIMIT 1")
    suspend fun getTile(tileKey: String, style: String): MapTileEntity?

    @Query("SELECT tileKey FROM map_tiles WHERE style = :style")
    suspend fun getAllCachedTileKeys(style: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: MapTileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<MapTileEntity>)

    @Query("SELECT COUNT(*) FROM map_tiles")
    fun getCachedTilesCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM map_tiles")
    fun getTotalCachedBytes(): Flow<Long>

    @Query("DELETE FROM map_tiles WHERE regionId = :regionId")
    suspend fun deleteTilesForRegion(regionId: Long)

    @Query("DELETE FROM map_tiles")
    suspend fun clearAllTiles()
}

@Dao
interface OfflineRegionDao {
    @Query("SELECT * FROM offline_regions ORDER BY createdAt DESC")
    fun getAllRegions(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions WHERE id = :id")
    suspend fun getRegionById(id: Long): OfflineRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: OfflineRegionEntity): Long

    @Update
    suspend fun updateRegion(region: OfflineRegionEntity)

    @Delete
    suspend fun deleteRegion(region: OfflineRegionEntity)

    @Query("DELETE FROM offline_regions WHERE id = :id")
    suspend fun deleteRegionById(id: Long)
}

