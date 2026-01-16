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
fun NotificationPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Ongoing notifications
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryNotificationOngoing))

        SwitchPreference(
            title = stringResource(R.string.preferencesNotificationLocation),
            summary = stringResource(R.string.preferencesNotificationLocationSummary),
            checked = preferences.notificationLocation,
            onCheckedChange = { preferences.notificationLocation = it }
        )

        // Background notifications
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryNotificationBackground))

        SwitchPreference(
            title = stringResource(R.string.preferencesNotificationEvents),
            summary = stringResource(R.string.preferencesNotificationEventsSummary),
            checked = preferences.notificationEvents,
            onCheckedChange = { preferences.notificationEvents = it }
        )

        // Error notifications
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryNotificationErrors))

        SwitchPreference(
            title = stringResource(R.string.preferencesNotificationGeocoderErrors),
            summary = stringResource(R.string.preferencesNotificationGeocoderErrorsSummary),
            checked = preferences.notificationGeocoderErrors,
            onCheckedChange = { preferences.notificationGeocoderErrors = it }
        )
    }
}
