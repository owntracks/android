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
import org.owntracks.android.preferences.types.MonitoringMode

@Composable
fun ReportingPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    val monitoringModeEntries = listOf(
        MonitoringMode.Quiet to stringResource(R.string.monitoringModeDialogQuietTitle),
        MonitoringMode.Manual to stringResource(R.string.monitoringModeDialogManualTitle),
        MonitoringMode.Significant to stringResource(R.string.monitoringModeDialogSignificantTitle),
        MonitoringMode.Move to stringResource(R.string.monitoringModeDialogMoveTitle)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ListPreference(
            title = stringResource(R.string.monitoringModeDialogTitle),
            value = preferences.monitoring,
            entries = monitoringModeEntries,
            onValueChange = { preferences.monitoring = it }
        )

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
