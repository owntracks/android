package org.owntracks.android.ui.preferences.load

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesLoadBinding
import timber.log.Timber

@SuppressLint("GoogleAppIndexingApiWarning")
@AndroidEntryPoint
class LoadActivity : AppCompatActivity() {
  private val viewModel: LoadViewModel by viewModels()
  private lateinit var binding: UiPreferencesLoadBinding
  private var importJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    binding =
        DataBindingUtil.setContentView<UiPreferencesLoadBinding>(this, R.layout.ui_preferences_load)
            .apply {
              vm = viewModel
              lifecycleOwner = this@LoadActivity
              setSupportActionBar(appbar.toolbar)

              // Handle window insets for edge-to-edge
              ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                appbar.root.updatePadding(top = insets.top)
                WindowInsetsCompat.CONSUMED
              }
            }
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.displayedConfiguration.collect { invalidateOptionsMenu() } }
        launch {
          viewModel.configurationImportStatus.collect {
            invalidateOptionsMenu()
            Timber.d("ImportStatus is $it")
            if (it == ImportStatus.SAVED) {
              finish()
            }
          }
        }
      }
    }
    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setHasBack(false)
    handleIntent(intent)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId
    if (itemId == R.id.save) {
      viewModel.saveConfiguration()
      return true
    } else if (itemId == R.id.close || itemId == android.R.id.home) {
      finish()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun setHasBack(hasBackArrow: Boolean) {
    supportActionBar?.run { setDisplayHomeAsUpEnabled(hasBackArrow) }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_load, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.close).isVisible =
        viewModel.configurationImportStatus.value !== ImportStatus.LOADING
    menu.findItem(R.id.save).isVisible =
        viewModel.configurationImportStatus.value === ImportStatus.SUCCESS
    return true
  }

  private fun handleIntent(intent: Intent?) {
    if (intent == null) {
      Timber.e("no intent provided")
      return
    }

    setHasBack(intent.getBundleExtra("_args")?.getBoolean(FLAG_IN_APP, false) ?: false)

    val action = intent.action
    if (Intent.ACTION_VIEW == action) {
      val uri = intent.data
      if (uri != null) {
        Timber.v("uri: %s", uri)
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
          importJob?.cancel()
          importJob =
              lifecycleScope.launch {
                try {
                  val content = withContext(Dispatchers.IO) { getContentFromURI(uri) }
                  viewModel.extractPreferences(content)
                } catch (e: IOException) {
                  viewModel.configurationImportFailed(e)
                } catch (e: SecurityException) {
                  viewModel.configurationImportFailed(e)
                }
              }
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
        Snackbar.make(binding.root, R.string.loadActivityNoFileExplorerFound, Snackbar.LENGTH_SHORT)
            .show()
      }
    }
  }

  private val filePickerResultLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
          val uri = it.data?.data
          importJob?.cancel()
          importJob =
              lifecycleScope.launch {
                val content =
                    if (uri != null) {
                      try {
                        withContext(Dispatchers.IO) { getContentFromURI(uri) }
                      } catch (e: IOException) {
                        Timber.e(e, "Could not extract content from $uri")
                        ByteArray(0)
                      } catch (e: SecurityException) {
                        Timber.e(e, "Could not extract content from $uri")
                        ByteArray(0)
                      }
                    } else {
                      ByteArray(0)
                    }
                viewModel.extractPreferences(content)
              }
        } else {
          finish()
        }
      }

  @Throws(IOException::class)
  private fun getContentFromURI(uri: Uri): ByteArray =
      contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: throw IOException("Could not open input stream for $uri")

  companion object {
    const val FLAG_IN_APP = "INAPP"
  }
}
