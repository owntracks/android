package org.owntracks.android.ui.status

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.ui.mixins.ServiceStarter
import org.owntracks.android.ui.status.logs.LogViewerActivity
import org.owntracks.android.ui.theme.OwnTracksTheme

@AndroidEntryPoint
class StatusActivity :
    AppCompatActivity(),
    ServiceStarter by ServiceStarter.Impl() {

    @Inject lateinit var preferences: Preferences

    val viewModel: StatusViewModel by viewModels()
    private val batteryOptimizationIntents by lazy { BatteryOptimizingIntents(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme {
                StatusScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onViewLogsClick = {
                        startActivity(
                            Intent(this@StatusActivity, LogViewerActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    batteryOptimizationIntents = batteryOptimizationIntents,
                    modifier = Modifier.fillMaxSize()
                )
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
