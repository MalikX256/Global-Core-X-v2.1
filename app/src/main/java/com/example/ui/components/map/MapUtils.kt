package com.example.ui.components.map

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

object MapUtils {
    const val TILE_SIZE = 256.0
    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Converts latitude/longitude to Mercator world pixel coordinates at zoom level
     */
    fun latLngToPixel(lat: Double, lng: Double, zoom: Float): Offset {
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val clampedLng = ((lng + 180.0) % 360.0) - 180.0

        val mapSize = TILE_SIZE * 2.0.pow(zoom.toDouble())
        val x = (clampedLng + 180.0) / 360.0 * mapSize

        val sinLat = sin(clampedLat * Math.PI / 180.0)
        val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI)) * mapSize

        return Offset(x.toFloat(), y.toFloat())
    }

    /**
     * Converts Mercator world pixel coordinates to LatLng at zoom level
     */
    fun pixelToLatLng(pixelX: Float, pixelY: Float, zoom: Float): Pair<Double, Double> {
        val mapSize = TILE_SIZE * 2.0.pow(zoom.toDouble())
        val lng = (pixelX.toDouble() / mapSize) * 360.0 - 180.0
        val n = Math.PI - (2.0 * Math.PI * pixelY.toDouble()) / mapSize
        val lat = 180.0 / Math.PI * atan(0.5 * (exp(n) - exp(-n)))
        return Pair(lat.coerceIn(-85.0, 85.0), lng.coerceIn(-180.0, 180.0))
    }

    /**
     * Calculates distance in meters between two coordinates using Haversine formula
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun formatDistance(meters: Double, isMiles: Boolean = false): String {
        return if (isMiles) {
            val miles = meters * 0.000621371
            if (miles < 0.1) "${(miles * 5280).roundToInt()} ft" else "${String.format("%.1f", miles)} mi"
        } else {
            if (meters < 1000) "${meters.roundToInt()} m" else "${String.format("%.1f", meters / 1000.0)} km"
        }
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }

    fun formatSpeed(speedKmh: Float, isMiles: Boolean = false): String {
        return if (isMiles) {
            "${(speedKmh * 0.621371f).roundToInt()} mph"
        } else {
            "${speedKmh.roundToInt()} km/h"
        }
    }

    /**
     * Converts Lat/Lng to Slippy Map tile X and Y at integer zoom level.
     */
    fun latLngToTileXY(lat: Double, lng: Double, zoom: Int): Pair<Int, Int> {
        val clampedLat = lat.coerceIn(-85.05112878, 85.05112878)
        val clampedLng = ((lng + 180.0) % 360.0) - 180.0
        val n = 1 shl zoom
        val x = ((clampedLng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val latRad = Math.toRadians(clampedLat)
        val y = ((1.0 - asinh(tan(latRad)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
        return Pair(x, y)
    }

    /**
     * Generates the list of all tile coordinates (z, x, y) for a geographic bounding box across zoom levels.
     */
    fun calculateTilesForBounds(
        minLat: Double,
        minLng: Double,
        maxLat: Double,
        maxLng: Double,
        minZoom: Int,
        maxZoom: Int
    ): List<Triple<Int, Int, Int>> {
        val tiles = mutableListOf<Triple<Int, Int, Int>>()
        for (z in minZoom..maxZoom) {
            val (minX, maxY) = latLngToTileXY(minLat, minLng, z)
            val (maxX, minY) = latLngToTileXY(maxLat, maxLng, z)

            val startX = min(minX, maxX)
            val endX = max(minX, maxX)
            val startY = min(minY, maxY)
            val endY = max(minY, maxY)

            for (x in startX..endX) {
                for (y in startY..endY) {
                    tiles.add(Triple(z, x, y))
                }
            }
        }
        return tiles
    }
}
