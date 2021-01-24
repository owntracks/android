package org.owntracks.android.services

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.worker.Scheduler
import org.owntracks.android.support.EncryptionProvider
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException

class MessageProcessorEndpointHttpTest {
    @Mock
    private lateinit var testPreferences: Preferences

    @Mock
    private var messageProcessor: MessageProcessor? = null

    @Mock
    private val scheduler: Scheduler? = null

    @Mock
    private lateinit var encryptionProvider: EncryptionProvider

    private var parser: Parser? = null
    private lateinit var messageLocation: MessageLocation

    @Before
    fun setupPreferences() {
        testPreferences = mock {
            on { encryptionKey } doReturn "testEncryptionKey"
            on { tlsCaCrt } doReturn ""
            on { tlsCaCrt } doReturn ""
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
    }

    @Test
    fun `Given a simple request, the auth headers are not set`() {
        val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
        messageProcessorEndpointHttp.checkConfigurationComplete()
        val request = messageProcessorEndpointHttp.getRequest(messageLocation)
        assertNotNull(request)
        assertNull(request!!.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION))
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_USERNAME))
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_DEVICE))
        assertEquals(MessageProcessorEndpointHttp.METHOD, request.method)
        assertEquals("http://example.com/owntracks/test", request.url.toString())
    }

    @Test
    fun `Given a username and deviceId, correct HTTP headers are set`() {
        `when`(testPreferences.username).thenReturn("username")
        `when`(testPreferences.deviceId).thenReturn("device")
        val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
        messageProcessorEndpointHttp.checkConfigurationComplete()
        val request = messageProcessorEndpointHttp.getRequest(messageLocation)
        assertNotNull(request)
        assertEquals(request!!.header(MessageProcessorEndpointHttp.HEADER_USERNAME), "username")
        assertEquals(request.header(MessageProcessorEndpointHttp.HEADER_DEVICE), "device")
    }

    @Test
    fun `Given a username and password, the correct auth HTTP headers are set`() {
        `when`(testPreferences.username).thenReturn("username")
        `when`(testPreferences.password).thenReturn("password")
        val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
        messageProcessorEndpointHttp.checkConfigurationComplete()
        val request = messageProcessorEndpointHttp.getRequest(messageLocation)
        assertNotNull(request)
        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", request!!.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION))
        assertEquals("username", request.header(MessageProcessorEndpointHttp.HEADER_USERNAME))
    }

    @Test
    @Throws(ConfigurationIncompleteException::class)
    fun `Given a URL with embedded auth, the correct HTTP headers are set`() {
        `when`(testPreferences.deviceId).thenReturn("device_preferences")
        `when`(testPreferences.username).thenReturn("username_ignored")
        `when`(testPreferences.password).thenReturn("password_ignored")
        `when`(testPreferences.url).thenReturn("http://username_url:password_url@example.com/owntracks/test")
        val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
        messageProcessorEndpointHttp.checkConfigurationComplete()
        val request = messageProcessorEndpointHttp.getRequest(messageLocation)
        assertNotNull(request)
        assertEquals("Basic dXNlcm5hbWVfdXJsOnBhc3N3b3JkX3VybA==", request!!.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION))
        assertEquals("username_url", request.header(MessageProcessorEndpointHttp.HEADER_USERNAME))
        assertEquals("device_preferences", request.header(MessageProcessorEndpointHttp.HEADER_DEVICE))
        assertEquals("http://username_url:password_url@example.com/owntracks/test", request.url.toString())
    }

    @Test
    fun `Given no auth details, the auth HTTP header is not set`() {
        val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
        messageProcessorEndpointHttp.checkConfigurationComplete()
        val request = messageProcessorEndpointHttp.getRequest(messageLocation)
        assertNotNull(request)
        assertNull(request!!.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION))
    }

    @Test(expected = ConfigurationIncompleteException::class)
    fun `Given an invalid URL, the messageProcessor throws the right exception`() {
        val urls = arrayOf("htt://example.com/owntracks/test", "tt://example", "example.com")
        for (url in urls) {
            `when`(testPreferences.url).thenReturn(url)
            val messageProcessorEndpointHttp = MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null)
            messageProcessorEndpointHttp.checkConfigurationComplete()
        }
    }
}