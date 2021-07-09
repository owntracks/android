package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiStatusBinding
import org.owntracks.android.support.DrawerProvider
import javax.inject.Inject

@AndroidEntryPoint
class StatusActivity : AppCompatActivity() {
    @Inject
    lateinit var drawerProvider: DrawerProvider
    val viewModel: StatusViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: UiStatusBinding = DataBindingUtil.setContentView(this, R.layout.ui_status);
        binding.vm = viewModel
        binding.lifecycleOwner = this
        setSupportActionBar(binding.appbar.toolbar)
        drawerProvider.attach(binding.appbar.toolbar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            binding.dozeWhiteListed.setOnClickListener {

                MaterialAlertDialogBuilder(this)
                        .setIcon(R.drawable.ic_baseline_battery_charging_full_24)
                        .setTitle(getString(R.string.batteryOptimizationWhitelistDialogTitle))
                        .setMessage(getString(R.string.batteryOptimizationWhitelistDialogMessage))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.batteryOptimizationWhitelistDialogButtonLabel)) { _, _ ->
                            startActivity(
                                    Intent(
                                            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                    )
                            )
                        }.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDozeModeWhitelisted()
    }
}