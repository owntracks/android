package org.owntracks.android.ui.preferences.load

import android.content.ContentResolver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.codec.binary.Base64
import org.apache.hc.core5.net.URIBuilder
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Parser.EncryptionException
import org.owntracks.android.support.SimpleIdlingResource
import timber.log.Timber

@HiltViewModel
class LoadViewModel
@Inject
constructor(
    private val preferences: Preferences,
    private val parser: Parser,
    private val waypointsRepo: WaypointsRepo,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
  val saveConfigurationIdlingResource = SimpleIdlingResource("importStatus", true)
  private var configuration: MessageConfiguration? = null

  private val mutableConfig = MutableLiveData("")
  val displayedConfiguration: LiveData<String> = mutableConfig

  private val mutableImportStatus = MutableLiveData(ImportStatus.LOADING)
  val configurationImportStatus: LiveData<ImportStatus> = mutableImportStatus

  private val mutableImportError = MutableLiveData<String>()
  val importError: LiveData<String> = mutableImportError

  private fun setConfiguration(json: String) {
    when (val message = parser.fromJson(json.toByteArray())) {
      is MessageConfiguration -> {
        configuration = message
        try {
          mutableConfig.postValue(parser.toUnencryptedJsonPretty(message))
          mutableImportStatus.postValue(ImportStatus.SUCCESS)
        } catch (e: IOException) {
          configurationImportFailed(e)
        }
      }
      is MessageWaypoints -> {
        configuration = MessageConfiguration().apply { message.waypoints?.run { waypoints = this } }
        try {
          mutableConfig.postValue(parser.toUnencryptedJsonPretty(message))
          mutableImportStatus.postValue(ImportStatus.SUCCESS)
        } catch (e: IOException) {
          configurationImportFailed(e)
        }
      }
      else -> {
        throw IOException("Message is not a valid configuration message")
      }
    }
  }

  fun saveConfiguration() {
    viewModelScope.launch(ioDispatcher) {
      saveConfigurationIdlingResource.setIdleState(false)
      mutableImportStatus.postValue(ImportStatus.LOADING)
      Timber.d("Saving configuration $configuration")
      configuration?.run {
        preferences.importConfiguration(this)
        if (waypoints.isNotEmpty()) {
          waypointsRepo.importFromMessage(waypoints)
        }
      }
      Timber.d("Setting ImportStatus to saved")
      mutableImportStatus.postValue(ImportStatus.SAVED)
      saveConfigurationIdlingResource.setIdleState(true)
    }
  }

  fun extractPreferences(content: ByteArray) {
    try {
      setConfiguration(String(content, StandardCharsets.UTF_8))
    } catch (e: IOException) {
      configurationImportFailed(e)
    } catch (e: EncryptionException) {
      configurationImportFailed(e)
    }
  }

  /**
   * Extract preferences from uri. We've not parsed the uri for validity yet, hence accepting a
   * string
   *
   * @param uriString a string containing maybe a URI
   */
  fun extractPreferencesFromUri(uriString: String) {
    val uri =
        try {
          URI(uriString)
        } catch (e: URISyntaxException) {
          configurationImportFailed(e)
          return
        }
    try {
      if (ContentResolver.SCHEME_FILE == uri.scheme) {
        // Note: left here to avoid breaking compatibility.  May be removed
        // with sufficient testing. Will not work on Android >5 without granting
        // READ_EXTERNAL_STORAGE permission
        Timber.v("using file:// uri")
        val r = BufferedReader(InputStreamReader(FileInputStream(uri.path)))
        val total = StringBuilder()
        var content: String?
        while (r.readLine().also { content = it } != null) {
          total.append(content)
        }
        setConfiguration(total.toString())
      } else if ("owntracks" == uri.scheme && "/config" == uri.path) {
        Timber.v("Importing config using owntracks: scheme")

        val queryParams = URIBuilder(uri, Charsets.UTF_8).queryParams
        val urlQueryParam: MutableList<String> = ArrayList()
        val configQueryParam: MutableList<String> = ArrayList()
        for (queryParam in queryParams) {
          if (queryParam.name == "url") {
            urlQueryParam.add(queryParam.value)
          }
          if (queryParam.name == "inline") {
            configQueryParam.add(queryParam.value)
          }
        }
        when {
          configQueryParam.size == 1 -> {
            val config: ByteArray = Base64.decodeBase64(configQueryParam[0].toByteArray())
            setConfiguration(String(config, StandardCharsets.UTF_8))
          }
          urlQueryParam.size == 1 -> {
            val remoteConfigUrl = URL(urlQueryParam[0])
            val client = OkHttpClient()
            val request: Request = Request.Builder().url(remoteConfigUrl).build()
            client
                .newCall(request)
                .enqueue(
                    object : Callback {
                      override fun onFailure(call: Call, e: IOException) {
                        configurationImportFailed(
                            Exception("Failure fetching config from remote URL", e))
                      }

                      @Throws(IOException::class)
                      override fun onResponse(call: Call, response: Response) {
                        try {
                          response.body.use { responseBody ->
                            if (!response.isSuccessful) {
                              configurationImportFailed(
                                  IOException(
                                      String.format("Unexpected status code: %s", response)))
                              return
                            }
                            setConfiguration(responseBody?.string() ?: "")
                          }
                        } catch (e: EncryptionException) {
                          configurationImportFailed(e)
                        }
                      }
                    })
            // This is async, so result handled on the callback
          }
          else -> {
            throw IOException("Invalid config URL")
          }
        }
      } else {
        throw IOException("Invalid config URL")
      }
    } catch (e: OutOfMemoryError) {
      configurationImportFailed(e)
    } catch (e: IOException) {
      configurationImportFailed(e)
    } catch (e: EncryptionException) {
      configurationImportFailed(e)
    } catch (e: IllegalArgumentException) {
      configurationImportFailed(e)
    }
  }

  fun configurationImportFailed(e: Throwable) {
    Timber.e(e)
    mutableImportError.postValue(e.message)
    mutableImportStatus.postValue(ImportStatus.FAILED)
  }
}
