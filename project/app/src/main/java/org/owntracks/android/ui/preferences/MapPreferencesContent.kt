package org.owntracks.android.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences

@Composable
fun MapPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SwitchPreference(
            title = stringResource(R.string.preferencesShowWaypointsOnMap),
            summary = stringResource(R.string.preferencesShowWaypointsOnMapSummary),
            checked = preferences.showRegionsOnMap,
            onCheckedChange = { preferences.showRegionsOnMap = it }
        )

        SwitchPreference(
            title = stringResource(R.string.preferencesEnableMapRotation),
            summary = stringResource(R.string.preferencesEnableMapRotationSummary),
            checked = preferences.enableMapRotation,
            onCheckedChange = { preferences.enableMapRotation = it }
        )
    }
}
