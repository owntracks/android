package org.owntracks.android.ui.preferences.about

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.ui.theme.OwnTracksTheme

/**
 * Internal navigation state for the About screens
 */
private enum class AboutNavState {
    About,
    Licenses
}

@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme {
                var currentScreen by rememberSaveable { mutableStateOf(AboutNavState.About) }

                // Handle system back button for internal navigation
                BackHandler(enabled = currentScreen == AboutNavState.Licenses) {
                    currentScreen = AboutNavState.About
                }

                when (currentScreen) {
                    AboutNavState.About -> {
                        AboutScreen(
                            onBackClick = { finish() },
                            onLicensesClick = { currentScreen = AboutNavState.Licenses },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AboutNavState.Licenses -> {
                        LicensesScreen(
                            onBackClick = { currentScreen = AboutNavState.About },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
