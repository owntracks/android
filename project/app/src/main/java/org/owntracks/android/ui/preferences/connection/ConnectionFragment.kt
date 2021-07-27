package org.owntracks.android.ui.preferences.connection

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.owntracks.android.R
import org.owntracks.android.services.MessageProcessorEndpointHttp
import org.owntracks.android.support.Preferences
import org.owntracks.android.ui.preferences.AbstractPreferenceFragment
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL


@AndroidEntryPoint
class ConnectionFragment : AbstractPreferenceFragment() {
    private var filePickerPreferenceKey: Int = 0
    private val hiddenHTTPModePreferences =
            listOf(
                    R.string.preferenceKeyHost,
                    R.string.preferenceKeyPort,
                    R.string.preferenceKeyClientId,
                    R.string.preferenceKeyWS,
                    R.string.preferencesSecurity,
                    R.string.preferencesParameters
            )
    private val hiddenMQTTModePreferences = listOf(R.string.preferenceKeyURL)

    @SuppressLint("ResourceType")
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferencesFix(savedInstanceState, rootKey)

        setPreferencesFromResource(R.xml.preferences_connection, rootKey)

        findPreference<Preference>(getString(R.string.preferenceKeyModeId))?.setOnPreferenceChangeListener { _, newValue ->
            preferences.mode = newValue.toString().toInt()
            setPreferenceVisibilityBasedOnMode(preferences.mode)
            true
        }

        findPreference<Preference>(getString(R.string.preferenceKeyPassword))?.run {
            setOnPreferenceChangeListener { preference, _ ->
                preference.summaryProvider = preference.summaryProvider
                true
            }
            setSummaryProvider {
                if (preferences.password.isBlank()) {
                    getString(com.takisoft.preferencex.R.string.not_set)
                } else {
                    getString(R.string.preferencesTLSClientCrtPasswordSetSummary)
                }
            }
        }

        findPreference<Preference>(getString(R.string.preferenceKeyTLSCaCrt))?.run {
            val preferenceKey = R.string.preferenceKeyTLSCaCrt
            this.setViewId(preferenceKey)
            setSummaryProvider {
                if (preferences.tlsCaCrt.isBlank()) getString(com.takisoft.preferencex.R.string.not_set) else preferences.tlsCaCrt
            }
            setOnPreferenceClickListener {
                this@ConnectionFragment.view?.findViewById<View>(preferenceKey)
                        ?.run {
                            certificateFieldPopupMenu(
                                    this,
                                    preferenceKey,
                                    copyFileToPrivateStorageActivityResult
                            ).show()
                        }
                true
            }
        }
        findPreference<Preference>(getString(R.string.preferenceKeyTLSClientCrt))?.run {
            val preferenceKey = R.string.preferenceKeyTLSClientCrt
            this.setViewId(preferenceKey)
            setSummaryProvider {
                if (preferences.tlsClientCrt.isBlank()) getString(com.takisoft.preferencex.R.string.not_set) else preferences.tlsClientCrt
            }
            setOnPreferenceClickListener {
                this@ConnectionFragment.view?.findViewById<View>(preferenceKey)
                        ?.run {
                            certificateFieldPopupMenu(
                                    this,
                                    preferenceKey,
                                    copyFileToPrivateStorageActivityResult
                            ).show()
                        }
                true
            }
        }

        findPreference<Preference>(getString(R.string.preferenceKeyTLSClientCrtPassword))?.setSummaryProvider {
            if (preferences.tlsClientCrtPassword.isBlank())
                getString(com.takisoft.preferencex.R.string.not_set)
            else
                getString(R.string.preferencesTLSClientCrtPasswordSetSummary)
        }

        setPreferenceVisibilityBasedOnMode(preferences.mode)
    }


    // Some settings become visible depending on the value of the mode setting
    private fun setPreferenceVisibilityBasedOnMode(mode: Int) {
        hiddenHTTPModePreferences.forEach {
            findPreference<Preference>(getString(it))?.isVisible =
                    mode != MessageProcessorEndpointHttp.MODE_ID
        }
        hiddenMQTTModePreferences.forEach {
            findPreference<Preference>(getString(it))?.isVisible =
                    mode == MessageProcessorEndpointHttp.MODE_ID
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        when (preference?.key) {
            getString(R.string.preferenceKeyURL) -> {
                displayPreferenceDialog(
                        ValidatingEditTextPreferenceDialogFragmentCompat(
                                R.string.preferencesUrlValidationError
                        ) { url ->
                            try {
                                URL(url)
                                true
                            } catch (e: MalformedURLException) {
                                false
                            }
                        }, preference.key
                )
            }
            getString(R.string.preferenceKeyPort) -> {
                displayPreferenceDialog(
                        ValidatingEditTextPreferenceDialogFragmentCompat(
                                errorMessage = R.string.preferencesPortValidationError,
                                maxLength = 5
                        ) { port ->
                            try {
                                port.toInt() in 1..65535
                            } catch (e: NumberFormatException) {
                                true
                            }
                        }, preference.key
                )
            }
            getString(R.string.preferenceKeyTrackerId) -> {
                displayPreferenceDialog(
                        ValidatingEditTextPreferenceDialogFragmentCompat(
                                errorMessage = R.string.valEmpty,
                                maxLength = 2
                        ), preference.key
                )
            }
            getString(R.string.preferenceKeyKeepalive) -> {
                val minimumKeepalive = if (preferences.isExperimentalFeatureEnabled(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE)) 1 else preferences.minimumKeepalive
                displayPreferenceDialog(
                        ValidatingEditTextPreferenceDialogFragmentCompat(
                                errorMessage = R.string.preferencesKeepaliveValidationError,
                                errorArgs = minimumKeepalive
                        )
                        { keepalive ->
                            try {
                                keepalive.toInt() >= minimumKeepalive
                            } catch (e: NumberFormatException) {
                                false
                            }
                        },
                        preference.key
                )
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    /* Certificate popup and parsing bits */
    private fun uriToFilename(uri: Uri): String {
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
        return ""
    }

    private val copyFileToPrivateStorageActivityResult =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    uri?.run {
                        Timber.v("CopyTask with URI: %s", this)
                        val filename: String = uriToFilename(this)
                        Timber.v("filename for save is: %s", filename)
                        requireContext().applicationContext.contentResolver.openInputStream(this)
                                .use { inputStream ->
                                    requireContext().applicationContext.openFileOutput(
                                            filename,
                                            Context.MODE_PRIVATE
                                    )
                                            .use { outputStream ->
                                                val buffer = ByteArray(256)
                                                var bytesRead: Int
                                                while (inputStream!!.read(buffer)
                                                                .also { bytesRead = it } != -1
                                                ) {
                                                    outputStream.write(buffer, 0, bytesRead)
                                                }
                                            }
                                }
                        Timber.v("copied file to private storage: %s", filename)
                        lifecycleScope.launch(Dispatchers.Main) {
                            preferences.setPreference(filePickerPreferenceKey, filename)
                            findPreference<Preference>(getString(filePickerPreferenceKey))?.run {
                                summaryProvider = summaryProvider
                            }
                        }
                    }
                }
            }

    private fun certificateFieldPopupMenu(
            view: View,
            @StringRes preferenceKey: Int,
            activityResultLauncher: ActivityResultLauncher<String>
    ) = PopupMenu(requireContext(), view).apply {
        menuInflater.inflate(R.menu.picker, menu)
        setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val filename = preferences.getPreference(preferenceKey) as String
                        try {
                            Timber.v("Deleting certificate: $filename")
                            val result = requireContext().applicationContext.deleteFile(filename)
                            Timber.v("Certificate deletion success: $result")
                        } catch (e: Exception) {
                            Timber.w(e, "Unable to remove certificate file $filename")
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            preferences.setPreference(preferenceKey, "")
                            findPreference<Preference>(getString(preferenceKey))?.run {
                                summaryProvider = summaryProvider
                            }
                        }
                    }
                    true
                }
                R.id.select -> {
                    filePickerPreferenceKey = preferenceKey
                    activityResultLauncher.launch("*/*")
                    true
                }
                else -> true
            }
        }
    }
}
