package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.GeoShiftApplication
import com.example.data.model.LocationPoint
import kotlin.math.cos
import kotlin.math.sin

class OverlayJoystickService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, OverlayJoystickService::class.java)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayJoystickService::class.java)
            context.stopService(intent)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD0F172A.toInt())
            setPadding(16, 16, 16, 16)
        }

        // Header bar with drag handle and close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "GeoShift"
            setTextColor(0xFF06B6D4.toInt())
            textSize = 12f
            setPadding(8, 0, 16, 0)
        }

        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(0x00000000)
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener {
                stopSelf()
            }
        }

        header.addView(title)
        header.addView(closeBtn)
        rootLayout.addView(header)

        // Joystick directional grid
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }

        fun stepLocation(dxMeters: Double, dyMeters: Double) {
            val app = application as? GeoShiftApplication ?: return
            val cur = app.mockLocationManager.currentLocation.value
            val speedKmh = app.joystickSettings.value.speedKmh
            val metersPerSecond = speedKmh / 3.6f
            val stepSize = (metersPerSecond * 0.8f).toDouble().coerceAtLeast(1.0)

            val dLat = (dyMeters * stepSize) / 111111.0
            val dLon = (dxMeters * stepSize) / (111111.0 * cos(Math.toRadians(cur.latitude)))

            val bearing = when {
                dyMeters > 0 && dxMeters == 0.0 -> 0f
                dyMeters < 0 && dxMeters == 0.0 -> 180f
                dxMeters > 0 && dyMeters == 0.0 -> 90f
                dxMeters < 0 && dyMeters == 0.0 -> 270f
                else -> cur.bearing
            }

            val next = LocationPoint(
                latitude = cur.latitude + dLat,
                longitude = cur.longitude + dLon,
                speedKmh = speedKmh,
                bearing = bearing,
                label = "Joystick Walking"
            )
            app.mockLocationManager.updateLocation(next)
        }

        // Top button (North)
        val btnUp = ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_up_float)
            setBackgroundColor(0x3338BDF8.toInt())
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { stepLocation(0.0, 1.0) }
        }

        // Middle row (West, Center, East)
        val midRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnLeft = ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_down_float)
            rotation = 90f
            setBackgroundColor(0x3338BDF8.toInt())
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { stepLocation(-1.0, 0.0) }
        }

        val btnCenter = TextView(this).apply {
            text = "🕹️"
            textSize = 16f
            setPadding(12, 12, 12, 12)
        }

        val btnRight = ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_down_float)
            rotation = 270f
            setBackgroundColor(0x3338BDF8.toInt())
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { stepLocation(1.0, 0.0) }
        }

        midRow.addView(btnLeft)
        midRow.addView(btnCenter)
        midRow.addView(btnRight)

        // Bottom button (South)
        val btnDown = ImageButton(this).apply {
            setImageResource(android.R.drawable.arrow_down_float)
            setBackgroundColor(0x3338BDF8.toInt())
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { stepLocation(0.0, -1.0) }
        }

        grid.addView(btnUp)
        grid.addView(midRow)
        grid.addView(btnDown)
        rootLayout.addView(grid)

        // Drag to move window
        header.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(rootLayout, params)
                        return true
                    }
                }
                return false
            }
        })

        overlayView = rootLayout
        windowManager?.addView(rootLayout, params)
    }

    override fun onDestroy() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
