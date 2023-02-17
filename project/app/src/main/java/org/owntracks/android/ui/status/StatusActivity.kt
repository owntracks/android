package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.R
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.support.DrawerProvider
import org.owntracks.android.ui.status.logs.LogViewerActivity

@AndroidEntryPoint
class StatusActivity : AppCompatActivity() {
    @Inject
    lateinit var drawerProvider: DrawerProvider
    val viewModel: StatusViewModel by viewModels()
    private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<UiStatusBinding>(this, R.layout.ui_status)
            .apply {
                vm = viewModel
                lifecycleOwner = this@StatusActivity
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
                        .setPositiveButton(getString(R.string.batteryOptimizationWhitelistDialogButtonLabel)) { _, _ ->
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
                        Intent(
                            this@StatusActivity,
                            LogViewerActivity::class.java
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDozeModeWhitelisted()
    }
}
