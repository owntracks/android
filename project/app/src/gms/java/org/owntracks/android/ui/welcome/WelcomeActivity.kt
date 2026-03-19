package org.owntracks.android.ui.welcome

import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {

    private val googleApi by lazy { GoogleApiAvailability.getInstance() }

    override val welcomePages: List<WelcomePage> by lazy {
        buildList {
            add(WelcomePage.Intro)
            add(WelcomePage.ConnectionSetup)
            add(WelcomePage.LocationPermission)
            // Notification permission only for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(WelcomePage.NotificationPermission)
            }
            // Play Services page only if not available
            if (googleApi.isGooglePlayServicesAvailable(this@WelcomeActivity) != ConnectionResult.SUCCESS) {
                add(WelcomePage.PlayServices)
            }
            add(WelcomePage.Finish)
        }
    }

    override val playServicesPageContent: (@Composable (snackbarHostState: SnackbarHostState, onCanProceed: (Boolean) -> Unit) -> Unit)?
        get() = { snackbarHostState, onCanProceed ->
            PlayServicesPage(
                onCanProceed = onCanProceed,
                snackbarHostState = snackbarHostState
            )
        }
}
