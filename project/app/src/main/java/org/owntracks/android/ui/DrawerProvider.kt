package org.owntracks.android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.preferences.about.AboutActivity
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.waypoints.WaypointsActivity

@ActivityScoped
class DrawerProvider
@Inject
constructor(@ActivityContext activity: Context?, private val scheduler: Scheduler) {
  private val activity: AppCompatActivity = activity as AppCompatActivity
  private var navigationView: NavigationView? = null

  fun attach(toolbar: Toolbar, drawerLayout: DrawerLayout, navigationView: NavigationView) {
    this.navigationView = navigationView

    // Setup drawer toggle
    val toggle =
        ActionBarDrawerToggle(
            activity,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close,
        )
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    // Handle navigation item clicks
    navigationView.setNavigationItemSelectedListener { menuItem: MenuItem? ->
      handleNavigationItemSelected(menuItem!!)
      drawerLayout.closeDrawer(GravityCompat.START)
      true
    }

    // Highlight current activity
    highlightCurrentActivity(navigationView)
  }

  fun updateHighlight() {
    navigationView?.let { highlightCurrentActivity(it) }
  }

  private fun highlightCurrentActivity(navigationView: NavigationView) {
    var itemId = -1
    when (activity) {
      is MapActivity -> {
        itemId = R.id.nav_map
      }
      is ContactsActivity -> {
        itemId = R.id.nav_contacts
      }
      is WaypointsActivity -> {
        itemId = R.id.nav_waypoints
      }
      is StatusActivity -> {
        itemId = R.id.nav_status
      }
      is AboutActivity -> {
        itemId = R.id.nav_about
      }
      is PreferencesActivity -> {
        itemId = R.id.nav_preferences
      }
    }

    if (itemId != -1) {
      navigationView.setCheckedItem(itemId)
    }
  }

  private fun handleNavigationItemSelected(item: MenuItem) {
    val itemId = item.itemId

    when (itemId) {
      R.id.nav_map -> {
        navigateToActivity(MapActivity::class.java)
      }
      R.id.nav_contacts -> {
        navigateToActivity(ContactsActivity::class.java)
      }
      R.id.nav_waypoints -> {
        navigateToActivity(WaypointsActivity::class.java)
      }
      R.id.nav_status -> {
        navigateToActivity(StatusActivity::class.java)
      }
      R.id.nav_preferences -> {
        navigateToActivity(PreferencesActivity::class.java)
      }
      R.id.nav_about -> {
        navigateToActivity(AboutActivity::class.java)
      }
      R.id.nav_exit -> {
        exitApp()
      }
    }
  }

  private fun navigateToActivity(activityClass: Class<out Activity?>) {
    if (activity.javaClass != activityClass) {
      val intent = Intent(activity, activityClass)
      activity.startActivity(intent)
    }
  }

  private fun exitApp() {
    activity.stopService(Intent(activity, BackgroundService::class.java))
    activity.finishAffinity()
    scheduler.cancelAllTasks()
    Process.killProcess(Process.myPid())
  }
}
