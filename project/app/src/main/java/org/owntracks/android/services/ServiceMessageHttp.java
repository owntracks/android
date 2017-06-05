package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;

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
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.SocketFactory;
import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;
import org.owntracks.android.support.Parser;
import org.owntracks.android.services.ServiceMessage.EndpointState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class ServiceMessageHttp implements OutgoingMessageProcessor {
    // Headers according to https://github.com/owntracks/recorder#http-mode
    private static final String HEADER_USERNAME = "X-Limit-U";
    private static final String HEADER_DEVICE = "X-Limit-D";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD = "HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD";
    private static final String HTTP_BUNDLE_KEY_USERINFO = "HTTP_BUNDLE_KEY_USERINFO";
    private static final String HTTP_BUNDLE_KEY_URL = "HTTP_BUNDLE_KEY_URL";
    private static final String HTTP_BUNDLE_KEY_MESSAGE_ID = "HTTP_BUNDLE_KEY_MESSAGE_ID";

    private static String headerUsername;
    private static String headerDevice;

    private String endpointUrl;
    private String endpointUserInfo;

    private static OkHttpClient mHttpClient;
    public static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
    private ThreadPoolExecutor mOutgoingMessageProcessorExecutor;
    private PowerManager powerManager;
    private ConnectivityManager connectivityManager;
    private ServiceMessage service;
    private ServiceProxy context;

    public void onCreate(ServiceProxy c) {
        Timber.v("loaded HTTP endoint");

    }

    private static ServiceMessageHttp instance;
    public static ServiceMessageHttp getInstance() {
        if(instance == null) {
            instance = new ServiceMessageHttp();
        }
        return instance;
    }

    public ServiceMessageHttp() {
        Preferences.registerOnPreferenceChangedListener(new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {

            }

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(Preferences.Keys.URL.equals(key))
                    loadEndpointUrl();
                if(Preferences.Keys.TLS_CLIENT_CRT.equals(key) || Preferences.Keys.TLS_CLIENT_CRT_PASSWORD.equals(key) ||Preferences.Keys.TLS_CA_CRT.equals(key))
                    loadHTTPClient();
                if(Preferences.Keys.USERNAME.equals(key))
                    headerUsername = Preferences.getStringOrNull(Preferences.Keys.USERNAME);
                if(Preferences.Keys.DEVICE_ID.equals(key))
                    headerDevice = Preferences.getStringOrNull(Preferences.Keys.DEVICE_ID);



            }
        });

        loadEndpointUrl();
        loadHTTPClient();
    }

    private void loadHTTPClient() {
        String tlsCaCrt = Preferences.getTlsCaCrtName();
        String tlsClientCrt = Preferences.getTlsClientCrtName();
        SocketFactory.SocketFactoryOptions socketFactoryOptions = new SocketFactory.SocketFactoryOptions();

        if (tlsCaCrt.length() > 0) {
            try {
                socketFactoryOptions.withCaInputStream(context.openFileInput(tlsCaCrt));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (tlsClientCrt.length() > 0)	{
            try {
                socketFactoryOptions.withClientP12InputStream(context.openFileInput(tlsClientCrt)).withClientP12Password(Preferences.getTlsClientCrtPassword());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }

        try {
            SocketFactory f = new SocketFactory(socketFactoryOptions);
            mHttpClient = new OkHttpClient.Builder().sslSocketFactory(f, (X509TrustManager) f.getTrustManagers()[0]).build();
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
            service.onEndpointStateChanged(EndpointState.IDLE);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            service.onEndpointStateChanged(EndpointState.ERROR_CONFIGURATION);
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

    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    boolean sendMessage(Bundle b) {
        String body = b.getString(HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD);
        String url = b.getString(HTTP_BUNDLE_KEY_URL);
        String userInfo = b.getString(HTTP_BUNDLE_KEY_USERINFO);
        long messageId = b.getLong(HTTP_BUNDLE_KEY_MESSAGE_ID);

        Timber.v("url:%s, userInfo:%s, messageId:%s", url, userInfo,  messageId);

        if(body == null || url == null)
            return false;

        Request.Builder request = new Request.Builder().url(url).method("POST", RequestBody.create(JSON, body));

        if(userInfo != null) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
        } else if(Preferences.getAuth()) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString((Preferences.getUsername()+":"+Preferences.getPassword()).getBytes(), Base64.NO_WRAP));

        }

        if(headerUsername != null) {
            request.header(HEADER_USERNAME, headerUsername);
        }
        if(headerDevice != null) {
            request.header(HEADER_DEVICE, headerDevice);
        }


        try {
            Response r = mHttpClient.newCall(request.build()).execute();

            if((r != null) && (r.isSuccessful())) {
                Timber.v("got HTTP response");

                try {
                    //Timber.v("code: %s, streaming response to parser", r.body().string() );

                    MessageBase[] result = Parser.fromJson(r.body().byteStream());
                    ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.IDLE, "Response "+r.code() + ", " + result.length);

                    for (MessageBase aResult : result) {
                        onMessageReceived(aResult);
                    }

                    //Non JSON return value
                } catch (IOException e) {
                    ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "HTTP " +r.code() + ", JsonParseException");
                    Timber.e("error:JsonParseException responseCode:%s", r.code());
                    e.printStackTrace();                } catch (Parser.EncryptionException e) {

                    ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "Response: "+r.code() + ", EncryptionException");
                    Timber.e("error:EncryptionException");
                }
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, e);
            return false;
        }
    }

    @Deprecated
    public static int postMessage(final String body, @Nullable final String url, @Nullable final String userInfo, final Context c, final Long messageId) {
        Timber.v("url:%s, userInfo:%s, messageId:%s", url, userInfo,  messageId);

        if(url == null) {
            Timber.e("url not configured. messageId:%s", messageId);
            return GcmNetworkManager.RESULT_FAILURE;
        }

        if(c instanceof ServiceMessageHttpGcm && mHttpClient == null) {
            Timber.e("sevice not available. Binding and rescheduling GCM task");
            ServiceProxy.bind(App.getContext());
            return GcmNetworkManager.RESULT_RESCHEDULE;
        }



        Request.Builder request = new Request.Builder().url(url).method("POST", RequestBody.create(JSON, body));

         if(userInfo != null) {
            request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
        } else if(Preferences.getAuth()) {
             request.header(HEADER_AUTHORIZATION, "Basic " + android.util.Base64.encodeToString((Preferences.getUsername()+":"+Preferences.getPassword()).getBytes(), Base64.NO_WRAP));

         }
        
        if(headerUsername != null) {
            request.header(HEADER_USERNAME, headerUsername);
        }
        if(headerDevice != null) {
            request.header(HEADER_DEVICE, headerDevice);
        }

        try {
             Response r = mHttpClient.newCall(request.build()).execute();

             if((r != null) && (r.isSuccessful())) {
                 Timber.v("got HTTP response");

                 try {
                     //Timber.v("code: %s, streaming response to parser", r.body().string() );

                     MessageBase[] result = Parser.fromJson(r.body().byteStream());
                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.IDLE, "Response "+r.code() + ", " + result.length);

                     for (MessageBase aResult : result) {
                        onMessageReceived(aResult);
                    }

                //Non JSON return value
                } catch (IOException e) {
                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "HTTP " +r.code() + ", JsonParseException");
                    Timber.e("error:JsonParseException responseCode:%s", r.code());
e.printStackTrace();                } catch (Parser.EncryptionException e) {

                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "Response: "+r.code() + ", EncryptionException");
                     Timber.e("error:EncryptionException");
                 }
                 return onMessageDelivered(c, messageId);
            } else {
                return onMessageDeliveryFailed(messageId);
            }

        } catch (IOException e) {
            e.printStackTrace();
            ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, e);
            return onMessageDeliveryFailed(messageId);
        }
    }


    private static int onMessageDelivered(@NonNull Context c, @Nullable Long messageId) {
        if(messageId == null || messageId == 0) {
            Timber.e("messageId:null");
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        ServiceProxy.getServiceMessage().onMessageDelivered(messageId);
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private static int onMessageDeliveryFailed(Long messageId) {
        if(messageId == null || messageId == 0) {
            Timber.e("messageId:null");
        }

        return GcmNetworkManager.RESULT_FAILURE;
    }

    private static void onMessageReceived(@NonNull MessageBase message) {
        ServiceProxy.getServiceMessage().onMessageReceived(message);
    }



    Bundle httpMessageToBundle(MessageBase m) throws IOException, Parser.EncryptionException {
        Bundle b = new Bundle();
        b.putString(HTTP_BUNDLE_KEY_MESSAGE_PAYLOAD, Parser.toJson(m));
        b.putString(HTTP_BUNDLE_KEY_USERINFO, getInstance().endpointUserInfo);
        b.putString(HTTP_BUNDLE_KEY_URL, getInstance().endpointUrl);
        b.putLong(Dispatcher.BUNDLE_KEY_MESSAGE_ID, m.getMessageId());
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
    public void onAssociate(ServiceMessage service) {
        this.service = service;
    }

    @Override
    public void onDestroy() {

    }

    private void scheduleMessage(MessageBase m) {
        try {
            Bundle b = httpMessageToBundle(m);
            b.putString(Dispatcher.BUNDLE_KEY_ACTION, Dispatcher.TASK_SEND_MESSAGE_MQTT);
            App.getDispatcher().scheduleMessage(b);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Parser.EncryptionException e) {
            e.printStackTrace();
        }
    }
}
