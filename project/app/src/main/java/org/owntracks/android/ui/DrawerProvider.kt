package org.owntracks.android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
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
import timber.log.Timber

@ActivityScoped
class DrawerProvider
@Inject
constructor(@ActivityContext activity: Context, private val scheduler: Scheduler) {
  private val activity = activity as AppCompatActivity

  private fun getActivity(): Activity {
    return activity
  }

  private fun drawerItemForClass(
      activeActivity: AppCompatActivity,
      targetActivityClass: Class<*>,
      @StringRes targetActivityTitleResource: Int,
      @DrawableRes iconResource: Int
  ): PrimaryDrawerItem {
    return PrimaryDrawerItem()
        .withName(activeActivity.getString(targetActivityTitleResource))
        .withSelectable(false)
        .withIcon(iconResource)
        .withIconTintingEnabled(true)
        .withTag(targetActivityClass)
        .withIdentifier(targetActivityClass.hashCode().toLong())
  }

  private fun secondaryDrawerItemForClass(
      activeActivity: AppCompatActivity,
      targetActivityClass: Class<*>?,
      @StringRes targetActivityTitleResource: Int,
      @DrawableRes iconResource: Int
  ): SecondaryDrawerItem {
    val sdi =
        SecondaryDrawerItem()
            .withName(activeActivity.getString(targetActivityTitleResource))
            .withIcon(iconResource)
            .withIconTintingEnabled(true)
            .withSelectable(false)
    // The exit operation has no target class
    if (targetActivityClass != null) {
      sdi.withTag(targetActivityClass)
      sdi.withIdentifier(targetActivityClass.hashCode().toLong())
    } else {
      sdi.withIdentifier(EXIT_OPERATION_ID.toLong())
    }

    return sdi
  }

  fun attach(toolbar: Toolbar) {
    DrawerBuilder()
        .withActivity(activity)
        .withToolbar(toolbar)
        .withStickyFooterShadow(false)
        .withStickyFooterDivider(true)
        .addDrawerItems(
            drawerItemForClass(
                activity,
                MapActivity::class.java,
                R.string.title_activity_map,
                R.drawable.ic_baseline_layers_24,
            ),
            drawerItemForClass(
                activity,
                ContactsActivity::class.java,
                R.string.title_activity_contacts,
                R.drawable.ic_baseline_supervisor_account_24,
            ),
            drawerItemForClass(
                activity,
                WaypointsActivity::class.java,
                R.string.title_activity_waypoints,
                R.drawable.ic_baseline_adjust_24,
            ),
        )
        .addStickyDrawerItems(
            secondaryDrawerItemForClass(
                activity,
                StatusActivity::class.java,
                R.string.title_activity_status,
                R.drawable.ic_baseline_beenhere_24,
            ),
            secondaryDrawerItemForClass(
                activity,
                PreferencesActivity::class.java,
                R.string.title_activity_preferences,
                R.drawable.ic_baseline_settings_24,
            ),
            secondaryDrawerItemForClass(
                activity,
                AboutActivity::class.java,
                R.string.title_activity_about,
                R.drawable.ic_baseline_info_24,
            ),
            secondaryDrawerItemForClass(
                activity,
                null,
                R.string.title_exit,
                R.drawable.ic_baseline_power_settings_new_24,
            ),
        )
        .withOnDrawerItemClickListener { _: View?, _: Int, drawerItem: IDrawerItem<*, *>? ->
          if (drawerItem == null) return@withOnDrawerItemClickListener false
          if (drawerItem is SecondarySwitchDrawerItem) return@withOnDrawerItemClickListener true

          // Finish when exit app drawer option selected
          if (drawerItem.identifier == EXIT_OPERATION_ID.toLong()) {
            // Stop the background service
            activity.stopService((Intent(activity, BackgroundService::class.java)))
            // Finish the activity
            activity.finishAffinity()
            // Kill scheduled tasks
            scheduler.cancelAllTasks()
            Process.killProcess(Process.myPid())
            return@withOnDrawerItemClickListener true
          }

          val targetclass = drawerItem.tag as Class<*>

          if (activity.javaClass == targetclass) {
            return@withOnDrawerItemClickListener false
          }
          if (AppCompatActivity::class.java.isAssignableFrom(targetclass)) {
            // If the target class is an AppCompatActivity, we can start it
            @Suppress("UNCHECKED_CAST") startActivity(targetclass as Class<out AppCompatActivity>)
          } else {
            // Otherwise, we just log a warning
            Timber.w("Cannot start activity $targetclass from drawer")
          }

          false // return false to enable withCloseOnClick
        }
        .build()
  }

  private fun startActivity(activityClass: Class<out Activity>) {
    val activity: Context = getActivity()
    val intent = Intent(activity, activityClass)
    activity.startActivity(intent)
  }

  companion object {
    private const val EXIT_OPERATION_ID = 88296
  }
}
