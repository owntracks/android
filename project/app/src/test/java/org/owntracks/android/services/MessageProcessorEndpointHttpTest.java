package org.owntracks.android.services;

import androidx.core.content.ContextCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(PowerMockRunner.class)
@PrepareForTest(ContextCompat.class)
@PowerMockIgnore("javax.net.ssl.*")
public class MessageProcessorEndpointHttpTest {
    private MessageProcessorEndpointHttp messageProcessorEndpointHttp;

    @Mock
    private Preferences testPreferences;

    @Mock
    private MessageProcessor messageProcessor;

    @Mock
    private Scheduler scheduler;

    @Mock
    private EncryptionProvider encryptionProvider;

    private Parser parser;

    private MessageLocation messageLocation;


    @Before
    public void setup() {
        messageLocation = new MessageLocation();
        messageLocation.setAcc(10);
        messageLocation.setAlt(20);
        messageLocation.setBatt(30);
        messageLocation.setConn("TestConn");
        messageLocation.setLat(50.1);
        messageLocation.setLon(60.2);
        messageLocation.setTst(123456789);
        messageLocation.setVelocity((int) 5.6);
        messageLocation.setVac((int) 1.7);

        testPreferences = mock(Preferences.class);
        when(testPreferences.getTlsCaCrtName()).thenReturn("");
        when(testPreferences.getTlsClientCrtName()).thenReturn("");

        when(testPreferences.getUsername()).thenReturn("");
        when(testPreferences.getDeviceId()).thenReturn("");
        when(testPreferences.getPassword()).thenReturn("");
        when(testPreferences.getUrl()).thenReturn("http://example.com/owntracks/test");
        parser = new Parser(encryptionProvider);
    }


    @Test
    public void EndpointCorrectlyInitializesSelfWithDefaultSettings() {
        when(testPreferences.getUrl()).thenReturn("");
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);
    }

    @Test
    public void EndpointCorrectlyInitializesRequestWithoutAuth() throws ConfigurationIncompleteException {
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);

        messageProcessorEndpointHttp.checkConfigurationComplete();

        okhttp3.Request request = messageProcessorEndpointHttp.getRequest(messageLocation);
        assertNotNull(request);
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION));
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_USERNAME));
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_DEVICE));
        assertEquals(MessageProcessorEndpointHttp.METHOD, request.method());
        assertEquals("http://example.com/owntracks/test", request.url().toString());

    }


    @Test
    public void EndpointCorrectlyInitializesRequestWithoutAuthAndWithUser() throws ConfigurationIncompleteException {

        when(testPreferences.getUsername()).thenReturn("username");
        when(testPreferences.getDeviceId()).thenReturn("device");
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);
        messageProcessorEndpointHttp.checkConfigurationComplete();
        okhttp3.Request request = messageProcessorEndpointHttp.getRequest(messageLocation);
        assertNotNull(request);
        assertEquals(request.header(MessageProcessorEndpointHttp.HEADER_USERNAME), "username");
        assertEquals(request.header(MessageProcessorEndpointHttp.HEADER_DEVICE), "device");
    }

    @Test
    public void EndpointCorrectlyInitializesRequestWithAuth() throws ConfigurationIncompleteException {
        when(testPreferences.getUsername()).thenReturn("username");
        when(testPreferences.getPassword()).thenReturn("password");
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);
        messageProcessorEndpointHttp.checkConfigurationComplete();
        okhttp3.Request request = messageProcessorEndpointHttp.getRequest(messageLocation);
        assertNotNull(request);
        System.out.println(request.headers().toString());
        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", request.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION));
        assertEquals("username", request.header(MessageProcessorEndpointHttp.HEADER_USERNAME));
    }

    @Test
    public void EndpointCorrectlyInitializesRequestWithAuthFromUrl() throws ConfigurationIncompleteException {
        when(testPreferences.getDeviceId()).thenReturn("device_preferences");
        when(testPreferences.getUsername()).thenReturn("username_ignored");
        when(testPreferences.getPassword()).thenReturn("password_ignored");
        when(testPreferences.getUrl()).thenReturn("http://username_url:password_url@example.com/owntracks/test");
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);
        messageProcessorEndpointHttp.checkConfigurationComplete();
        okhttp3.Request request = messageProcessorEndpointHttp.getRequest(messageLocation);
        assertNotNull(request);
        System.out.println(request.headers().toString());
        assertEquals("Basic dXNlcm5hbWVfdXJsOnBhc3N3b3JkX3VybA==", request.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION));
        assertEquals("username_url", request.header(MessageProcessorEndpointHttp.HEADER_USERNAME));
        assertEquals("device_preferences", request.header(MessageProcessorEndpointHttp.HEADER_DEVICE));

        assertEquals("http://username_url:password_url@example.com/owntracks/test", request.url().toString());
    }


    @Test
    public void EndpointCorrectlyInitializesRequestWithAuthAndEmptyCredentialsFromPreferences() throws ConfigurationIncompleteException {
        messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);

        messageProcessorEndpointHttp.checkConfigurationComplete();

        okhttp3.Request request = messageProcessorEndpointHttp.getRequest(messageLocation);
        assertNotNull(request);
        assertNull(request.header(MessageProcessorEndpointHttp.HEADER_AUTHORIZATION));

    }

    @Test(expected = ConfigurationIncompleteException.class)
    public void EndpointCorrectlyFailsOnInvalidUrl() throws ConfigurationIncompleteException {
        String[] urls = {"htt://example.com/owntracks/test", "tt://example", "example.com"};

        for (String url : urls) {
            when(testPreferences.getUrl()).thenReturn(url);
            messageProcessorEndpointHttp = new MessageProcessorEndpointHttp(messageProcessor, parser, testPreferences, scheduler, null);
            messageProcessorEndpointHttp.checkConfigurationComplete();
        }
    }
}