package com.example

import android.app.Application
import com.example.data.db.GeoShiftDatabase
import com.example.data.model.AppVersionMode
import com.example.data.model.DriftSettings
import com.example.data.model.JoystickSettings
import com.example.mock.DriftSimulator
import com.example.mock.MockLocationManager
import com.example.mock.TripSimulator
import kotlinx.coroutines.flow.MutableStateFlow

class GeoShiftApplication : Application() {
    val database by lazy { GeoShiftDatabase.getDatabase(this) }
    val mockLocationManager by lazy { MockLocationManager(this) }

    val tripSimulator by lazy {
        TripSimulator { updatedPoint ->
            mockLocationManager.updateLocation(updatedPoint)
        }
    }

    val driftSimulator by lazy {
        DriftSimulator { updatedPoint ->
            mockLocationManager.updateLocation(updatedPoint)
        }
    }

    val appVersionMode = MutableStateFlow(AppVersionMode.FULL)
    val joystickSettings = MutableStateFlow(JoystickSettings())
    val driftSettings = MutableStateFlow(DriftSettings())
    val selectedLanguage = MutableStateFlow("en") // en, in, es, ja, zh, de
    val isDarkMode = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light

    override fun onCreate() {
        super.onCreate()
    }
}
