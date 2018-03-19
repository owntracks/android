package org.owntracks.android.services;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.owntracks.android.App;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageClear;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.services.MessageProcessor.EndpointState;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.OutgoingMessageProcessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionPool;
import okhttp3.Dns;
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
        String tlsCaCrt = App.getPreferences().getTlsCaCrtName();
        String tlsClientCrt = App.getPreferences().getTlsClientCrtName();
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
                socketFactoryOptions.withClientP12InputStream(App.getContext().openFileInput(tlsClientCrt)).withClientP12Password(App.getPreferences().getTlsClientCrtPassword());
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
        headerUsername = App.getPreferences().getStringOrNull(Preferences.Keys.USERNAME);
        headerDevice = App.getPreferences().getStringOrNull(Preferences.Keys.DEVICE_ID);
    }


    private void loadEndpointUrl() {
        URL endpoint;
        try {
            endpoint = new URL(App.getPreferences().getUrl());
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

    private int sendMessage(MessageBase message) {
        long messageId = message.getMessageId();
        String body = null;
        try {
            body = App.getParser().toJson(message);
        } catch (IOException | Parser.EncryptionException e) {
            getMessageProcessor().onMessageDeliveryFailedFinal(messageId);
        }

        //String url = b.getString(HTTP_BUNDLE_KEY_URL);
        //String userInfo = b.getString(HTTP_BUNDLE_KEY_USERINFO);
        //long messageId = b.getLong(Scheduler.BUNDLE_KEY_MESSAGE_ID);

        Timber.v("url:%s, userInfo:%s, messageId:%s", this.endpointUrl, this.endpointUserInfo,  messageId);

        if(body == null || this.endpointUrl == null) {
            Timber.e("body or url null");
            return getMessageProcessor().onMessageDeliveryFailed(messageId);
        }

        Request.Builder request = new Request.Builder().url(this.endpointUrl).method("POST", RequestBody.create(JSON, body));
        //request.addHeader("Accept-Encoding", "gzip");
        if(this.endpointUserInfo != null) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString(this.endpointUserInfo.getBytes(), Base64.NO_WRAP));
        } else if(App.getPreferences().getAuth()) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString((App.getPreferences().getUsername()+":"+App.getPreferences().getPassword()).getBytes(), Base64.NO_WRAP));
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
                Timber.v("request was successful");
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
                Timber.e("request was not successful");
                if(r != null)
                    App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR.setMessage("Response "+r.code() ));
                else
                    App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR.setMessage("Response empty" ));

                return getMessageProcessor().onMessageDeliveryFailed(messageId);
            }

        } catch (Exception e) {
            Timber.e(e,"error:IOException. Delivery failed ");
            App.getMessageProcessor().onEndpointStateChanged(EndpointState.ERROR.setError(e));
            return getMessageProcessor().onMessageDeliveryFailed(messageId);
        }

        return getMessageProcessor().onMessageDelivered(messageId);
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
            Timber.e(e);
        }
        return b;
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
        App.getScheduler().cancelHttpTasks();
        App.getPreferences().unregisterOnPreferenceChangedListener(this);

    }

    @Override
    public void onEnterForeground() {

    }

    private void scheduleMessage(MessageBase m) {
            //Bundle b = httpMessageToBundle(m);
        //b.putString(Scheduler.BUNDLE_KEY_ACTION, Scheduler.ONEOFF_TASK_SEND_MESSAGE_HTTP);
        //if(App.isInForeground())
            //    sendMessage(b);
        //  else
        //      App.getScheduler().scheduleMessage(b);
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
            headerUsername = App.getPreferences().getStringOrNull(Preferences.Keys.USERNAME);
        else if(Preferences.Keys.DEVICE_ID.equals(key))
            headerDevice = App.getPreferences().getStringOrNull(Preferences.Keys.DEVICE_ID);
    }

    @Override
    public boolean isConfigurationComplete() {
        return this.endpointUrl != null;
    }

    public static class DebugDnsSelector implements Dns {

        public enum Mode {
            SYSTEM,
            IPV6_FIRST,
            IPV4_FIRST,
            IPV6_ONLY,
            IPV4_ONLY
        }

        private Mode mode;

        public DebugDnsSelector(Mode mode) {
            this.mode = mode;
        }

        public static Dns byName(String ipMode) {
            Mode selectedMode;
            switch (ipMode) {
                case "ipv6":
                    selectedMode = Mode.IPV6_FIRST;
                    break;
                case "ipv4":
                    selectedMode = Mode.IPV4_FIRST;
                    break;
                case "ipv6only":
                    selectedMode = Mode.IPV6_ONLY;
                    break;
                case "ipv4only":
                    selectedMode = Mode.IPV4_ONLY;
                    break;
                default:
                    selectedMode = Mode.SYSTEM;
                    break;
            }

            return new DebugDnsSelector(selectedMode);
        }

        @Override public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Timber.v("looking up %s idle:%s powersave:%s mainthread:%s", hostname, App.getPowerManager().isDeviceIdleMode(), App.getPowerManager().isPowerSaveMode(), Looper.myLooper() == Looper.getMainLooper());
            }

            if (hostname == null) throw new UnknownHostException("hostname == null");
            InetAddress l;
            try {
                l = InetAddress.getByName(hostname);
            }catch (UnknownHostException e) {
                //e.printStackTrace();
                Timber.e("message: %s", e.getMessage());
                throw e;
            }
            Timber.v("address: %s", l.getHostAddress());
            return Arrays.asList(l);
        }

        public void addOverride(String hostname, InetAddress address) {
        }
    }
}
