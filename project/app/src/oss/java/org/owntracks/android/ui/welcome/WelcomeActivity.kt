package org.owntracks.android.ui.welcome

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {

  override val fragmentList by lazy {
    listOf(
        introFragment,
        connectionSetupFragment,
        locationPermissionFragment,
        notificationPermissionFragment,
        finishFragment)
  }
}
