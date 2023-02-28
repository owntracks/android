package org.owntracks.android.ui.preferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
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
import org.owntracks.android.di.IoDispatcher
import org.owntracks.android.di.MainDispatcher
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.services.MessageProcessor
import timber.log.Timber

@AndroidEntryPoint
class ConnectionFragment : AbstractPreferenceFragment(), Preferences.OnPreferenceChangeListener {
    @Inject
    lateinit var messageProcessor: MessageProcessor

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    private lateinit var menuProvider: PreferencesMenuProvider

    override fun onAttach(context: Context) {
        super.onAttach(context)
        menuProvider = PreferencesMenuProvider(requireActivity(), messageProcessor)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_connection, rootKey)
        setPreferenceVisibility()

        setOf(Preferences::password, Preferences::tlsClientCrtPassword).forEach { property ->
            findPreference<EditTextPreference>(property.name)?.apply {
                setSummaryProvider {
                    requireContext().getString(
                        if (property.getter.call(preferences)
                                .isBlank()
                        ) {
                            R.string.preferencesNotSet
                        } else {
                            R.string.preferencesSet
                        }
                    )
                }
            }
        }

        mapOf(
            Preferences::tlsCaCrt to caCrtLauncher,
            Preferences::tlsClientCrt to clientCertLauncher
        )
            .forEach { (property, launcher) ->
                findPreference<FilePickerPreference>(property.name)?.apply {
                    setSummary(property)
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
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

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

    private val caCrtLauncher = getLauncher(Preferences::tlsCaCrt, "ca.crt")
    private val clientCertLauncher = getLauncher(Preferences::tlsClientCrt, "client.p12")
    private fun uriToFilename(uri: Uri, defaultFilename: String): String {
        if (uri.scheme.equals("content")) {
            requireContext().applicationContext.contentResolver.query(uri, null, null, null, null)
                .use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index < 0) {
                            throw IndexOutOfBoundsException("DISPLAY_NAME column not present in data store")
                        }
                        return cursor.getString(index)
                    }
                }
        }
        return defaultFilename
    }

    private fun getLauncher(property: KMutableProperty<String>, defaultFilename: String) =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val maybeContentUri = it.data?.data
            maybeContentUri?.also { contentUri ->
                val filename = uriToFilename(contentUri, defaultFilename)
                lifecycleScope.launch(ioDispatcher) {
                    requireContext().applicationContext.run {
                        contentResolver.openInputStream(contentUri)
                            .use { inputStream ->
                                openFileOutput(filename, Context.MODE_PRIVATE)
                                    .use { fileOutputStream ->
                                        Timber.i("Writing content to local storage")
                                        inputStream!!.copyTo(fileOutputStream)
                                    }
                            }
                    }
                    property.setter.call(preferences, filename)
                    setSummary(property)
                }
            } ?: run {
                Snackbar.make(
                    requireView(),
                    R.string.unableToCopyCertificate,
                    Snackbar.LENGTH_LONG
                )
            }
        }

    private fun setSummary(preference: KProperty<String>) {
        lifecycleScope.launch(mainDispatcher) {
            findPreference<Preference>(preference.name)?.summary =
                preference.getter.call(preferences)
                    .ifBlank { requireContext().getString(R.string.preferencesNotSet) }
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

    override fun onPreferenceChanged(properties: List<String>) {
        if (Preferences::mode.name in properties) {
            setPreferenceVisibility()
        }
        if (Preferences::tlsCaCrt.name in properties) {
            setSummary(Preferences::tlsCaCrt)
        }
        if (Preferences::tlsClientCrt.name in properties) {
            setSummary(Preferences::tlsClientCrt)
        }
    }

    companion object {
        const val FRAGMENT_DIALOG = "androidx.preference.PreferenceFragment.DIALOG"
    }
}
