package org.owntracks.android.ui.status

import android.os.Build
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.ui.base.BaseActivity

@AndroidEntryPoint
class StatusActivity : BaseActivity<UiStatusBinding?, StatusMvvm.ViewModel<StatusMvvm.View?>?>(),
    StatusMvvm.View {
    val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindAndAttachContentView(R.layout.ui_status, savedInstanceState)
        setSupportToolbar(binding!!.appbar.toolbar)
        setDrawer(binding!!.appbar.toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding!!.dozeWhiteListed.setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_baseline_battery_charging_full_24)
                    .setTitle(getString(R.string.batteryOptimizationWhitelistDialogTitle))
                    .setMessage(getString(R.string.batteryOptimizationWhitelistDialogMessage))
                    .setCancelable(true)
                    .setPositiveButton(getString(R.string.batteryOptimizationWhitelistDialogButtonLabel)) { _, _ ->
                        if (viewModel?.dozeWhitelisted?.value == true) {
                            startActivity(batteryOptimizationIntents.settingsIntent)
                        } else {
                            startActivity(batteryOptimizationIntents.directPackageIntent)
                        }
                    }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.refreshDozeModeWhitelisted()
    }
}