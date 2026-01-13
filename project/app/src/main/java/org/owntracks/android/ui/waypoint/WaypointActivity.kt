package org.owntracks.android.ui.waypoint

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.location.geofencing.Latitude
import org.owntracks.android.location.geofencing.Longitude
import org.owntracks.android.location.roundForDisplay
import org.owntracks.android.ui.theme.OwnTracksTheme

@AndroidEntryPoint
class WaypointActivity : AppCompatActivity() {
    private val viewModel: WaypointViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val waypointId = if (intent.hasExtra("waypointId")) {
            intent.getLongExtra("waypointId", 0)
        } else {
            null
        }

        if (waypointId != null && waypointId != 0L) {
            viewModel.loadWaypoint(waypointId)
        }

        setContent {
            OwnTracksTheme {
                val waypoint by viewModel.waypoint.observeAsState()

                // Initialize state from waypoint when it loads
                var description by remember(waypoint?.id) {
                    mutableStateOf(waypoint?.description ?: "")
                }
                var latitude by remember(waypoint?.id) {
                    mutableStateOf(
                        waypoint?.geofenceLatitude?.value?.roundForDisplay() ?: ""
                    )
                }
                var longitude by remember(waypoint?.id) {
                    mutableStateOf(
                        waypoint?.geofenceLongitude?.value?.roundForDisplay() ?: ""
                    )
                }
                var radius by remember(waypoint?.id) {
                    mutableStateOf(
                        waypoint?.geofenceRadius?.toString() ?: ""
                    )
                }

                WaypointScreen(
                    description = description,
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    canDelete = viewModel.canDeleteWaypoint(),
                    onDescriptionChange = { description = it },
                    onLatitudeChange = { latitude = it },
                    onLongitudeChange = { longitude = it },
                    onRadiusChange = { radius = it },
                    onSaveClick = {
                        val lat = latitude.toDoubleOrNull()
                        val lon = longitude.toDoubleOrNull()
                        val rad = radius.toIntOrNull()
                        if (lat != null && lon != null && rad != null) {
                            viewModel.saveWaypoint(
                                description,
                                Latitude(lat),
                                Longitude(lon),
                                rad
                            )
                            finish()
                        }
                    },
                    onDeleteClick = {
                        viewModel.delete()
                        finish()
                    },
                    onBackClick = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
