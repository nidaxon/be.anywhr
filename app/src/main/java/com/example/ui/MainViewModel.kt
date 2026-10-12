package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GeoShiftApplication
import com.example.data.model.AppVersionMode
import com.example.data.model.BookmarkEntity
import com.example.data.model.DriftSettings
import com.example.data.model.JoystickSettings
import com.example.data.model.LocationPoint
import com.example.data.model.SavedRouteEntity
import com.example.data.model.TripMode
import com.example.mock.DriftStatus
import com.example.mock.TripStatus
import com.example.service.MockLocationService
import com.example.service.OverlayJoystickService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.cos

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GeoShiftApplication
    private val mockManager = app.mockLocationManager
    private val tripSim = app.tripSimulator
    private val driftSim = app.driftSimulator
    private val bookmarkDao = app.database.bookmarkDao()
    private val routeDao = app.database.routeDao()

    val currentLocation = mockManager.currentLocation
    val isMocking = mockManager.isMocking
    val mockPermissionGranted = mockManager.mockPermissionGranted
    val lastError = mockManager.lastErrorMessage

    private val _selectedTarget = MutableStateFlow(currentLocation.value)
    val selectedTarget: StateFlow<LocationPoint> = _selectedTarget.asStateFlow()

    private val _waypoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val waypoints: StateFlow<List<LocationPoint>> = _waypoints.asStateFlow()

    val tripStatus: StateFlow<TripStatus> = tripSim.status
    val driftStatus: StateFlow<DriftStatus> = driftSim.status

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val routes: StateFlow<List<SavedRouteEntity>> = routeDao.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appVersionMode = app.appVersionMode
    val joystickSettings = app.joystickSettings
    val selectedLanguage = app.selectedLanguage
    val isDarkMode = app.isDarkMode

    private val _isInAppJoystickVisible = MutableStateFlow(true)
    val isInAppJoystickVisible: StateFlow<Boolean> = _isInAppJoystickVisible.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    fun toggleMocking() {
        if (isMocking.value) {
            mockManager.stopMocking()
            tripSim.stopTrip()
            driftSim.stopDrift()
            MockLocationService.stopService(getApplication())
        } else {
            val started = mockManager.startMocking()
            if (started) {
                MockLocationService.startService(getApplication())
            }
        }
    }

    fun teleportTo(point: LocationPoint) {
        _selectedTarget.value = point
        mockManager.setLocationDirect(point)
        if (!isMocking.value) {
            val started = mockManager.startMocking()
            if (started) {
                MockLocationService.startService(getApplication())
            }
        }
    }

    fun setSelectedTarget(point: LocationPoint) {
        _selectedTarget.value = point
    }

    fun stepLocation(dxMeters: Double, dyMeters: Double, bearing: Float) {
        val cur = currentLocation.value
        val speedKmh = joystickSettings.value.speedKmh

        val dLat = dyMeters / 111111.0
        val dLon = dxMeters / (111111.0 * cos(Math.toRadians(cur.latitude)))

        val nextPoint = LocationPoint(
            latitude = cur.latitude + dLat,
            longitude = cur.longitude + dLon,
            speedKmh = speedKmh,
            bearing = bearing,
            label = "Joystick"
        )
        _selectedTarget.value = nextPoint
        mockManager.updateLocation(nextPoint)
    }

    fun addWaypoint(point: LocationPoint) {
        _waypoints.value = _waypoints.value + point
    }

    fun removeWaypoint(index: Int) {
        if (index in _waypoints.value.indices) {
            val list = _waypoints.value.toMutableList()
            list.removeAt(index)
            _waypoints.value = list
        }
    }

    fun clearWaypoints() {
        _waypoints.value = emptyList()
    }

    fun startTrip(points: List<LocationPoint>, speedKmh: Float, mode: TripMode) {
        if (points.size >= 2) {
            if (!isMocking.value) {
                mockManager.startMocking()
                MockLocationService.startService(getApplication())
            }
            tripSim.startTrip(points, speedKmh, mode)
        }
    }

    fun pauseTrip() = tripSim.pauseTrip()
    fun resumeTrip() = tripSim.resumeTrip()
    fun stopTrip() = tripSim.stopTrip()

    fun startDrift(center: LocationPoint, radiusMeters: Double, intervalSeconds: Int, isSmooth: Boolean) {
        if (!isMocking.value) {
            mockManager.startMocking()
            MockLocationService.startService(getApplication())
        }
        driftSim.startDrift(center, radiusMeters, intervalSeconds, isSmooth)
    }

    fun stopDrift() = driftSim.stopDrift()

    fun saveBookmark(name: String, category: String, description: String, colorHex: String) {
        viewModelScope.launch {
            val target = selectedTarget.value
            val entity = BookmarkEntity(
                name = name,
                category = category,
                description = description,
                colorHex = colorHex,
                latitude = target.latitude,
                longitude = target.longitude,
                altitude = target.altitude
            )
            bookmarkDao.insertBookmark(entity)
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(bookmark)
        }
    }

    fun importBookmarks(list: List<BookmarkEntity>) {
        viewModelScope.launch {
            bookmarkDao.insertAll(list)
        }
    }

    fun saveRoute(name: String, points: List<LocationPoint>, speedKmh: Float, mode: TripMode) {
        viewModelScope.launch {
            val waypointsStr = points.joinToString(";") { "${it.latitude},${it.longitude}" }
            val route = SavedRouteEntity(
                name = name,
                waypointsJson = waypointsStr,
                speedKmh = speedKmh,
                mode = mode.name
            )
            routeDao.insertRoute(route)
        }
    }

    fun setAppVersionMode(mode: AppVersionMode) {
        app.appVersionMode.value = mode
    }

    fun setLanguage(lang: String) {
        app.selectedLanguage.value = lang
    }

    fun setTheme(isDark: Boolean?) {
        app.isDarkMode.value = isDark
    }

    fun toggleInAppJoystick() {
        _isInAppJoystickVisible.value = !_isInAppJoystickVisible.value
    }

    fun updateJoystickSpeed(speed: Float) {
        joystickSettings.value = joystickSettings.value.copy(speedKmh = speed)
    }

    fun toggleJoystickLock() {
        joystickSettings.value = joystickSettings.value.copy(isLocked = !joystickSettings.value.isLocked)
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }

    fun clearError() {
        mockManager.clearError()
    }
}
