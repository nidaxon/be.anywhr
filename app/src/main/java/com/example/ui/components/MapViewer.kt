package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.LocationPoint
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

@Composable
fun MapViewer(
    modifier: Modifier = Modifier,
    currentLocation: LocationPoint,
    selectedTarget: LocationPoint,
    waypoints: List<LocationPoint>,
    driftCenter: LocationPoint?,
    driftRadiusMeters: Double,
    isDrifting: Boolean,
    onTargetChanged: (LocationPoint) -> Unit,
    onAddWaypoint: (LocationPoint) -> Unit
) {
    var centerLat by remember { mutableDoubleStateOf(selectedTarget.latitude) }
    var centerLon by remember { mutableDoubleStateOf(selectedTarget.longitude) }
    var zoomLevel by remember { mutableIntStateOf(16) }
    var useTileLayer by remember { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val tileSize = 256f

        // Center tile coordinates
        val centerTileX = lonToTileX(centerLon, zoomLevel)
        val centerTileY = latToTileY(centerLat, zoomLevel)

        // Touch gestures for panning and zooming
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomLevel, centerLat, centerLon) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // Zoom update
                        if (zoom != 1f) {
                            if (zoom > 1.15f && zoomLevel < 19) {
                                zoomLevel++
                            } else if (zoom < 0.85f && zoomLevel > 3) {
                                zoomLevel--
                            }
                        }

                        // Pan update
                        val dxTiles = pan.x / tileSize
                        val dyTiles = pan.y / tileSize
                        val newTileX = centerTileX - dxTiles
                        val newTileY = centerTileY - dyTiles

                        centerLon = tileXToLon(newTileX, zoomLevel).coerceIn(-180.0, 180.0)
                        centerLat = tileYToLat(newTileY, zoomLevel).coerceIn(-85.0, 85.0)

                        onTargetChanged(
                            selectedTarget.copy(
                                latitude = centerLat,
                                longitude = centerLon
                            )
                        )
                    }
                }
        ) {
            // Render OSM Slippy Map Tiles
            if (useTileLayer) {
                val minTileX = floor(centerTileX - (widthPx / tileSize) / 2f).toInt() - 1
                val maxTileX = floor(centerTileX + (widthPx / tileSize) / 2f).toInt() + 1
                val minTileY = floor(centerTileY - (heightPx / tileSize) / 2f).toInt() - 1
                val maxTileY = floor(centerTileY + (heightPx / tileSize) / 2f).toInt() + 1
                val maxTiles = 1 shl zoomLevel

                for (tx in minTileX..maxTileX) {
                    val wrappedTx = ((tx % maxTiles) + maxTiles) % maxTiles
                    for (ty in minTileY..maxTileY) {
                        if (ty in 0 until maxTiles) {
                            val screenX = (widthPx / 2f) + (tx - centerTileX).toFloat() * tileSize
                            val screenY = (heightPx / 2f) + (ty - centerTileY).toFloat() * tileSize

                            val tileUrl = "https://tile.openstreetmap.org/$zoomLevel/$wrappedTx/$ty.png"

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(tileUrl)
                                    .setHeader("User-Agent", "GeoShift-AndroidApp/1.0")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .size(tileSize.dp * (1f / LocalDensity.current.density))
                                    .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                            )
                        }
                    }
                }
            }

            // Grid & Vector Overlay Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasCenter = Offset(widthPx / 2f, heightPx / 2f)

                // If tile layer is off, draw a stylish coordinate grid
                if (!useTileLayer) {
                    val gridSpacing = 80f
                    for (x in 0..(widthPx / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(x * gridSpacing, 0f),
                            end = Offset(x * gridSpacing, heightPx),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..(heightPx / gridSpacing).toInt()) {
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, y * gridSpacing),
                            end = Offset(widthPx, y * gridSpacing),
                            strokeWidth = 1f
                        )
                    }
                }

                // Helper to convert lat/lng to screen pixel offset
                fun toScreenOffset(lat: Double, lon: Double): Offset {
                    val tx = lonToTileX(lon, zoomLevel)
                    val ty = latToTileY(lat, zoomLevel)
                    val px = canvasCenter.x + (tx - centerTileX).toFloat() * tileSize
                    val py = canvasCenter.y + (ty - centerTileY).toFloat() * tileSize
                    return Offset(px, py)
                }

                // Draw Drift Circle if active
                if (isDrifting && driftCenter != null) {
                    val driftOffset = toScreenOffset(driftCenter.latitude, driftCenter.longitude)
                    // Meters to pixels at this latitude & zoom
                    val metersPerPx = (156543.03392 * cos(Math.toRadians(driftCenter.latitude))) / (1 shl zoomLevel)
                    val radiusPx = (driftRadiusMeters / metersPerPx).toFloat().coerceAtLeast(10f)

                    drawCircle(
                        color = Color(0x3306B6D4),
                        radius = radiusPx,
                        center = driftOffset
                    )
                    drawCircle(
                        color = Color(0xFF06B6D4),
                        radius = radiusPx,
                        center = driftOffset,
                        style = Stroke(width = 2.5f)
                    )
                }

                // Draw Route Polyline between waypoints
                if (waypoints.size >= 2) {
                    val path = Path()
                    val firstPt = toScreenOffset(waypoints[0].latitude, waypoints[0].longitude)
                    path.moveTo(firstPt.x, firstPt.y)

                    for (i in 1 until waypoints.size) {
                        val pt = toScreenOffset(waypoints[i].latitude, waypoints[i].longitude)
                        path.lineTo(pt.x, pt.y)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF3B82F6),
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }

                // Draw Waypoint Markers
                for ((idx, wp) in waypoints.withIndex()) {
                    val pt = toScreenOffset(wp.latitude, wp.longitude)
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = 14f,
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = pt
                    )
                }

                // Draw Current Active Spoofed Location Pin
                val currOffset = toScreenOffset(currentLocation.latitude, currentLocation.longitude)
                drawCircle(
                    color = Color(0x5510B981),
                    radius = 24f,
                    center = currOffset
                )
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 12f,
                    center = currOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = currOffset
                )
            }
        }

        // Center Crosshair (Precision Target Pin)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                // Outer ring
                drawCircle(
                    color = Color(0xFF06B6D4),
                    radius = 16f,
                    center = c,
                    style = Stroke(width = 3f)
                )
                // Center point
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = 5f,
                    center = c
                )
                // Crosshairs
                drawLine(Color(0xFF06B6D4), Offset(c.x, c.y - 28f), Offset(c.x, c.y - 18f), strokeWidth = 3f)
                drawLine(Color(0xFF06B6D4), Offset(c.x, c.y + 18f), Offset(c.x, c.y + 28f), strokeWidth = 3f)
                drawLine(Color(0xFF06B6D4), Offset(c.x - 28f, c.y), Offset(c.x - 18f, c.y), strokeWidth = 3f)
                drawLine(Color(0xFF06B6D4), Offset(c.x + 18f, c.y), Offset(c.x + 28f, c.y), strokeWidth = 3f)
            }
        }

        // Top Floating Info Card (Selected Target Coordinates)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp).rotate(currentLocation.bearing)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${String.format("%.5f", centerLat)}, ${String.format("%.5f", centerLon)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Zoom: $zoomLevel • ${if (useTileLayer) "Street Map" else "Vector Grid"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Floating Map Action Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter to Current Mock Location
            SmallFloatingActionButton(
                onClick = {
                    centerLat = currentLocation.latitude
                    centerLon = currentLocation.longitude
                    onTargetChanged(currentLocation)
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("recenter_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter on Location")
            }

            // Add Waypoint at Center
            SmallFloatingActionButton(
                onClick = {
                    val pt = LocationPoint(
                        latitude = centerLat,
                        longitude = centerLon,
                        label = "Waypoint ${waypoints.size + 1}"
                    )
                    onAddWaypoint(pt)
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_waypoint_button")
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "Add Waypoint")
            }

            // Toggle Layer
            SmallFloatingActionButton(
                onClick = { useTileLayer = !useTileLayer },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Layers, contentDescription = "Toggle Map Layer")
            }

            // Zoom In
            SmallFloatingActionButton(
                onClick = { if (zoomLevel < 19) zoomLevel++ },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out
            SmallFloatingActionButton(
                onClick = { if (zoomLevel > 3) zoomLevel-- },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }
    }
}

// Slippy map projection calculations
private fun lonToTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat)
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLon(x: Double, zoom: Int): Double {
    return x / (1 shl zoom) * 360.0 - 180.0
}

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * y / (1 shl zoom)
    return Math.toDegrees(atan(sinh(n)))
}
