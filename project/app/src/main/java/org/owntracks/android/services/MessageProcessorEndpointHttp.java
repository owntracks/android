package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.MessageProcessor.EndpointState;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

public class MessageProcessorEndpointHttp extends MessageProcessorEndpoint implements Preferences.OnPreferenceChangedListener {
    public static final int MODE_ID = 3;

    // Headers according to https://github.com/owntracks/recorder#http-mode
    public static final String HEADER_USERNAME = "X-Limit-U";
    public static final String HEADER_DEVICE = "X-Limit-D";
    public static final String HEADER_USERAGENT = "User-Agent";
    public static final String METHOD = "POST";

    public static final String HEADER_AUTHORIZATION = "Authorization";

    private static String httpEndpointHeaderUser = "";
    private static String httpEndpointHeaderDevice = "";
    private static String httpEndpointHeaderPassword = "";

    //private String endpointUrl;
    //private String endpointUserInfo;

    private static OkHttpClient mHttpClient;
    private static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");

    public static final String USERAGENT = "Owntracks/"+ BuildConfig.VERSION_CODE;
    public static final String HTTPTOPIC = "owntracks/http/";

    protected Preferences preferences;
    protected Parser parser;
    protected Scheduler scheduler;
    private HttpUrl httpEndpoint;


    public MessageProcessorEndpointHttp(MessageProcessor messageProcessor, Parser parser, Preferences preferences, Scheduler scheduler, EventBus eventBus) {
        super(messageProcessor);
        this.parser = parser;
        this.preferences = preferences;
        this.scheduler = scheduler;

        preferences.registerOnPreferenceChangedListener(this);
        loadEndpointUrl();
        loadHTTPClient();

    }

    @Override
    public void onCreateFromProcessor() {

    }

    @Nullable
    private SocketFactory getSocketFactory() {
        String tlsCaCrt = preferences.getTlsCaCrtName();
        String tlsClientCrt = preferences.getTlsClientCrtName();

        if(tlsCaCrt.length() == 0 && tlsClientCrt.length() == 0) {
            return null;
        }

        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

        if (tlsCaCrt.length() > 0) {
            try {
                socketFactoryOptions.withCaInputStream(App.getContext().openFileInput(tlsCaCrt));
            } catch (FileNotFoundException e) {
                Timber.e(e);
                return null;
            }
        }

        if (tlsClientCrt.length() > 0)	{
            try {
                socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(preferences.getTlsClientCrtPassword());
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

    private void loadHTTPClient() {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel( HttpLoggingInterceptor.Level.NONE);

        SocketFactory f = getSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool())
                .addInterceptor(logging)
                .cache(null);

        if(f != null) {
            builder.sslSocketFactory(f, (X509TrustManager) f.getTrustManagers()[0]);
        }

        mHttpClient = builder.build();
    }


    private void loadEndpointUrl() {
        try {
            httpEndpointHeaderUser = preferences.getUsername();
            httpEndpointHeaderDevice = preferences.getDeviceId();

            httpEndpoint = HttpUrl.get(preferences.getUrl());

            if(!httpEndpoint.username().isEmpty() && !httpEndpoint.password().isEmpty()) {
                httpEndpointHeaderUser = httpEndpoint.username();
                httpEndpointHeaderPassword = httpEndpoint.password();
            } else if(preferences.getAuth()) {
                httpEndpointHeaderPassword = preferences.getPassword();
            }


            messageProcessor.onEndpointStateChanged(EndpointState.IDLE);
        } catch (IllegalArgumentException e) {
            httpEndpoint = null;
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.setError(e));
        }
    }

    @Nullable
    public Request getRequest(MessageBase message) {
        if(!this.isConfigurationComplete()) {
            return null;
        }
        Timber.v("url:%s, messageId:%s", this.httpEndpoint, message.getMessageId());

        String body;
        try {
            body = parser.toJson(message);
        } catch (IOException e) { // Message serialization failed. This shouldn't happen.
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR.setMessage(e.getMessage()));
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

            return request.build();
        } catch (Exception e) {
            Timber.e(e,"invalid header specified");
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.setMessage(e.getMessage()));
            httpEndpoint = null;
            return null;
        }
    }


    public static boolean isSet(String str) {
        return str != null && str.length() > 0;
    }

    private void sendMessage(MessageBase message) {
        long messageId = message.getMessageId();
        Request request = getRequest(message);
        if(request == null) {
            messageProcessor.onMessageDeliveryFailedFinal(message.getMessageId());
            return;
        }

        try {
            //Send request
            Response r = mHttpClient.newCall(request).execute();

            // Message was send. Handle delivered message
            if((r.isSuccessful())) {
                Timber.v("request was successful");
                // Handle response
                if(r.body() != null ) {
                    try {

                        MessageBase[] result = parser.fromJson(r.body().byteStream());
                        messageProcessor.onEndpointStateChanged(EndpointState.IDLE.setMessage("Response " + r.code() + ", " + result.length));

                        for (MessageBase aResult : result) {
                            onMessageReceived(aResult);
                        }
                    } catch (JsonProcessingException e ) {
                        Timber.e("error:JsonParseException responseCode:%s", r.code());
                        messageProcessor.onEndpointStateChanged(EndpointState.IDLE.setMessage("HTTP " +r.code() + ", JsonParseException"));
                    } catch (Parser.EncryptionException e) {
                        Timber.e("error:JsonParseException responseCode:%s", r.code());
                        messageProcessor.onEndpointStateChanged(EndpointState.ERROR.setMessage("HTTP: "+r.code() + ", EncryptionException"));
                    }
                }
            // Server could be contacted but returned non success HTTP code
            } else {
                Timber.e("request was not successful. HTTP code %s", r.code());
                messageProcessor.onEndpointStateChanged(EndpointState.ERROR.setMessage("HTTP code "+r.code() ));
                messageProcessor.onMessageDeliveryFailed(messageId);
                return;
            }
        // Message was not send
        } catch (Exception e) {
            Timber.e(e,"error:IOException. Delivery failed ");
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR.setError(e));
            messageProcessor.onMessageDeliveryFailed(messageId);
            return;
        }

        messageProcessor.onMessageDelivered(messageId);
    }

    @Override
    public void processOutgoingMessage(MessageBase message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        sendMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageClear message) {
        sendMessage(message);
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
        if(Preferences.Keys.URL.equals(key) || Preferences.Keys.USERNAME.equals(key) || Preferences.Keys.PASSWORD.equals(key) || Preferences.Keys.DEVICE_ID.equals(key))
            loadEndpointUrl();
        else if(Preferences.Keys.TLS_CLIENT_CRT.equals(key) || Preferences.Keys.TLS_CLIENT_CRT_PASSWORD.equals(key) ||Preferences.Keys.TLS_CA_CRT.equals(key))
            loadHTTPClient();
    }

    @Override
    public boolean isConfigurationComplete() {
        return this.httpEndpoint != null;
    }

    @Override
    int getModeId() {
        return MODE_ID;
    }

    @Override
    protected MessageBase onFinalizeMessage(MessageBase message) {
        // Build pseudo topic based on tid
        if(message.hasTid()) {
            message.setTopic(HTTPTOPIC + message.getTid());
        }
        return message;
    }
}
