package org.owntracks.android.ui.preferences.load

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.owntracks.android.R
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.di.CoroutineScopes
import org.owntracks.android.model.Parser
import org.owntracks.android.model.Parser.EncryptionException
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.model.messages.MessageWaypoints
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.test.SimpleIdlingResource
import timber.log.Timber

@HiltViewModel
class LoadViewModel
@Inject
constructor(
    private val preferences: Preferences,
    private val parser: Parser,
    private val waypointsRepo: WaypointsRepo,
    @CoroutineScopes.IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @Named("saveConfigurationIdlingResource")
    private val saveConfigurationIdlingResource: SimpleIdlingResource,
    @ApplicationContext private val context: Context
) : ViewModel() {
  private var configuration: MessageConfiguration? = null

  private val mutableConfigItems = MutableStateFlow<List<ConfigItem>>(emptyList())
  val configItems: StateFlow<List<ConfigItem>> = mutableConfigItems

  private val mutableImportStatus = MutableStateFlow(ImportStatus.LOADING)
  val configurationImportStatus: StateFlow<ImportStatus> = mutableImportStatus

  private val mutableImportError = MutableStateFlow<String?>(null)
  val importError: StateFlow<String?> = mutableImportError

  private val mutableDisplayedConfiguration = MutableStateFlow("")
  val displayedConfiguration: StateFlow<String> = mutableDisplayedConfiguration

  private fun setConfiguration(json: String) {
    when (val message = parser.fromJson(json.toByteArray())) {
      is MessageConfiguration -> {
        configuration = message
        mutableDisplayedConfiguration.value = parser.toJsonPlain(message)
        mutableConfigItems.value = buildConfigItems(message)
        mutableImportStatus.value = ImportStatus.SUCCESS
      }
      is MessageWaypoints -> {
        configuration = MessageConfiguration().apply { message.waypoints?.run { waypoints = this } }
        mutableDisplayedConfiguration.value = parser.toJsonPlain(message)
        mutableConfigItems.value = buildConfigItems(configuration!!)
        mutableImportStatus.value = ImportStatus.SUCCESS
      }
      else -> {
        throw IOException(context.getString(R.string.loadActivityErrorInvalidConfigMessage))
      }
    }
  }

  private fun buildConfigItems(message: MessageConfiguration): List<ConfigItem> {
    val items = mutableListOf<ConfigItem>()
    val keys = message.keys
    if (keys.isNotEmpty()) {
      val changedItems = mutableListOf<ConfigItem.KeyValue>()
      var unchangedCount = 0
      keys.forEach { key ->
        val incoming = message[key]
        val pair = preferences.resolvedPreferencePair(key, incoming)
        if (pair == null) {
          // Key not in importable preferences — show raw value as a change
          changedItems +=
              ConfigItem.KeyValue(
                  key,
                  incoming?.toString() ?: "",
                  oldValue = null,
                  labelRes = PREFERENCE_KEY_LABELS[key])
        } else {
          val (currentStr, newStr) = pair
          if (currentStr == newStr) {
            unchangedCount++
          } else {
            changedItems +=
                ConfigItem.KeyValue(
                    key, newStr ?: "", oldValue = currentStr, labelRes = PREFERENCE_KEY_LABELS[key])
          }
        }
      }
      if (changedItems.isNotEmpty() || unchangedCount > 0) {
        items += ConfigItem.Header(SECTION_SETTINGS)
        items += changedItems
        if (unchangedCount > 0) {
          items += ConfigItem.Summary(unchangedCount)
        }
      }
    }
    val waypoints = message.waypoints
    if (waypoints.isNotEmpty()) {
      items += ConfigItem.Header(SECTION_WAYPOINTS)
      waypoints.forEach { waypoint ->
        items +=
            ConfigItem.Waypoint(
                description = waypoint.description ?: "",
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                radius = waypoint.radius)
      }
    }
    return items
  }

  fun saveConfiguration() {
    viewModelScope.launch(ioDispatcher) {
      saveConfigurationIdlingResource.setIdleState(false)
      mutableImportStatus.value = ImportStatus.LOADING
      Timber.d("Saving configuration $configuration")
      configuration?.run {
        preferences.importConfiguration(this)
        if (waypoints.isNotEmpty()) {
          waypointsRepo.importFromMessage(waypoints)
        }
      }
      Timber.d("Setting ImportStatus to saved")
      mutableImportStatus.value = ImportStatus.SAVED
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
  @OptIn(ExperimentalEncodingApi::class)
  fun extractPreferencesFromUri(uriString: String) {
    val uri =
        try {
          URI(uriString)
        } catch (e: URISyntaxException) {
          configurationImportFailed(e)
          return
        }
    try {
      if (!preferences.allowConfigurationByURIAndConfigFile) {
        throw IOException(context.getString(R.string.loadActivityErrorExternalConfigDisabled))
      }
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

        val queryParams =
            uri.rawQuery
                ?.split("&")
                ?.mapNotNull { param ->
                  val parts = param.split("=", limit = 2)
                  if (parts.size == 2)
                      java.net.URLDecoder.decode(parts[0], "UTF-8") to
                          java.net.URLDecoder.decode(parts[1], "UTF-8")
                  else null
                }
                ?.groupBy({ it.first }, { it.second }) ?: emptyMap()
        val urlQueryParam = queryParams["url"] ?: emptyList()
        val configQueryParam = queryParams["inline"] ?: emptyList()
        when {
          configQueryParam.size == 1 -> {
            val config: ByteArray = Base64.decode(configQueryParam[0].toByteArray())
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
                            Exception(context.getString(R.string.loadActivityErrorFetchFailed), e))
                      }

                      @Throws(IOException::class)
                      override fun onResponse(call: Call, response: Response) {
                        try {
                          response.body.use { responseBody ->
                            if (!response.isSuccessful) {
                              configurationImportFailed(
                                  IOException(
                                      context.getString(
                                          R.string.loadActivityErrorUnexpectedStatusCode,
                                          response)))
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
            throw IOException(context.getString(R.string.loadActivityErrorInvalidConfigUrl))
          }
        }
      } else {
        throw IOException(context.getString(R.string.loadActivityErrorInvalidConfigUrl))
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
    mutableImportError.value = e.message
    mutableImportStatus.value = ImportStatus.FAILED
  }

  companion object {
    val SECTION_SETTINGS = R.string.loadActivitySectionSettings
    val SECTION_WAYPOINTS = R.string.loadActivitySectionWaypoints
  }
}
