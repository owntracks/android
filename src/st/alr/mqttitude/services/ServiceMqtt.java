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
//import st.alr.mqttitude.support.OnNewLocationListener;
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
        INITIAL, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED_WAITINGFORINTERNET, DISCONNECTED_USERDISCONNECT, DISCONNECTED_DATADISABLED, DISCONNECTED
    }
    
    private static final int NOTIFCATION_ID = 1337;

    private static MQTT_CONNECTIVITY mqttConnectivity = MQTT_CONNECTIVITY.DISCONNECTED;
    private short keepAliveSeconds;
    private String mqttClientId;
    private MqttClient mqttClient;
    private static SharedPreferences sharedPreferences;
    private static ServiceMqtt instance;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private LocalBinder<ServiceMqtt> mBinder;
    private Thread workerThread;
    private Runnable deferredPublish;

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
        changeMqttConnectivity(MQTT_CONNECTIVITY.INITIAL);
        mBinder = new LocalBinder<ServiceMqtt>(this);
        keepAliveSeconds = 15 * 60;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        doStart(intent, startId);
        return START_STICKY;
    }

    private void doStart(final Intent intent, final int startId) {
   //     init();
        

        Thread thread1 = new Thread(){
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

        
        // If there is no mqttClient, something went horribly wrong
//        if (mqttClient == null) {
//            Log.e(this.toString(), "handleStart: !mqttClient");
//            stopSelf();
//            return;
//        }        
        
        // Respect user's wish to stay disconnected
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

        // Don't do anything when already connected
        if (!isConnected())
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
        }
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
            String brokerAddress = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_HOST, Defaults.VALUE_BROKER_HOST);
            String brokerPort = sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PORT, Defaults.VALUE_BROKER_PORT);

            String handle = "ssl";
            
            if(sharedPreferences.getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, Defaults.VALUE_BROKER_SECURITY_NONE) == Defaults.VALUE_BROKER_SECURITY_NONE)
                handle = "tcp";
            
            mqttClient = new MqttClient(handle+"://" + brokerAddress + ":" + brokerPort, getClientId(), null);
            mqttClient.setCallback(this);
        
        } catch (MqttException e)
        {
            // something went wrong!
            mqttClient = null;
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
        }


        

    }

    private javax.net.ssl.SSLSocketFactory getSSLSocketFactory() throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
        InputStream caInput = new BufferedInputStream(new FileInputStream(sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, "")));
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
        workerThread = Thread.currentThread(); // We connect, so we're the worker thread
        Log.v(this.toString(), "connectToBroker");

        init();
        
        try
        {
            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTING);
            MqttConnectOptions options = new MqttConnectOptions();

            

         // TODO: Make this nicer
         if(sharedPreferences.getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, Defaults.VALUE_BROKER_SECURITY_NONE) == Defaults.VALUE_BROKER_SECURITY_SSL)
            options.setSocketFactory(getSSLSocketFactory());
                        
         if(!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").equals(""))
             options.setPassword(sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, "").toCharArray());
        
         if(!sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, "").equals(""))
             options.setUserName(sharedPreferences.getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, ""));
         


            
            options.setKeepAliveInterval(keepAliveSeconds); 
            options.setConnectionTimeout(10);
            
            mqttClient.connect(options);

            changeMqttConnectivity(MQTT_CONNECTIVITY.CONNECTED);

            return true;
        } 
         catch (Exception e) // Catch paho and socket factory exceptions 
        {
            Log.e(this.toString(), e.toString());
            //TODO: send reason to user
            changeMqttConnectivity(MQTT_CONNECTIVITY.DISCONNECTED);
            return false;
        }

    }


   
    private void onConnect() {
   
        
        if (!isConnected()) {
            Log.e(this.toString(), "onConnect: !isConnected");
        }  
    }


    public void disconnect(boolean fromUser)
    {
        Log.v(this.toString(), "disconnect");
        if(fromUser)
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

            if(workerThread != null) {
                workerThread.interrupt();
            }

        }
    }

    
    @SuppressLint("Wakelock") // Lint check derps with the wl.release() call. 
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


    public void publish(String topicStr, String payload) {
        publish(topicStr, payload, true);
    }
    
    public void publish(String topicStr, String payload, boolean retained) {
        boolean isOnline = isOnline(false);
        boolean isConnected = isConnected();

        if (!isOnline || !isConnected) {
            return;
        }
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(0);
        message.setRetained(retained);
        
        try
        {
            mqttClient.getTopic(topicStr).publish(message);            
        } catch (MqttException e)
        {
            Log.e(this.toString(), e.getMessage());
            e.printStackTrace();
        }
    }

    public void onEvent(Events.MqttConnectivityChanged event) {
        mqttConnectivity = event.getConnectivity();
        if (deferredPublish != null && event.getConnectivity() == MQTT_CONNECTIVITY.CONNECTED)
            deferredPublish.run();

    }
    
    
  

    /**
     * @category CONNECTIVITY STATUS
     */
    private void changeMqttConnectivity(MQTT_CONNECTIVITY newConnectivity) {

        EventBus.getDefault().post(new Events.MqttConnectivityChanged(newConnectivity));
        mqttConnectivity = newConnectivity;
    }
    
    private boolean isOnline(boolean shouldCheckIfOnWifi)
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        return     netInfo != null 
             //   && (!shouldCheckIfOnWifi || (netInfo.getType() == ConnectivityManager.TYPE_WIFI))
                && netInfo.isAvailable() 
                && netInfo.isConnected();
    }
    
    public boolean isConnected()
    {
        return ((mqttClient != null) && (mqttClient.isConnected() == true));
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
    

//    
//    /**
//     * @category OBSERVERS
//     */
//    private class NetworkConnectionIntentReceiver extends BroadcastReceiver
//    {
//
//        @SuppressLint("Wakelock")
//        @Override
//        public void onReceive(Context ctx, Intent intent)
//        {
//            Log.v(this.toString(), "NetworkConnectionIntentReceiver: onReceive");
//            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
//            wl.acquire();
//
//            if (isOnline(true) && !isConnected() && !isConnecting()) {
//                Log.v(this.toString(), "NetworkConnectionIntentReceiver: triggerting doStart(null, -1)");
//                doStart(null, 1);
//            
//            }
//            wl.release();
//        }
//    }
//        
//

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

        if(this.alarmManagerPositioning != null)
            this.alarmManagerPositioning.cancel(pendingIntentPositioning);
        
        
        super.onDestroy();
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) { }
    
    public static String getConnectivityText() {
        
        switch (ServiceMqtt.getConnectivity()) {
            case CONNECTED:
                return App.getInstance().getString(R.string.connectivityConnected);
            case CONNECTING:
                return App.getInstance().getString(R.string.connectivityConnecting);
            case DISCONNECTING:
                return App.getInstance().getString(R.string.connectivityDisconnecting);
            // More verbose disconnect states could be added here. For now any flavor of disconnected is treated the same
            default:
                return App.getInstance().getString(R.string.connectivityDisconnected);
        }
    }
    
    public void publishWithTimeout(final String topic, final String payload, final boolean retained, int timeout, final MqttPublish callback) {
        Log.v(this.toString(), topic + ":" + payload);
        if (getConnectivity() == MQTT_CONNECTIVITY.CONNECTED) {
            callback.publishing();
            publish(topic, payload, retained);
            callback.publishSuccessfull();
            
        } else {
            Log.d(this.toString(), "No broker connection established yet, deferring publish");
            callback.waiting();
            deferredPublish = new Runnable() {
                @Override
                public void run() {
                    deferredPublish = null;
                    Log.d(this.toString(), "Broker connection established, publishing deferred message");
                    callback.publishing();
                    publish(topic, payload, retained);
                    callback.publishSuccessfull();
                }
                
            };
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(this.toString(),  "Publish timed out");
                    deferredPublish = null;
                    callback.publishTimeout();
                }
            }, timeout * 1000);        
        }
    }

}


