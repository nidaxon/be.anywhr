package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppVersionMode
import com.example.data.model.LocationPoint
import com.example.ui.LocalAppLanguage
import com.example.ui.MainViewModel
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.DeveloperOptionsGuideDialog
import com.example.ui.components.DriftConfigDialog
import com.example.ui.components.ImportExportDialog
import com.example.ui.components.InteractiveJoystickOverlay
import com.example.ui.components.LiteLocationSelector
import com.example.ui.components.MapViewer
import com.example.ui.components.RoutePlannerSheet
import com.example.ui.components.SaveBookmarkDialog
import com.example.ui.components.SearchLocationDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.stringApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val useDark = isDarkModePref ?: isSystemInDarkTheme()
            val selectedLang by viewModel.selectedLanguage.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalAppLanguage provides selectedLang) {
                MyApplicationTheme(darkTheme = useDark) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val selectedTarget by viewModel.selectedTarget.collectAsStateWithLifecycle()
    val isMocking by viewModel.isMocking.collectAsStateWithLifecycle()
    val mockPermissionGranted by viewModel.mockPermissionGranted.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val appMode by viewModel.appVersionMode.collectAsStateWithLifecycle()
    val waypoints by viewModel.waypoints.collectAsStateWithLifecycle()
    val tripStatus by viewModel.tripStatus.collectAsStateWithLifecycle()
    val driftStatus by viewModel.driftStatus.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val joystickSettings by viewModel.joystickSettings.collectAsStateWithLifecycle()
    val isJoystickInApp by viewModel.isInAppJoystickVisible.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showSearchDialog by remember { mutableStateOf(false) }
    var showRouteSheet by remember { mutableStateOf(false) }
    var showDriftDialog by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showSaveBookmarkDialog by remember { mutableStateOf(false) }
    var showImportExportDialog by remember { mutableStateOf(false) }
    var showDevGuideDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Runtime Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Map or Lite Coordinate View
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBarContent(
                    appMode = appMode,
                    waypointCount = waypoints.size,
                    onToggleMode = {
                        val newMode = if (appMode == AppVersionMode.FULL) AppVersionMode.LITE else AppVersionMode.FULL
                        viewModel.setAppVersionMode(newMode)
                    },
                    onOpenSearch = { showSearchDialog = true },
                    onOpenBookmarks = { showBookmarksSheet = true },
                    onOpenRoutes = { showRouteSheet = true },
                    onOpenSettings = { showSettingsDialog = true }
                )

                // Mock Permission Banner (if not selected in Developer Options)
                if (!mockPermissionGranted || lastError != null) {
                    MockPermissionBanner(
                        errorMsg = lastError ?: stringApp("dev_options_msg"),
                        onClick = { showDevGuideDialog = true }
                    )
                }

                // Main Display (Full Map or Lite View)
                Box(modifier = Modifier.weight(1f)) {
                    if (appMode == AppVersionMode.FULL) {
                        MapViewer(
                            currentLocation = currentLocation,
                            selectedTarget = selectedTarget,
                            waypoints = waypoints,
                            driftCenter = driftStatus.centerPoint,
                            driftRadiusMeters = driftStatus.radiusMeters,
                            isDrifting = driftStatus.isRunning,
                            onTargetChanged = { pt -> viewModel.setSelectedTarget(pt) },
                            onAddWaypoint = { pt -> viewModel.addWaypoint(pt) }
                        )
                    } else {
                        LiteLocationSelector(
                            currentLocation = currentLocation,
                            selectedTarget = selectedTarget,
                            isMocking = isMocking,
                            onTeleport = { pt -> viewModel.teleportTo(pt) },
                            onSaveBookmark = { pt ->
                                viewModel.setSelectedTarget(pt)
                                showSaveBookmarkDialog = true
                            }
                        )
                    }

                    // Floating Interactive Joystick (In-App)
                    if (isJoystickInApp) {
                        InteractiveJoystickOverlay(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 100.dp),
                            settings = joystickSettings,
                            onStep = { dx, dy, bearing ->
                                viewModel.stepLocation(dx, dy, bearing)
                            },
                            onUpdateSpeed = { speed ->
                                viewModel.updateJoystickSpeed(speed)
                            },
                            onToggleLock = {
                                viewModel.toggleJoystickLock()
                            },
                            onClose = {
                                viewModel.toggleInAppJoystick()
                            }
                        )
                    }
                }

                // Bottom Floating Control Dock
                BottomControlDock(
                    isMocking = isMocking,
                    isJoystickVisible = isJoystickInApp,
                    isDrifting = driftStatus.isRunning,
                    isTripRunning = tripStatus.isRunning,
                    onToggleMocking = { viewModel.toggleMocking() },
                    onTeleportHere = { viewModel.teleportTo(selectedTarget) },
                    onToggleDrift = { showDriftDialog = true },
                    onToggleJoystick = { viewModel.toggleInAppJoystick() },
                    onSaveLocation = { showSaveBookmarkDialog = true }
                )
            }
        }
    }

    // Dialogs & Sheets
    if (showSearchDialog) {
        SearchLocationDialog(
            onDismiss = { showSearchDialog = false },
            onSelectLocation = { pt ->
                viewModel.setSelectedTarget(pt)
                viewModel.teleportTo(pt)
            }
        )
    }

    if (showRouteSheet) {
        RoutePlannerSheet(
            waypoints = waypoints,
            tripStatus = tripStatus,
            onDismiss = { showRouteSheet = false },
            onStartTrip = { pts, spd, mode -> viewModel.startTrip(pts, spd, mode) },
            onPauseTrip = { viewModel.pauseTrip() },
            onResumeTrip = { viewModel.resumeTrip() },
            onStopTrip = { viewModel.stopTrip() },
            onRemoveWaypoint = { idx -> viewModel.removeWaypoint(idx) },
            onClearWaypoints = { viewModel.clearWaypoints() },
            onSaveRoute = { name, pts, spd, mode -> viewModel.saveRoute(name, pts, spd, mode) }
        )
    }

    if (showDriftDialog) {
        DriftConfigDialog(
            currentPoint = currentLocation,
            driftStatus = driftStatus,
            onDismiss = { showDriftDialog = false },
            onStartDrift = { center, radius, interval, smooth ->
                viewModel.startDrift(center, radius, interval, smooth)
            },
            onStopDrift = { viewModel.stopDrift() }
        )
    }

    if (showBookmarksSheet) {
        BookmarksSheet(
            bookmarks = bookmarks,
            onDismiss = { showBookmarksSheet = false },
            onTeleport = { pt -> viewModel.teleportTo(pt) },
            onAddToRoute = { pt -> viewModel.addWaypoint(pt) },
            onDelete = { b -> viewModel.deleteBookmark(b) },
            onOpenImportExport = {
                showBookmarksSheet = false
                showImportExportDialog = true
            }
        )
    }

    if (showSaveBookmarkDialog) {
        SaveBookmarkDialog(
            targetPoint = selectedTarget,
            onDismiss = { showSaveBookmarkDialog = false },
            onSave = { name, cat, desc, col ->
                viewModel.saveBookmark(name, cat, desc, col)
            }
        )
    }

    if (showImportExportDialog) {
        ImportExportDialog(
            bookmarks = bookmarks,
            routes = routes,
            onDismiss = { showImportExportDialog = false },
            onImportBookmarks = { list -> viewModel.importBookmarks(list) }
        )
    }

    if (showDevGuideDialog) {
        DeveloperOptionsGuideDialog(
            onDismiss = {
                showDevGuideDialog = false
                viewModel.clearError()
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentMode = appMode,
            currentTheme = isDarkModePref,
            currentLanguage = selectedLanguage,
            isOverlayActive = isOverlayRunning,
            onDismiss = { showSettingsDialog = false },
            onModeChanged = { mode -> viewModel.setAppVersionMode(mode) },
            onThemeChanged = { theme -> viewModel.setTheme(theme) },
            onLanguageChanged = { lang -> viewModel.setLanguage(lang) },
            onToggleOverlay = { active -> viewModel.setOverlayRunning(active) },
            onOpenDevGuide = { showDevGuideDialog = true }
        )
    }
}

@Composable
private fun TopAppBarContent(
    appMode: AppVersionMode,
    waypointCount: Int,
    onToggleMode: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenRoutes: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("app_branding_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "be",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "be.anywhr",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = stringApp("tagline"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Right Action Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Full / Lite mode switch pill
                Surface(
                    onClick = onToggleMode,
                    shape = RoundedCornerShape(20.dp),
                    color = if (appMode == AppVersionMode.FULL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(end = 4.dp).testTag("mode_toggle_pill")
                ) {
                    Text(
                        text = if (appMode == AppVersionMode.FULL) stringApp("mode_full_short") else stringApp("mode_lite_short"),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (appMode == AppVersionMode.FULL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                // Search
                IconButton(onClick = onOpenSearch, modifier = Modifier.size(38.dp).testTag("search_icon_button")) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Bookmarks
                IconButton(onClick = onOpenBookmarks, modifier = Modifier.size(38.dp).testTag("bookmarks_icon_button")) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmarks", tint = MaterialTheme.colorScheme.onSurface)
                }

                // Routes Planner (with badge if waypoints added)
                IconButton(onClick = onOpenRoutes, modifier = Modifier.size(38.dp).testTag("routes_icon_button")) {
                    if (waypointCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge { Text("$waypointCount") }
                            }
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = "Route Planner", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Icon(Icons.Default.AltRoute, contentDescription = "Route Planner", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Settings
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(38.dp).testTag("settings_icon_button")) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun MockPermissionBanner(
    errorMsg: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().testTag("mock_permission_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Developer Options Setup Required",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                )
                Text(
                    text = "Tap to set be.anywhr as Mock Location App in Android Settings.",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                )
            }
        }
    }
}

@Composable
private fun BottomControlDock(
    isMocking: Boolean,
    isJoystickVisible: Boolean,
    isDrifting: Boolean,
    isTripRunning: Boolean,
    onToggleMocking: () -> Unit,
    onTeleportHere: () -> Unit,
    onToggleDrift: () -> Unit,
    onToggleJoystick: () -> Unit,
    onSaveLocation: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Start/Stop Mocking Button
            Button(
                onClick = onToggleMocking,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("toggle_mock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMocking) stringApp("stop_mock") else stringApp("start_mock"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Teleport Button
            IconButton(
                onClick = onTeleportHere,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("dock_teleport_button")
            ) {
                Icon(
                    Icons.Default.FlightTakeoff,
                    contentDescription = "Teleport Here",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Drift Mode Toggle
            IconButton(
                onClick = onToggleDrift,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDrifting) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("dock_drift_button")
            ) {
                Icon(
                    Icons.Default.Radar,
                    contentDescription = "Drift Randomizer",
                    tint = if (isDrifting) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Joystick Toggle
            IconButton(
                onClick = onToggleJoystick,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isJoystickVisible) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("dock_joystick_button")
            ) {
                Icon(
                    Icons.Default.SportsEsports,
                    contentDescription = "Toggle Joystick",
                    tint = if (isJoystickVisible) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Save Bookmark
            IconButton(
                onClick = onSaveLocation,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("dock_bookmark_button")
            ) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = "Save Bookmark",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
