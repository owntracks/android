package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.util.Base64;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
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
    private static final String HEADER_USERNAME = "X-Limit-U";
    private static final String HEADER_DEVICE = "X-Limit-D";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static String headerUsername;
    private static String headerDevice;

    private String endpointUrl;
    private String endpointUserInfo;

    private static OkHttpClient mHttpClient;
    private static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");

    public static final String USERAGENT = "Owntracks/"+ BuildConfig.VERSION_CODE;

    protected Preferences preferences;
    protected Parser parser;
    protected Scheduler scheduler;


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

    private void loadHTTPClient() {
        String tlsCaCrt = preferences.getTlsCaCrtName();
        String tlsClientCrt = preferences.getTlsClientCrtName();
        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

        if (tlsCaCrt.length() > 0) {
            try {
                socketFactoryOptions.withCaInputStream(App.getContext().openFileInput(tlsCaCrt));
            } catch (FileNotFoundException e) {
                Timber.e(e);
            }
        }

        if (tlsClientCrt.length() > 0)	{
            try {
                socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(preferences.getTlsClientCrtPassword());
            } catch (FileNotFoundException e1) {
                Timber.e(e1);
            }
        }

        try {
            SocketFactory f = new SocketFactory(socketFactoryOptions);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel( HttpLoggingInterceptor.Level.NONE);

            mHttpClient = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    //.dns(new DebugDnsSelector(DebugDnsSelector.Mode.IPV4_FIRST))
                    .connectionPool(new ConnectionPool())
                    .sslSocketFactory(f, (X509TrustManager) f.getTrustManagers()[0])
                    .addInterceptor(logging).build();



        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | IOException | UnrecoverableKeyException | CertificateException e) {
            //e.printStackTrace();
            Timber.e(e);
        }
        headerUsername = preferences.getStringOrNull(Preferences.Keys.USERNAME);
        headerDevice = preferences.getStringOrNull(Preferences.Keys.DEVICE_ID);
    }


    private void loadEndpointUrl() {
        URL endpoint;
        try {
            endpoint = new URL(preferences.getUrl());

            this.endpointUserInfo = endpoint.getUserInfo();

            if (this.endpointUserInfo != null && this.endpointUserInfo.length() > 0) {
                this.endpointUrl = endpoint.toString().replace(endpointUserInfo+"@", "");
            } else {
                this.endpointUrl = endpoint.toString();
            }
            Timber.v("endpointUrl:%s, endpointUserInfo:%s", this.endpointUrl, this.endpointUserInfo );

            messageProcessor.onEndpointStateChanged(EndpointState.IDLE);
        } catch (MalformedURLException e) {
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.setError(e));
        }
    }

    private void sendMessage(MessageBase message) {
        long messageId = message.getMessageId();
        String body;
        try {
            body = parser.toJson(message);
        } catch (IOException | Parser.EncryptionException e) {
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            return;
        }

        Timber.v("url:%s, userInfo:%s, messageId:%s", this.endpointUrl, this.endpointUserInfo,  messageId);

        if(body == null || this.endpointUrl == null) {
            Timber.e("body or url null");
            messageProcessor.onMessageDeliveryFailed(messageId);
            return;
        }

        Request.Builder request = new Request.Builder().url(this.endpointUrl).header("User-Agent",USERAGENT).method("POST", RequestBody.create(JSON, body));
        if(this.endpointUserInfo != null) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString(this.endpointUserInfo.getBytes(), Base64.NO_WRAP));
        } else if(preferences.getAuth()) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString((preferences.getUsername()+":"+preferences.getPassword()).getBytes(), Base64.NO_WRAP));
        }

        try {
            if (headerUsername != null) {
                request.header(HEADER_USERNAME, headerUsername);
            }
            if (headerDevice != null) {
                request.header(HEADER_DEVICE, headerDevice);
            }
        } catch (IllegalAccessError e) {
            Timber.e(e,"invalid header specified");
            messageProcessor.onMessageDeliveryFailedFinal(messageId);
            messageProcessor.onEndpointStateChanged(EndpointState.ERROR.setMessage("invalid value for user or device header"));
            return;
        }

        try {
            //Send request
            Response r = mHttpClient.newCall(request.build()).execute();

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
        if(Preferences.Keys.URL.equals(key))
            loadEndpointUrl();
        else if(Preferences.Keys.TLS_CLIENT_CRT.equals(key) || Preferences.Keys.TLS_CLIENT_CRT_PASSWORD.equals(key) ||Preferences.Keys.TLS_CA_CRT.equals(key))
            loadHTTPClient();
        else if(Preferences.Keys.USERNAME.equals(key))
            headerUsername = preferences.getStringOrNull(Preferences.Keys.USERNAME);
        else if(Preferences.Keys.DEVICE_ID.equals(key))
            headerDevice = preferences.getStringOrNull(Preferences.Keys.DEVICE_ID);
    }

    @Override
    public boolean isConfigurationComplete() {
        return this.endpointUrl != null;
    }

    @Override
    int getModeId() {
        return MODE_ID;
    }

    @Override
    protected MessageBase onFinalizeMessage(MessageBase message) {
        // Build pseudo topic based on tid
        if(message.hasTid()) {
            message.setTopic("owntracks/http/" + message.getTid());
        }
        return message;
    }
}
