package org.owntracks.android.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.RequirementsChecker
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.theme.OwnTracksTheme

abstract class BaseWelcomeActivity : AppCompatActivity() {
    @Inject lateinit var preferences: Preferences

    @Inject lateinit var requirementsChecker: RequirementsChecker

    /**
     * List of pages to display in the welcome flow.
     * Override in GMS/OSS variants to customize.
     */
    abstract val welcomePages: List<WelcomePage>

    /**
     * Optional Play Services page content for GMS variant.
     * Returns null for OSS variant.
     */
    open val playServicesPageContent: (@Composable (snackbarHostState: SnackbarHostState, onCanProceed: (Boolean) -> Unit) -> Unit)?
        get() = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (preferences.setupCompleted) {
            startActivity(
                Intent(this, MapActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
            finish()
            return
        }

        setContent {
            OwnTracksTheme {
                WelcomeScreen(
                    pages = welcomePages,
                    hasLocationPermissions = { requirementsChecker.hasLocationPermissions() },
                    hasBackgroundLocationPermission = { requirementsChecker.hasBackgroundLocationPermission() },
                    hasNotificationPermissions = { requirementsChecker.hasNotificationPermissions() },
                    onLocationPermissionGranted = {
                        preferences.userDeclinedEnableLocationPermissions = false
                    },
                    onLocationPermissionDenied = {
                        preferences.userDeclinedEnableLocationPermissions = true
                    },
                    onBackgroundLocationPermissionGranted = {
                        preferences.userDeclinedEnableBackgroundLocationPermissions = false
                    },
                    onBackgroundLocationPermissionDenied = {
                        preferences.userDeclinedEnableBackgroundLocationPermissions = true
                    },
                    onNotificationPermissionGranted = {
                        preferences.userDeclinedEnableNotificationPermissions = false
                    },
                    onNotificationPermissionDenied = {
                        preferences.userDeclinedEnableNotificationPermissions = true
                    },
                    onSetupComplete = {
                        preferences.setupCompleted = true
                        startActivity(
                            Intent(this, MapActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    },
                    onOpenPreferences = {
                        preferences.setupCompleted = true
                        startActivity(
                            Intent(this, PreferencesActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    },
                    userDeclinedLocationPermission = preferences.userDeclinedEnableLocationPermissions,
                    userDeclinedNotificationPermission = preferences.userDeclinedEnableNotificationPermissions,
                    playServicesPageContent = playServicesPageContent
                )
            }
        }
    }
}
