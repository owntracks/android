package org.owntracks.android.ui.waypoints

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.test.SimpleIdlingResource
import org.owntracks.android.test.ThresholdIdlingResourceInterface
import org.owntracks.android.ui.NotificationsStash
import org.owntracks.android.ui.mixins.NotificationsPermissionRequested
import org.owntracks.android.ui.navigation.Destination
import org.owntracks.android.ui.navigation.toActivityClass
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.theme.OwnTracksTheme
import org.owntracks.android.ui.waypoint.WaypointActivity

@AndroidEntryPoint
class WaypointsActivity :
    AppCompatActivity(),
    NotificationsPermissionRequested by NotificationsPermissionRequested.Impl() {

    @Inject lateinit var notificationsStash: NotificationsStash

    @Inject lateinit var preferences: Preferences

    @Inject
    @Named("outgoingQueueIdlingResource")
    @get:VisibleForTesting
    lateinit var outgoingQueueIdlingResource: ThresholdIdlingResourceInterface

    @Inject
    @Named("publishResponseMessageIdlingResource")
    @get:VisibleForTesting
    lateinit var publishResponseMessageIdlingResource: SimpleIdlingResource

    @Inject
    @Named("waypointsRecyclerViewIdlingResource")
    lateinit var waypointsRecyclerViewIdlingResource: ThresholdIdlingResourceInterface

    private val viewModel: WaypointsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        postNotificationsPermissionInit(this, preferences, notificationsStash)

        setContent {
            OwnTracksTheme {
                val waypoints by viewModel.waypointsFlow.collectAsStateWithLifecycle()

                WaypointsScreen(
                    waypoints = waypoints,
                    onNavigate = { destination ->
                        navigateToDestination(destination)
                    },
                    onAddClick = {
                        startActivity(Intent(this@WaypointsActivity, WaypointActivity::class.java))
                    },
                    onWaypointClick = { waypoint ->
                        startActivity(
                            Intent(this@WaypointsActivity, WaypointActivity::class.java)
                                .putExtra("waypointId", waypoint.id)
                        )
                    },
                    onImportClick = {
                        startActivity(Intent(this@WaypointsActivity, LoadActivity::class.java))
                    },
                    onExportClick = {
                        viewModel.exportWaypoints()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestNotificationsPermission()
    }

    private fun navigateToDestination(destination: Destination) {
        val activityClass = destination.toActivityClass() ?: return
        if (this.javaClass != activityClass) {
            startActivity(Intent(this, activityClass))
        }
    }
}
