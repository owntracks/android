package org.owntracks.android.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.MonitoringMode

@Composable
fun ReportingPreferencesContent(
    preferences: Preferences,
    onDeleteSentData: () -> Unit,
    onResendSentData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monitoringModeEntries = listOf(
        MonitoringMode.Quiet to stringResource(R.string.monitoringModeDialogQuietTitle),
        MonitoringMode.Manual to stringResource(R.string.monitoringModeDialogManualTitle),
        MonitoringMode.Significant to stringResource(R.string.monitoringModeDialogSignificantTitle),
        MonitoringMode.Move to stringResource(R.string.monitoringModeDialogMoveTitle)
    )

    val retentionEntries = listOf(
        0 to stringResource(R.string.preferencesRetentionForever),
        1 to stringResource(R.string.preferencesRetention1Hour),
        6 to stringResource(R.string.preferencesRetention6Hours),
        24 to stringResource(R.string.preferencesRetention1Day),
        168 to stringResource(R.string.preferencesRetention7Days),
        720 to stringResource(R.string.preferencesRetention30Days),
        2160 to stringResource(R.string.preferencesRetention90Days),
        8760 to stringResource(R.string.preferencesRetention1Year)
    )

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showResendConfirmation by remember { mutableStateOf(false) }

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

        PreferenceCategory(
            title = stringResource(R.string.preferencesDataRetention)
        )

        ListPreference(
            title = stringResource(R.string.preferencesDataRetentionTitle),
            summary = stringResource(R.string.preferencesDataRetentionSummary),
            value = preferences.dataRetentionHours,
            entries = retentionEntries,
            onValueChange = { preferences.dataRetentionHours = it }
        )

        ListPreference(
            title = stringResource(R.string.preferencesSentDataRetentionTitle),
            summary = stringResource(R.string.preferencesSentDataRetentionSummary),
            value = preferences.sentDataRetentionHours,
            entries = retentionEntries,
            onValueChange = { preferences.sentDataRetentionHours = it }
        )

        PreferenceItem(
            title = stringResource(R.string.preferencesResendSentData),
            summary = stringResource(R.string.preferencesResendSentDataSummary),
            onClick = { showResendConfirmation = true }
        )

        PreferenceItem(
            title = stringResource(R.string.preferencesDeleteSentData),
            summary = stringResource(R.string.preferencesDeleteSentDataSummary),
            onClick = { showDeleteConfirmation = true }
        )
    }

    if (showResendConfirmation) {
        AlertDialog(
            onDismissRequest = { showResendConfirmation = false },
            title = { Text(stringResource(R.string.preferencesResendSentDataConfirmTitle)) },
            text = { Text(stringResource(R.string.preferencesResendSentDataConfirmMessage)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResendSentData()
                        showResendConfirmation = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResendConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.preferencesDeleteSentDataConfirmTitle)) },
            text = { Text(stringResource(R.string.preferencesDeleteSentDataConfirmMessage)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSentData()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
