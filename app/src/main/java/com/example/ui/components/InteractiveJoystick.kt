package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JoystickSettings
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun InteractiveJoystickOverlay(
    modifier: Modifier = Modifier,
    settings: JoystickSettings,
    onStep: (dxMeters: Double, dyMeters: Double, bearing: Float) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onToggleLock: () -> Unit,
    onClose: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var thumbX by remember { mutableFloatStateOf(0f) }
    var thumbY by remember { mutableFloatStateOf(0f) }
    var isDraggingThumb by remember { mutableStateOf(false) }

    val baseSize = settings.sizeDp.dp
    val thumbSize = (settings.sizeDp / 3.2f).dp
    val maxRadiusPx = (settings.sizeDp * 1.5f) // max drag radius

    // Continuous stepping when dragging or locked
    LaunchedEffect(isDraggingThumb, thumbX, thumbY, settings.isLocked, settings.speedKmh) {
        while (isDraggingThumb || settings.isLocked) {
            val dist = sqrt(thumbX * thumbX + thumbY * thumbY)
            if (dist > 8f) {
                val normX = thumbX / dist
                val normY = thumbY / dist // In screen coords: up is -Y, down is +Y

                // In geographic coords: North is +dLat, East is +dLon
                // Screen X -> East (+), Screen -Y -> North (+)
                val geoDx = normX.toDouble()
                val geoDy = -normY.toDouble()

                val bearingDeg = ((Math.toDegrees(kotlin.math.atan2(geoDx, geoDy)) + 360) % 360).toFloat()

                // Step scaled to speed (500ms intervals)
                val mps = settings.speedKmh / 3.6
                val stepMeters = mps * 0.5

                onStep(geoDx * stepMeters, geoDy * stepMeters, bearingDeg)
            }
            delay(400L)
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .alpha(settings.opacityPercent / 100f)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f))
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(8.dp)
            .testTag("interactive_joystick_overlay")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Header
            Row(
                modifier = Modifier
                    .width(baseSize)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Move Joystick",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${settings.speedKmh.toInt()} km/h",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.size(28.dp).testTag("joystick_lock_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Movement",
                            tint = if (settings.isLocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp).testTag("joystick_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Joystick",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Joystick Base Pad
            Box(
                modifier = Modifier
                    .size(baseSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                    .pointerInput(settings.isLocked) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingThumb = true
                            },
                            onDragEnd = {
                                if (!settings.isLocked) {
                                    thumbX = 0f
                                    thumbY = 0f
                                    isDraggingThumb = false
                                }
                            },
                            onDragCancel = {
                                if (!settings.isLocked) {
                                    thumbX = 0f
                                    thumbY = 0f
                                    isDraggingThumb = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = thumbX + dragAmount.x
                                val newY = thumbY + dragAmount.y
                                val dist = sqrt(newX * newX + newY * newY)
                                val limit = (baseSize.toPx() / 2f) - (thumbSize.toPx() / 2f)
                                if (dist <= limit) {
                                    thumbX = newX
                                    thumbY = newY
                                } else {
                                    thumbX = (newX / dist) * limit
                                    thumbY = (newY / dist) * limit
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Direction indicators
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "North",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "South",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "West",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "East",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(20.dp)
                )

                // Draggable Stick Thumb
                Box(
                    modifier = Modifier
                        .offset { IntOffset(thumbX.roundToInt(), thumbY.roundToInt()) }
                        .size(thumbSize)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (settings.isLocked) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Speed Mode Selector
            Row(
                modifier = Modifier.width(baseSize),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedButton(
                    icon = Icons.Default.DirectionsWalk,
                    isSelected = settings.speedKmh <= 6f,
                    onClick = { onUpdateSpeed(4.5f) },
                    contentDesc = "Walk 4.5 km/h"
                )
                SpeedButton(
                    icon = Icons.Default.DirectionsRun,
                    isSelected = settings.speedKmh in 6.1f..15f,
                    onClick = { onUpdateSpeed(12f) },
                    contentDesc = "Jog 12 km/h"
                )
                SpeedButton(
                    icon = Icons.Default.DirectionsBike,
                    isSelected = settings.speedKmh in 15.1f..35f,
                    onClick = { onUpdateSpeed(25f) },
                    contentDesc = "Bike 25 km/h"
                )
                SpeedButton(
                    icon = Icons.Default.DirectionsCar,
                    isSelected = settings.speedKmh > 35f,
                    onClick = { onUpdateSpeed(60f) },
                    contentDesc = "Drive 60 km/h"
                )
            }
        }
    }
}

@Composable
private fun SpeedButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentDesc: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
