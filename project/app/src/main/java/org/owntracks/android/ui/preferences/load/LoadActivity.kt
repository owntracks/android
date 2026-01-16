package org.owntracks.android.ui.preferences.load

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import javax.inject.Inject
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.R
import org.owntracks.android.ui.theme.OwnTracksTheme
import timber.log.Timber

@SuppressLint("GoogleAppIndexingApiWarning")
@AndroidEntryPoint
class LoadActivity : AppCompatActivity() {
    @Inject
    lateinit var preferences: Preferences

    private val viewModel: LoadViewModel by viewModels()
    private var hasBackArrow by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme(dynamicColor = preferences.dynamicColorsEnabled) {
                val importStatus by viewModel.configurationImportStatus.observeAsState(initial = ImportStatus.LOADING)
                val displayedConfiguration by viewModel.displayedConfiguration.observeAsState(initial = "")
                val importError by viewModel.importError.observeAsState(initial = null)

                LoadScreen(
                    importStatus = importStatus,
                    displayedConfiguration = displayedConfiguration,
                    importError = importError,
                    hasBackArrow = hasBackArrow,
                    onBackClick = { finish() },
                    onCloseClick = { finish() },
                    onSaveClick = { viewModel.saveConfiguration() }
                )
            }
        }

        viewModel.configurationImportStatus.observe(this) {
            Timber.d("ImportStatus is $it")
            if (it == ImportStatus.SAVED) {
                finish()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        hasBackArrow = false
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            Timber.e("no intent provided")
            return
        }

        hasBackArrow = intent.getBundleExtra("_args")?.getBoolean(FLAG_IN_APP, false) ?: false

        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            val uri = intent.data
            if (uri != null) {
                Timber.v("uri: %s", uri)
                if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
                    viewModel.extractPreferences(getContentFromURI(uri))
                } else {
                    viewModel.extractPreferencesFromUri(uri.toString())
                }
            } else {
                viewModel.configurationImportFailed(
                    Exception(getString(R.string.preferencesImportNoURIGiven)))
            }
        } else {
            val pickerIntent = Intent(Intent.ACTION_GET_CONTENT)
            pickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
            pickerIntent.type = "*/*"
            try {
                filePickerResultLauncher.launch(
                    Intent.createChooser(pickerIntent, getString(R.string.loadActivitySelectAFile)))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(this, R.string.loadActivityNoFileExplorerFound, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val filePickerResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                var content = ByteArray(0)
                try {
                    content = it.data?.data?.run(this::getContentFromURI) ?: ByteArray(0)
                } catch (e: IOException) {
                    Timber.e(e, "Could not extract content from ${it.data}")
                }
                viewModel.extractPreferences(content)
            } else {
                finish()
            }
        }

    @Throws(IOException::class)
    private fun getContentFromURI(uri: Uri): ByteArray {
        contentResolver.openInputStream(uri).use { stream ->
            val output = ByteArray(stream!!.available())
            val bytesRead = stream.read(output)
            Timber.d("Read %d bytes from content URI", bytesRead)
            return output
        }
    }

    companion object {
        const val FLAG_IN_APP = "INAPP"
    }
}
