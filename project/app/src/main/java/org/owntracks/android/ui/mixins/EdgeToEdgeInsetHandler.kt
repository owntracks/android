package org.owntracks.android.ui.mixins

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

/**
 * Mixin interface for handling edge-to-edge window insets on activities with drawer navigation.
 * Provides default implementation for applying system bar insets to appbar and navigation drawer.
 */
interface EdgeToEdgeInsetHandler {

  /**
   * Applies window insets to drawer layout, appbar, and navigation view for edge-to-edge display.
   *
   * @param drawerLayout The root drawer layout
   * @param appBarView The app bar view to receive top insets (must be an AppBarLayout)
   * @param navigationView The navigation drawer to receive top and bottom insets
   */
  fun AppCompatActivity.applyDrawerEdgeToEdgeInsets(
      drawerLayout: DrawerLayout,
      appBarView: View,
      navigationView: NavigationView
  ) {
    ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, windowInsets ->
      val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

      appBarView.updatePadding(top = insets.top)
      navigationView.updatePadding(top = insets.top, bottom = insets.bottom)

      WindowInsetsCompat.CONSUMED
    }
  }

  /** Default implementation that can be used by activities */
  class Impl : EdgeToEdgeInsetHandler
}
