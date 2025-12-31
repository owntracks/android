package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.ui.DrawerProvider
import org.owntracks.android.ui.mixins.AppBarInsetHandler
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.status.logs.LogViewerActivity

@AndroidEntryPoint
class StatusActivity :
    AppCompatActivity(),
    ServiceStarter by ServiceStarter.Impl(),
    AppBarInsetHandler by AppBarInsetHandler.Impl() {
  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject lateinit var preferences: Preferences

  val viewModel: StatusViewModel by viewModels()
  private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }
  private lateinit var binding: UiStatusBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    binding =
        DataBindingUtil.setContentView<UiStatusBinding>(this, R.layout.ui_status).apply {
          vm = viewModel
          lifecycleOwner = this@StatusActivity
          appbar.toolbar.apply {
            setSupportActionBar(this)
            drawerProvider.attach(this, drawerLayout, navigationView)
          }
          dozeWhiteListed.setOnClickListener {
            MaterialAlertDialogBuilder(this@StatusActivity)
                .setIcon(R.drawable.ic_baseline_battery_charging_full_24)
                .setTitle(getString(R.string.batteryOptimizationWhitelistDialogTitle))
                .setMessage(getString(R.string.batteryOptimizationWhitelistDialogMessage))
                .setCancelable(true)
                .setPositiveButton(
                    getString(R.string.batteryOptimizationWhitelistDialogButtonLabel),
                ) { _, _ ->
                  if (viewModel.dozeWhitelisted.value == true) {
                    startActivity(batteryOptimizationIntents.settingsIntent)
                  } else {
                    startActivity(batteryOptimizationIntents.directPackageIntent)
                  }
                }
                .show()
          }
          viewLogsButton.setOnClickListener {
            startActivity(
                Intent(this@StatusActivity, LogViewerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
          }
          locationPermissions.setOnClickListener {
            val showLocationPermissionsStarter = {
              startActivity(
                  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                  },
              )
            }
            if (viewModel.locationPermissions.value !=
                R.string.statusLocationPermissionsFineBackground) {
              MaterialAlertDialogBuilder(this@StatusActivity)
                  .setTitle(R.string.statusLocationPermissionsPromptTitle)
                  .setMessage(R.string.statusLocationPermissionsPromptText)
                  .setIcon(R.drawable.ic_baseline_my_location_24)
                  .setPositiveButton(R.string.statusLocationPermissionsPromptPositiveButton) { _, _
                    ->
                    showLocationPermissionsStarter()
                  }
                  .setNegativeButton(R.string.statusLocationPermissionsPromptNegativeButton) {
                      dialog,
                      _ ->
                    dialog.cancel()
                  }
                  .show()
            } else {
              showLocationPermissionsStarter()
            }
          }

          applyAppBarEdgeToEdgeInsets(drawerLayout, appbar.root, navigationView)
        }
    supportActionBar?.apply {
      setDisplayShowHomeEnabled(true)
      setDisplayHomeAsUpEnabled(false)
    }
    startService(this)
  }

  override fun onResume() {
    super.onResume()
    drawerProvider.updateHighlight()
    viewModel.refreshDozeModeWhitelisted()
    viewModel.refreshLocationPermissions()
  }
}

@BindingAdapter("endpointState")
fun LinearLayout.setVisibility(endpointState: EndpointState) {
  isVisible = !(endpointState.error == null && endpointState.message == null)
}

@BindingAdapter("endpointState")
fun TextView.setText(endpointState: EndpointState) {
  text =
      if (endpointState.error != null) {
        endpointState.getErrorLabel(context)
      } else {
        endpointState.message
      }
}
