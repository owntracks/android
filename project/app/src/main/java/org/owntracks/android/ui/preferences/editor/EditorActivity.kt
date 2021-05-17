package org.owntracks.android.ui.preferences.editor

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import com.google.android.material.snackbar.Snackbar
import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView
import com.rengwuxian.materialedittext.MaterialEditText
import org.greenrobot.eventbus.EventBus
import org.owntracks.android.BuildConfig
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesEditorBinding
import org.owntracks.android.support.Events.RestartApp
import org.owntracks.android.ui.base.BaseActivity
import org.owntracks.android.ui.base.navigator.Navigator
import org.owntracks.android.ui.preferences.load.LoadActivity
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class EditorActivity :
    BaseActivity<UiPreferencesEditorBinding?, EditorMvvm.ViewModel<EditorMvvm.View?>?>(),
    EditorMvvm.View {
    private var configExportUri: Uri? = null

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disablesAnimation()
        bindAndAttachContentView(R.layout.ui_preferences_editor, savedInstanceState)
        setHasEventBus(false)
        setSupportToolbar(binding!!.toolbar, true, true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_configuration, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exportConfigurationFile -> {
                exportConfigurationToFile()
                return true
            }
            R.id.importConfigurationFile -> {
                showImportConfigurationFilePickerView()
                return true
            }
            R.id.importConfigurationSingleValue -> {
                showEditorView()
                return true
            }
            R.id.restart -> {
                eventBus.post(RestartApp())
                return false
            }
            else -> return false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showImportConfigurationFilePickerView() {
        val b = Bundle()
        b.putBoolean(LoadActivity.FLAG_IN_APP, true)
        navigator.startActivity(LoadActivity::class.java, b)
    }

    private fun showEditorView() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.ui_preferences_editor_dialog, null)

        // Set autocomplete items
        val inputKeyView: MaterialAutoCompleteTextView = layout.findViewById(R.id.inputKey)
        inputKeyView.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                preferences.importKeys
            )
        )
        builder.setTitle(R.string.preferencesEditor)
            .setPositiveButton(R.string.accept) { dialog: DialogInterface, _: Int ->
                val inputValue: MaterialEditText = layout.findViewById(R.id.inputValue)
                val key = inputKeyView.text.toString()
                val value = inputValue.text.toString()
                try {
                    preferences.importKeyValue(key, value)
                    viewModel!!.onPreferencesValueForKeySetSuccessful()
                    dialog.dismiss()
                } catch (e: IllegalAccessException) {
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
            Snackbar.make(
                findViewById(R.id.effectiveConfiguration),
                R.string.preferencesExportSuccess,
                Snackbar.LENGTH_SHORT
            ).show()
            revokeExportUriPermissions()
        }

    private fun revokeExportUriPermissions() {
        configExportUri?.let {
            revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            configExportUri = null
        }
    }

    private fun getRandomHexString(): String {
        return Random().nextInt(0X1000000).toString(16)
    }

    override fun exportConfigurationToFile(): Boolean {
        revokeExportUriPermissions()
        val key = getRandomHexString()
        configExportUri = Uri.parse("content://${BuildConfig.APPLICATION_ID}.config/$key")
        val shareIntent = ShareCompat.IntentBuilder.from(this)
            .setType("text/plain")
            .setSubject("Owntracks Configuration File")
            .setChooserTitle(R.string.exportConfiguration)
            .setStream(configExportUri)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission("android", configExportUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntentActivityLauncher.launch(shareIntent)
        return true
    }

    override fun displayLoadFailed() {
        Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesLoadFailed,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun displayPreferencesValueForKeySetFailedKey() {
        Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorKeyError,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun displayPreferencesValueForKeySetFailedValue() {
        Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorValueError,
            Snackbar.LENGTH_SHORT
        ).show()
    }
}