package org.owntracks.android.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.tool.util.L;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Base64;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;

import org.antlr.v4.runtime.misc.NotNull;
import org.owntracks.android.R;
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
import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;
import org.owntracks.android.support.receiver.Parser;
import org.owntracks.android.services.ServiceMessage.EndpointState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class ServiceMessageHttp implements ProxyableService, OutgoingMessageProcessor, RejectedExecutionHandler, StatelessMessageEndpoint {

    private static final String TAG = "ServiceMessageHttp";
    private static final String METHOD_POST = "POST";
    private String endpointUrl;
    private String endpointUserInfo;


    private MessageSender messageSender;
    private MessageReceiver messageReceiver;
    private ServiceProxy context;
    private Exception error;
    private static OkHttpClient mHttpClient;
    public static final MediaType JSON  = MediaType.parse("application/json; charset=utf-8");
    private ThreadPoolExecutor mOutgoingMessageProcessorExecutor;
    private PowerManager powerManager;
    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate(ServiceProxy c) {
        Timber.v("loaded HTTP backend");
        this.context = c;

        this.mOutgoingMessageProcessorExecutor = new ThreadPoolExecutor(2,2,1,  TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());

        powerManager = PowerManager.class.cast(context.getSystemService(Context.POWER_SERVICE));
        connectivityManager =  ConnectivityManager.class.cast(context.getSystemService(Context.CONNECTIVITY_SERVICE));

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
            }
        });

        loadEndpointUrl();
        loadHTTPClient();
    }

    private void loadHTTPClient() {


        //  ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)//
//                .tlsVersions(TlsVersion.TLS_1_2)
        //              .build();
        // mHttpClient.setConnectionSpecs(Collections.singletonList(spec));

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

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }





    }


    private void loadEndpointUrl() {
        URL endpoint = null;
        try {
            endpoint = new URL(Preferences.getUrl());
            changeState(EndpointState.IDLE, null);
        } catch (MalformedURLException e) {
            changeState(EndpointState.DISCONNECTED_CONFIGINCOMPLETE, null);
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
    public void setMessageSenderCallback(MessageSender callback) {
        this.messageSender = callback;
    }

    @Override
    public void setMessageReceiverCallback(MessageReceiver callback) {
        this.messageReceiver = callback;
    }


    private static EndpointState state = EndpointState.IDLE;


    public static EndpointState getState() {
        return state;
    }

    @Override
    public String getConnectionState() {
        int id;
        switch (getState()) {
            case IDLE:
                id = R.string.connectivityIdle;
                break;
            case CONNECTED:
                id = R.string.connectivityConnected;
                break;
            case CONNECTING:
                id = R.string.connectivityConnecting;
                break;
            case DISCONNECTED_DATADISABLED:
                id = R.string.connectivityDisconnectedDataDisabled;
                break;
            case DISCONNECTED_ERROR:
                id = R.string.error;
                break;
            case DISCONNECTED_CONFIGINCOMPLETE:
                id = R.string.connectivityDisconnectedConfigIncomplete;
                break;
            default:
                id = R.string.connectivityDisconnected;

        }
        return context.getString(id);
    }


    private static void setLastState(String message) {
        StatisticsProvider.setString(StatisticsProvider.BACKEND_LAST_MESSAGE, message);
        StatisticsProvider.setTime(StatisticsProvider.BACKEND_LAST_MESSAGE_TST);

    }


    @Override
    public void onDestroy() {

    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {

    }

    @Override
    public void onEvent(Events.Dummy event) {

    }



    private void postMessage(MessageBase message) {

        try {

            String wireMessage = Parser.serializeSync(message);

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
                setLastState("TX1:queued/1");
                Timber.v("messageId:%s, strategy:indirect, reason:idle", message.getMessageId());
                prepareAndPostIndirect(wireMessage, message);

            } else if (networkAvailable && message.getOutgoingTTL() > 0 && Preferences.getHttpSchedulerAllowDirectStrategy()){
                setLastState("TX1:queued/2");
                Timber.v("messageId:%s, strategy:direct", message.getMessageId());
                prepareAndPostDirect(wireMessage, message);


            } else {
                setLastState("TX1:queued/3");
                Timber.v("messageId:%s, strategy:indirect, reason:network_fail/ttl_fail/no_override", message.getMessageId());
                prepareAndPostIndirect(wireMessage, message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            setLastState("TX1: "+ e.getClass().getSimpleName());

            messageSender.onMessageDeliveryFailed(message.getMessageId());
        }

    }


    public static int postMessage(String body, String url, @Nullable String userInfo, Context c, Long messageId) {
        Timber.v("url:%s, userInfo:%s, messageId:%s", url, userInfo,  messageId);

        Request.Builder request = new Request.Builder().url(url).method("POST", RequestBody.create(JSON, body));

        if(userInfo != null) {
            request.header("Authorization", "Basic " + android.util.Base64.encodeToString(userInfo.getBytes(), Base64.NO_WRAP));
        }

        try {
             Response r = mHttpClient.newCall(request.build()).execute();

             //We got a response, treat as delivered successful
             if(r != null ) {

                 try {
                    MessageBase[] result = Parser.deserializeSyncArray(r.body().byteStream());
                     setLastState("TX:"+r.code() + ", RX:" + result.length);

                     for (MessageBase aResult : result) {
                        onMessageReceived(aResult);
                    }

                //Non JSON return value
                } catch (IOException e) {
                    setLastState("TX2:"+r.code() + ", RX:JsonParseException");
                    Timber.e("error:JsonParseException responseCode:%s", r.code());
                }
                return onMessageDelivered(c, messageId);
            } else {
                return onMessageDeliveryFailed(c, messageId);
            }

        } catch (IOException e) {
            e.printStackTrace();
            setLastState("TX2:IOException");

            return onMessageDeliveryFailed(c, messageId);
        }
    }

    private static int onMessageDelivered(@NotNull Context c, @Nullable Long messageId) {
        if(messageId == null || messageId == 0) {
            Timber.e("messageId:null");
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        ServiceProxy.getServiceMessageHttp().messageSender.onMessageDelivered(messageId);
        return GcmNetworkManager.RESULT_SUCCESS;
    }

    private static int onMessageDeliveryFailed(@NotNull Context c, Long messageId) {
        if(messageId == null || messageId == 0) {
            Timber.e("messageId:null");
            return GcmNetworkManager.RESULT_FAILURE;
        }

        //GCM Network Manager will automatically retry sending if the message
        if(c instanceof ServiceMessageHttpGcm) {
            return GcmNetworkManager.RESULT_RESCHEDULE;
        } else {
            ServiceProxy.getServiceMessageHttp().messageSender.onMessageDeliveryFailed(messageId);
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }


    private static void onMessageReceived(@NotNull MessageBase message) {
        ServiceProxy.getServiceMessageHttp().messageReceiver.onMessageReceived(message);
    }




    private boolean prepareAndPostDirect(String wireMessage, @NotNull MessageBase message) {
        Timber.v("messageId:%s", message.getMessageId());

        postMessage(wireMessage, this.endpointUrl, this.endpointUserInfo, context, message.getMessageId());
        return true;
    }


    private boolean prepareAndPostIndirect(String wireMessage, @NotNull MessageBase message) {
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
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

    }

    @Override
    public boolean sendMessage(MessageBase message) {
        message.setOutgoingProcessor(this);
        this.mOutgoingMessageProcessorExecutor.execute(message);
        return true;
    }

    private void changeState(Exception e) {
        error = e;
        changeState(EndpointState.DISCONNECTED_ERROR, e);
    }

    private void changeState(EndpointState newState, Exception e) {
        state = newState;
        EventBus.getDefault().postSticky(new Events.EndpointStateChanged(newState, e));
    }



}
