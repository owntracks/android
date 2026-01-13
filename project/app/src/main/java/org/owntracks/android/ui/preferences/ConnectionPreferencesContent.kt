package org.owntracks.android.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode

@Composable
fun ConnectionPreferencesContent(
    preferences: Preferences,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Trigger recomposition when mode changes
    var currentMode by remember { mutableStateOf(preferences.mode) }

    val modeEntries = listOf(
        ConnectionMode.MQTT to stringResource(R.string.mode_mqtt_private_label),
        ConnectionMode.HTTP to stringResource(R.string.mode_http_private_label)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Connection Mode
        ListPreference(
            title = stringResource(R.string.preferencesProfileId),
            value = preferences.mode,
            entries = modeEntries,
            onValueChange = {
                preferences.mode = it
                currentMode = it
            }
        )

        // Endpoint section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionEndpoint))

        // HTTP URL (only visible in HTTP mode)
        if (preferences.mode == ConnectionMode.HTTP) {
            EditTextPreference(
                title = stringResource(R.string.preferencesUrl),
                value = preferences.url,
                onValueChange = { preferences.url = it },
                keyboardType = KeyboardType.Uri,
                validator = { it.toHttpUrlOrNull() != null },
                validationError = stringResource(R.string.preferencesUrlValidationError)
            )
        }

        // MQTT Host (only visible in MQTT mode)
        if (preferences.mode == ConnectionMode.MQTT) {
            EditTextPreference(
                title = stringResource(R.string.preferencesHost),
                value = preferences.host,
                onValueChange = { preferences.host = it },
                validator = { it.isNotBlank() },
                validationError = stringResource(R.string.preferencesHostValidationError)
            )

            EditIntPreference(
                title = stringResource(R.string.preferencesPort),
                value = preferences.port,
                onValueChange = { preferences.port = it },
                summary = preferences.port.toString(),
                minValue = 1,
                maxValue = 65535,
                validationError = stringResource(R.string.preferencesPortValidationError)
            )

            EditTextPreference(
                title = stringResource(R.string.preferencesClientId),
                value = preferences.clientId,
                onValueChange = { preferences.clientId = it },
                validator = { it.isNotBlank() && it.length <= 23 },
                validationError = stringResource(R.string.preferencesClientIdValidationError)
            )

            SwitchPreference(
                title = stringResource(R.string.preferencesWebsocket),
                checked = preferences.ws,
                onCheckedChange = { preferences.ws = it }
            )
        }

        // Identification section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionIdentification))

        EditTextPreference(
            title = stringResource(R.string.preferencesDeviceName),
            value = preferences.deviceId,
            onValueChange = { preferences.deviceId = it },
            validator = { it.isNotBlank() },
            validationError = stringResource(R.string.preferencesDeviceNameValidationError)
        )

        EditTextPreference(
            title = stringResource(R.string.preferencesTrackerId),
            value = preferences.tid.toString(),
            onValueChange = {
                preferences.tid = org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars(it)
            },
            validator = { it.isNotBlank() && it.length <= 2 },
            validationError = stringResource(R.string.preferencesTrackerIdValidationError)
        )

        // Credentials section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionCredentials))

        EditTextPreference(
            title = stringResource(R.string.preferencesUsername),
            value = preferences.username,
            onValueChange = { preferences.username = it }
        )

        EditTextPreference(
            title = stringResource(R.string.preferencesBrokerPassword),
            value = preferences.password,
            onValueChange = { preferences.password = it },
            isPassword = true
        )

        // TLS section (only visible in MQTT mode)
        if (preferences.mode == ConnectionMode.MQTT) {
            PreferenceCategory(title = stringResource(R.string.tls))

            SwitchPreference(
                title = stringResource(R.string.tls),
                checked = preferences.tls,
                onCheckedChange = { preferences.tls = it }
            )

            // TLS Client Certificate selection
            PreferenceItem(
                title = stringResource(R.string.preferencesClientCrt),
                summary = if (preferences.tlsClientCrt.isNotBlank()) {
                    preferences.tlsClientCrt
                } else {
                    stringResource(R.string.preferencesNotSet)
                },
                enabled = preferences.tls,
                onClick = {
                    // Certificate selection is handled by the activity
                    // This would require a callback
                }
            )

            PreferenceItem(
                title = stringResource(R.string.preferencesCaCrtInstall),
                onClick = {
                    // Opens security settings - handled by activity
                }
            )

            // Parameters section
            PreferenceCategory(title = stringResource(R.string.preferencesParameters))

            EditIntPreference(
                title = stringResource(R.string.preferencesKeepalive),
                value = preferences.keepalive,
                onValueChange = { preferences.keepalive = it },
                summary = preferences.keepalive.toString(),
                dialogMessage = stringResource(R.string.preferencesKeepaliveDialogMessage),
                minValue = 0,
                validationError = stringResource(R.string.preferencesKeepaliveValidationError, 0)
            )

            SwitchPreference(
                title = stringResource(R.string.preferencesCleanSessionEnabled),
                checked = preferences.cleanSession,
                onCheckedChange = { preferences.cleanSession = it }
            )
        }

        // Actions section
        PreferenceCategory(title = stringResource(R.string.preferencesActions))

        PreferenceItem(
            title = stringResource(R.string.reconnect),
            onClick = onReconnect
        )
    }
}
