package org.owntracks.android.testutils

import android.content.Intent
import androidx.core.net.toUri
import androidx.test.espresso.IdlingResource
import androidx.test.platform.app.InstrumentationRegistry
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.owntracks.android.R
import timber.log.Timber

@OptIn(ExperimentalEncodingApi::class)
class TestWithAnHTTPServerImpl : TestWithAnHTTPServer {
  private lateinit var mockWebServer: MockWebServer
  override val webserverPort
    get() = mockWebServer.port

  override fun startServer(dispatcher: Dispatcher) {
    mockWebServer = MockWebServer()
    mockWebServer.start()
    Timber.i("Started HTTP server on port ${mockWebServer.port}")
    mockWebServer.dispatcher = dispatcher
  }

  override fun startServer(responses: Map<String, String>) {
    startServer(MockJSONResponseDispatcher(responses))
  }

  override fun stopServer() {
    if (this::mockWebServer.isInitialized) {
      Timber.i("Stopping HTTP Server on port ${mockWebServer.port}")
      mockWebServer.shutdown()
    }
  }

  override fun configureHTTPConnectionToLocal(idlingResource: IdlingResource) {
    val config =
        Base64.encode(
            """
            {
                "_type": "configuration",
                "mode": 3,
                "connectionTimeoutSeconds": 2,
                "url": "http://localhost:${mockWebServer.port}"
            }
            """
                .trimIndent()
                .toByteArray())
    InstrumentationRegistry.getInstrumentation()
        .targetContext
        .startActivity(
            Intent(Intent.ACTION_VIEW).apply {
              data = "owntracks:///config?inline=$config".toUri()
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
    idlingResource.use { clickOn(R.id.save) }
  }

  class MockJSONResponseDispatcher(private val responses: Map<String, String>) : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      val errorResponse = MockResponse().setResponseCode(404)
      Timber.i("Webserver received request $request")
      return responses[request.path]?.let {
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-type", "application/json")
            .setBody(it)
      } ?: errorResponse
    }
  }
}
