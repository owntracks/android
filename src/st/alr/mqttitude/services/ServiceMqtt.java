
package st.alr.mqttitude.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import st.alr.mqttitude.R;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Defaults.State;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.MqttPublish;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class ServiceMqtt extends ServiceBindable implements MqttCallback
{


    private static State.ServiceMqtt state = State.ServiceMqtt.INITIAL;
    
    private short keepAliveSeconds;
    private String mqttClientId;
    private MqttClient mqttClient;
    private SharedPreferences sharedPreferences;
    private static ServiceMqtt instance;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private Thread workerThread;
    private LinkedList<DeferredPublishable> deferredPublishables;
    private Exception error;
    private HandlerThread pubThread;
    private Handler pubHandler;

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        workerThread = null;
        error = null;
        changeState(Defaults.State.ServiceMqtt.INITIAL);
        keepAliveSeconds = 15 * 60;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        deferredPublishables = new LinkedList<DeferredPublishable>();
        EventBus.getDefault().register(this);
        
        pubThread = new HandlerThread("MQTTPUBTHREAD");
        pubThread.start();
        pubHandler = new Handler(pubThread.getLooper());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        doStart(intent, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    private void doStart(final Intent intent, final int startId) {
        // init();

        Thread thread1 = new Thread() {
            @Override
            public void run() {
                handleStart(intent, startId);
                if (this == workerThread) // Clean up worker thread
                    workerThread = null;
            }

            @Override
            public void interrupt() {
                if (this == workerThread) // Clean up worker thread
                    workerThread = null;
                super.interrupt();
            }
        };
        thread1.start();
    }

    void handleStart(Intent intent, int startId) {
        Log.v(this.toString(), "handleStart");

 
        // Respect user's wish to stay disconnected. Overwrite with startId == -1 to reconnect manually afterwards
        if ((state == Defaults.State.ServiceMqtt.DISCONNECTED_USERDISCONNECT) && startId != -1) {
            Log.d(this.toString(), "handleStart: respecting user disconnect ");

            return;
        }

        if (isConnecting()) {
            Log.d(this.toString(), "handleStart: already connecting");
            return;
        }

        // Respect user's wish to not use data
        if (!isBackgroundDataEnabled()) {
            Log.e(this.toString(), "handleStart: !isBackgroundDataEnabled");
            changeState(Defaults.State.ServiceMqtt.DISCONNECTED_DATADISABLED);
            return;
        }

        // Don't do anything unless we're disconnected
        if (isDisconnected())
        {
            Log.v(this.toString(), "handleStart: !isConnected");
            // Check if there is a data connection
            if (isOnline(true))
            {
                if (connect())
                {
                    Log.v(this.toString(), "handleStart: connect sucessfull");
                    onConnect();
                }
            }
            else
            {
                Log.e(this.toString(), "handleStart: !isOnline");
                changeState(Defaults.State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET);
            }
        } else {
            Log.d(this.toString(), "handleStart: already connected");

        }
    }
    
    private boolean isDisconnected(){
        Log.v(this.toString(), "disconnect check: " + state);
        return state == Defaults.State.ServiceMqtt.INITIAL 
                || state == Defaults.State.ServiceMqtt.DISCONNECTED 
                || state == Defaults.State.ServiceMqtt.DISCONNECTED_USERDISCONNECT 
                || state == Defaults.State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET 
                || state == Defaults.State.ServiceMqtt.DISCONNECTED_ERROR;
    }

    
    /**
     * @category CONNECTION HANDLING
     */
    private void init()
    {
        Log.v(this.toString(), "initMqttClient");

        if (mqttClient != null) {
            return;
        }

        try
        {
            String brokerAddress = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_HOST,
                    Defaults.VALUE_BROKER_HOST);
            String brokerPort = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PORT,
                    Defaults.VALUE_BROKER_PORT);
            String prefix = getBrokerSecurityMode() == Defaults.VALUE_BROKER_SECURITY_NONE ? "tcp"
                    : "ssl";

            mqttClient = new MqttClient(prefix + "://" + brokerAddress + ":" + brokerPort,
                    getClientId(), null);
            mqttClient.setCallback(this);

        } catch (MqttException e)
        {
            // something went wrong!
            mqttClient = null;
            changeState(Defaults.State.ServiceMqtt.DISCONNECTED);
        }
    }

    private int getBrokerSecurityMode() {
        return sharedPreferences.getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY,
                Defaults.VALUE_BROKER_SECURITY_NONE);
    }

    //
    private javax.net.ssl.SSLSocketFactory getSSLSocketFactory() throws CertificateException,
            KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
        InputStream caInput = new BufferedInputStream(new FileInputStream(
                sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, "")));
        java.security.cert.Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    private boolean connect()
    {
        workerThread = Thread.currentThread(); // We connect, so we're the
                                               // worker thread
        Log.v(this.toString(), "connect");
        error = null; // clear previous error on connect
        init();

        try
        {
            changeState(Defaults.State.ServiceMqtt.CONNECTING);
            MqttConnectOptions options = new MqttConnectOptions();

            if (getBrokerSecurityMode() == Defaults.VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT)
                options.setSocketFactory(this.getSSLSocketFactory());

            if (!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").equals(""))
                options.setPassword(sharedPreferences.getString(
                        Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").toCharArray());

            if (!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, "").equals(""))
                options.setUserName(sharedPreferences.getString(
                        Defaults.SETTINGS_KEY_BROKER_USERNAME, ""));

            //setWill(options);
            options.setKeepAliveInterval(keepAliveSeconds);
            options.setConnectionTimeout(10);

            mqttClient.connect(options);

            Log.d(this.toString(), "No error during connect");
            changeState(Defaults.State.ServiceMqtt.CONNECTED);

            return true;

        } catch (Exception e) { // Catch paho and socket factory exceptions
            Log.e(this.toString(), e.toString());
            changeState(e);
            return false;
        }
    }

    private void setWill(MqttConnectOptions m) {
        StringBuffer payload = new StringBuffer();
        payload.append("{");
        payload.append("\"type\": ").append("\"").append("_lwt").append("\"");
        payload.append(", \"tst\": ").append("\"").append((int) (new Date().getTime() / 1000))
                .append("\"");
        payload.append("}");

        m.setWill(mqttClient.getTopic(sharedPreferences.getString(Defaults.SETTINGS_KEY_TOPIC,
                Defaults.VALUE_TOPIC)), payload.toString().getBytes(), 0, false);

    }

    private void onConnect() {

        if (!isConnected())
            Log.e(this.toString(), "onConnect: !isConnected");
    }

    public void disconnect(boolean fromUser)
    {
        Log.v(this.toString(), "disconnect");
        
        if(isConnecting()) // throws MqttException.REASON_CODE_CONNECT_IN_PROGRESS when disconnecting while connect is in progress. 
            return;
        
        if (fromUser)
            changeState(Defaults.State.ServiceMqtt.DISCONNECTED_USERDISCONNECT);

        try
        {
            if (isConnected())
                mqttClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mqttClient = null;

            if (workerThread != null) {
                workerThread.interrupt();
            }

        }
    }

    @SuppressLint("Wakelock")
    // Lint check derps with the wl.release() call.
    @Override
    public void connectionLost(Throwable t)
    {
        Log.e(this.toString(), "error: " + t.toString());
        // we protect against the phone switching off while we're doing this
        // by requesting a wake lock - we request the minimum possible wake
        // lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (!isOnline(true))
        {
            changeState(Defaults.State.ServiceMqtt.DISCONNECTED_WAITINGFORINTERNET);
        }
        else
        {
            changeState(Defaults.State.ServiceMqtt.DISCONNECTED);
        }
        wl.release();
    }

    public void reconnect() {
        disconnect(true);
        doStart(null, -1);
    }

    public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {

    }

    public void onEvent(Events.StateChanged.ServiceMqtt event) {
        if (event.getState() == Defaults.State.ServiceMqtt.CONNECTED)
            publishDeferrables();
    }

    private void changeState(Exception e) {
        error = e; 
        changeState(Defaults.State.ServiceMqtt.DISCONNECTED_ERROR, e);
    }

    private void changeState(Defaults.State.ServiceMqtt newState) {
        changeState(newState, null);
    }

    
    private void changeState(Defaults.State.ServiceMqtt newState, Exception e) {
        Log.d(this.toString(), "ServiceMqtt state changed to: " + newState);
        state = newState;
        EventBus.getDefault().postSticky(new Events.StateChanged.ServiceMqtt(newState, e));
    }

    private boolean isOnline(boolean shouldCheckIfOnWifi)
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null
                && netInfo.isAvailable()
                && netInfo.isConnected();
    }

    public boolean isConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }
    
    public static boolean isErrorState(Defaults.State.ServiceMqtt state) {
        return state == Defaults.State.ServiceMqtt.DISCONNECTED_ERROR;
    }
    
    public boolean hasError(){
        return error != null;
    }

    public boolean isConnecting() {
        return (mqttClient != null) && state == Defaults.State.ServiceMqtt.CONNECTING;
    }

    private boolean isBackgroundDataEnabled() {
        return isOnline(false);
    }

    /**
     * @category MISC
     */
    public static ServiceMqtt getInstance() {
        return instance;
    }

    private String getClientId()
    {
        if (mqttClientId == null)
        {
            mqttClientId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

            // MQTT specification doesn't allow client IDs longer than 23 chars
            if (mqttClientId.length() > 22)
                mqttClientId = mqttClientId.substring(0, 22);
        }

        return mqttClientId;
    }


    @Override
    public void onDestroy()
    {
        // disconnect immediately
        disconnect(false);

        changeState(Defaults.State.ServiceMqtt.DISCONNECTED);

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

        super.onDestroy();
    }


    public static Defaults.State.ServiceMqtt getState() {
        return state;
    }
    
    public static String getErrorMessage() {
        Exception e = getInstance().error;

        if(getInstance() != null && getInstance().hasError() && e.getCause() != null)
            return "Error: " + e.getCause().getLocalizedMessage();
        else
            return "Error: " + getInstance().getString(R.string.na);

    }
    
    public static String getStateAsString(){
        return Defaults.State.toString(state);
    }
    
    public static String stateAsString(Defaults.State.ServiceLocator state) {
        return Defaults.State.toString(state);
    }

    private void deferPublish(final DeferredPublishable p) {
        p.wait(deferredPublishables, new Runnable() {

            @Override
            public void run() {
                deferredPublishables.remove(p);
                if(!p.isPublishing())//might happen that the publish is in progress while the timeout occurs.
                    p.publishFailed();
            }
        });
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, false, 0, 0, null, null);
    }

    public void publish(String topic, String payload, boolean retained) {
        publish(topic, payload, retained, 0, 0, null, null);
    }

    public void publish(final String topic, final String payload, final boolean retained, final int qos, final int timeout,
            final MqttPublish callback, final Object extra) {
        
        
                      publish(new DeferredPublishable(topic, payload, retained, qos, timeout, callback, extra));
                
    }

    private void publish(final DeferredPublishable p) {
  
        
        pubHandler.post(new Runnable() {
            
            @Override
            public void run() {

        if(Looper.getMainLooper().getThread() == Thread.currentThread()){
            Log.e(this.toString(), "PUB ON MAIN THREAD");
        }
        
        
        if (!isOnline(false) || !isConnected()) {
            Log.d(this.toString(), "pub deferred");
            
            deferPublish(p);
            doStart(null, 1);
            return;
        }

        try
        {
            p.publishing();
            mqttClient.getTopic(p.getTopic()).publish(p);
            p.publishSuccessfull();
        } catch (MqttException e)
        {
            Log.e(this.toString(), e.getMessage());
            e.printStackTrace();
            p.cancelWait();
            p.publishFailed();
        }
            }
        });

    }

    private void publishDeferrables() {        
        for (Iterator<DeferredPublishable> iter = deferredPublishables.iterator(); iter.hasNext(); ) {
            DeferredPublishable p = iter.next();
            iter.remove();
            publish(p);
        }
    }

    private class DeferredPublishable extends MqttMessage {
        private Handler timeoutHandler;
        private MqttPublish callback;
        private String topic;
        private int timeout = 0;
        private boolean isPublishing;
        private Object extra;
        
        public DeferredPublishable(String topic, String payload, boolean retained, int qos,
                int timeout, MqttPublish callback, Object extra) {
            
            super(payload.getBytes());
            this.setQos(qos);
            this.setRetained(retained);
            this.extra = extra;
            this.callback = callback;
            this.topic = topic;
            this.timeout = timeout;
        }

        public void publishFailed() {
            if (callback != null)
                callback.publishFailed(extra);
        }

        public void publishSuccessfull() {
            if (callback != null)
                callback.publishSuccessfull(extra);
            cancelWait();

        }

        public void publishing() {
            isPublishing = true;
            if (callback != null)
                callback.publishing(extra);
        }
        
        public boolean isPublishing(){
            return isPublishing;
        }

        public String getTopic() {
            return topic;
        }
        
        public void cancelWait(){
            if(timeoutHandler != null)
                this.timeoutHandler.removeCallbacksAndMessages(this);
        }

        public void wait(LinkedList<DeferredPublishable> queue, Runnable onRemove) {
            if (timeoutHandler != null) {
                Log.d(this.toString(), "This DeferredPublishable already has a timeout set");
                return;
            }

            // No need signal waiting for timeouts of 0. The command will be
            // failed right away
            if (callback != null && timeout > 0)
                callback.publishWaiting(extra);

            queue.addLast(this);
            this.timeoutHandler = new Handler();
            this.timeoutHandler.postDelayed(onRemove, timeout * 1000);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {}

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    @Override
    protected void onStartOnce() {}

}
