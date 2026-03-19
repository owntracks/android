package org.owntracks.android.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.owntracks.android.R

/**
 * Composable containing the map FABs (My Location and Map Layers).
 * Positioned at the bottom-end of the screen, above the bottom navigation.
 */
@Composable
fun MapFabs(
    myLocationStatus: MyLocationStatus,
    myLocationEnabled: Boolean,
    onMyLocationClick: () -> Unit,
    onMapLayersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        MapLayersFab(onClick = onMapLayersClick)
        MyLocationFab(
            status = myLocationStatus,
            enabled = myLocationEnabled,
            onClick = onMyLocationClick
        )
    }
}

@Composable
private fun MapLayersFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_layers_24),
            contentDescription = stringResource(R.string.mapLayerDialogTitle)
        )
    }
}

@Composable
private fun MyLocationFab(
    status: MyLocationStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (status) {
        MyLocationStatus.DISABLED -> R.drawable.ic_baseline_location_disabled_24
        MyLocationStatus.AVAILABLE -> R.drawable.ic_baseline_location_searching_24
        MyLocationStatus.FOLLOWING -> R.drawable.ic_baseline_my_location_24
    }

    val tintColor = when (status) {
        MyLocationStatus.FOLLOWING -> colorResource(R.color.fabMyLocationForegroundActiveTint)
        else -> colorResource(R.color.fabMyLocationForegroundInActiveTint)
    }

    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        containerColor = colorResource(R.color.fabMyLocationBackground),
        modifier = modifier.alpha(if (enabled) 1f else 0.5f)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(R.string.currentLocationButtonLabel),
            tint = tintColor
        )
    }
}
