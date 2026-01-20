package org.owntracks.android.ui.welcome

import android.os.Build
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {

    override val welcomePages: List<WelcomePage> by lazy {
        buildList {
            add(WelcomePage.Intro)
            add(WelcomePage.ConnectionSetup)
            add(WelcomePage.LocationPermission)
            // Notification permission only for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(WelcomePage.NotificationPermission)
            }
            add(WelcomePage.Finish)
        }
    }
}
