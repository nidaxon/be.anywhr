package com.example.mock

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.data.model.LocationPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockLocationManager(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _isMocking = MutableStateFlow(false)
    val isMocking: StateFlow<Boolean> = _isMocking.asStateFlow()

    private val _currentLocation = MutableStateFlow(
        LocationPoint(
            latitude = -6.175392, // Default Monas Jakarta
            longitude = 106.827153,
            label = "Monas, Jakarta"
        )
    )
    val currentLocation: StateFlow<LocationPoint> = _currentLocation.asStateFlow()

    private val _mockPermissionGranted = MutableStateFlow(true)
    val mockPermissionGranted: StateFlow<Boolean> = _mockPermissionGranted.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)

    fun startMocking(): Boolean {
        try {
            for (provider in providers) {
                try {
                    locationManager.removeTestProvider(provider)
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        ProviderProperties.POWER_USAGE_LOW,
                        ProviderProperties.ACCURACY_FINE
                    )
                } else {
                    @Suppress("DEPRECATION")
                    locationManager.addTestProvider(
                        provider,
                        false, false, false, false, true, true, true,
                        1, // power requirement
                        1  // accuracy
                    )
                }
                locationManager.setTestProviderEnabled(provider, true)
            }
            _isMocking.value = true
            _mockPermissionGranted.value = true
            _lastErrorMessage.value = null
            // Push initial point immediately
            updateLocation(_currentLocation.value)
            return true
        } catch (e: SecurityException) {
            Log.e("MockLocationManager", "SecurityException: Mock location not enabled in developer options", e)
            _mockPermissionGranted.value = false
            _isMocking.value = false
            _lastErrorMessage.value = "Mock location permission not granted. Please select GeoShift as the mock location app in Developer Options."
            return false
        } catch (e: Exception) {
            Log.e("MockLocationManager", "Failed to start mock provider", e)
            _isMocking.value = false
            _lastErrorMessage.value = e.localizedMessage ?: "Failed to start mock provider"
            return false
        }
    }

    fun stopMocking() {
        for (provider in providers) {
            try {
                locationManager.setTestProviderEnabled(provider, false)
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) {
                Log.w("MockLocationManager", "Error removing test provider $provider: ${e.message}")
            }
        }
        _isMocking.value = false
    }

    fun updateLocation(point: LocationPoint) {
        _currentLocation.value = point

        if (!_isMocking.value) return

        for (provider in providers) {
            try {
                val mockLoc = Location(provider).apply {
                    latitude = point.latitude
                    longitude = point.longitude
                    altitude = point.altitude
                    accuracy = point.accuracy
                    speed = (point.speedKmh / 3.6f)
                    bearing = point.bearing
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        bearingAccuracyDegrees = 1.0f
                        speedAccuracyMetersPerSecond = 0.5f
                        verticalAccuracyMeters = 1.0f
                    }
                }
                locationManager.setTestProviderLocation(provider, mockLoc)
            } catch (e: SecurityException) {
                _mockPermissionGranted.value = false
                _isMocking.value = false
                _lastErrorMessage.value = "Mock permission revoked. Re-enable in Developer Options."
            } catch (e: Exception) {
                Log.w("MockLocationManager", "Error pushing location to $provider: ${e.message}")
            }
        }
    }

    fun setLocationDirect(point: LocationPoint) {
        _currentLocation.value = point
        if (_isMocking.value) {
            updateLocation(point)
        }
    }

    fun clearError() {
        _lastErrorMessage.value = null
    }
}
