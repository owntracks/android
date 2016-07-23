package org.owntracks.android.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.owntracks.android.R;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageCmd;
import org.owntracks.android.messages.MessageEncrypted;
import org.owntracks.android.messages.MessageEvent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.EncryptionProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StatisticsProvider;
import org.owntracks.android.support.interfaces.MessageReceiver;
import org.owntracks.android.support.interfaces.MessageSender;
import org.owntracks.android.support.interfaces.ServiceMessageEndpoint;
import org.owntracks.android.support.interfaces.StatelessMessageEndpoint;
import org.owntracks.android.support.receiver.Parser;
import org.owntracks.android.services.ServiceMessage.EndpointState;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class ServiceMessageHttp implements ProxyableService, OutgoingMessageProcessor, RejectedExecutionHandler, StatelessMessageEndpoint {

    private static final String TAG = "ServiceMessageHttp";
    private static final String METHOD_POST = "POST";
    private URL endpointUrl;
    private PausableThreadPoolExecutor pubPool;
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;
    private ServiceProxy context;
    private Exception error;

    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "loaded HTTP backend");
        this.context = c;

        initPausedPubPool();
        Preferences.registerOnPreferenceChangedListener(new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {

            }

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(Preferences.Keys.URL.equals(key))
                    loadEndpointUrl();
            }
        });

        loadEndpointUrl();
    }

    private void loadEndpointUrl() {
        Log.v(TAG, "loadEndpointUrl()");
        this.endpointUrl = getEndpointUrl();

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
    public String getStateAsString() {
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



    @Override
    public void onDestroy() {

    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {

    }
    private void initPausedPubPool() {
        Log.v(TAG, "Executor initPausedPubPool with new paused queue");
        if(pubPool != null && !pubPool.isShutdown()) {
            Log.v(TAG, "Executor shutting down existing executor " + pubPool);
            pubPool.shutdownNow();
        }
        this.pubPool = new PausableThreadPoolExecutor(1,1,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
        this.pubPool.setRejectedExecutionHandler(this);
        Log.v(TAG, "Executor created new executor instance: " + pubPool);
        pubPool.resume();
    }



    @Override
    public void onEvent(Events.Dummy event) {

    }


    private void postMessage(MessageBase message) {
        Log.v(TAG, "publishMessage: " + message + ", q size: " + pubPool.getQueue().size());
        try {
            if(postMessage(Parser.serializeSync(message).getBytes()))
                messageSender.onMessageDelivered(message);
        } catch (SocketTimeoutException e) {
            pubPool.requeue(message);
        } catch (Parser.EncryptionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HttpURLConnection setUrlConnectionProperties(HttpURLConnection urlConnection, int messageLenght) throws ProtocolException {
        urlConnection.setRequestMethod(METHOD_POST);
        urlConnection.setRequestProperty( "Content-Type", "application/json");
        urlConnection.setRequestProperty( "charset", "utf-8");
        urlConnection.setRequestProperty( "Content-Length", Integer.toString( messageLenght ));
        urlConnection.setFixedLengthStreamingMode(messageLenght);
        urlConnection.setUseCaches( false );

        if (urlConnection.getURL().getUserInfo() != null) {
            urlConnection.setRequestProperty("Authorization", "Basic " + android.util.Base64.encodeToString(urlConnection.getURL().getUserInfo().getBytes(), Base64.NO_WRAP));
        }
        return urlConnection;
    }

    private boolean postMessage(byte[] message) throws SocketTimeoutException{
        boolean r = false;
        if(endpointUrl == null) {
            changeState(EndpointState.DISCONNECTED_CONFIGINCOMPLETE, null);
            return r;
        }

        HttpURLConnection urlConnection = null;
        try {
            // create connection
            urlConnection = (HttpURLConnection) endpointUrl.openConnection();

            setUrlConnectionProperties(urlConnection, message.length);
            boolean redirect = false;
            urlConnection.getOutputStream().write(message);


            // normally, 3xx is redirect
            int status = urlConnection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                    redirect = true;
            }

            Log.e(TAG,"HttpURLConnection: statusCode - " + status);

            if (redirect) {
                // get redirect url from "location" header field
                String newUrl = urlConnection.getHeaderField("Location");

                // open the new connnection again
                urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                setUrlConnectionProperties(urlConnection, message.length);
                urlConnection.getOutputStream().write(message);
                status = urlConnection.getResponseCode();
                Log.d(TAG,"redirect: statusCode - " + status + " newUrl:"+newUrl);
                Log.d(TAG,"statusCode for new session - " + status);

            }

            InputStream is;

            if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
                is = urlConnection.getInputStream();
            } else {
                is = urlConnection.getErrorStream();
            }

            try {
                MessageBase[] result = Parser.deserializeSyncArray(is);
                Log.v(TAG, "deserialized messages: " + result.length);
                for (MessageBase aResult : result) {
                    Log.v(TAG, "deserialized message: " + message);
                    messageReceiver.onMessageReceived(aResult);
                }


            }catch (IOException e) {
                Log.e(TAG, "result message could not be serialized");
                e.printStackTrace();
            }


            r = true;

        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return r;
    }

    @Override
    public void processMessage(MessageBase message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageCmd message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageEvent message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageLocation message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageTransition message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageWaypoint message) {
        postMessage(message);
    }

    @Override
    public void processMessage(MessageWaypoints message) {
        postMessage(message);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

    }

    @Override
    public void sendMessage(MessageBase message) {
        Log.v(TAG, "sendMessage base: " + message + " " + message.getClass());


        message.setOutgoingProcessor(this);
        Log.v(TAG, "enqueueing message to pubPool. running: " + pubPool.isRunning() + ", q size:" + pubPool.getQueue().size());
        StatisticsProvider.setInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH, pubPool.getQueueLength());

        this.pubPool.queue(message);
        this.messageSender.onMessageQueued(message);
    }

    private void changeState(Exception e) {
        error = e;
        changeState(EndpointState.DISCONNECTED_ERROR, e);
    }

    private void changeState(EndpointState newState, Exception e) {
        //Log.d(TAG, "ServiceMessageMqtt state changed to: " + newState);
        state = newState;
        EventBus.getDefault().postSticky(new Events.EndpointStateChanged(newState, e));
    }


    public URL getEndpointUrl() {
        try {
            Log.v(TAG, "getEndpointUrl() - " +Preferences.getUrl() );
            changeState(EndpointState.IDLE, null);
            return new URL(Preferences.getUrl());
        } catch (MalformedURLException e) {
            changeState(EndpointState.DISCONNECTED_CONFIGINCOMPLETE, null);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void test() {

    }
}
