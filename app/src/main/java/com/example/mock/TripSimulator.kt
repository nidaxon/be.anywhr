package com.example.mock

import com.example.data.model.LocationPoint
import com.example.data.model.TripMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class TripStatus(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentPoint: LocationPoint? = null,
    val currentLegIndex: Int = 0,
    val totalWaypoints: Int = 0,
    val remainingDistanceMeters: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val speedKmh: Float = 15.0f,
    val tripMode: TripMode = TripMode.LOOP
)

class TripSimulator(
    private val onLocationUpdated: (LocationPoint) -> Unit
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _status = MutableStateFlow(TripStatus())
    val status: StateFlow<TripStatus> = _status.asStateFlow()

    private var waypoints: List<LocationPoint> = emptyList()
    private var currentIndex = 0
    private var isReversing = false
    private var currentPosition: LocationPoint? = null
    private var currentSpeedKmh: Float = 15.0f
    private var mode: TripMode = TripMode.LOOP

    fun startTrip(
        points: List<LocationPoint>,
        speedKmh: Float = 15.0f,
        tripMode: TripMode = TripMode.LOOP
    ) {
        if (points.size < 2) return
        stopTrip()

        waypoints = points
        currentIndex = 0
        isReversing = false
        currentSpeedKmh = speedKmh
        mode = tripMode
        currentPosition = points.first()

        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += points[i].distanceTo(points[i + 1])
        }

        _status.value = TripStatus(
            isRunning = true,
            isPaused = false,
            currentPoint = currentPosition,
            currentLegIndex = 0,
            totalWaypoints = points.size,
            remainingDistanceMeters = totalDist,
            totalDistanceMeters = totalDist,
            speedKmh = currentSpeedKmh,
            tripMode = mode
        )

        currentPosition?.let { onLocationUpdated(it) }

        job = scope.launch {
            val tickIntervalMs = 500L
            while (isActive) {
                if (_status.value.isPaused) {
                    delay(tickIntervalMs)
                    continue
                }

                val current = currentPosition ?: break
                val targetIndex = getTargetIndex()
                if (targetIndex < 0 || targetIndex >= waypoints.size) {
                    // Reached end of trip
                    stopTrip()
                    break
                }

                val target = waypoints[targetIndex]
                val distToTarget = current.distanceTo(target)
                val metersPerSecond = currentSpeedKmh / 3.6f
                val stepMeters = metersPerSecond * (tickIntervalMs / 1000.0f)

                if (distToTarget <= stepMeters) {
                    // Reached target waypoint
                    currentPosition = target
                    onLocationUpdated(target)
                    advanceWaypoint()
                } else {
                    // Interpolate step towards target
                    val bearing = current.bearingTo(target)
                    val radBearing = Math.toRadians(bearing.toDouble())
                    val dLat = (stepMeters * cos(radBearing)) / 111111.0
                    val dLon = (stepMeters * sin(radBearing)) / (111111.0 * cos(Math.toRadians(current.latitude)))

                    val nextPoint = LocationPoint(
                        latitude = current.latitude + dLat,
                        longitude = current.longitude + dLon,
                        speedKmh = currentSpeedKmh,
                        bearing = bearing,
                        label = "Auto Trip: Leg ${currentIndex + 1}/${waypoints.size}"
                    )
                    currentPosition = nextPoint
                    onLocationUpdated(nextPoint)
                }

                updateMetrics()
                delay(tickIntervalMs)
            }
        }
    }

    private fun getTargetIndex(): Int {
        return if (!isReversing) {
            currentIndex + 1
        } else {
            currentIndex - 1
        }
    }

    private fun advanceWaypoint() {
        if (!isReversing) {
            if (currentIndex + 1 < waypoints.size - 1) {
                currentIndex++
            } else {
                // At the last waypoint
                when (mode) {
                    TripMode.ONE_WAY -> {
                        stopTrip()
                    }
                    TripMode.LOOP -> {
                        // Loop back to start
                        currentIndex = 0
                        currentPosition = waypoints[0]
                    }
                    TripMode.REVERSE -> {
                        // Reverse direction (ping-pong)
                        isReversing = true
                        currentIndex = waypoints.size - 1
                    }
                }
            }
        } else {
            // Reversing backwards
            if (currentIndex - 1 > 0) {
                currentIndex--
            } else {
                // Reached start again
                isReversing = false
                currentIndex = 0
            }
        }
    }

    private fun updateMetrics() {
        val current = currentPosition ?: return
        var remDist = 0.0

        val targetIdx = getTargetIndex()
        if (targetIdx in waypoints.indices) {
            remDist += current.distanceTo(waypoints[targetIdx])
            if (!isReversing) {
                for (i in targetIdx until waypoints.size - 1) {
                    remDist += waypoints[i].distanceTo(waypoints[i + 1])
                }
            } else {
                for (i in targetIdx downTo 1) {
                    remDist += waypoints[i].distanceTo(waypoints[i - 1])
                }
            }
        }

        _status.value = _status.value.copy(
            currentPoint = current,
            currentLegIndex = currentIndex,
            remainingDistanceMeters = remDist
        )
    }

    fun pauseTrip() {
        _status.value = _status.value.copy(isPaused = true)
    }

    fun resumeTrip() {
        _status.value = _status.value.copy(isPaused = false)
    }

    fun setSpeed(speedKmh: Float) {
        currentSpeedKmh = speedKmh.coerceIn(1.0f, 150.0f)
        _status.value = _status.value.copy(speedKmh = currentSpeedKmh)
    }

    fun stopTrip() {
        job?.cancel()
        job = null
        _status.value = TripStatus(isRunning = false)
    }
}
