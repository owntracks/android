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

public class ServiceMessageHttp implements StatelessMessageEndpoint, OutgoingMessageProcessor {
    // Headers according to https://github.com/owntracks/recorder#http-mode
    private static final String HEADER_USERNAME = "X-Limit-U";
    private static final String HEADER_DEVICE = "X-Limit-D";

    private static final String HEADER_AUTHORIZATION = "Authorization";
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
        this.context = c;
        this.mOutgoingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        powerManager = PowerManager.class.cast(context.getSystemService(Context.POWER_SERVICE));
        connectivityManager =  ConnectivityManager.class.cast(context.getSystemService(Context.CONNECTIVITY_SERVICE));

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

    @Override
    public boolean isReady() {
        return mHttpClient != null;
    }

    @Override
    public void probe() {
        Timber.d("endpointUrl:%s, httpClient:%s", this.endpointUrl, mHttpClient);
    }


    @Override
    public void onDestroy() {
        //TODO: unregister preferences change listener
    }

    @Override
    public void onStartCommand(Intent intent) {

    }

    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    private void postMessage(MessageBase message) {

        try {

            String wireMessage = Parser.toJson(message);

            Timber.d("outgoing message:%s", wireMessage);
            boolean idleMode =  false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                idleMode = powerManager.isDeviceIdleMode();
            }

            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            boolean networkAvailable = netInfo != null && netInfo.isConnected();

            // If the device is in idle mode (Doze), send the message via GCM Network Manager. The message is automatically send during a maintenance period
            // If the device is not in idle mode and network is available, send the message directly. If we already tried to send it directly before (TTL == 0), send it via GCM network manager
            // If the device is not in idle mode but no network is available, send the message via GCM Network Manager.  The message is automatically send when a connection is available.

            if(idleMode) {
                Timber.v("messageId:%s, strategy:indirect, reason:idle", message.getMessageId());
                prepareAndPostIndirect(wireMessage, message);

            } else if (networkAvailable && message.getOutgoingTTL() > 0 && Preferences.getHttpSchedulerAllowDirectStrategy()){
                Timber.v("messageId:%s, strategy:direct", message.getMessageId());
                prepareAndPostDirect(wireMessage, message);


            } else {
                Timber.v("messageId:%s, strategy:indirect, reason:network_fail/ttl_fail/no_override", message.getMessageId());
                prepareAndPostIndirect(wireMessage, message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            service.onMessageDeliveryFailed(message.getMessageId());

        }
    }

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

             //We got a response, treat as delivered successful
             if(r != null ) {
                 Timber.v("got HTTP response");
                 try {
                     Timber.v("code: %s, streaming response to parser", r.code() );

                     MessageBase[] result = Parser.fromJson(r.body().byteStream());
                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.IDLE, "Response "+r.code() + ", " + result.length);

                     for (MessageBase aResult : result) {
                        onMessageReceived(aResult);
                    }

                //Non JSON return value
                } catch (IOException e) {
                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "HTTP " +r.code() + ", JsonParseException");
                    Timber.e("error:JsonParseException responseCode:%s", r.code());
                } catch (Parser.EncryptionException e) {
                     ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, "Response: "+r.code() + ", EncryptionException");
                     Timber.e("error:EncryptionException");
                 }
                 return onMessageDelivered(c, messageId);
            } else {
                return onMessageDeliveryFailed(c, messageId);
            }

        } catch (IOException e) {
            e.printStackTrace();
            ServiceProxy.getServiceMessage().onEndpointStateChanged(EndpointState.ERROR, e);
            return onMessageDeliveryFailed(c, messageId);
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

    private static int onMessageDeliveryFailed(@NonNull Context c, Long messageId) {
        if(messageId == null || messageId == 0) {
            Timber.e("messageId:null");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        //GCM Network Manager will automatically retry sending if the message
        if(c instanceof ServiceMessageHttpGcm) {
            return GcmNetworkManager.RESULT_RESCHEDULE;
        } else {
            ServiceProxy.getServiceMessage().onMessageDeliveryFailed(messageId);
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    private static void onMessageReceived(@NonNull MessageBase message) {
        ServiceProxy.getServiceMessage().onMessageReceived(message);
    }


    private boolean prepareAndPostDirect(String wireMessage, @NonNull MessageBase message) {
        Timber.v("messageId:%s", message.getMessageId());
        postMessage(wireMessage, this.endpointUrl, this.endpointUserInfo, context, message.getMessageId());
        return true;
    }

    private boolean prepareAndPostIndirect(String wireMessage, @NonNull MessageBase message) {
        Timber.v("messageId:%s", message.getMessageId());
        Bundle b = new Bundle();

        b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_USERINFO, this.endpointUserInfo);
        b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_URL, this.endpointUrl);
        b.putLong(ServiceMessageHttpGcm.BUNDLE_KEY_MESSAGE_ID, message.getMessageId());
        b.putString(ServiceMessageHttpGcm.BUNDLE_KEY_REQUEST_BODY, wireMessage);

        Task task = new OneoffTask.Builder()
                .setService(ServiceMessageHttpGcm.class)
                .setTag("owntracks_mid_"+message.getMessageId())
                .setExecutionWindow(0, 5)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setExtras(b)
                .setUpdateCurrent(false)
                .setPersisted(false)
                .setRequiresCharging(false)
                .build();
        GcmNetworkManager.getInstance(context).schedule(task);
        return true;
    }

    @Override
    public void processOutgoingMessage(MessageBase message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageCmd message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageEvent message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageLocation message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageTransition message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoint message) {
        postMessage(message);
    }

    @Override
    public void processOutgoingMessage(MessageWaypoints message) {
        postMessage(message);
    }

    @Override
    public void onSetService(ServiceMessage service) {
        this.service = service;

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

    @Override
    public boolean sendMessage(MessageBase message) {
        message.setOutgoingProcessor(this);
        this.mOutgoingMessageProcessorExecutor.execute(message);
        return true;
    }
}
