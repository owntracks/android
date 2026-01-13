package org.owntracks.android.ui.preferences

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.mixins.WorkManagerInitExceptionNotifier
import org.owntracks.android.ui.navigation.Destination
import org.owntracks.android.ui.preferences.about.AboutActivity
import org.owntracks.android.ui.preferences.editor.EditorActivity
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.theme.OwnTracksTheme
import org.owntracks.android.ui.waypoints.WaypointsActivity

@AndroidEntryPoint
class PreferencesActivity :
    AppCompatActivity(),
    WorkManagerInitExceptionNotifier by WorkManagerInitExceptionNotifier.Impl(),
    ServiceStarter by ServiceStarter.Impl() {

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var messageProcessor: MessageProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme {
                PreferencesScreen(
                    preferences = preferences,
                    onNavigate = { destination ->
                        navigateToDestination(destination)
                    },
                    onNavigateToStatus = {
                        startActivity(Intent(this, StatusActivity::class.java))
                    },
                    onNavigateToAbout = {
                        startActivity(Intent(this, AboutActivity::class.java))
                    },
                    onNavigateToEditor = {
                        startActivity(Intent(this, EditorActivity::class.java))
                    },
                    onExitApp = {
                        stopService(Intent(this, BackgroundService::class.java))
                        finishAffinity()
                        Process.killProcess(Process.myPid())
                    },
                    onThemeChange = { theme ->
                        applyTheme(theme)
                    },
                    onReconnect = {
                        lifecycleScope.launch {
                            messageProcessor.reconnect()
                        }
                    }
                )
            }
        }

        // We may have come here straight from the WelcomeActivity, so start the service.
        startService(this)

        notifyOnWorkManagerInitFailure(this)
    }

    private fun navigateToDestination(destination: Destination) {
        val activityClass = when (destination) {
            Destination.Map -> MapActivity::class.java
            Destination.Contacts -> ContactsActivity::class.java
            Destination.Waypoints -> WaypointsActivity::class.java
            Destination.Preferences -> return // Already on Preferences
            else -> return // Ignore other destinations
        }
        startActivity(Intent(this, activityClass))
    }

    private fun applyTheme(theme: AppTheme) {
        val mode = when (theme) {
            AppTheme.Auto -> Preferences.SYSTEM_NIGHT_AUTO_MODE
            AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
