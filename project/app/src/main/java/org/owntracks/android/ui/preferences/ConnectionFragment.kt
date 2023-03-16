package org.owntracks.android.ui.preferences

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
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

    private lateinit var menuProvider: PreferencesMenuProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuProvider = PreferencesMenuProvider(requireActivity(), messageProcessor)
    }

    private val booleanSummaryProperties = setOf(
        Preferences::password,
        Preferences::tlsClientCrtPassword,
        Preferences::tlsCaCrt,
        Preferences::tlsClientCrt
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_connection, rootKey)
        setPreferenceVisibility()

        // Set the initial summaries
        booleanSummaryProperties.forEach { property ->
            setBooleanIndicatorSummary(property)
        }

        mapOf(
            Preferences::tlsCaCrt to caCrtLauncher,
            Preferences::tlsClientCrt to clientCertLauncher
        )
            .forEach { (property, launcher) ->
                findPreference<FilePickerPreference>(property.name)?.apply {
                    setOnPreferenceClickListener {
                        PopupMenu(it.context, view)
                            .apply {
                                menuInflater.inflate(R.menu.picker, menu)
                                setOnMenuItemClickListener { item: MenuItem ->
                                    when (item.itemId) {
                                        R.id.clear -> {
                                            lifecycleScope.launch(ioDispatcher) {
                                                val filename = property.getter.call(preferences)
                                                try {
                                                    Timber.v("Deleting certificate: $filename")
                                                    val result =
                                                        requireContext().applicationContext.deleteFile(filename)
                                                    Timber.v("Certificate deletion success: $result")
                                                } catch (e: Exception) {
                                                    Timber.w(e, "Unable to remove certificate file $filename")
                                                }
                                                property.setter.call(preferences, "")
                                                setBooleanIndicatorSummary(property)
                                            }
                                        }
                                        R.id.select -> {
                                            launcher.launch(
                                                Intent.createChooser(
                                                    Intent(Intent.ACTION_GET_CONTENT).apply {
                                                        addCategory(Intent.CATEGORY_OPENABLE)
                                                        type = "*/*"
                                                    },
                                                    requireContext().getString(R.string.loadActivitySelectAFile)
                                                )
                                            )
                                        }
                                    }
                                    true
                                }
                            }
                            .show()
                        true
                    }
                }
            }
    }

    /**
     * Called when an edit text wants to display a preference dialog. For []ValidatingEditTextPreference],
     *
     * @param preference
     */
    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference.key) {
            Preferences::url.name -> {
                actuallyDisplayPreferenceDialog(
                    ValidatingEditTextPreferenceDialogFragmentCompat(
                        R.string.preferencesUrlValidationError
                    ) { url ->
                        try {
                            URL(url)
                            true
                        } catch (e: MalformedURLException) {
                            false
                        }
                    },
                    preference.key
                )
            }
            Preferences::deviceId.name -> {
                actuallyDisplayPreferenceDialog(
                    ValidatingEditTextPreferenceDialogFragmentCompat(
                        R.string.preferencesDeviceNameValidationError
                    ) { deviceName -> deviceName.isNotBlank() },
                    preference.key
                )
            }
            Preferences::tid.name -> {
                actuallyDisplayPreferenceDialog(
                    ValidatingEditTextPreferenceDialogFragmentCompat(
                        R.string.preferencesTrackerIdValidationError,
                        maxLength = 2
                    ) { deviceName -> deviceName.isNotBlank() },
                    preference.key
                )
            }

            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    /**
     * Display a dialog fragment for the preference
     *
     * @param fragment the dialog to display
     * @param key the preference key for which this is a dialog
     */
    private fun actuallyDisplayPreferenceDialog(fragment: Fragment, key: String) {
        fragment.apply {
            arguments = Bundle(1).apply { putString("key", key) }
            setTargetFragment(this@ConnectionFragment, 0)
        }

        when (fragment) {
            is DialogFragment -> {
                fragment.show(parentFragmentManager, FRAGMENT_DIALOG)
            }
            else -> {
                parentFragmentManager.beginTransaction()
                    .add(fragment, FRAGMENT_DIALOG)
                    .commit()
            }
        }
    }

    private val caCrtLauncher = getFilePickerLauncher(Preferences::tlsCaCrt)
    private val clientCertLauncher = getFilePickerLauncher(Preferences::tlsClientCrt)

    private fun getFilePickerLauncher(property: KMutableProperty<String>) =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val maybeContentUri = it.data?.data
            maybeContentUri?.also { contentUri ->
                lifecycleScope.launch(ioDispatcher) {
                    requireContext().applicationContext.run {
                        contentResolver.openInputStream(contentUri)
                            .use { inputStream ->
                                inputStream?.run {
                                    property.setter.call(preferences, Base64.encodeToString(readBytes(), NO_WRAP))
                                    setBooleanIndicatorSummary(property)
                                }
                            }
                    }
                }
            } ?: run {
                Snackbar.make(
                    requireView(),
                    R.string.unableToCopyCertificate,
                    Snackbar.LENGTH_LONG
                )
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
                    if (preference.getter.call(preferences)
                            .isBlank()
                    ) {
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
