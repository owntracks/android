package org.owntracks.android.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode

@Composable
fun ConnectionPreferencesContent(
    preferences: Preferences,
    endpointState: EndpointState,
    nextReconnectTime: java.time.Instant?,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    onReconnect: () -> Unit,
    onTryReconnectNow: () -> Unit,
    currentWifiSsid: String? = null,
    modifier: Modifier = Modifier
) {
    // Trigger recomposition when mode or toggle preferences change
    var currentMode by remember { mutableStateOf(preferences.mode) }
    var tlsEnabled by remember { mutableStateOf(preferences.tls) }
    var wsEnabled by remember { mutableStateOf(preferences.ws) }
    var cleanSessionEnabled by remember { mutableStateOf(preferences.cleanSession) }
    var localNetworkEnabled by remember { mutableStateOf(preferences.localNetworkEnabled) }
    var localNetworkTlsEnabled by remember { mutableStateOf(preferences.localNetworkTls) }

    // String preferences also need local state for recomposition
    var host by remember { mutableStateOf(preferences.host) }
    var url by remember { mutableStateOf(preferences.url) }
    var port by remember { mutableStateOf(preferences.port) }
    var clientId by remember { mutableStateOf(preferences.clientId) }
    var deviceId by remember { mutableStateOf(preferences.deviceId) }
    var tid by remember { mutableStateOf(preferences.tid.toString()) }
    var username by remember { mutableStateOf(preferences.username) }
    var password by remember { mutableStateOf(preferences.password) }
    var keepalive by remember { mutableStateOf(preferences.keepalive) }
    var localNetworkSsid by remember { mutableStateOf(preferences.localNetworkSsid) }
    var localNetworkHost by remember { mutableStateOf(preferences.localNetworkHost) }
    var localNetworkPort by remember { mutableStateOf(preferences.localNetworkPort) }

    val modeEntries = listOf(
        ConnectionMode.MQTT to stringResource(R.string.mode_mqtt_private_label),
        ConnectionMode.HTTP to stringResource(R.string.mode_http_private_label)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Connection status card at top
        ConnectionStatusCard(
            modifier = Modifier.padding(top = 12.dp),
            endpointState = endpointState,
            connectionEnabled = preferences.connectionEnabled,
            canStartConnection = isConfigurationComplete(preferences),
            nextReconnectTime = nextReconnectTime,
            onStartConnection = onStartConnection,
            onStopConnection = onStopConnection,
            onReconnect = onReconnect,
            onTryReconnectNow = onTryReconnectNow
        )

        // Endpoint section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionEndpoint))

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

        // HTTP URL (only visible in HTTP mode)
        if (preferences.mode == ConnectionMode.HTTP) {
            EditTextPreference(
                title = stringResource(R.string.preferencesUrl),
                value = url,
                onValueChange = {
                    preferences.url = it
                    url = it
                },
                keyboardType = KeyboardType.Uri,
                validator = { it.toHttpUrlOrNull() != null },
                validationError = stringResource(R.string.preferencesUrlValidationError)
            )
        }

        // MQTT Host (only visible in MQTT mode)
        if (preferences.mode == ConnectionMode.MQTT) {
            EditTextPreference(
                title = stringResource(R.string.preferencesHost),
                value = host,
                onValueChange = {
                    preferences.host = it
                    host = it
                },
                keyboardType = KeyboardType.Uri,
                validator = { it.isNotBlank() },
                validationError = stringResource(R.string.preferencesHostValidationError)
            )

            EditIntPreference(
                title = stringResource(R.string.preferencesPort),
                value = port,
                onValueChange = {
                    preferences.port = it
                    port = it
                },
                summary = port.toString(),
                minValue = 1,
                maxValue = 65535,
                validationError = stringResource(R.string.preferencesPortValidationError)
            )

            EditTextPreference(
                title = stringResource(R.string.preferencesClientId),
                value = clientId,
                onValueChange = {
                    preferences.clientId = it
                    clientId = it
                },
                validator = { it.isNotBlank() && it.length <= 23 },
                validationError = stringResource(R.string.preferencesClientIdValidationError)
            )

            SwitchPreference(
                title = stringResource(R.string.preferencesWebsocket),
                checked = wsEnabled,
                onCheckedChange = {
                    preferences.ws = it
                    wsEnabled = it
                }
            )
        }

        // Identification section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionIdentification))

        EditTextPreference(
            title = stringResource(R.string.preferencesDeviceName),
            value = deviceId,
            onValueChange = {
                preferences.deviceId = it
                deviceId = it
            },
            validator = { it.isNotBlank() },
            validationError = stringResource(R.string.preferencesDeviceNameValidationError)
        )

        EditTextPreference(
            title = stringResource(R.string.preferencesTrackerId),
            value = tid,
            onValueChange = {
                preferences.tid = org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars(it)
                tid = it
            },
            validator = { it.isNotBlank() && it.length <= 2 },
            validationError = stringResource(R.string.preferencesTrackerIdValidationError)
        )

        // Credentials section
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryConnectionCredentials))

        EditTextPreference(
            title = stringResource(R.string.preferencesUsername),
            value = username,
            onValueChange = {
                preferences.username = it
                username = it
            }
        )

        EditTextPreference(
            title = stringResource(R.string.preferencesBrokerPassword),
            value = password,
            onValueChange = {
                preferences.password = it
                password = it
            },
            isPassword = true
        )

        // TLS section (only visible in MQTT mode)
        if (preferences.mode == ConnectionMode.MQTT) {
            PreferenceCategory(title = stringResource(R.string.tls))

            SwitchPreference(
                title = stringResource(R.string.tls),
                checked = tlsEnabled,
                onCheckedChange = {
                    preferences.tls = it
                    tlsEnabled = it
                }
            )

            // Certificate options only shown when TLS is enabled
            if (tlsEnabled) {
                // Info banner explaining certificates are optional
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.preferencesTlsCertificatesInfo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PreferenceItem(
                    title = stringResource(R.string.preferencesClientCrt),
                    summary = if (preferences.tlsClientCrt.isNotBlank()) {
                        preferences.tlsClientCrt
                    } else {
                        stringResource(R.string.preferencesNotSet)
                    },
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
            }

            // Parameters section
            PreferenceCategory(title = stringResource(R.string.preferencesParameters))

            EditIntPreference(
                title = stringResource(R.string.preferencesKeepalive),
                value = keepalive,
                onValueChange = {
                    preferences.keepalive = it
                    keepalive = it
                },
                summary = keepalive.toString(),
                dialogMessage = stringResource(R.string.preferencesKeepaliveDialogMessage),
                minValue = 0,
                validationError = stringResource(R.string.preferencesKeepaliveValidationError, 0)
            )

            SwitchPreference(
                title = stringResource(R.string.preferencesCleanSessionEnabled),
                checked = cleanSessionEnabled,
                onCheckedChange = {
                    preferences.cleanSession = it
                    cleanSessionEnabled = it
                }
            )

            // Local Network section
            PreferenceCategory(title = stringResource(R.string.preferencesCategoryLocalNetwork))

            SwitchPreference(
                title = stringResource(R.string.preferencesLocalNetworkEnabled),
                summary = stringResource(R.string.preferencesLocalNetworkEnabledSummary),
                checked = localNetworkEnabled,
                onCheckedChange = {
                    preferences.localNetworkEnabled = it
                    localNetworkEnabled = it
                }
            )

            if (localNetworkEnabled) {
                EditTextWithButtonPreference(
                    title = stringResource(R.string.preferencesLocalNetworkSsid),
                    value = localNetworkSsid,
                    onValueChange = {
                        preferences.localNetworkSsid = it
                        localNetworkSsid = it
                    },
                    buttonLabel = stringResource(R.string.preferencesLocalNetworkUseCurrentSsid),
                    onButtonClick = { currentWifiSsid }
                )

                EditTextPreference(
                    title = stringResource(R.string.preferencesLocalNetworkHost),
                    value = localNetworkHost,
                    onValueChange = {
                        preferences.localNetworkHost = it
                        localNetworkHost = it
                    },
                    keyboardType = KeyboardType.Uri
                )

                EditIntPreference(
                    title = stringResource(R.string.preferencesLocalNetworkPort),
                    value = localNetworkPort,
                    onValueChange = {
                        preferences.localNetworkPort = it
                        localNetworkPort = it
                    },
                    summary = localNetworkPort.toString(),
                    minValue = 1,
                    maxValue = 65535,
                    validationError = stringResource(R.string.preferencesPortValidationError)
                )

                SwitchPreference(
                    title = stringResource(R.string.preferencesLocalNetworkTls),
                    checked = localNetworkTlsEnabled,
                    onCheckedChange = {
                        preferences.localNetworkTls = it
                        localNetworkTlsEnabled = it
                    }
                )

                // Show info card when currently on local network
                val isOnLocalNetwork = currentWifiSsid != null &&
                        localNetworkSsid.isNotBlank() &&
                        currentWifiSsid == localNetworkSsid

                if (isOnLocalNetwork) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.preferencesLocalNetworkActive),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
