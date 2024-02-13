package org.owntracks.android.services

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.owntracks.android.data.repos.EndpointStateRepo
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.net.http.HttpMessageProcessorEndpoint
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.support.EncryptionProvider
import org.owntracks.android.support.Parser
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException

class HttpMessageProcessorEndpointTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var testPreferences: Preferences

    @Mock
    private lateinit var messageProcessor: MessageProcessor

    @Mock
    private lateinit var encryptionProvider: EncryptionProvider

    @Mock
    private lateinit var application: Application

    private val endpointStateRepo = EndpointStateRepo()

    @Mock
    private lateinit var parser: Parser
    private lateinit var messageLocation: MessageLocation

    @Before
    fun setupPreferences() {
        testPreferences = mock {
            on { encryptionKey } doReturn "testEncryptionKey"
            on { tlsClientCrt } doReturn ""
            on { username } doReturn ""
            on { deviceId } doReturn ""
            on { password } doReturn ""
            on { url } doReturn "http://example.com/owntracks/test"
        }
        encryptionProvider = mock { on { isPayloadEncryptionEnabled } doReturn false }
        messageProcessor = mock {}
        messageLocation = MessageLocation()
        messageLocation.accuracy = 10
        messageLocation.altitude = 20
        messageLocation.battery = 30
        messageLocation.conn = "TestConn"
        messageLocation.latitude = 50.1
        messageLocation.longitude = 60.2
        messageLocation.timestamp = 123456789
        messageLocation.velocity = 5.6.toInt()
        messageLocation.verticalAccuracy = 1.7.toInt()
        parser = Parser(encryptionProvider)
        application = mock {}
    }

    @Test
    fun `Given a simple request, the auth headers are not set`() =
        runTest {
            val httpMessageProcessorEndpoint =
                HttpMessageProcessorEndpoint(
                    messageProcessor,
                    parser,
                    testPreferences,
                    application,
                    endpointStateRepo,
                    mock {},
                    this,
                    StandardTestDispatcher()
                )
            val configuration = httpMessageProcessorEndpoint.getEndpointConfiguration()
            configuration.validate()
            val request = httpMessageProcessorEndpoint.getRequest(configuration, messageLocation)
            assertNotNull(request)
            assertNull(request.header(HttpMessageProcessorEndpoint.HEADER_AUTHORIZATION))
            assertNull(request.header(HttpMessageProcessorEndpoint.HEADER_USERNAME))
            assertNull(request.header(HttpMessageProcessorEndpoint.HEADER_DEVICE))
            assertEquals("POST", request.method)
            assertEquals("http://example.com/owntracks/test", request.url.toString())
        }

    @Test
    fun `Given a username and deviceId, correct HTTP headers are set`() = runTest {
        `when`(testPreferences.username).thenReturn("username")
        `when`(testPreferences.deviceId).thenReturn("device")
        val httpMessageProcessorEndpoint =
            HttpMessageProcessorEndpoint(
                messageProcessor,
                parser,
                testPreferences,
                application,
                endpointStateRepo,
                mock {},
                this,
                StandardTestDispatcher()
            )
        val configuration = httpMessageProcessorEndpoint.getEndpointConfiguration()
        configuration.validate()
        val request = httpMessageProcessorEndpoint.getRequest(configuration, messageLocation)
        assertNotNull(request)
        assertEquals(request.header(HttpMessageProcessorEndpoint.HEADER_USERNAME), "username")
        assertEquals(request.header(HttpMessageProcessorEndpoint.HEADER_DEVICE), "device")
    }

    @Test
    fun `Given a username and password, the correct auth HTTP headers are set`() = runTest {
        `when`(testPreferences.username).thenReturn("username")
        `when`(testPreferences.password).thenReturn("password")
        val httpMessageProcessorEndpoint =
            HttpMessageProcessorEndpoint(
                messageProcessor,
                parser,
                testPreferences,
                application,
                endpointStateRepo,
                mock {},
                this,
                StandardTestDispatcher()
            )
        val configuration = httpMessageProcessorEndpoint.getEndpointConfiguration()
        configuration.validate()
        val request = httpMessageProcessorEndpoint.getRequest(configuration, messageLocation)
        assertNotNull(request)
        assertEquals(
            "Basic dXNlcm5hbWU6cGFzc3dvcmQ=",
            request.header(HttpMessageProcessorEndpoint.HEADER_AUTHORIZATION)
        )
        assertEquals("username", request.header(HttpMessageProcessorEndpoint.HEADER_USERNAME))
    }

    @Test
    @Throws(ConfigurationIncompleteException::class)
    fun `Given a URL with embedded auth, the correct HTTP headers are set`() = runTest {
        `when`(testPreferences.deviceId).thenReturn("device_preferences")
        `when`(testPreferences.username).thenReturn("username_ignored")
        `when`(testPreferences.password).thenReturn("password_ignored")
        `when`(testPreferences.url).thenReturn("http://username_url:password_url@example.com/owntracks/test")
        val httpMessageProcessorEndpoint =
            HttpMessageProcessorEndpoint(
                messageProcessor,
                parser,
                testPreferences,
                application,
                endpointStateRepo,
                mock {},
                this,
                StandardTestDispatcher()
            )
        val configuration = httpMessageProcessorEndpoint.getEndpointConfiguration()
        configuration.validate()
        val request = httpMessageProcessorEndpoint.getRequest(configuration, messageLocation)
        assertNotNull(request)
        assertEquals(
            "Basic dXNlcm5hbWVfdXJsOnBhc3N3b3JkX3VybA==",
            request.header(HttpMessageProcessorEndpoint.HEADER_AUTHORIZATION)
        )
        assertEquals("username_url", request.header(HttpMessageProcessorEndpoint.HEADER_USERNAME))
        assertEquals(
            "device_preferences",
            request.header(HttpMessageProcessorEndpoint.HEADER_DEVICE)
        )
        assertEquals(
            "http://username_url:password_url@example.com/owntracks/test",
            request.url.toString()
        )
    }

    @Test
    fun `Given no auth details, the auth HTTP header is not set`() = runTest {
        val httpMessageProcessorEndpoint =
            HttpMessageProcessorEndpoint(
                messageProcessor,
                parser,
                testPreferences,
                application,
                endpointStateRepo,
                mock {},
                this,
                StandardTestDispatcher()
            )
        val configuration = httpMessageProcessorEndpoint.getEndpointConfiguration()
        configuration.validate()
        val request = httpMessageProcessorEndpoint.getRequest(configuration, messageLocation)
        assertNotNull(request)
        assertNull(request.header(HttpMessageProcessorEndpoint.HEADER_AUTHORIZATION))
    }

    @Test(expected = ConfigurationIncompleteException::class)
    fun `Given an invalid URL, the messageProcessor throws the right exception`() = runTest {
        val urls = arrayOf("htt://example.com/owntracks/test", "tt://example", "example.com")
        for (url in urls) {
            `when`(testPreferences.url).thenReturn(url)
            val httpMessageProcessorEndpoint =
                HttpMessageProcessorEndpoint(
                    messageProcessor,
                    parser,
                    testPreferences,
                    application,
                    endpointStateRepo,
                    mock {},
                    this,
                    StandardTestDispatcher()
                )
            httpMessageProcessorEndpoint.getEndpointConfiguration()
        }
    }
}
