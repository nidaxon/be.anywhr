package com.example.mock

import com.example.data.model.LocationPoint
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
import kotlin.math.sqrt
import kotlin.random.Random

data class DriftStatus(
    val isRunning: Boolean = false,
    val centerPoint: LocationPoint? = null,
    val currentDriftPoint: LocationPoint? = null,
    val targetDriftPoint: LocationPoint? = null,
    val radiusMeters: Double = 50.0,
    val intervalSeconds: Int = 10,
    val secondsUntilNextPick: Int = 10,
    val isSmooth: Boolean = true
)

class DriftSimulator(
    private val onLocationUpdated: (LocationPoint) -> Unit
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _status = MutableStateFlow(DriftStatus())
    val status: StateFlow<DriftStatus> = _status.asStateFlow()

    fun startDrift(
        center: LocationPoint,
        radiusMeters: Double = 50.0,
        intervalSeconds: Int = 10,
        isSmooth: Boolean = true
    ) {
        stopDrift()

        var currentPoint = center
        _status.value = DriftStatus(
            isRunning = true,
            centerPoint = center,
            currentDriftPoint = center,
            targetDriftPoint = center,
            radiusMeters = radiusMeters,
            intervalSeconds = intervalSeconds,
            secondsUntilNextPick = intervalSeconds,
            isSmooth = isSmooth
        )

        job = scope.launch {
            while (isActive) {
                // Generate a random point uniformly distributed inside circle
                val angle = Random.nextDouble(0.0, 2 * Math.PI)
                val dist = sqrt(Random.nextDouble(0.0, 1.0)) * radiusMeters

                val dLat = (dist * cos(angle)) / 111111.0
                val dLon = (dist * sin(angle)) / (111111.0 * cos(Math.toRadians(center.latitude)))

                val target = LocationPoint(
                    latitude = center.latitude + dLat,
                    longitude = center.longitude + dLon,
                    speedKmh = 4.0f,
                    label = "Drift Radius: ${radiusMeters.toInt()}m"
                )

                _status.value = _status.value.copy(
                    targetDriftPoint = target,
                    secondsUntilNextPick = intervalSeconds
                )

                if (!isSmooth) {
                    // Instant jump
                    currentPoint = target
                    _status.value = _status.value.copy(currentDriftPoint = target)
                    onLocationUpdated(target)

                    // Countdown timer
                    for (sec in intervalSeconds downTo 1) {
                        if (!isActive) break
                        _status.value = _status.value.copy(secondsUntilNextPick = sec)
                        delay(1000L)
                    }
                } else {
                    // Smooth drift: interpolate smoothly over the interval
                    val totalSteps = (intervalSeconds * 10).coerceAtLeast(1) // 100ms per step
                    val startLat = currentPoint.latitude
                    val startLon = currentPoint.longitude

                    for (step in 1..totalSteps) {
                        if (!isActive) break
                        val fraction = step.toDouble() / totalSteps
                        val interpolatedLat = startLat + (target.latitude - startLat) * fraction
                        val interpolatedLon = startLon + (target.longitude - startLon) * fraction

                        val interpolated = LocationPoint(
                            latitude = interpolatedLat,
                            longitude = interpolatedLon,
                            speedKmh = 4.0f,
                            label = "Drifting (~${radiusMeters.toInt()}m radius)"
                        )
                        currentPoint = interpolated
                        _status.value = _status.value.copy(
                            currentDriftPoint = interpolated,
                            secondsUntilNextPick = ((totalSteps - step) / 10) + 1
                        )
                        onLocationUpdated(interpolated)
                        delay(100L)
                    }
                }
            }
        }
    }

    fun stopDrift() {
        job?.cancel()
        job = null
        _status.value = DriftStatus(isRunning = false)
    }
}
