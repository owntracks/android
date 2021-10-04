package org.owntracks.android.services;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.owntracks.android.BuildConfig;
import org.owntracks.android.R;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.services.MessageProcessor.EndpointState;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.ConfigurationIncompleteException;
import org.owntracks.android.support.preferences.OnModeChangedPreferenceChangedListener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class MessageProcessorEndpointHttp extends MessageProcessorEndpoint implements OnModeChangedPreferenceChangedListener {
    public static final int MODE_ID = 3;

    // Headers according to https://github.com/owntracks/recorder#http-mode
    static final String HEADER_USERNAME = "X-Limit-U";
    static final String HEADER_DEVICE = "X-Limit-D";
    private static final String HEADER_USERAGENT = "User-Agent";
    static final String METHOD = "POST";

    static final String HEADER_AUTHORIZATION = "Authorization";

    private static String httpEndpointHeaderUser = "";
    private static String httpEndpointHeaderDevice = "";
    private static String httpEndpointHeaderPassword = "";

    private static OkHttpClient mHttpClient;
    private static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");

    public static final String USERAGENT = "Owntracks-Android/"+ BuildConfig.VERSION_CODE;
    private static final String HTTPTOPIC = "owntracks/http/";

    private Preferences preferences;
    private Parser parser;
    private Scheduler scheduler;
    private Context applicationContext;
    private HttpUrl httpEndpoint;

    public MessageProcessorEndpointHttp(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, Context applicationContext) {
        super(messageProcessor);
        this.parser = parser;
        this.preferences = preferences;
        this.scheduler = scheduler;
        this.applicationContext = applicationContext;

        preferences.registerOnPreferenceChangedListener(this);
        loadEndpointUrl();
    }

    @Override
    public void onCreateFromProcessor() {
        try {
            checkConfigurationComplete();
        } catch (ConfigurationIncompleteException e) {
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.withError(e));
        }
    }

    @Nullable
    private SocketFactory getSocketFactory() {
        String tlsCaCrt = preferences.getTlsCaCrt();
        String tlsClientCrt = preferences.getTlsClientCrt();

        if(tlsCaCrt.length() == 0 && tlsClientCrt.length() == 0) {
            return null;
        }

        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

        if (tlsCaCrt.length() > 0) {
            try {
                socketFactoryOptions.withCaInputStream(applicationContext.openFileInput(tlsCaCrt));
            } catch (FileNotFoundException e) {
                Timber.e(e);
                return null;
            }
        }

        if (tlsClientCrt.length() > 0)	{
            try {
                socketFactoryOptions.withClientP12InputStream(applicationContext.openFileInput(tlsClientCrt)).withClientP12Password(preferences.getTlsClientCrtPassword());
            } catch (FileNotFoundException e1) {
                Timber.e(e1);
                return null;
            }
        }

        try {
            return new SocketFactory(socketFactoryOptions);
        } catch (Exception e) {
            return null;
        }
    }

    private OkHttpClient getHttpClient() {
        if(preferences.getDontReuseHttpClient()) {
            return createHttpClient();
        }

        if(mHttpClient == null)
            mHttpClient = createHttpClient();

        return mHttpClient;
    }



    private OkHttpClient createHttpClient() {
        Timber.d("creating new HTTP client instance");
        SocketFactory f = getSocketFactory();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(1, 1, TimeUnit.MICROSECONDS))
                .retryOnConnectionFailure(false)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .cache(null);

        if(f != null) {
            builder.sslSocketFactory(f, (X509TrustManager) f.getTrustManagers()[0]);
        }
        return builder.build();
    }


    private void loadEndpointUrl() {
        try {
            httpEndpointHeaderUser = preferences.getUsername();
            httpEndpointHeaderDevice = preferences.getDeviceId();

            httpEndpoint = HttpUrl.get(preferences.getUrl());

            if(!httpEndpoint.username().isEmpty() && !httpEndpoint.password().isEmpty()) {
                httpEndpointHeaderUser = httpEndpoint.username();
                httpEndpointHeaderPassword = httpEndpoint.password();
            } else if(!preferences.getPassword().trim().equals("")) {
                httpEndpointHeaderPassword = preferences.getPassword();
            }

            messageProcessor.onEndpointStateChanged(EndpointState.IDLE);
        } catch (IllegalArgumentException e) {
            httpEndpoint = null;
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.withError(e));
        }
    }

    @Nullable
    Request getRequest(MessageBase message) {
        try {
            this.checkConfigurationComplete();
        } catch (ConfigurationIncompleteException e) {
            return null;
        }
        Timber.d("url:%s, messageId:%s", this.httpEndpoint, message.getMessageId());

        String body;
        try {
            body = message.toJson(parser);
        } catch (IOException e) { // Message serialization failed. This shouldn't happen.
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR.withMessage(e.getMessage()));
            return null;
        }


        // Any exception here (invalid header value, invalid URL, etc) will persist for all future messages until configuration is fixed.
        // Setting httpEndpoint to null will make sure no message can be send until the problem is corrected.
        try {
            Request.Builder request = new Request.Builder().url(this.httpEndpoint).header(HEADER_USERAGENT,USERAGENT).method(METHOD, RequestBody.create(JSON, body));

            if(isSet(httpEndpointHeaderUser) && isSet(httpEndpointHeaderPassword)) {
                request.header(HEADER_AUTHORIZATION, Credentials.basic(httpEndpointHeaderUser, httpEndpointHeaderPassword));
            }

            if (isSet(httpEndpointHeaderUser)) {
                request.header(HEADER_USERNAME, httpEndpointHeaderUser);
            }

            if (isSet(httpEndpointHeaderDevice)) {
                request.header(HEADER_DEVICE, httpEndpointHeaderDevice);
            }


            request.cacheControl(CacheControl.FORCE_NETWORK);
            return request.build();
        } catch (Exception e) {
            Timber.e(e,"invalid header specified");
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.withError(e));
            httpEndpoint = null;
            return null;
        }
    }


    private static boolean isSet(String str) {
        return str != null && str.length() > 0;
    }

    void sendMessage(MessageBase message) throws OutgoingMessageSendingException {
        message.addMqttPreferences(preferences);
        // HTTP messages carry the topic field in the body of the message, rather than MQTT which
        // simply publishes the message to that topic.
        message.setTopicVisible();
        String messageId = message.getMessageId();
        long startTime = System.nanoTime();
        Request request = getRequest(message);
        if(request == null) {
            messageProcessor.onMessageDeliveryFailedFinal(message.getMessageId());
            return;
        }
        try(Response response = getHttpClient().newCall(request).execute()) {
            // Message was send. Handle delivered message
            if((response.isSuccessful())) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                Timber.i("Message id=%s sent in %dms", messageId, TimeUnit.NANOSECONDS.toMillis(duration));
                // Handle response
                if(response.body() != null ) {
                    try {
                        MessageBase[] result = parser.fromJson(response.body().byteStream());
                        //TODO apply i18n here
                        messageProcessor.onEndpointStateChanged(EndpointState.IDLE.withMessage(String.format(Locale.ROOT,"Response %d, (%d msgs received)", response.code(), result.length)));
                        for (MessageBase aResult : result) {
                            onMessageReceived(aResult);
                        }
                    } catch (JsonProcessingException e ) {
                        Timber.e("JsonParseException HTTP status: %s", response.code());
                        messageProcessor.onEndpointStateChanged(EndpointState.IDLE.withMessage(String.format(Locale.ROOT,"HTTP status %d, JsonParseException", response.code())));
                    } catch (Parser.EncryptionException e) {
                        Timber.e("JsonParseException HTTP status: %s", response.code());
                        messageProcessor.onEndpointStateChanged(EndpointState.ERROR.withMessage(String.format(Locale.ROOT,"HTTP status: %d, EncryptionException", response.code())));
                    }
                }
            // Server could be contacted but returned non success HTTP code
            } else {
                Exception httpException = new Exception(String.format("HTTP request failed. Status: %s", response.code()));
                Timber.e(httpException);
                messageProcessor.onEndpointStateChanged(EndpointState.ERROR.withMessage(String.format(Locale.ROOT, "HTTP code %d", response.code())));
                messageProcessor.onMessageDeliveryFailed(messageId);
                throw new OutgoingMessageSendingException(httpException);
            }
        // Message was not send
        } catch (IOException e) {
            Timber.e(e, "HTTP Delivery failed ");
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR.withError(e));
            messageProcessor.onMessageDeliveryFailed(messageId);
            throw new OutgoingMessageSendingException(e);
        }
        messageProcessor.onMessageDelivered(message);
    }

    @Override
    public void onDestroy() {
        scheduler.cancelHttpTasks();
        preferences.unregisterOnPreferenceChangedListener(this);

    }

    @Override
    public void onAttachAfterModeChanged() {
        //NOOP
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (
                preferences.getPreferenceKey(R.string.preferenceKeyURL).equals(key)
                        || preferences.getPreferenceKey(R.string.preferenceKeyUsername).equals(key)
                        || preferences.getPreferenceKey(R.string.preferenceKeyPassword).equals(key)
                        || preferences.getPreferenceKey(R.string.preferenceKeyDeviceId).equals(key)) {
            messageProcessor.resetMessageSleepBlock();
            loadEndpointUrl();
        } else if (preferences.getPreferenceKey(R.string.preferenceKeyTLSClientCrt).equals(key)
                || preferences.getPreferenceKey(R.string.preferenceKeyTLSClientCrtPassword).equals(key)
                || preferences.getPreferenceKey(R.string.preferenceKeyTLSCaCrt).equals(key)) {
            mHttpClient = null;
        }
    }

    @Override
    public void checkConfigurationComplete() throws ConfigurationIncompleteException {
        if (this.httpEndpoint == null) {
            throw new ConfigurationIncompleteException("HTTP Endpoint is missing");
        }
    }

    @Override
    int getModeId() {
        return MODE_ID;
    }

    @Override
    protected MessageBase onFinalizeMessage(MessageBase message) {
        // Build pseudo topic based on tid
        if(message.hasTrackerId()) {
            message.setTopic(HTTPTOPIC + message.getTrackerId());
        }
        return message;
    }
}
