package org.owntracks.android.ui.preferences.editor

import android.content.Intent
import android.content.Intent.ACTION_CREATE_DOCUMENT
import android.content.Intent.EXTRA_TITLE
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.livedata.observeAsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.R
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.theme.OwnTracksTheme
import timber.log.Timber

@AndroidEntryPoint
class EditorActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModels()

    @Inject @CoroutineScopes.MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher

    @Inject @CoroutineScopes.IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private var snackbarHostState: SnackbarHostState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme {
                val effectiveConfiguration by viewModel.effectiveConfiguration.observeAsState(initial = "")
                val snackbarState = remember { SnackbarHostState() }
                snackbarHostState = snackbarState
                val scope = rememberCoroutineScope()

                EditorScreen(
                    effectiveConfiguration = effectiveConfiguration,
                    preferenceKeys = viewModel.preferenceKeys,
                    onBackClick = { finish() },
                    onExportClick = { exportConfigurationToFile() },
                    onImportFileClick = { showImportConfigurationFilePickerView() },
                    onSetPreferenceValue = { key, value ->
                        try {
                            viewModel.setNewPreferenceValue(key, value)
                            Result.success(Unit)
                        } catch (e: NoSuchElementException) {
                            Timber.w(e)
                            scope.launch {
                                snackbarState.showSnackbar(getString(R.string.preferencesEditorKeyError))
                            }
                            Result.failure(e)
                        } catch (e: IllegalArgumentException) {
                            Timber.w(e)
                            scope.launch {
                                snackbarState.showSnackbar(getString(R.string.preferencesEditorValueError))
                            }
                            Result.failure(e)
                        }
                    },
                    snackbarHostState = snackbarState
                )
            }
        }

        viewModel.configLoadError.observe(this) {
            if (it != null) {
                displayLoadFailed()
            }
        }
    }

    private fun showImportConfigurationFilePickerView() {
        val b = Bundle()
        b.putBoolean(LoadActivity.FLAG_IN_APP, true)
        startActivity(Intent(this, LoadActivity::class.java), b)
    }

    private val saveIntentActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            when (activityResult.resultCode) {
                RESULT_OK -> {
                    val exportedConfig = viewModel.effectiveConfiguration.value
                    if (exportedConfig != null) {
                        activityResult.data?.data?.apply {
                            kotlinx.coroutines.MainScope().launch(ioDispatcher) {
                                contentResolver.openOutputStream(this@apply)?.use {
                                    it.write(exportedConfig.toByteArray())
                                }
                                withContext(mainDispatcher) {
                                    showSnackbar(getString(R.string.preferencesExportSuccess))
                                }
                            }
                        } ?: run {
                            Timber.e("Could not export config, save location was null")
                            showSnackbar(getString(R.string.preferencesExportError))
                        }
                    } else {
                        Timber.e("Could not export config, config was null")
                        showSnackbar(getString(R.string.preferencesExportError))
                    }
                }
                RESULT_CANCELED -> {
                    Timber.e("Could not export config, export was cancelled")
                    showSnackbar(getString(R.string.preferencesExportError))
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
        showSnackbar(getString(R.string.preferencesLoadFailed))
    }

    private fun showSnackbar(message: String) {
        kotlinx.coroutines.MainScope().launch {
            snackbarHostState?.showSnackbar(message)
        }
    }
}
