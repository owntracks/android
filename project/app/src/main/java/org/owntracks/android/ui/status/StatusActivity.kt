package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.DateFormatter
import org.owntracks.android.ui.DrawerProvider
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.status.logs.LogViewerActivity

@AndroidEntryPoint
class StatusActivity : AppCompatActivity(), ServiceStarter by ServiceStarter.Impl() {
  @Inject lateinit var drawerProvider: DrawerProvider

  @Inject lateinit var preferences: Preferences

  val viewModel: StatusViewModel by viewModels()
  private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    UiStatusBinding.inflate(layoutInflater).apply {
      setContentView(root)
      appbar.toolbar.apply {
        setSupportActionBar(this)
        drawerProvider.attach(this)
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
              .setPositiveButton(R.string.statusLocationPermissionsPromptPositiveButton) { _, _ ->
                showLocationPermissionsStarter()
              }
              .setNegativeButton(R.string.statusLocationPermissionsPromptNegativeButton) { dialog, _
                ->
                dialog.cancel()
              }
              .show()
        } else {
          showLocationPermissionsStarter()
        }
      }
      lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
          launch {
            viewModel.endpointState.collect { endpointState ->
              connectedStatus.text = endpointState.getLabel(this@StatusActivity)
              endpointStateContainer.visibility =
                  if (endpointState.error == null && endpointState.message == null) {
                    LinearLayout.GONE
                  } else {
                    LinearLayout.VISIBLE
                  }
              connectedStatusMessage.text =
                  if (endpointState.error != null) {
                    endpointState.getErrorLabel(this@StatusActivity)
                  } else {
                    endpointState.message
                  }
            }
          }
          launch {
            viewModel.endpointQueueLength.collect { queueLength ->
              endpointQueueLength.text =
                  String.format(resources.configuration.locales[0], "%,d", queueLength)
            }
          }
          launch {
            viewModel.dozeWhitelisted.collect { whitelisted ->
              dozeWhiteListedValue.text =
                  getString(
                      if (whitelisted) {
                        R.string.statusBatteryDozeWhiteListEnabled
                      } else {
                        R.string.statusBatteryDozeWhiteListDisabled
                      },
                  )
            }
          }
          launch {
            viewModel.currentLocation.collect { location ->
              lastBackgroundUpdate.text =
                  if (location != null) {
                    DateFormatter.formatDate(location.time)
                  } else {
                    getString(R.string.na)
                  }
            }
          }
          launch {
            viewModel.serviceStarted.collect { serviceStarted ->
              backgroundServiceStarted.text = DateFormatter.formatDate(serviceStarted)
            }
          }
          launch {
            viewModel.locationPermissions.collect { locationPermissions ->
              locationPermissionsValue.text = getString(locationPermissions)
            }
          }
        }
      }
    }
    startService(this)
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshDozeModeWhitelisted()
    viewModel.refreshLocationPermissions()
  }
}
