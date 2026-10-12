package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.GeoShiftApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.LocationPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MockLocationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var locationCollectorJob: Job? = null

    companion object {
        const val CHANNEL_ID = "geoshift_mock_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.geoshift.ACTION_START"
        const val ACTION_STOP = "com.example.geoshift.ACTION_STOP"
        const val ACTION_PAUSE_RESUME = "com.example.geoshift.ACTION_PAUSE_RESUME"

        fun startService(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val app = application as? GeoShiftApplication
                app?.mockLocationManager?.stopMocking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_RESUME -> {
                val app = application as? GeoShiftApplication
                if (app?.tripSimulator?.status?.value?.isRunning == true) {
                    if (app.tripSimulator.status.value.isPaused) {
                        app.tripSimulator.resumeTrip()
                    } else {
                        app.tripSimulator.pauseTrip()
                    }
                }
            }
            ACTION_START, null -> {
                startForegroundNotification()
                observeLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val initialLoc = (application as? GeoShiftApplication)?.mockLocationManager?.currentLocation?.value
        val notification = buildNotification(initialLoc)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeLocationUpdates() {
        locationCollectorJob?.cancel()
        locationCollectorJob = serviceScope.launch {
            val app = application as? GeoShiftApplication ?: return@launch
            app.mockLocationManager.currentLocation.collectLatest { point ->
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification(point))
            }
        }
    }

    private fun buildNotification(point: LocationPoint?): Notification {
        val latStr = point?.let { String.format("%.5f", it.latitude) } ?: "0.00000"
        val lonStr = point?.let { String.format("%.5f", it.longitude) } ?: "0.00000"
        val speedStr = point?.let { String.format("%.1f km/h", it.speedKmh) } ?: "0.0 km/h"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MockLocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.geoshift_app_icon_1791801288140)
            .setContentTitle("GeoShift Mock GPS Active")
            .setContentText("Lat: $latStr, Lng: $lonStr • $speedStr")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Latitude: $latStr\nLongitude: $lonStr\nSpeed: $speedStr\nStatus: Mock GPS Broadcasting")
            )
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Mocking", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GeoShift Mock Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active mock GPS status and spoofed coordinates"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        locationCollectorJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
