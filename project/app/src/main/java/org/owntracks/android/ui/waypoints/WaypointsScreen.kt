package org.owntracks.android.ui.waypoints

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.location.geofencing.Geofence
import org.owntracks.android.ui.navigation.BottomNavBar
import org.owntracks.android.ui.navigation.Destination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointsScreen(
    waypoints: List<WaypointModel>,
    onNavigate: (Destination) -> Unit,
    onAddClick: () -> Unit,
    onWaypointClick: (WaypointModel) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_waypoints)) },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.addWaypoint))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.waypointsImport)) },
                                onClick = {
                                    showMenu = false
                                    onImportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.exportWaypointsToEndpoint)) },
                                onClick = {
                                    showMenu = false
                                    onExportClick()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentDestination = Destination.Waypoints,
                onNavigate = onNavigate
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (waypoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.waypointListPlaceholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(
                    items = waypoints,
                    key = { it.id }
                ) { waypoint ->
                    WaypointItem(
                        waypoint = waypoint,
                        onClick = { onWaypointClick(waypoint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WaypointItem(
    waypoint: WaypointModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = waypoint.description.ifEmpty { stringResource(R.string.na) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            waypoint.lastTriggered?.let { lastTriggered ->
                Text(
                    text = getRelativeTimeSpan(lastTriggered),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Text(
            text = getTransitionText(waypoint.lastTransition),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getTransitionText(transition: Int): String {
    return when (transition) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> stringResource(R.string.waypoint_region_inside)
        Geofence.GEOFENCE_TRANSITION_EXIT -> stringResource(R.string.waypoint_region_outside)
        else -> stringResource(R.string.waypoint_region_unknown)
    }
}

private fun getRelativeTimeSpan(instant: Instant): String {
    return DateUtils.getRelativeTimeSpanString(
        instant.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
