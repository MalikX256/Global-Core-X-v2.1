package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Nominatim Geocoding Models
@JsonClass(generateAdapter = true)
data class NominatimSearchResult(
    @Json(name = "place_id") val placeId: Long? = null,
    @Json(name = "lat") val lat: String,
    @Json(name = "lon") val lon: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "importance") val importance: Double? = null
)

@JsonClass(generateAdapter = true)
data class NominatimReverseResult(
    @Json(name = "display_name") val displayName: String = "",
    @Json(name = "address") val address: NominatimAddress? = null
)

@JsonClass(generateAdapter = true)
data class NominatimAddress(
    @Json(name = "road") val road: String? = null,
    @Json(name = "suburb") val suburb: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "state") val state: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "postcode") val postcode: String? = null
)

// OSRM Routing Models
@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    @Json(name = "code") val code: String,
    @Json(name = "routes") val routes: List<OsrmRoute> = emptyList(),
    @Json(name = "waypoints") val waypoints: List<OsrmWaypoint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    @Json(name = "distance") val distance: Double, // in meters
    @Json(name = "duration") val duration: Double, // in seconds
    @Json(name = "weight") val weight: Double? = null,
    @Json(name = "geometry") val geometry: OsrmGeometry? = null,
    @Json(name = "legs") val legs: List<OsrmLeg> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmGeometry(
    @Json(name = "type") val type: String = "LineString",
    @Json(name = "coordinates") val coordinates: List<List<Double>> = emptyList() // [ [lng, lat], ... ]
)

@JsonClass(generateAdapter = true)
data class OsrmLeg(
    @Json(name = "distance") val distance: Double,
    @Json(name = "duration") val duration: Double,
    @Json(name = "summary") val summary: String = "",
    @Json(name = "steps") val steps: List<OsrmStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmStep(
    @Json(name = "distance") val distance: Double,
    @Json(name = "duration") val duration: Double,
    @Json(name = "name") val name: String = "",
    @Json(name = "maneuver") val maneuver: OsrmManeuver? = null
)

@JsonClass(generateAdapter = true)
data class OsrmManeuver(
    @Json(name = "type") val type: String = "turn",
    @Json(name = "modifier") val modifier: String? = null,
    @Json(name = "instruction") val instruction: String? = null,
    @Json(name = "location") val location: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmWaypoint(
    @Json(name = "name") val name: String = "",
    @Json(name = "location") val location: List<Double> = emptyList()
)

// Gemini API Models
@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)
