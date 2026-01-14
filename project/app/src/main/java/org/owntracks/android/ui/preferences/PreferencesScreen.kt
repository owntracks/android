package org.owntracks.android.ui.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.AppTheme
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
    onNavigate: (Destination) -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onExitApp: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by rememberSaveable(stateSaver = PreferenceScreen.Saver) { mutableStateOf(PreferenceScreen.Root) }

    Scaffold(
        topBar = {
            PreferencesTopAppBar(
                currentScreen = currentScreen,
                onBackClick = { currentScreen = PreferenceScreen.Root }
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
        PreferencesScreenInner(
            preferences = preferences,
            currentScreen = currentScreen,
            onNavigateToScreen = { currentScreen = it },
            onNavigateToStatus = onNavigateToStatus,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToEditor = onNavigateToEditor,
            onExitApp = onExitApp,
            onThemeChange = onThemeChange,
            onReconnect = onReconnect,
            modifier = Modifier.padding(paddingValues)
        )
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
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PreferencesScreenInner(
            preferences = preferences,
            currentScreen = currentScreen,
            onNavigateToScreen = onNavigateToScreen,
            onNavigateToStatus = onNavigateToStatus,
            onNavigateToAbout = onNavigateToAbout,
            onNavigateToEditor = onNavigateToEditor,
            onExitApp = onExitApp,
            onThemeChange = onThemeChange,
            onReconnect = onReconnect,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * TopAppBar for Preferences screen with back navigation for sub-screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesTopAppBar(
    currentScreen: PreferenceScreen,
    onBackClick: () -> Unit,
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
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
    onReconnect: () -> Unit,
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
            modifier = modifier
        )
        PreferenceScreen.Connection -> ConnectionPreferencesContent(
            preferences = preferences,
            onReconnect = onReconnect,
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
