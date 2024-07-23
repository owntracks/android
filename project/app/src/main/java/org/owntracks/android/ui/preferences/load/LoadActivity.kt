package org.owntracks.android.ui.preferences.load


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.CircularProgressIndicator

import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.test.espresso.IdlingResource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesLoadBinding
import org.owntracks.android.ui.compose.AppTheme

import timber.log.Timber
import java.io.IOException

@SuppressLint("GoogleAppIndexingApiWarning")
@AndroidEntryPoint
class LoadActivity : AppCompatActivity() {
  private val viewModel: LoadViewModel by viewModels()
  private lateinit var binding: UiPreferencesLoadBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
    super.onCreate(savedInstanceState)
    setContent {
      LoadView(viewModel)
    }
  }


  @Composable
  fun WrapperWithBar(content: @Composable (paddingValues: PaddingValues) -> Unit) {
    AppTheme {
      Scaffold(
          topBar = {
            TopAppBar(
                title = {
                  Text(text = "OOF")
                },
            )
          },
          content = content,
      )
    }
  }

  @Composable
  fun LoadView(vm: LoadViewModel) {
    val importStatus = vm.configurationImportStatus.collectAsState().value
    val displayedConfiguration = vm.displayedConfiguration.collectAsState().value
    val importError = vm.importError.collectAsState()
    WrapperWithBar { paddingValues ->
      Box(
          modifier = Modifier
                  .fillMaxSize()
                  .padding(paddingValues),
      ) {
        when (importStatus) {
          ImportStatus.LOADING -> {
            Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .visible(importStatus == ImportStatus.LOADING),
            ) {
              CircularProgressIndicator(
                  modifier = Modifier.align(Center),
              )
            }
          }

          ImportStatus.SUCCESS -> {
            Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .visible(importStatus == ImportStatus.SUCCESS)
                        .padding(16.dp),
            ) {
              Text(
                  text = displayedConfiguration,
                  modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          ImportStatus.FAILED -> {
            Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .visible(importStatus == ImportStatus.FAILED)
                        .padding(16.dp),
            ) {
              Text(
                  text = stringResource(
                      id = R.string.errorPreferencesImportFailed,
                      importError,
                  ),
                  modifier = Modifier.fillMaxWidth(),

                  )
            }
          }
          else -> {}
        }
      }
    }
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
          viewModel.extractPreferences(getContentFromURI(uri))
        } else {
          viewModel.extractPreferencesFromUri(uri.toString())
        }
      } else {
        viewModel.configurationImportFailed(
            Exception(getString(R.string.preferencesImportNoURIGiven)),
        )
      }
    } else {
      val pickerIntent = Intent(Intent.ACTION_GET_CONTENT)
      pickerIntent.addCategory(Intent.CATEGORY_OPENABLE)
      pickerIntent.type = "*/*"
      try {
        filePickerResultLauncher.launch(
            Intent.createChooser(pickerIntent, getString(R.string.loadActivitySelectAFile)),
        )
      } catch (ex: ActivityNotFoundException) {
        Snackbar.make(binding.root, R.string.loadActivityNoFileExplorerFound, Snackbar.LENGTH_SHORT)
            .show()
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

  @get:VisibleForTesting
  val saveConfigurationIdlingResource: IdlingResource
    get() = viewModel.saveConfigurationIdlingResource

  companion object {
    const val FLAG_IN_APP = "INAPP"
  }
}

@Composable
fun Modifier.visible(isVisible: Boolean): Modifier {
  return if (isVisible) {
    this
  } else {
    this.then(Modifier.size(0.dp))
  }
}
