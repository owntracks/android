package org.owntracks.android.ui.waypoints

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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

/**
 * Full Waypoints screen with Scaffold, TopAppBar, and BottomNavBar.
 * Used when WaypointsActivity is launched as a standalone activity.
 */
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
            WaypointsTopAppBar(
                onAddClick = onAddClick,
                showMenu = showMenu,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onImportClick = {
                    showMenu = false
                    onImportClick()
                },
                onExportClick = {
                    showMenu = false
                    onExportClick()
                }
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
        WaypointsScreenContent(
            waypoints = waypoints,
            onWaypointClick = onWaypointClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Content-only version of the Waypoints screen without Scaffold.
 * Used within the NavHost when hosted in a single-activity architecture.
 * The top bar is managed by the parent MapActivity's Scaffold.
 */
@Composable
fun WaypointsScreenContent(
    waypoints: List<WaypointModel>,
    onWaypointClick: (WaypointModel) -> Unit,
    onAddClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (waypoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
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
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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

/**
 * TopAppBar for Waypoints screen, extracted for reuse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointsTopAppBar(
    onAddClick: () -> Unit,
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(R.string.title_activity_waypoints)) },
        actions = {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.addWaypoint))
            }
            Box {
                IconButton(onClick = onShowMenu) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.waypointsImport)) },
                        onClick = onImportClick
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.exportWaypointsToEndpoint)) },
                        onClick = onExportClick
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
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
