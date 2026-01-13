package org.owntracks.android.ui.preferences

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesBinding
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.mixins.WorkManagerInitExceptionNotifier
import org.owntracks.android.ui.preferences.about.AboutActivity
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.waypoints.WaypointsActivity

@AndroidEntryPoint
open class PreferencesActivity :
    AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    WorkManagerInitExceptionNotifier by WorkManagerInitExceptionNotifier.Impl(),
    ServiceStarter by ServiceStarter.Impl() {
  private lateinit var binding: UiPreferencesBinding

  protected open val startFragment: Fragment
    get() = PreferencesFragment()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.ui_preferences)
    binding =
        DataBindingUtil.setContentView<UiPreferencesBinding>(this, R.layout.ui_preferences).apply {
          appbar.toolbar.run {
            setSupportActionBar(this)
          }

          // Apply edge-to-edge insets
          ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            appbar.root.updatePadding(top = systemBars.top)
            bottomNavigation.updatePadding(bottom = systemBars.bottom)
            insets
          }

          // Setup bottom navigation
          bottomNavigation.selectedItemId = R.id.nav_preferences
          bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
              R.id.nav_map -> {
                startActivity(Intent(this@PreferencesActivity, MapActivity::class.java))
                true
              }
              R.id.nav_contacts -> {
                startActivity(Intent(this@PreferencesActivity, ContactsActivity::class.java))
                true
              }
              R.id.nav_waypoints -> {
                startActivity(Intent(this@PreferencesActivity, WaypointsActivity::class.java))
                true
              }
              R.id.nav_preferences -> true
              else -> false
            }
          }
        }

    supportFragmentManager.run {
      addOnBackStackChangedListener {
        if (supportFragmentManager.fragments.isEmpty()) {
          setToolbarTitle(title)
        } else {
          setToolbarTitle(
              (supportFragmentManager.fragments[0] as PreferenceFragmentCompat)
                  .preferenceScreen
                  .title)
        }
      }
      beginTransaction().replace(R.id.content_frame, startFragment, null).commit()
      when (intent.getStringExtra(START_FRAGMENT_KEY)) {
        ConnectionFragment::class.java.name ->
            beginTransaction().replace(R.id.content_frame, ConnectionFragment()).commit()
      }
      executePendingTransactions()
    }

    // We may have come here straight from the WelcomeActivity, so start the service.
    startService(this)

    notifyOnWorkManagerInitFailure(this)
  }

  private fun setToolbarTitle(text: CharSequence?) {
    binding.appbar.toolbar.title = text
  }

  override fun onPreferenceStartFragment(
      caller: PreferenceFragmentCompat,
      pref: Preference
  ): Boolean {
    val args = pref.extras
    val fragment = supportFragmentManager.fragmentFactory.instantiate(classLoader, pref.fragment!!)
    fragment.arguments = args
    // Replace the existing Fragment with the new Fragment
    supportFragmentManager
        .beginTransaction()
        .replace(R.id.content_frame, fragment)
        .addToBackStack(pref.key)
        .commit()

    return true
  }

  fun navigateToStatus() {
    startActivity(Intent(this, StatusActivity::class.java))
  }

  fun navigateToAbout() {
    startActivity(Intent(this, AboutActivity::class.java))
  }

  fun exitApp() {
    stopService(Intent(this, BackgroundService::class.java))
    finishAffinity()
    Process.killProcess(Process.myPid())
  }

  companion object {
    const val START_FRAGMENT_KEY = "startFragment"
  }
}
