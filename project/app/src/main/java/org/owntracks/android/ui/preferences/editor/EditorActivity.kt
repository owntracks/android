package org.owntracks.android.ui.preferences.editor

import android.content.DialogInterface
import android.content.Intent
import android.content.Intent.ACTION_CREATE_DOCUMENT
import android.content.Intent.EXTRA_TITLE
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesEditorBinding
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.ui.preferences.load.LoadActivity
import timber.log.Timber

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {
  private val viewModel: EditorViewModel by viewModels()

  @Inject @CoroutineScopes.MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher

  @Inject @CoroutineScopes.IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    UiPreferencesEditorBinding.inflate(layoutInflater).apply {
      setContentView(root)
      setSupportActionBar(appbar.toolbar)
      viewModel.effectiveConfiguration.observe(this@EditorActivity) {
        effectiveConfiguration.text = it
      }
    }
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, viewModel.preferenceKeys))
    builder
        .setTitle(R.string.preferencesEditor)
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

  private val saveIntentActivityLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult
        ->
        when (activityResult.resultCode) {
          RESULT_OK -> {
            val exportedConfig = viewModel.effectiveConfiguration.value
            if (exportedConfig != null) {
              activityResult.data?.data?.apply {
                lifecycleScope.launch(ioDispatcher) {
                  contentResolver.openOutputStream(this@apply)?.use {
                    it.write(exportedConfig.toByteArray())
                  }
                  withContext(mainDispatcher) {
                    Snackbar.make(
                            findViewById(R.id.effectiveConfiguration),
                            R.string.preferencesExportSuccess,
                            Snackbar.LENGTH_SHORT)
                        .show()
                  }
                }
              }
                  ?: run {
                    Timber.e("Could not export config, save location was null")
                    Snackbar.make(
                            findViewById(R.id.effectiveConfiguration),
                            R.string.preferencesExportError,
                            Snackbar.LENGTH_SHORT)
                        .show()
                  }
            } else {
              Timber.e("Could not export config, config was null")
              Snackbar.make(
                      findViewById(R.id.effectiveConfiguration),
                      R.string.preferencesExportError,
                      Snackbar.LENGTH_SHORT)
                  .show()
            }
          }
          RESULT_CANCELED -> {
            Timber.e("Could not export config, export was cancelled")
            Snackbar.make(
                    findViewById(R.id.effectiveConfiguration),
                    R.string.preferencesExportError,
                    Snackbar.LENGTH_SHORT)
                .show()
          }
        }
      }

  private fun exportConfigurationToFile() {
    val shareIntent =
        Intent(ACTION_CREATE_DOCUMENT).apply {
          type = "*/*"
          putExtra(EXTRA_TITLE, "config.otrc")
        }
    saveIntentActivityLauncher.launch(shareIntent)
  }

  private fun displayLoadFailed() {
    Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesLoadFailed,
            Snackbar.LENGTH_SHORT)
        .show()
  }

  private fun displayPreferencesValueForKeySetFailedKey() {
    Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorKeyError,
            Snackbar.LENGTH_SHORT)
        .show()
  }

  private fun displayPreferencesValueForKeySetFailedValue() {
    Snackbar.make(
            findViewById(R.id.effectiveConfiguration),
            R.string.preferencesEditorValueError,
            Snackbar.LENGTH_SHORT)
        .show()
  }
}
