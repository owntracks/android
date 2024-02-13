package org.owntracks.android.ui.preferences

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import android.security.KeyChain
import android.security.KeyChain.EXTRA_NAME
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.ValidatingEditTextPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.owntracks.android.R
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import org.owntracks.android.support.RunThingsOnOtherThreads
import timber.log.Timber

@AndroidEntryPoint
class ConnectionFragment : AbstractPreferenceFragment(), Preferences.OnPreferenceChangeListener {
    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    @CoroutineScopes.IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @CoroutineScopes.MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    lateinit var runThingsOnOtherThreads: RunThingsOnOtherThreads

    private lateinit var menuProvider: PreferencesMenuProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuProvider = PreferencesMenuProvider(this, messageProcessor)
    }

    private val booleanSummaryProperties = setOf(
        Preferences::password
    )

    private val certificateInstallerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_connection, rootKey)
        setPreferenceVisibility()

        // Set the initial summaries
        booleanSummaryProperties.forEach { property ->
            setBooleanIndicatorSummary(property)
        }

        // Set the validators on the preferences that need them
        mapOf(
            Preferences::url.name to { input: String -> input.toHttpUrlOrNull() != null },
            Preferences::port.name to { port ->
                port.isNotBlank() && port.toIntOrNull() != null && (1..65535).contains(
                    port.toInt()
                )
            },
            Preferences::deviceId.name to { input: String -> input.isNotBlank() },
            Preferences::host.name to { input: String -> input.isNotBlank() },
            Preferences::tid.name to { input: String -> input.isNotBlank() && input.length <= 2 },
            Preferences::clientId.name to { input: String -> input.isNotBlank() },
            Preferences::keepalive.name to { input: String ->
                input.toIntOrNull() != null && input.toInt() >= 0
            }
        ).forEach { (preferenceName, validator) ->
            findPreference<ValidatingEditTextPreference>(preferenceName)?.apply {
                validationFunction = validator
            }
        }

        findPreference<ValidatingEditTextPreference>(Preferences::keepalive.name)?.validationErrorArgs = 0

        /* We need to work out if the given cert still exists. We also need to do this off-main thread */
        lifecycleScope.launch(Dispatchers.IO) {
            val shouldClearPreference = if (preferences.tlsClientCrt.isNotBlank()) {
                val certChain = KeyChain.getCertificateChain(requireActivity(), preferences.tlsClientCrt)
                if (certChain.isNullOrEmpty()) {
                    Timber.w("Client cert for ${preferences.tlsClientCrt} no longer exists in device store.")
                }
                true
            } else {
                false
            }
            // However, we need to update the UI on the main thread
            launch(Dispatchers.Main) {
                if (shouldClearPreference) {
                    findPreference<PopupMenuPreference>(Preferences::tlsClientCrt.name)?.setValue("")
                }
                findPreference<Preference>(Preferences::tlsClientCrt.name)?.isEnabled = true
            }
        }
        findPreference<Preference>("tlsClientCertInstall")?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isVisible = true
            }
            setOnPreferenceClickListener {
                certificateInstallerLauncher.launch(
                    KeyChain.createInstallIntent()
                        .apply { putExtra(EXTRA_NAME, "owntracks-client-cert") }
                )
                true
            }
        }

        findPreference<Preference>("tlsCAInstall")?.apply {
            setOnPreferenceClickListener {
                startActivity(Intent(ACTION_SECURITY_SETTINGS))
                true
            }
        }
        findPreference<PopupMenuPreference>(Preferences::tlsClientCrt.name)?.apply {
            setOnPreferenceClickListener {
                val choosePrivateKeyLaunch = {
                    KeyChain.choosePrivateKeyAlias(
                        requireActivity(),
                        { alias ->
                            if (alias != null) {
                                runThingsOnOtherThreads.postOnMainHandlerDelayed({ setValue(alias) }, 0)
                            }
                        },
                        null,
                        null,
                        null,
                        null
                    )
                }
                if (preferences.tlsClientCrt.isBlank()) {
                    choosePrivateKeyLaunch()
                } else {
                    showMenu({ preferences.tlsClientCrt = "" }, { choosePrivateKeyLaunch() })
                }
                true
            }
        }
    }

    /**
     * Called when an edit text wants to display a preference dialog. For []ValidatingEditTextPreference],
     *
     * @param preference
     */
    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is ValidatingEditTextPreference -> {
                ValidatingEditTextPreferenceDialogFragmentCompat(preference).apply {
                    arguments = Bundle(1).apply { putString("key", preference.key) }
                    setTargetFragment(this@ConnectionFragment, 0)
                }
                    .show(parentFragmentManager, FRAGMENT_DIALOG)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    /**
     * Sets the summary for a preference that should either show "Set" or "Not set"
     *
     * @param preference
     */
    private fun setBooleanIndicatorSummary(preference: KProperty<String>) {
        lifecycleScope.launch(mainDispatcher) {
            findPreference<Preference>(preference.name)?.summary =
                requireContext().getString(
                    if (preference.getter.call(preferences).isBlank()) {
                        R.string.preferencesNotSet
                    } else {
                        R.string.preferencesSet
                    }
                )
        }
    }

    override fun onStop() {
        preferences.unregisterOnPreferenceChangedListener(this)
        requireActivity().removeMenuProvider(menuProvider)
        super.onStop()
    }

    override fun onStart() {
        setPreferenceVisibility()
        preferences.registerOnPreferenceChangedListener(this)
        super.onStart()
        requireActivity().addMenuProvider(menuProvider)
    }

    /**
     * Show / hide preferences based on which mode is set
     *
     */
    private fun setPreferenceVisibility() {
        listOf(
            "preferenceGroupTLS",
            "preferenceGroupParameters",
            Preferences::host.name,
            Preferences::port.name,
            Preferences::clientId.name,
            Preferences::ws.name
        ).map {
            findPreference<Preference>(it)?.isVisible = preferences.mode == ConnectionMode.MQTT
        }
        listOf(Preferences::url.name).map {
            findPreference<Preference>(it)?.isVisible = preferences.mode == ConnectionMode.HTTP
        }
    }

    override fun onPreferenceChanged(properties: Set<String>) {
        if (Preferences::mode.name in properties) {
            setPreferenceVisibility()
        }
        // Set the summaries of the changed booleanSummary properties
        booleanSummaryProperties.map { it.name }
            .intersect(properties)
            .forEach { propertyName ->
                setBooleanIndicatorSummary(booleanSummaryProperties.first { it.name == propertyName })
            }
    }

    companion object {
        const val FRAGMENT_DIALOG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}
