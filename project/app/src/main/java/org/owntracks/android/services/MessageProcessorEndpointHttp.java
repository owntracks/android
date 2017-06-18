package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.Parser;
import org.owntracks.android.services.MessageProcessor.EndpointState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

public class MessageProcessorEndpointHttp implements OutgoingMessageProcessor, Preferences.OnPreferenceChangedListener {
    // Headers according to https://github.com/owntracks/recorder#http-mode
    private static final String HEADER_USERNAME = "X-Limit-U";
    private static final String HEADER_DEVICE = "X-Limit-D";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD = "HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD";
    private static final String HTTP_BUNDLE_KEY_USERINFO = "HTTP_BUNDLE_KEY_USERINFO";
    private static final String HTTP_BUNDLE_KEY_URL = "HTTP_BUNDLE_KEY_URL";

    private static String headerUsername;
    private static String headerDevice;

    private String endpointUrl;
    private String endpointUserInfo;

    private static OkHttpClient mHttpClient;
    private static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");

    private static MessageProcessorEndpointHttp instance;
    public static MessageProcessorEndpointHttp getInstance() {
        if(instance == null) {
            instance = new MessageProcessorEndpointHttp();
        }
        return instance;
    }


    private MessageProcessorEndpointHttp() {
        App.getPreferences().registerOnPreferenceChangedListener(this);
        loadEndpointUrl();
        loadHTTPClient();
    }

    @Override
    public void onCreateFromProcessor() {

    }

    private void loadHTTPClient() {
        String tlsCaCrt = Preferences.getTlsCaCrtName();
        String tlsClientCrt = Preferences.getTlsClientCrtName();
        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

        if (tlsCaCrt.length() > 0) {
            try {
                socketFactoryOptions.withCaInputStream(App.getContext().openFileInput(tlsCaCrt));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (tlsClientCrt.length() > 0)	{
            try {
                socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(Preferences.getTlsClientCrtPassword());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }

        try {
            SocketFactory f = new SocketFactory(socketFactoryOptions);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel( HttpLoggingInterceptor.Level.BODY);

            mHttpClient = new OkHttpClient.Builder().sslSocketFactory(f, (X509TrustManager) f.getTrustManagers()[0]).addInterceptor(logging).build();



        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | IOException | UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
        }
        headerUsername = Preferences.getStringOrNull(Preferences.Keys.USERNAME);
        headerDevice = Preferences.getStringOrNull(Preferences.Keys.DEVICE_ID);
    }


    private void loadEndpointUrl() {
        URL endpoint;
        try {
            endpoint = new URL(Preferences.getUrl());
            App.getMessageProcessor().onEndpointStateChanged(EndpointState.IDLE);
        } catch (MalformedURLException e) {
            App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION.setError(e));
            return;
        }

        this.endpointUserInfo = endpoint.getUserInfo();

        if (this.endpointUserInfo != null && this.endpointUserInfo.length() > 0) {
            this.endpointUrl = endpoint.toString().replace(endpointUserInfo+"@", "");
        } else {
            this.endpointUrl = endpoint.toString();
        }
        Timber.v("endpointUrl:%s, endpointUserInfo:%s", this.endpointUrl, this.endpointUserInfo );


    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    boolean sendMessage(Bundle b) {
        String body = b.getString(HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD);
        String url = b.getString(HTTP_BUNDLE_KEY_URL);
        String userInfo = b.getString(HTTP_BUNDLE_KEY_USERINFO);
        long messageId = b.getLong(Scheduler.BUNDLE_KEY_MESSAGE_ID);

        Timber.v("url:%s, userInfo:%s, messageId:%s", url, userInfo,  messageId);

        if(body == null || url == null)
            return false;

        Request.Builder request = new Request.Builder().url(url).method("POST", RequestBody.create(JSON, body));
        //request.addHeader("Accept-Encoding", "gzip");
        if(userInfo != null) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
        } else if(App.getPreferences().getAuth()) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString((Preferences.getUsername()+":"+Preferences.getPassword()).getBytes(), Base64.NO_WRAP));
        }

        if(headerUsername != null) {
            request.header(HEADER_USERNAME, headerUsername);
        }
        if(headerDevice != null) {
            request.header(HEADER_DEVICE, headerDevice);
        }

        try {
            //Send request
            Response r = mHttpClient.newCall(request.build()).execute();

            // Handle delivered message
            if((r != null) && (r.isSuccessful())) {
                getMessageProcessor().onMessageDelivered(messageId);

                // Handle response
                if(r.body() != null ) {
                    try {

                        MessageBase[] result = App.getParser().fromJson(r.body().byteStream());
                        App.getMessageProcessor().onEndpointStateChanged(EndpointState.IDLE.setMessage("Response " + r.code() + ", " + result.length));

                        for (MessageBase aResult : result) {
                            getMessageProcessor().onMessageReceived(aResult);
                        }
                    } catch (JsonProcessingException e ) {
                        Timber.e("error:JsonParseException responseCode:%s", r.code());
                        App.getMessageProcessor().onEndpointStateChanged(EndpointState.IDLE.setMessage("HTTP " +r.code() + ", JsonParseException"));
                    } catch (Parser.EncryptionException e) {
                        Timber.e("error:JsonParseException responseCode:%s", r.code());
                        App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR.setMessage("HTTP: "+r.code() + ", EncryptionException"));
                    }
                }
            } else {
                getMessageProcessor().onMessageDeliveryFailed(messageId);
                return false;
            }

        } catch (Exception e) {
            Timber.e("error:IOException. Delivery failed ");
            App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR.setError(e));
            getMessageProcessor().onMessageDeliveryFailed(messageId);
            return false;
        }

        return true;
    }

    private static MessageProcessor getMessageProcessor() {
        return App.getMessageProcessor();
    }

    private Bundle httpMessageToBundle(MessageBase m)  {
        Bundle b = new Bundle();
        b.putLong(Scheduler.BUNDLE_KEY_MESSAGE_ID, m.getMessageId());

        try {
            b.putString(HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD, App.getParser().toJson(m));
            b.putString(HTTP_BUNDLE_KEY_USERINFO, getInstance().endpointUserInfo);
            b.putString(HTTP_BUNDLE_KEY_URL, getInstance().endpointUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    @Override
    public void processOutgoingMessage(MessageBase message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {

        message.setTopic(Preferences.getPubTopicBase());
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        scheduleMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageClear message) {
        scheduleMessage(message);
    }

    @Override
    public void onDestroy() {
        App.getPreferences().unregisterOnPreferenceChangedListener(this);

    }

    @Override
    public void onEnterForeground() {

    }

    private void scheduleMessage(MessageBase m) {
            Bundle b = httpMessageToBundle(m);
            b.putString(Scheduler.BUNDLE_KEY_ACTION, Scheduler.ONEOFF_TASK_SEND_MESSAGE_HTTP);
            if(App.isInForeground())
                sendMessage(b);
            else
                App.getScheduler().scheduleMessage(b);
    }

    @Override
    public void onAttachAfterModeChanged() {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Preferences.Keys.URL.equals(key))
            loadEndpointUrl();
        else if(Preferences.Keys.TLS_CLIENT_CRT.equals(key) || Preferences.Keys.TLS_CLIENT_CRT_PASSWORD.equals(key) ||Preferences.Keys.TLS_CA_CRT.equals(key))
            loadHTTPClient();
        else if(Preferences.Keys.USERNAME.equals(key))
            headerUsername = Preferences.getStringOrNull(Preferences.Keys.USERNAME);
        else if(Preferences.Keys.DEVICE_ID.equals(key))
            headerDevice = Preferences.getStringOrNull(Preferences.Keys.DEVICE_ID);
    }
}
