package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeocodingClient
import com.example.data.api.PopularLocation
import com.example.data.model.LocationPoint
import com.example.ui.stringApp

@Composable
fun LiteLocationSelector(
    modifier: Modifier = Modifier,
    currentLocation: LocationPoint,
    selectedTarget: LocationPoint,
    isMocking: Boolean,
    onTeleport: (LocationPoint) -> Unit,
    onSaveBookmark: (LocationPoint) -> Unit
) {
    var latInput by remember(selectedTarget.latitude) {
        mutableStateOf(String.format("%.6f", selectedTarget.latitude))
    }
    var lonInput by remember(selectedTarget.longitude) {
        mutableStateOf(String.format("%.6f", selectedTarget.longitude))
    }
    var altInput by remember(selectedTarget.altitude) {
        mutableStateOf(selectedTarget.altitude.toInt().toString())
    }

    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Modern HUD Status Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().testTag("lite_hud_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isMocking) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMocking) stringApp("status_active") else stringApp("status_idle"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMocking) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "LITE MODE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grid of telemetry
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryItem(
                            label = "Latitude",
                            value = String.format("%.6f°", currentLocation.latitude)
                        )
                        TelemetryItem(
                            label = "Longitude",
                            value = String.format("%.6f°", currentLocation.longitude)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryItem(
                            label = "Altitude",
                            value = "${currentLocation.altitude.toInt()} m"
                        )
                        TelemetryItem(
                            label = "Speed",
                            value = "${String.format("%.1f", currentLocation.speedKmh)} km/h"
                        )
                        TelemetryItem(
                            label = "Accuracy",
                            value = "±${currentLocation.accuracy.toInt()} m"
                        )
                    }
                }
            }
        }

        // Manual Coordinate Input Panel
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringApp("set_coords"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Quick Paste Button
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text?.trim()
                                if (!clipText.isNullOrBlank()) {
                                    // Try parse lat,lng
                                    val parts = clipText.split(",", ";", " ", "\t")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    if (parts.size >= 2) {
                                        val pLat = parts[0].toDoubleOrNull()
                                        val pLon = parts[1].toDoubleOrNull()
                                        if (pLat != null && pLon != null) {
                                            latInput = pLat.toString()
                                            lonInput = pLon.toString()
                                        }
                                    }
                                }
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Latitude input with micro adjustments
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { latInput = it },
                            label = { Text(stringApp("lat")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("latitude_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StepButton(text = "+.01") {
                            val v = latInput.toDoubleOrNull() ?: 0.0
                            latInput = String.format("%.6f", v + 0.01)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        StepButton(text = "-.01") {
                            val v = latInput.toDoubleOrNull() ?: 0.0
                            latInput = String.format("%.6f", v - 0.01)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Longitude input with micro adjustments
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = lonInput,
                            onValueChange = { lonInput = it },
                            label = { Text(stringApp("lng")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("longitude_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StepButton(text = "+.01") {
                            val v = lonInput.toDoubleOrNull() ?: 0.0
                            lonInput = String.format("%.6f", v + 0.01)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        StepButton(text = "-.01") {
                            val v = lonInput.toDoubleOrNull() ?: 0.0
                            lonInput = String.format("%.6f", v - 0.01)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = altInput,
                        onValueChange = { altInput = it },
                        label = { Text(stringApp("alt") + " (m)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val lat = latInput.toDoubleOrNull() ?: selectedTarget.latitude
                                val lon = lonInput.toDoubleOrNull() ?: selectedTarget.longitude
                                val alt = altInput.toDoubleOrNull() ?: 15.0
                                val target = LocationPoint(
                                    latitude = lat,
                                    longitude = lon,
                                    altitude = alt,
                                    label = "Manual Input"
                                )
                                onTeleport(target)
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("lite_teleport_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringApp("teleport"), fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = {
                                val lat = latInput.toDoubleOrNull() ?: selectedTarget.latitude
                                val lon = lonInput.toDoubleOrNull() ?: selectedTarget.longitude
                                val target = LocationPoint(
                                    latitude = lat,
                                    longitude = lon,
                                    label = "Point (${String.format("%.4f", lat)}, ${String.format("%.4f", lon)})"
                                )
                                onSaveBookmark(target)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Save Bookmark", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Popular World Presets Carousel
        item {
            Column {
                Text(
                    text = stringApp("search_presets"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(GeocodingClient.popularLocations) { item ->
                        PresetCard(item = item) {
                            latInput = String.format("%.6f", item.latitude)
                            lonInput = String.format("%.6f", item.longitude)
                            val target = LocationPoint(
                                latitude = item.latitude,
                                longitude = item.longitude,
                                label = item.name
                            )
                            onTeleport(target)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(width = 44.dp, height = 40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun PresetCard(item: PopularLocation, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                maxLines = 1
            )
        }
    }
}
