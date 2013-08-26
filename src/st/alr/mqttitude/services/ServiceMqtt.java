
package st.alr.mqttitude.services;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.MqttPublish;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class ServiceMqtt extends Service implements MqttCallback
{

    public static enum MQTT_CONNECTIVITY {
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED, DISCONNECTED_ERROR
    }

    private static MQTT_CONNECTIVITY mqttConnectivity = MQTT_CONNECTIVITY.DISCONNECTED;
    private short keepAliveSeconds;
    private String mqttClientId;
    private MqttClient mqttClient;
    private static SharedPreferences sharedPreferences;
    private static ServiceMqtt instance;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private LocalBinder<ServiceMqtt> mBinder;
    private Thread workerThread;
    private LinkedList<DeferredPublishable> deferredPublishables;
    private static MqttException error;

    // An alarm for rising in special times to fire the
    // pendingIntentPositioning
    private AlarmManager alarmManagerPositioning;
    // A PendingIntent for calling a receiver in special times
    public PendingIntent pendingIntentPositioning;

    /**
     * @category SERVICE HANDLING
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        workerThread = null;
        error = null;
        changeMqttConnectivity(MQTT_CONNECTIVITY.INITIAL);
        mBinder = new LocalBinder<ServiceMqtt>(this);
        keepAliveSeconds = 15 * 60;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        deferredPublishables = new LinkedList<DeferredPublishable>();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        doStart(intent, startId);
        return START_STICKY;
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
        if ((mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT) && startId != -1) {
            return;
        }

        // No need to connect if we're already connecting
        if (isConnecting()) {
            return;
        }

        // Respect user's wish to not use data
        if (!isBackgroundDataEnabled()) {
            Log.e(this.toString(), "handleStart: !isBackgroundDataEnabled");
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_DATADISABLED);
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
                    Log.v(this.toString(), "handleStart: connectToBroker() == true");
                    onConnect();
                }
            }
            else
            {
                Log.e(this.toString(), "handleStart: !isOnline");
                changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET);
            }
        } else {
            Log.d(this.toString(), "handleStart: not disconnected");

        }
    }
    
    private boolean isDisconnected(){
        return mqttConnectivity == MQTT_CONNECTIVITY.INITIAL || mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED || mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT || mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET || mqttConnectivity == MQTT_CONNECTIVITY.DISCONNECTED_ERROR;
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
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
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
        Log.v(this.toString(), "connectToBroker");

        init();

        try
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTING);
            MqttConnectOptions options = new MqttConnectOptions();

            if (getBrokerSecurityMode() == Defaults.VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT)
                options.setSocketFactory(this.getSSLSocketFactory());

            if (!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").equals(""))
                options.setPassword(sharedPreferences.getString(
                        Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").toCharArray());

            if (!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, "").equals(""))
                options.setUserName(sharedPreferences.getString(
                        Defaults.SETTINGS_KEY_BROKER_USERNAME, ""));

            setWill(options);
            options.setKeepAliveInterval(keepAliveSeconds);
            options.setConnectionTimeout(10);

            mqttClient.connect(options);

            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTED);

            return true;

        } catch (MqttException e) { // Catch paho and socket factory exceptions
            Log.e(this.toString(), e.toString());
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_ERROR, e);
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
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

        if (!isConnected()) {
            Log.e(this.toString(), "onConnect: !isConnected");
        }
    }

    public void disconnect(boolean fromUser)
    {
        Log.v(this.toString(), "disconnect");
        if (fromUser)
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_USERDISCONNECT);

        try
        {
            if (mqttClient != null && mqttClient.isConnected())
            {
                mqttClient.disconnect();
            }
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
        // we protect against the phone switching off while we're doing this
        // by requesting a wake lock - we request the minimum possible wake
        // lock - just enough to keep the CPU running until we've finished
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
        wl.acquire();

        if (!isOnline(true))
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED_WAITINGFORINTERNET);
        }
        else
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
        }
        wl.release();
    }

    public void reconnect() {
        disconnect(true);
        doStart(null, -1);
    }

    @Override
    public void messageArrived(MqttTopic topic, MqttMessage message) throws MqttException {

    }

    // private boolean publish(String topicStr, String payload, boolean
    // retained, int qos) {
    // boolean isOnline = isOnline(false);
    // boolean isConnected = isConnected();
    //
    // if (!isOnline || !isConnected) {
    // return false;
    // }
    // MqttMessage message = new MqttMessage(payload.getBytes());
    // message.setQos(qos);
    // message.setRetained(retained);
    //
    // try
    // {
    // mqttClient.getTopic(topicStr).publish(message);
    // return true;
    // } catch (MqttException e)
    // {
    // Log.e(this.toString(), e.getMessage());
    // e.printStackTrace();
    // return false;
    // }
    // }

    public void onEvent(Events.MqttConnectivityChanged event) {
        mqttConnectivity = event.getConnectivity();

        if (event.getConnectivity() == MQTT_CONNECTIVITY.CONNECTED)
            publishDeferrables();

    }

    /**
     * @category CONNECTIVITY STATUS
     */
    private void changeMqttConnectivity(MQTT_CONNECTIVITY newConnectivity, MqttException e) {
        error = e; 
        changeMqttConnectivity(newConnectivity);
    }
    
    private void changeMqttConnectivity(MQTT_CONNECTIVITY newConnectivity) {
        EventBus.getDefault().post(new Events.MqttConnectivityChanged(newConnectivity));
        mqttConnectivity = newConnectivity;
    }

    private boolean isOnline(boolean shouldCheckIfOnWifi)
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return netInfo != null
                // && (!shouldCheckIfOnWifi || (netInfo.getType() ==
                // ConnectivityManager.TYPE_WIFI))
                && netInfo.isAvailable()
                && netInfo.isConnected();
    }

    public boolean isConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
    }
    
    public static boolean isErrorState(MQTT_CONNECTIVITY c) {
        return c == MQTT_CONNECTIVITY.DISCONNECTED_ERROR;
    }
    
    public static boolean hasError(){
        return error != null;
    }

    public boolean isConnecting() {
        return (mqttClient != null) && mqttConnectivity == MQTT_CONNECTIVITY.CONNECTING;
    }

    private boolean isBackgroundDataEnabled() {
        return isOnline(false);
    }

    public static MQTT_CONNECTIVITY getConnectivity() {
        return mqttConnectivity;
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
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public class LocalBinder<T> extends Binder
    {
        private WeakReference<ServiceMqtt> mService;

        public LocalBinder(ServiceMqtt service) {
            mService = new WeakReference<ServiceMqtt>(service);
        }

        public ServiceMqtt getService() {
            return mService.get();
        }

        public void close() {
            mService = null;
        }
    }

    @Override
    public void onDestroy()
    {
        // disconnect immediately
        disconnect(false);

        changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);

        if (mBinder != null) {
            mBinder.close();
            mBinder = null;
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

        if (this.alarmManagerPositioning != null)
            this.alarmManagerPositioning.cancel(pendingIntentPositioning);

        super.onDestroy();
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) {
    }


    public static String getConnectivityText() {
        MQTT_CONNECTIVITY c = getConnectivity();
        if(isErrorState(c) && hasError())
            return error.toString();
        
        switch (c) {
            case CONNECTED:
                return App.getInstance().getString(R.string.connectivityConnected);
            case CONNECTING:
                return App.getInstance().getString(R.string.connectivityConnecting);
            case DISCONNECTING:
                return App.getInstance().getString(R.string.connectivityDisconnecting);
            default:
                return App.getInstance().getString(R.string.connectivityDisconnected);
        }

    }
    
    private void deferPublish(final DeferredPublishable p) {
        p.wait(deferredPublishables, new Runnable() {

            @Override
            public void run() {
                deferredPublishables.remove(p);
                if(!p.isPublishing())//might haben that the publish is in progress while the timeout occurs.
                    p.publishFailed();
            }
        });
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, false, 0, 0, null);
    }

    public void publish(String topic, String payload, boolean retained) {
        publish(topic, payload, retained, 0, 0, null);
    }

    public void publish(String topic, String payload, boolean retained, int qos, int timeout,
            MqttPublish callback) {
        publish(new DeferredPublishable(topic, payload, retained, qos, timeout, callback));
    }

    private void publish(DeferredPublishable p) {
        boolean isOnline = isOnline(false);
        boolean isConnected = isConnected();

        if (!isOnline || !isConnected) {
            deferPublish(p);
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

        public DeferredPublishable(String topic, String payload, boolean retained, int qos,
                int timeout, MqttPublish callback) {
            super(payload.getBytes());
            this.setQos(qos);
            this.setRetained(retained);

            this.callback = callback;
            this.topic = topic;
            this.timeout = timeout;
        }

        public void publishFailed() {
            if (callback != null)
                callback.publishFailed();
        }

        public void publishSuccessfull() {
            if (callback != null)
                callback.publishSuccessfull();
            cancelWait();

        }

        public void publishing() {
            isPublishing = true;
            if (callback != null)
                callback.publishing();
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
                callback.publishWaiting();

            queue.addLast(this);
            this.timeoutHandler = new Handler();
            this.timeoutHandler.postDelayed(onRemove, timeout * 1000);
        }
    }

}
