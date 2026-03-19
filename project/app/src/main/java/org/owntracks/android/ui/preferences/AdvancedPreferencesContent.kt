package org.owntracks.android.ui.preferences

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.owntracks.android.R
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ReverseGeocodeProvider

@Composable
fun AdvancedPreferencesContent(
    preferences: Preferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val geocoderEntries = listOf(
        ReverseGeocodeProvider.None to "None",
        ReverseGeocodeProvider.Device to "Device (Google)",
        ReverseGeocodeProvider.OpenCage to "OpenCage"
    )

    val showOpencageKey = preferences.reverseGeocodeProvider == ReverseGeocodeProvider.OpenCage

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Services
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryAdvancedServices))

        SwitchPreference(
            title = stringResource(R.string.preferencesRemoteCommand),
            summary = stringResource(R.string.preferencesRemoteCommandSummary),
            checked = preferences.cmd,
            onCheckedChange = { preferences.cmd = it }
        )

        SwitchPreference(
            title = stringResource(R.string.preferencesRemoteConfiguration),
            summary = stringResource(R.string.preferencesRemoteConfigurationSummary),
            checked = preferences.remoteConfiguration,
            onCheckedChange = { preferences.remoteConfiguration = it }
        )

        // Locator
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryAdvancedLocator))

        EditIntPreference(
            title = stringResource(R.string.preferencesIgnoreInaccurateLocations),
            value = preferences.ignoreInaccurateLocations,
            onValueChange = { preferences.ignoreInaccurateLocations = it },
            summary = stringResource(R.string.preferencesIgnoreInaccurateLocationsSummary),
            dialogMessage = stringResource(R.string.preferencesIgnoreInaccurateLocationsDialog),
            minValue = 0
        )

        EditIntPreference(
            title = stringResource(R.string.preferencesLocatorDisplacement),
            value = preferences.locatorDisplacement,
            onValueChange = { preferences.locatorDisplacement = it },
            summary = stringResource(R.string.preferencesLocatorDisplacementSummary),
            dialogMessage = stringResource(R.string.preferencesLocatorDisplacementDialog),
            minValue = 0
        )

        EditIntPreference(
            title = stringResource(R.string.preferencesLocatorInterval),
            value = preferences.locatorInterval,
            onValueChange = { preferences.locatorInterval = it },
            summary = stringResource(R.string.preferencesLocatorIntervalSummary),
            dialogMessage = stringResource(R.string.preferencesLocatorIntervalDialog),
            minValue = 0
        )

        EditIntPreference(
            title = stringResource(R.string.preferencesMoveModeLocatorInterval),
            value = preferences.moveModeLocatorInterval,
            onValueChange = { preferences.moveModeLocatorInterval = it },
            summary = stringResource(R.string.preferencesMoveModeLocatorIntervalSummary),
            dialogMessage = stringResource(R.string.preferencesMoveModeLocatorIntervalDialog),
            minValue = 0
        )

        SwitchPreference(
            title = stringResource(R.string.preferencesPegLocatorFastestIntervalToInterval),
            summary = stringResource(R.string.preferencesPegLocatorFastestIntervalToIntervalSummary),
            checked = preferences.pegLocatorFastestIntervalToInterval,
            onCheckedChange = { preferences.pegLocatorFastestIntervalToInterval = it }
        )

        // Encryption
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryAdvancedEncryption))

        EditTextPreference(
            title = stringResource(R.string.preferencesEncryptionKey),
            value = preferences.encryptionKey,
            onValueChange = { preferences.encryptionKey = it },
            summary = stringResource(R.string.preferencesEncryptionKeySummary),
            dialogMessage = stringResource(R.string.preferencesEncryptionKeyDialogMessage),
            isPassword = true
        )

        // Misc
        PreferenceCategory(title = stringResource(R.string.preferencesCategoryAdvancedMisc))

        SwitchPreference(
            title = stringResource(R.string.preferencesAutostart),
            summary = stringResource(R.string.preferencesAutostartSummary),
            checked = preferences.autostartOnBoot,
            onCheckedChange = { preferences.autostartOnBoot = it }
        )

        InfoPreference(
            summary = stringResource(R.string.preferencesAdvancedAutostartWarning),
            icon = painterResource(R.drawable.ic_outline_info_24)
        )

        ListPreference(
            title = stringResource(R.string.preferencesReverseGeocodeProvider),
            value = preferences.reverseGeocodeProvider,
            entries = geocoderEntries,
            onValueChange = { preferences.reverseGeocodeProvider = it }
        )

        if (showOpencageKey) {
            PreferenceItem(
                title = "",
                summary = stringResource(R.string.preferencesAdvancedOpencagePrivacy),
                icon = painterResource(R.drawable.baseline_privacy_tip_24),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(context.getString(R.string.opencagePrivacyPolicyUrl))
                    )
                    context.startActivity(intent)
                }
            )

            EditTextPreference(
                title = stringResource(R.string.preferencesOpencageGeocoderApiKey),
                value = preferences.opencageApiKey,
                onValueChange = { preferences.opencageApiKey = it },
                summary = stringResource(R.string.preferencesOpencageGeocoderApiKeySummary),
                dialogMessage = stringResource(R.string.preferencesOpencageGeocoderApiKeyDialog)
            )
        }
    }
}
