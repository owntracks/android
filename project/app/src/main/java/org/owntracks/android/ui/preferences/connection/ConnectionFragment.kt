package org.owntracks.android.ui.preferences.connection

import android.os.Bundle
import androidx.preference.DialogPreference
import androidx.preference.Preference
import com.rengwuxian.materialedittext.validation.METValidator
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.services.MessageProcessorEndpointMqtt
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.preferences.AbstractPreferenceFragment


@AndroidEntryPoint
class ConnectionFragment : AbstractPreferenceFragment() {
    private val hiddenHTTPModePreferences =
            listOf(R.string.preferencesParameters, R.string.preferencesSecurity)

    private lateinit var keepaliveValidator: METValidator

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)

        setPreferencesFromResource(R.xml.preferences_connection, rootKey)

        findPreference<Preference>(getString(R.string.preferenceKeyModeId))?.setOnPreferenceChangeListener { _, newValue ->
            preferences.mode = newValue.toString().toInt()
            setPreferenceVisibilityBasedOnMode(preferences.mode)
            true
        }

        findPreference<DialogPreferenceCompat>(getString(R.string.preferenceKeyHost))?.setSummaryProvider {
            when (preferences.mode) {
                MessageProcessorEndpointHttp.MODE_ID -> preferences.url
                MessageProcessorEndpointMqtt.MODE_ID -> if (preferences.host.isNotEmpty()) "${preferences.host}:${preferences.port}" else ""
                else -> ""
            }
        }

        findPreference<Preference>(getString(R.string.preferencesIdentification))?.setSummaryProvider {
            if (preferences.deviceId.isNotEmpty() && preferences.username.isNotEmpty()) {
                "${preferences.username} / ${preferences.deviceId}"
            } else if (preferences.username.isNotEmpty()) {
                preferences.username
            } else {
                preferences.deviceId
            }
        }

        keepaliveValidator =
                object : METValidator(
                        getString(
                                R.string.preferencesKeepaliveValidationError,
                                if (preferences.isExperimentalFeatureEnabled(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE)) 1 else preferences.minimumKeepalive
                        )
                ) {
                    override fun isValid(text: CharSequence, isEmpty: Boolean): Boolean {
                        return try {
                            val intValue = text.toString().toInt()
                            isEmpty || preferences.keepAliveInRange(intValue) || preferences.isExperimentalFeatureEnabled(
                                    Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE
                            ) && intValue >= 1
                        } catch (e: NumberFormatException) {
                            false
                        }
                    }
                }

        setPreferenceVisibilityBasedOnMode(preferences.mode)
    }

    // Some settings become visible depending on the value of the mode setting
    private fun setPreferenceVisibilityBasedOnMode(mode: Int) {
        when (mode) {
            MessageProcessorEndpointHttp.MODE_ID -> {
                hiddenHTTPModePreferences.forEach {
                    findPreference<Preference>(getString(it))?.isVisible =
                            false
                }
                findPreference<DialogPreference>(getString(R.string.preferenceKeyHost))?.dialogLayoutResource =
                        R.layout.ui_preferences_connection_host_http
            }
            MessageProcessorEndpointMqtt.MODE_ID -> {
                hiddenHTTPModePreferences.forEach {
                    findPreference<Preference>(getString(it))?.isVisible =
                            true
                }
                findPreference<DialogPreference>(getString(R.string.preferenceKeyHost))?.dialogLayoutResource =
                        R.layout.ui_preferences_connection_host_mqtt
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        when (preference?.key) {
            getString(R.string.preferenceKeyHost) -> {
                when (preferences.mode) {
                    MessageProcessorEndpointHttp.MODE_ID -> {
                        HttpHostDialogFragmentCompat(
                                preference.key,
                                HttpHostDialogFragmentCompat.Model(preferences.url)
                        ) { model ->
                            preferences.url = model.url
                            preference.summaryProvider = preference.summaryProvider
                        }.apply {
                            setTargetFragment(this@ConnectionFragment, 0)
                        }.show(parentFragmentManager, null)
                    }
                    MessageProcessorEndpointMqtt.MODE_ID -> {
                        MqttHostDialogFragmentCompat(
                                preference.key,
                                MqttHostDialogFragmentCompat.Model(
                                        preferences.host,
                                        preferences.port,
                                        preferences.clientId,
                                        preferences.ws
                                )
                        ) { model ->
                            preferences.host = model.host
                            model.port
                                    ?.also { preferences.port = it }
                                    ?.run { preferences.setPortDefault() }
                            preferences.clientId = model.clientId
                            preferences.ws = model.webSockets
                            preference.summaryProvider = preference.summaryProvider
                        }.apply {
                            setTargetFragment(this@ConnectionFragment, 0)
                        }.show(parentFragmentManager, null)
                    }
                }
            }
            getString(R.string.preferencesIdentification) -> {
                IdentificationDialogFragmentCompat(
                        preference.key,
                        IdentificationDialogFragmentCompat.Model(
                                preferences.username,
                                preferences.password,
                                preferences.deviceId,
                                preferences.trackerId
                        )
                ) { model ->
                    preferences.username = model.username
                    preferences.password = model.password
                    preferences.deviceId = model.deviceId
                    preferences.trackerId = model.trackerId
                    preference.summaryProvider = preference.summaryProvider
                }.apply {
                    setTargetFragment(this@ConnectionFragment, 0)
                }.show(parentFragmentManager, null)
            }
            getString(R.string.preferencesSecurity) -> {
                SecurityDialogFragmentCompat(
                        preference.key,
                        SecurityDialogFragmentCompat.Model(
                                preferences.tls,
                                preferences.tlsCaCrt,
                                preferences.tlsClientCrt,
                                preferences.tlsClientCrtPassword
                        )
                ) { model ->
                    preferences.tls = model.tlsEnabled
                    preferences.tlsCaCrt = model.tlsCaCert
                    preferences.tlsClientCrt = model.tlsClientCert
                    preferences.tlsClientCrtPassword = model.tlsClientCertPassword
                    preference.summaryProvider = preference.summaryProvider
                }.apply {
                    setTargetFragment(this@ConnectionFragment, 0)
                }.show(parentFragmentManager, null)
            }
            getString(R.string.preferencesParameters) -> {
                ParametersDialogFragmentCompat(
                        preference.key,
                        ParametersDialogFragmentCompat.Model(
                                preferences.cleanSession,
                                preferences.keepalive
                        ),
                        keepaliveValidator
                ) { model ->
                    preferences.cleanSession = model.cleanSession
                    model.keepalive
                            ?.also { preferences.keepalive = it }
                            ?: run { preferences.setKeepaliveDefault() }
                    preference.summaryProvider = preference.summaryProvider
                }.apply {
                    setTargetFragment(this@ConnectionFragment, 0)
                }.show(parentFragmentManager, null)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }
}