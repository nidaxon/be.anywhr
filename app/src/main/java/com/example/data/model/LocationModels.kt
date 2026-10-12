package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 15.0,
    val accuracy: Float = 3.0f,
    val speedKmh: Float = 5.0f,
    val bearing: Float = 0.0f,
    val label: String = ""
) {
    fun distanceTo(other: LocationPoint): Double {
        val r = 6371000.0 // Earth radius in meters
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun bearingTo(other: LocationPoint): Float {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLon = Math.toRadians(other.longitude - longitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val initialBearing = Math.toDegrees(atan2(y, x))
        return ((initialBearing + 360) % 360).toFloat()
    }
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Favorites",
    val colorHex: String = "#06B6D4",
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 15.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Routes",
    val waypointsJson: String, // serialized JSON array of lat,lng
    val speedKmh: Float = 15.0f,
    val mode: String = "LOOP", // ONEWAY, LOOP, REVERSE
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppVersionMode {
    FULL, // Visual interactive map interface + manual coordinate inputs
    LITE  // Lightweight text search + manual coordinates only (zero map overhead)
}

enum class TripMode {
    ONE_WAY,
    LOOP,
    REVERSE
}

data class JoystickSettings(
    val sizeDp: Int = 160,
    val opacityPercent: Int = 85,
    val speedKmh: Float = 12.0f,
    val isLocked: Boolean = false,
    val isOverlayEnabled: Boolean = false
)

data class DriftSettings(
    val isEnabled: Boolean = false,
    val centerLat: Double = 0.0,
    val centerLng: Double = 0.0,
    val radiusMeters: Double = 50.0,
    val intervalSeconds: Int = 10,
    val isSmoothDrift: Boolean = true
)
