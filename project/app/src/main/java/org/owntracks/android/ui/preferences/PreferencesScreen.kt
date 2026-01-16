package org.owntracks.android.ui.preferences

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.ui.navigation.BottomNavBar
import org.owntracks.android.ui.navigation.Destination

/**
 * Navigation destinations within the Preferences screen
 */
sealed class PreferenceScreen(val titleResId: Int) {
    data object Root : PreferenceScreen(R.string.title_activity_preferences)
    data object Connection : PreferenceScreen(R.string.preferencesServer)
    data object Map : PreferenceScreen(R.string.preferencesMap)
    data object Reporting : PreferenceScreen(R.string.preferencesReporting)
    data object Notification : PreferenceScreen(R.string.preferencesNotification)
    data object Advanced : PreferenceScreen(R.string.preferencesAdvanced)
    data object Experimental : PreferenceScreen(R.string.preferencesExperimental)

    companion object {
        val Saver: Saver<PreferenceScreen, String> = Saver(
            save = { screen ->
                when (screen) {
                    Root -> "root"
                    Connection -> "connection"
                    Map -> "map"
                    Reporting -> "reporting"
                    Notification -> "notification"
                    Advanced -> "advanced"
                    Experimental -> "experimental"
                }
            },
            restore = { value ->
                when (value) {
                    "connection" -> Connection
                    "map" -> Map
                    "reporting" -> Reporting
                    "notification" -> Notification
                    "advanced" -> Advanced
                    "experimental" -> Experimental
                    else -> Root
                }
            }
        )
    }
}

/**
 * Full Preferences screen with Scaffold, TopAppBar, and BottomNavBar.
 * Used when PreferencesActivity is launched as a standalone activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    preferences: Preferences,
    endpointStateRepo: EndpointStateRepo,
    onNavigate: (Destination) -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onExitApp: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onReconnect: () -> Unit,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable(stateSaver = PreferenceScreen.Saver) { mutableStateOf(PreferenceScreen.Root) }
    val endpointState by endpointStateRepo.endpointState.collectAsState()

    Scaffold(
        topBar = {
            PreferencesTopAppBar(
                currentScreen = currentScreen,
                endpointState = endpointState,
                preferences = preferences,
                onBackClick = { currentScreen = PreferenceScreen.Root },
                onStartConnection = onStartConnection,
                onStopConnection = onStopConnection,
                onReconnect = onReconnect
            )
        },
        bottomBar = {
            BottomNavBar(
                currentDestination = Destination.Preferences,
                onNavigate = onNavigate
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ConfigurationAlertBanner(
                preferences = preferences,
                onNavigateToConnection = { currentScreen = PreferenceScreen.Connection },
                showConfigureButton = currentScreen != PreferenceScreen.Connection
            )
            PreferencesScreenInner(
                preferences = preferences,
                currentScreen = currentScreen,
                onNavigateToScreen = { currentScreen = it },
                onNavigateToStatus = onNavigateToStatus,
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToEditor = onNavigateToEditor,
                onExitApp = onExitApp,
                onThemeChange = onThemeChange,
                onDynamicColorsChange = onDynamicColorsChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Content-only version of the Preferences screen without Scaffold.
 * Used within the NavHost when hosted in a single-activity architecture.
 * The top bar is managed by the parent MapActivity's Scaffold.
 */
@Composable
fun PreferencesScreenContent(
    preferences: Preferences,
    currentScreen: PreferenceScreen,
    onNavigateToScreen: (PreferenceScreen) -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onExitApp: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ConfigurationAlertBanner(
            preferences = preferences,
            onNavigateToConnection = { onNavigateToScreen(PreferenceScreen.Connection) },
            showConfigureButton = currentScreen != PreferenceScreen.Connection
        )
        PreferencesScreenInner(
            preferences = preferences,
            currentScreen = currentScreen,
            onNavigateToScreen = onNavigateToScreen,
            onNavigateToStatus = onNavigateToStatus,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToEditor = onNavigateToEditor,
            onExitApp = onExitApp,
            onThemeChange = onThemeChange,
            onDynamicColorsChange = onDynamicColorsChange,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

/**
 * TopAppBar for Preferences screen with back navigation for sub-screens.
 * Shows connection controls when on the Connection screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesTopAppBar(
    currentScreen: PreferenceScreen,
    endpointState: EndpointState,
    preferences: Preferences,
    onBackClick: () -> Unit,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.titleResId)) },
        navigationIcon = {
            if (currentScreen != PreferenceScreen.Root) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
        actions = {
            if (currentScreen == PreferenceScreen.Connection) {
                ConnectionControls(
                    endpointState = endpointState,
                    canStartConnection = isConfigurationComplete(preferences),
                    onStartConnection = onStartConnection,
                    onStopConnection = onStopConnection,
                    onReconnect = onReconnect
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}

/**
 * Connection control buttons with status indicator.
 */
@Composable
private fun ConnectionControls(
    endpointState: EndpointState,
    canStartConnection: Boolean,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Status indicator
        ConnectionStatusIndicator(endpointState = endpointState)

        Spacer(modifier = Modifier.width(4.dp))

        // Show different controls based on state
        when (endpointState) {
            EndpointState.CONNECTING -> {
                // Show stop button while connecting
                IconButton(onClick = onStopConnection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.connectionStop)
                    )
                }
            }
            EndpointState.CONNECTED -> {
                // Show reconnect and stop buttons when connected
                IconButton(onClick = onReconnect) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.reconnect)
                    )
                }
                IconButton(onClick = onStopConnection) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.connectionStop)
                    )
                }
            }
            EndpointState.DISCONNECTED,
            EndpointState.ERROR,
            EndpointState.ERROR_CONFIGURATION,
            EndpointState.IDLE,
            EndpointState.INITIAL -> {
                // Show start button when not connected (disabled if configuration incomplete)
                IconButton(
                    onClick = onStartConnection,
                    enabled = canStartConnection
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.connectionStart)
                    )
                }
            }
        }
    }
}

/**
 * Visual status indicator showing connection state as uppercase text.
 */
@Composable
private fun ConnectionStatusIndicator(
    endpointState: EndpointState,
    modifier: Modifier = Modifier
) {
    val statusText = when (endpointState) {
        EndpointState.CONNECTED -> stringResource(R.string.CONNECTED)
        EndpointState.CONNECTING -> stringResource(R.string.CONNECTING)
        EndpointState.DISCONNECTED -> stringResource(R.string.DISCONNECTED)
        EndpointState.ERROR -> stringResource(R.string.ERROR)
        EndpointState.ERROR_CONFIGURATION -> stringResource(R.string.ERROR_CONFIGURATION)
        EndpointState.IDLE -> stringResource(R.string.IDLE)
        EndpointState.INITIAL -> stringResource(R.string.INITIAL)
    }

    Text(
        text = statusText.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
    )
}

/**
 * Alert banner displayed when important connection preferences are missing.
 */
@Composable
fun ConfigurationAlertBanner(
    preferences: Preferences,
    onNavigateToConnection: () -> Unit,
    showConfigureButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val missingConfig = getMissingConfigurationMessage(preferences)

    if (missingConfig != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.configuration_missing_alert_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = missingConfig,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                if (showConfigureButton) {
                    TextButton(onClick = onNavigateToConnection) {
                        Text(
                            text = stringResource(R.string.configuration_missing_action),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns a message describing missing configuration, or null if configuration is complete.
 */
@Composable
private fun getMissingConfigurationMessage(preferences: Preferences): String? {
    return when (preferences.mode) {
        ConnectionMode.MQTT -> {
            if (preferences.host.isBlank()) {
                stringResource(R.string.configuration_missing_mqtt_host)
            } else {
                null
            }
        }
        ConnectionMode.HTTP -> {
            if (preferences.url.isBlank()) {
                stringResource(R.string.configuration_missing_http_url)
            } else {
                null
            }
        }
    }
}

/**
 * Checks if the connection configuration is complete.
 */
fun isConfigurationComplete(preferences: Preferences): Boolean {
    return when (preferences.mode) {
        ConnectionMode.MQTT -> preferences.host.isNotBlank()
        ConnectionMode.HTTP -> preferences.url.isNotBlank()
    }
}

/**
 * Inner content of the Preferences screen, switched based on current sub-screen.
 */
@Composable
private fun PreferencesScreenInner(
    preferences: Preferences,
    currentScreen: PreferenceScreen,
    onNavigateToScreen: (PreferenceScreen) -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onExitApp: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    when (currentScreen) {
        PreferenceScreen.Root -> RootPreferencesContent(
            preferences = preferences,
            onNavigateToScreen = onNavigateToScreen,
            onNavigateToStatus = onNavigateToStatus,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToEditor = onNavigateToEditor,
            onExitApp = onExitApp,
            onThemeChange = onThemeChange,
            onDynamicColorsChange = onDynamicColorsChange,
            modifier = modifier
        )
        PreferenceScreen.Connection -> ConnectionPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
        PreferenceScreen.Map -> MapPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
        PreferenceScreen.Reporting -> ReportingPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
        PreferenceScreen.Notification -> NotificationPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
        PreferenceScreen.Advanced -> AdvancedPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
        PreferenceScreen.Experimental -> ExperimentalPreferencesContent(
            preferences = preferences,
            modifier = modifier
        )
    }
}

@Composable
private fun RootPreferencesContent(
    preferences: Preferences,
    onNavigateToScreen: (PreferenceScreen) -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onExitApp: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeEntries = listOf(
        AppTheme.Light to "Always in light theme",
        AppTheme.Dark to "Always in dark theme",
        AppTheme.Auto to "Same as device"
    )

    val showExperimental = preferences.experimentalFeatures.contains(
        Preferences.EXPERIMENTAL_FEATURE_SHOW_EXPERIMENTAL_PREFERENCE_UI
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Theme selection
        ListPreference(
            title = stringResource(R.string.preferencesTheme),
            value = preferences.theme,
            entries = themeEntries,
            onValueChange = {
                preferences.theme = it
                onThemeChange(it)
            },
            icon = painterResource(R.drawable.ic_baseline_palette_24)
        )

        // Dynamic colors toggle (only shown on Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            SwitchPreference(
                title = stringResource(R.string.preferencesDynamicColors),
                summary = stringResource(R.string.preferencesDynamicColorsSummary),
                checked = preferences.dynamicColorsEnabled,
                onCheckedChange = {
                    preferences.dynamicColorsEnabled = it
                    onDynamicColorsChange(it)
                }
            )
        }

        // Navigation to sub-screens
        NavigationPreference(
            title = stringResource(R.string.preferencesServer),
            summary = when (preferences.mode) {
                org.owntracks.android.preferences.types.ConnectionMode.HTTP ->
                    stringResource(R.string.mode_http_private_label)
                org.owntracks.android.preferences.types.ConnectionMode.MQTT ->
                    stringResource(R.string.mode_mqtt_private_label)
            },
            icon = painterResource(R.drawable.ic_baseline_settings_ethernet_24),
            onClick = { onNavigateToScreen(PreferenceScreen.Connection) }
        )

        NavigationPreference(
            title = stringResource(R.string.preferencesMap),
            icon = painterResource(R.drawable.ic_baseline_map_24),
            onClick = { onNavigateToScreen(PreferenceScreen.Map) }
        )

        NavigationPreference(
            title = stringResource(R.string.preferencesReporting),
            icon = painterResource(R.drawable.ic_baseline_send_24),
            onClick = { onNavigateToScreen(PreferenceScreen.Reporting) }
        )

        NavigationPreference(
            title = stringResource(R.string.preferencesNotification),
            icon = painterResource(R.drawable.ic_baseline_notifications_24),
            onClick = { onNavigateToScreen(PreferenceScreen.Notification) }
        )

        NavigationPreference(
            title = stringResource(R.string.preferencesAdvanced),
            icon = painterResource(R.drawable.ic_baseline_settings_suggest_24),
            onClick = { onNavigateToScreen(PreferenceScreen.Advanced) }
        )

        if (showExperimental) {
            NavigationPreference(
                title = stringResource(R.string.preferencesExperimental),
                icon = painterResource(R.drawable.science_24),
                onClick = { onNavigateToScreen(PreferenceScreen.Experimental) }
            )
        }

        NavigationPreference(
            title = stringResource(R.string.configurationManagement),
            icon = painterResource(R.drawable.ic_baseline_import_export_24),
            onClick = onNavigateToEditor
        )

        // Info section
        PreferenceCategory(title = stringResource(R.string.preferencesInfo))

        NavigationPreference(
            title = stringResource(R.string.title_activity_status),
            icon = painterResource(R.drawable.ic_baseline_beenhere_24),
            onClick = onNavigateToStatus
        )

        NavigationPreference(
            title = stringResource(R.string.title_activity_about),
            icon = painterResource(R.drawable.ic_baseline_info_24),
            onClick = onNavigateToAbout
        )

        NavigationPreference(
            title = stringResource(R.string.title_exit),
            icon = painterResource(R.drawable.ic_baseline_power_settings_new_24),
            onClick = onExitApp
        )
    }
}
