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
fun ReportingPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SwitchPreference(
            title = stringResource(R.string.preferencesPubExtendedData),
            summary = stringResource(R.string.preferencesPubExtendedDataSummary),
            checked = preferences.extendedData,
            onCheckedChange = { preferences.extendedData = it }
        )

        SwitchPreference(
            title = stringResource(R.string.preferencesRepublishOnReconnect),
            summary = stringResource(R.string.preferencesRepublishOnReconnectSummary),
            checked = preferences.publishLocationOnConnect,
            onCheckedChange = { preferences.publishLocationOnConnect = it }
        )
    }
}
