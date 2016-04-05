package org.owntracks.android.services;

import android.content.Intent;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.owntracks.android.messages.Message;
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
import org.owntracks.android.support.receiver.Parser;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServiceMessageHttp implements ProxyableService, OutgoingMessageProcessor, RejectedExecutionHandler, ServiceMessageEndpoint {

    private static final String TAG = "ServiceMessageHttp";
    private static final String METHOD_POST = "POST";
    private URL endpointUrl;
    private PausableThreadPoolExecutor pubPool;
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;

    @Override
    public void onCreate(ServiceProxy c) {
        Log.v(TAG, "loaded HTTP backend");
        endpointUrl = getEndpointUrl();

        initPausedPubPool();

    }

    @Override
    public void setMessageSenderCallback(MessageSender callback) {
        this.messageSender = callback;
    }

    @Override
    public void setMessageReceiverCallback(MessageReceiver callback) {
        this.messageReceiver = callback;
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
        pubPool.resume(); // pause until client is setup and connected
    }



    @Override
    public void onEvent(Events.Dummy event) {

    }


    private void postMessage(MessageBase message) {
        MessageBase mm;
        Log.v(TAG, "publishMessage: " + message + ", q size: " + pubPool.getQueue().size());
        try {

            if(EncryptionProvider.isPayloadEncryptionEnabled() && !(message instanceof MessageEncrypted)) {
            mm = new MessageEncrypted();
                ((MessageEncrypted)mm).setdata(EncryptionProvider.encrypt(Parser.serializeSync(message)));
            } else {
                mm = message;
            }


            postMessage(Parser.serializeSync(mm).getBytes());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void postMessage(byte[] message) {

        HttpURLConnection urlConnection = null;
        try {
            // create connection
            urlConnection = (HttpURLConnection) endpointUrl.openConnection();
            urlConnection.setRequestMethod(METHOD_POST);

            urlConnection.setRequestProperty( "Content-Type", "application/json");
            urlConnection.setRequestProperty( "charset", "utf-8");
            urlConnection.setRequestProperty( "Content-Length", Integer.toString( message.length ));
            urlConnection.setUseCaches( false );

            urlConnection.getOutputStream().write(message);

            int statusCode = urlConnection.getResponseCode();
            Log.e(TAG,"HttpURLConnection: statusCode - " + statusCode);


            //TODO: parse returned message
            //InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            //return new JSONObject(getResponseText(in));

        } catch (MalformedURLException e) {

        } catch (SocketTimeoutException e) {
            //TODO: queue message
        } catch (IOException e) {
            // could not read response body
            // (could not create input stream)
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static String getResponseText(InputStream inStream) {
        // very nice trick from
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        return new Scanner(inStream).useDelimiter("\\A").next();
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


    public URL getEndpointUrl() {
        try {
            Log.v(TAG, "getEndpointUrl() - " +Preferences.getUrl() );
            return new URL(Preferences.getUrl());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
