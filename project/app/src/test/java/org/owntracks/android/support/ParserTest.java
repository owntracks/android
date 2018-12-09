package org.owntracks.android.support;

import com.fasterxml.jackson.core.JsonParseException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageUnknown;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;


@RunWith(PowerMockRunner.class)
@PrepareForTest({App.class})
public class ParserTest {
    private MessageLocation messageLocation;

    @Mock
    private
    Preferences testPreferences;

    // Need to mock this as we can't unit test native lib off-device
    @Mock
    private
    EncryptionProvider encryptionProvider;

    private String expectedJson = "{\"_type\":\"location\",\"acc\":10,\"alt\":20,\"batt\":30,\"conn\":\"TestConn\",\"inregions\":[\"Testregion1\",\"Testregion2\"],\"lat\":50.1,\"lon\":60.2,\"tst\":123456789,\"vac\":1,\"vel\":5}";

    @Before
    public void setupMessageLocation() {
        List<String> regions = new LinkedList<>();
        regions.add("Testregion1");
        regions.add("Testregion2");
        messageLocation = new MessageLocation();
        messageLocation.setAcc(10);
        messageLocation.setAlt(20);
        messageLocation.setBatt(30);
        messageLocation.setConn("TestConn");
        messageLocation.setLat(50.1);
        messageLocation.setLon(60.2);
        messageLocation.setTst(123456789);
        messageLocation.setVelocity((int)5.6);
        messageLocation.setVac((int)1.7);

        messageLocation.setInRegions(regions);

    }

    @Before
    public void setupEncryptionProvider() {
        testPreferences = mock(Preferences.class);
        when(testPreferences.getEncryptionKey()).thenReturn("testEncryptionKey");
        when(encryptionProvider.isPayloadEncryptionEnabled()).thenReturn(true);
    }

    public String getResourceFileAsString(String resourceFileName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceFileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    @Test
    public void ParserCorrectlyConvertsLocationToPrettyJSON() throws Exception {
        Parser parser = new Parser(null);
        String expected = getResourceFileAsString("prettyLocation.json");
        assertEquals(expected, parser.toJsonPlainPretty(messageLocation));
    }

    @Test
    public void ParserCorrectlyConvertsLocationToPlainJSON() throws Exception {
        Parser parser = new Parser(null);
        assertEquals(expectedJson, parser.toJsonPlain(messageLocation));
    }

    @Test
    public void ParserCorrectlyConvertsLocationToJSONWithEncryption() throws Exception {
        when(encryptionProvider.encrypt(anyString())).thenReturn("TestCipherText");

        Parser parser = new Parser(encryptionProvider);
        String expected = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}";
        assertEquals(expected, parser.toJson(messageLocation));
    }

    @Test
    public void ParserCorrectlyConvertsLocationToJSONWithEncryptionDisabled() throws Exception {
        when(encryptionProvider.isPayloadEncryptionEnabled()).thenReturn(false);
        when(encryptionProvider.encrypt(anyString())).thenReturn("TestCipherText");
        Parser parser = new Parser(encryptionProvider);
        assertEquals(expectedJson, parser.toJson(messageLocation));
    }


    @Test
    public void ParserReturnsMessageLocationFromValidLocationInput() throws Exception {
        Parser parser = new Parser(encryptionProvider);
        String input = "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"inregions\":[\"Testregion1\",\"Testregion2\"]}";
        MessageBase messageBase = parser.fromJson(input);
        assertEquals(MessageLocation.class, messageBase.getClass());
        MessageLocation messageLocation = (MessageLocation) messageBase;
        assertEquals(1514455575L, messageLocation.getTst());
        assertEquals("s5", messageLocation.getTid());
        assertEquals(1600, messageLocation.getAcc());
        assertEquals(0.0, messageLocation.getAlt(), 0);
        assertEquals(99, messageLocation.getBatt());
        assertEquals("w", messageLocation.getConn());
        assertEquals(52.3153748, messageLocation.getLatitude(), 0);
        assertEquals(5.0408462, messageLocation.getLongitude(), 0);
        assertEquals("p", messageLocation.getT());
        assertEquals(0, messageLocation.getVac(), 0);
        assertEquals(2, messageLocation.getInRegions().size());

    }

    @Test
    public void ParserReturnsMessageLocationFromValidEncryptedLocationInput() throws Exception {
        when(encryptionProvider.isPayloadEncryptionEnabled()).thenReturn(true);
        String messageLocationJSON = "{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0,\"vel\":2}";
        when(encryptionProvider.decrypt("TestCipherText")).thenReturn(messageLocationJSON);

        Parser parser = new Parser(encryptionProvider);
        String input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}";
        MessageBase messageBase = parser.fromJson(input);
        assertEquals(MessageLocation.class, messageBase.getClass());
        MessageLocation messageLocation = (MessageLocation) messageBase;
        assertEquals(1514455575L, messageLocation.getTst());
        assertEquals("s5", messageLocation.getTid());
        assertEquals(1600, messageLocation.getAcc());
        assertEquals(0.0, messageLocation.getAlt(), 0);
        assertEquals(99, messageLocation.getBatt());
        assertEquals("w", messageLocation.getConn());
        assertEquals(52.3153748, messageLocation.getLatitude(), 0);
        assertEquals(5.0408462, messageLocation.getLongitude(), 0);
        assertEquals("p", messageLocation.getT());
        assertEquals(0, messageLocation.getVac(), 0);
        assertEquals(2, messageLocation.getVelocity(), 0);
    }

    @Test(expected = Parser.EncryptionException.class)
    public void ParserShouldThrowExceptionWhenGivenEncryptedMessageWithEncryptionDisabled() throws Exception {
        when(encryptionProvider.isPayloadEncryptionEnabled()).thenReturn(false);
        Parser parser = new Parser(encryptionProvider);
        String input = "{\"_type\":\"encrypted\",\"data\":\"TestCipherText\"}";
        parser.fromJson(input);
    }

    @Test
    public void ParserShouldDecodeStreamOfMultipleMessageLocationsCorrectly() throws Exception {
        String multipleMessageLocationJSON = "[{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":1600,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":52.3153748,\"lon\":5.0408462,\"t\":\"p\",\"tst\":1514455575,\"vac\":0},{\"_type\":\"location\",\"tid\":\"s5\",\"acc\":95,\"alt\":0.0,\"batt\":99,\"conn\":\"w\",\"lat\":12.3153748,\"lon\":15.0408462,\"t\":\"p\",\"tst\":1514455579,\"vac\":0}]";
        Parser parser = new Parser(encryptionProvider);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(multipleMessageLocationJSON.getBytes());
        MessageBase[] messages = parser.fromJson(byteArrayInputStream);
        assertEquals(2, messages.length);
        for (MessageBase messagebase : messages) {
            assertEquals(MessageLocation.class, messagebase.getClass());
        }
        MessageLocation firstMessageLocation = (MessageLocation) messages[0];
        assertEquals(1514455575L, firstMessageLocation.getTst());
        MessageLocation secondMessageLocation = (MessageLocation) messages[1];
        assertEquals(1514455579L, secondMessageLocation.getTst());
    }

    @Test(expected = IOException.class)
    public void ParserShouldThrowExceptionOnEmptyArray() throws Exception {
        Parser parser = new Parser(encryptionProvider);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(new byte[0]);
        parser.fromJson(byteArrayInputStream);
    }

    @Test
    public void ParserReturnsMessageUnknownOnValidOtherJSON() throws Exception {
        Parser parser = new Parser(encryptionProvider);
        MessageBase message = parser.fromJson("{\"some\":\"invalid message\"}");
        assertEquals(MessageUnknown.class, message.getClass());
    }

    @Test(expected = JsonParseException.class)
    public void ParserThrowsCorrectExceptionWhenGivenInvalidJSON() throws Exception {
        Parser parser = new Parser(encryptionProvider);
        parser.fromJson("not JSON");
    }

}