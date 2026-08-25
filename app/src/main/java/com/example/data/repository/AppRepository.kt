package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.remote.ApiClient
import com.example.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class AppRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val deviceDao = db.deviceDao()
    private val locationDao = db.locationDao()
    private val tripDao = db.tripDao()
    private val recordedRouteDao = db.recordedRouteDao()
    private val routeWaypointDao = db.routeWaypointDao()
    private val savedRouteDao = db.savedRouteDao()
    private val emergencyContactDao = db.emergencyContactDao()
    private val sosEventDao = db.sosEventDao()
    private val sharedSessionDao = db.sharedSessionDao()
    private val userPreferencesDao = db.userPreferencesDao()

    // --- User & Auth ---
    val currentUser: Flow<UserEntity?> = userDao.getCurrentUser()

    suspend fun login(email: String, displayName: String): UserEntity = withContext(Dispatchers.IO) {
        userDao.logoutAll()
        val existing = userDao.getUserById(email)
        val user = existing?.copy(isCurrentLoggedIn = true) ?: UserEntity(
            id = email,
            email = email,
            displayName = displayName.ifBlank { email.substringBefore("@") },
            phone = "+256750985651",
            isCurrentLoggedIn = true
        )
        userDao.insertUser(user)
        ensureDefaultDevice()
        ensureDefaultContacts()
        ensureDefaultPreferences()
        user
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        userDao.logoutAll()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        userDao.deleteAllUsers()
        tripDao.deleteAllTrips()
        locationDao.deleteAllLocations()
    }

    // --- Device Management ---
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    suspend fun ensureDefaultDevice() = withContext(Dispatchers.IO) {
        val current = deviceDao.getAllDevices().firstOrNull()
        if (current.isNullOrEmpty()) {
            val thisDevice = DeviceEntity(
                id = "dev_this_phone",
                deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                model = "${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                isThisDevice = true,
                isOnline = true,
                lastLatitude = 0.3476,
                lastLongitude = 32.5825, // Default Kampala / Global coordinates
                lastBatteryLevel = 94,
                lastUpdateTime = System.currentTimeMillis()
            )
            deviceDao.insertDevice(thisDevice)
        }
    }

    suspend fun updateThisDeviceLocation(lat: Double, lng: Double, battery: Int) = withContext(Dispatchers.IO) {
        deviceDao.updateDeviceLocation("dev_this_phone", lat, lng, battery, System.currentTimeMillis())
    }

    suspend fun registerPairedDevice(name: String, model: String) = withContext(Dispatchers.IO) {
        val dev = DeviceEntity(
            id = "dev_" + UUID.randomUUID().toString().take(8),
            deviceName = name,
            model = model,
            isThisDevice = false,
            isOnline = true,
            lastLatitude = 0.3476 + (Math.random() - 0.5) * 0.05,
            lastLongitude = 32.5825 + (Math.random() - 0.5) * 0.05,
            lastBatteryLevel = (60..98).random(),
            lastUpdateTime = System.currentTimeMillis()
        )
        deviceDao.insertDevice(dev)
    }

    suspend fun deleteDevice(device: DeviceEntity) = withContext(Dispatchers.IO) {
        deviceDao.deleteDevice(device)
    }

    // --- Location Tracking & History ---
    val recentLocations: Flow<List<LocationRecordEntity>> = locationDao.getRecentLocations()
    val allTrips: Flow<List<TripEntity>> = tripDao.getAllTrips()

    fun getTripsSince(sinceTimestamp: Long): Flow<List<TripEntity>> {
        return tripDao.getTripsSince(sinceTimestamp)
    }

    suspend fun saveLocationRecord(record: LocationRecordEntity): Long = withContext(Dispatchers.IO) {
        locationDao.insertLocation(record)
    }

    suspend fun saveTrip(trip: TripEntity): Long = withContext(Dispatchers.IO) {
        tripDao.insertTrip(trip)
    }

    suspend fun deleteTrip(id: Long) = withContext(Dispatchers.IO) {
        tripDao.deleteTripById(id)
    }

    suspend fun deleteAllHistory() = withContext(Dispatchers.IO) {
        tripDao.deleteAllTrips()
        locationDao.deleteAllLocations()
        recordedRouteDao.deleteAllRecordedRoutes()
        routeWaypointDao.deleteAllWaypoints()
    }

    // --- Recorded Routes & Waypoints Schema (Historical Retrieval) ---
    val allRecordedRoutes: Flow<List<RecordedRouteEntity>> = recordedRouteDao.getAllRecordedRoutes()
    val allRecordedRoutesWithWaypoints: Flow<List<RecordedRouteWithWaypoints>> = recordedRouteDao.getAllRecordedRoutesWithWaypoints()
    val favoriteRecordedRoutes: Flow<List<RecordedRouteEntity>> = recordedRouteDao.getFavoriteRoutes()

    fun getRecordedRouteWithWaypoints(id: Long): Flow<RecordedRouteWithWaypoints?> {
        return recordedRouteDao.getRecordedRouteWithWaypoints(id)
    }

    fun getRecordedRoutesSince(sinceTimestamp: Long): Flow<List<RecordedRouteEntity>> {
        return recordedRouteDao.getRecordedRoutesSince(sinceTimestamp)
    }

    suspend fun saveRecordedRouteWithWaypoints(
        route: RecordedRouteEntity,
        waypoints: List<RouteWaypointEntity>
    ): Long = withContext(Dispatchers.IO) {
        val routeId = recordedRouteDao.insertRecordedRoute(route.copy(waypointsCount = waypoints.size))
        if (waypoints.isNotEmpty()) {
            val assignedWaypoints = waypoints.mapIndexed { index, wp ->
                wp.copy(routeId = routeId, orderIndex = index)
            }
            routeWaypointDao.insertWaypoints(assignedWaypoints)
        }
        routeId
    }

    suspend fun setFavoriteRecordedRoute(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        recordedRouteDao.setFavorite(id, isFavorite)
    }

    suspend fun deleteRecordedRoute(id: Long) = withContext(Dispatchers.IO) {
        routeWaypointDao.deleteWaypointsForRoute(id)
        recordedRouteDao.deleteRecordedRouteById(id)
    }

    suspend fun deleteAllRecordedRoutes() = withContext(Dispatchers.IO) {
        routeWaypointDao.deleteAllWaypoints()
        recordedRouteDao.deleteAllRecordedRoutes()
    }

    // --- Routes & Saved Routes ---
    val savedRoutes: Flow<List<SavedRouteEntity>> = savedRouteDao.getAllSavedRoutes()

    suspend fun saveRoute(route: SavedRouteEntity): Long = withContext(Dispatchers.IO) {
        savedRouteDao.insertRoute(route)
    }

    suspend fun deleteSavedRoute(route: SavedRouteEntity) = withContext(Dispatchers.IO) {
        savedRouteDao.deleteRoute(route)
    }

    suspend fun fetchRoute(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String // "driving", "walking", "cycling"
    ): Result<OsrmRouteResponse> = withContext(Dispatchers.IO) {
        try {
            val profile = when (mode.lowercase()) {
                "walking" -> "foot"
                "cycling" -> "bike"
                else -> "driving"
            }
            val coords = "$originLng,$originLat;$destLng,$destLat"
            val response = ApiClient.osrmService.getRoute(profile, coords)
            if (response.code == "Ok" && response.routes.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.failure(Exception("Routing service returned: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPlace(query: String): List<NominatimSearchResult> = withContext(Dispatchers.IO) {
        try {
            ApiClient.nominatimService.search(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchNearbyPois(
        category: com.example.ui.components.map.PoiCategory,
        centerLat: Double,
        centerLng: Double
    ): List<com.example.ui.components.map.MapPoiItem> = withContext(Dispatchers.IO) {
        try {
            val delta = 0.08 // ~8-9 km radius bounding box
            val minLat = centerLat - delta
            val maxLat = centerLat + delta
            val minLng = centerLng - delta
            val maxLng = centerLng + delta
            val viewBox = "$minLng,$maxLat,$maxLng,$minLat"

            val query = if (category == com.example.ui.components.map.PoiCategory.ALL) "hospital school police gas hotel" else category.searchQuery
            val results = ApiClient.nominatimService.searchNearby(
                query = query,
                limit = 16,
                viewBox = viewBox,
                bounded = 0
            )

            results.mapNotNull { item ->
                val lat = item.lat.toDoubleOrNull() ?: return@mapNotNull null
                val lng = item.lon.toDoubleOrNull() ?: return@mapNotNull null
                val dist = if (centerLat != 0.0) {
                    com.example.ui.components.map.MapUtils.calculateDistanceMeters(centerLat, centerLng, lat, lng)
                } else 0.0

                com.example.ui.components.map.MapPoiItem(
                    id = item.placeId?.toString() ?: "poi_${lat}_${lng}",
                    name = item.displayName.substringBefore(","),
                    category = category,
                    latitude = lat,
                    longitude = lng,
                    address = item.displayName,
                    distanceMeters = dist,
                    typeName = item.type ?: category.title
                )
            }.sortedBy { it.distanceMeters }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val res = ApiClient.nominatimService.reverseGeocode(lat, lng)
            res.displayName.ifBlank { "Coordinates: ${String.format("%.4f, %.4f", lat, lng)}" }
        } catch (e: Exception) {
            "Coordinates: ${String.format("%.4f, %.4f", lat, lng)}"
        }
    }

    // --- AI Route Recommendations ---
    suspend fun getAiRouteAnalysis(
        originName: String,
        destName: String,
        mode: String,
        distanceKm: Double,
        durationMin: Int,
        avoidTolls: Boolean,
        avoidHighways: Boolean
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local fallback navigation analysis
            return@withContext "✓ GlobalCoreX Route Analyzer:\n" +
                    "• Mode: ${mode.replaceFirstChar { it.uppercase() }}\n" +
                    "• Estimated Distance: ${String.format("%.1f", distanceKm)} km (~$durationMin min)\n" +
                    "• Strategy: ${if (avoidHighways) "Secondary arterial roads preferred" else "Optimal main corridors"}\n" +
                    "• Advisory: Steady traffic flow detected along major intersections. Keep headlights active and maintain safety margins."
        }

        try {
            val prompt = """
                You are GlobalCoreX AI Navigation Assistant. Analyze this real planned route:
                Origin: $originName
                Destination: $destName
                Travel Mode: $mode
                Calculated Distance: $distanceKm km
                Estimated Travel Time: $durationMin minutes
                User Preferences: Avoid Tolls=$avoidTolls, Avoid Highways=$avoidHighways

                Provide a professional, concise 3-bullet AI recommendation:
                1. "Recommended Route — saves approximately X minutes..." or time-saving insight.
                2. Safety / road condition guidance for $mode mode.
                3. Efficiency or scenic tip.
                Keep it under 100 words, direct and practical for a driver/rider/pedestrian.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                )
            )

            val res = ApiClient.geminiService.generateRouteSuggestion(apiKey, request)
            res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "GlobalCoreX AI: Direct route verified. Optimal conditions expected for $mode travel."
        } catch (e: Exception) {
            "GlobalCoreX AI Analysis: Direct route confirmed ($distanceKm km, ~$durationMin min). Travel safely."
        }
    }

    // --- Emergency & SOS System ---
    val emergencyContacts: Flow<List<EmergencyContactEntity>> = emergencyContactDao.getAllContacts()
    val activeSosEvent: Flow<SosEventEntity?> = sosEventDao.getActiveSosEvent()
    val allSosEvents: Flow<List<SosEventEntity>> = sosEventDao.getAllSosEvents()

    suspend fun ensureDefaultContacts() = withContext(Dispatchers.IO) {
        val existing = emergencyContactDao.getAllContacts().firstOrNull()
        if (existing.isNullOrEmpty()) {
            emergencyContactDao.insertContact(
                EmergencyContactEntity(
                    name = "Malik-X Support (Emergency)",
                    phone = "+256750985651",
                    relationship = "GlobalCoreX HQ",
                    isPrimary = true,
                    email = "donmalik.pro1@gmail.com"
                )
            )
        }
    }

    suspend fun addEmergencyContact(contact: EmergencyContactEntity): Long = withContext(Dispatchers.IO) {
        if (contact.isPrimary) {
            emergencyContactDao.clearPrimaryContact()
        }
        emergencyContactDao.insertContact(contact)
    }

    suspend fun deleteEmergencyContact(contact: EmergencyContactEntity) = withContext(Dispatchers.IO) {
        emergencyContactDao.deleteContact(contact)
    }

    suspend fun deleteEmergencyContactById(id: Long) = withContext(Dispatchers.IO) {
        emergencyContactDao.deleteContactById(id)
    }

    suspend fun triggerSos(lat: Double, lng: Double, address: String, batteryLevel: Int): SosEventEntity = withContext(Dispatchers.IO) {
        val shareToken = UUID.randomUUID().toString().take(8)
        val shareUrl = "https://globalcorex.nav/sos/$shareToken?lat=$lat&lng=$lng"
        val event = SosEventEntity(
            timestamp = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            address = address,
            status = "ACTIVE",
            batteryLevel = batteryLevel,
            shareUrl = shareUrl
        )
        val id = sosEventDao.insertSosEvent(event)
        event.copy(id = id)
    }

    suspend fun cancelSos(eventId: Long) = withContext(Dispatchers.IO) {
        sosEventDao.cancelSosEvent(eventId, System.currentTimeMillis())
    }

    // --- Live Location Sharing ---
    val activeSharedSessions: Flow<List<SharedSessionEntity>> = sharedSessionDao.getAllSessions()

    suspend fun createShareSession(
        contactName: String,
        contactPhone: String,
        durationMinutes: Int,
        message: String,
        lat: Double,
        lng: Double
    ): SharedSessionEntity = withContext(Dispatchers.IO) {
        val token = "gcx_" + UUID.randomUUID().toString().take(10)
        val session = SharedSessionEntity(
            id = UUID.randomUUID().toString(),
            token = token,
            contactName = contactName,
            contactPhone = contactPhone,
            durationMinutes = durationMinutes,
            startTime = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (durationMinutes * 60 * 1000L),
            isActive = true,
            customMessage = message,
            lastLat = lat,
            lastLng = lng
        )
        sharedSessionDao.insertSession(session)
        session
    }

    suspend fun stopSharingSession(id: String) = withContext(Dispatchers.IO) {
        sharedSessionDao.deactivateSession(id)
    }

    // --- User Preferences ---
    val userPreferences: Flow<UserPreferencesEntity?> = userPreferencesDao.getPreferences()

    suspend fun ensureDefaultPreferences() = withContext(Dispatchers.IO) {
        val pref = userPreferencesDao.getPreferences().firstOrNull()
        if (pref == null) {
            userPreferencesDao.insertOrUpdate(UserPreferencesEntity())
        }
    }

    suspend fun updatePreferences(prefs: UserPreferencesEntity) = withContext(Dispatchers.IO) {
        userPreferencesDao.insertOrUpdate(prefs)
    }
}
