package org.owntracks.android.ui.preferences.editor

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesEditorBinding
import org.owntracks.android.ui.preferences.load.LoadActivity
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModels()
    private var configExportUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<UiPreferencesEditorBinding>(this, R.layout.ui_preferences_editor)
            .apply {
                vm = viewModel
                lifecycleOwner = this@EditorActivity
                setSupportActionBar(appbar.toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        viewModel.configLoadError.observe(this) {
            if (it != null) {
                displayLoadFailed()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_configuration, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.exportConfigurationFile -> {
                exportConfigurationToFile()
                true
            }
            R.id.importConfigurationFile -> {
                showImportConfigurationFilePickerView()
                true
            }
            R.id.importConfigurationSingleValue -> {
                showEditorView()
                true
            }
            else -> false
        }
    }

    private fun showImportConfigurationFilePickerView() {
        val b = Bundle()
        b.putBoolean(LoadActivity.FLAG_IN_APP, true)
        startActivity(Intent(this, LoadActivity::class.java), b)
    }

    private fun showEditorView() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.ui_preferences_editor_dialog, null)

        // Set autocomplete items
        val inputKeyView = layout.findViewById<MaterialAutoCompleteTextView>(R.id.inputKey)
        inputKeyView.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                viewModel.preferenceKeys
            )
        )
        builder.setTitle(R.string.preferencesEditor)
            .setPositiveButton(R.string.accept) { dialog: DialogInterface, _: Int ->
                val inputValue = layout.findViewById<TextInputEditText>(R.id.inputValue)
                val key = inputKeyView.text.toString()
                val value = inputValue.text.toString()
                try {
                    viewModel.setNewPreferenceValue(key, value)
                    dialog.dismiss()
                } catch (e: NoSuchElementException) {
                    Timber.w(e)
                    displayPreferencesValueForKeySetFailedKey()
                } catch (e: IllegalArgumentException) {
                    Timber.w(e)
                    displayPreferencesValueForKeySetFailedValue()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setView(layout)
        builder.show()
    }

    private val shareIntentActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                RESULT_OK -> {
                    Snackbar.make(
                        findViewById(R.id.effectiveConfiguration),
                        R.string.preferencesExportSuccess,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
                RESULT_CANCELED -> {}
            }
            revokeExportUriPermissions()
        }

    private fun revokeExportUriPermissions() {
        configExportUri?.let {
            revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            configExportUri = null
        }
    }

    private fun getRandomHexString(): String {
        return Random().nextInt(0X1000000)
            .toString(16)
    }

    private fun exportConfigurationToFile(): Boolean {
        revokeExportUriPermissions()
        val key = getRandomHexString()
        configExportUri = Uri.parse("content://${BuildConfig.APPLICATION_ID}.config/$key")
        val shareIntent = ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setSubject(getString(R.string.exportConfigurationSubject))
            .setChooserTitle(R.string.exportConfiguration)
            .setStream(configExportUri)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission("android", configExportUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntentActivityLauncher.launch(shareIntent)
        return true
    }

    private fun displayLoadFailed() {
        Snackbar.make(findViewById(R.id.effectiveConfiguration), R.string.preferencesLoadFailed, Snackbar.LENGTH_SHORT)
            .show()
    }

    private fun displayPreferencesValueForKeySetFailedKey() {
        Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorKeyError,
            Snackbar.LENGTH_SHORT
        )
            .show()
    }

    private fun displayPreferencesValueForKeySetFailedValue() {
        Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorValueError,
            Snackbar.LENGTH_SHORT
        )
            .show()
    }
}
