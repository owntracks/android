package org.owntracks.android.testutils

import androidx.test.espresso.IdlingResource
import okhttp3.mockwebserver.Dispatcher
import org.junit.After

interface TestWithAnHTTPServer {
  /**
   * Configures the app on the device to use the local HTTP server, setting the mode and URL
   * correctly
   */
  fun configureHTTPConnectionToLocal(idlingResource: IdlingResource)

  /** Starts the HTTP server with the given dispatcher */
  fun startServer(dispatcher: Dispatcher)

  /**
   * Starts the HTTP server with a simple dispatcher that outputs a specific response on a specific
   * URL. The responses map parameter specifies a map of paths and responses to be generated (with a
   * 200 status code) when GET requests are made to that path. All other requests will receive a 404
   * response
   */
  fun startServer(responses: Map<String, String>)

  /** Stops the webserver. Probably should be called with a test @After function */
  fun stopServer()

  /** The port on which the webserver is listening */
  val webserverPort: Int

  @After
  fun stopMockWebserver() {
    stopServer()
  }
}
