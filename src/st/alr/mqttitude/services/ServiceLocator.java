
package st.alr.mqttitude.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.MqttPublish;

public abstract class ServiceLocator extends ServiceBindable implements MqttPublish {
    protected SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    protected Date lastPublish;
    private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
    private final String TAG = "ServiceLocator";
    protected ServiceMqtt serviceMqtt;
    private ServiceConnection mqttConnection;
    private static ServiceLocator instance;
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.v(this.TAG, "onCreate");

        instance = this;
        this.started = false;        
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if(key.equals(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES) || key.equals(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL))
                    handlePreferences();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        
        mqttConnection = new ServiceConnection() {
            
            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceMqtt = null;                
            }
            
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.v(this.toString(), "bound");

                serviceMqtt = (ServiceMqtt) ((ServiceBindable.ServiceBinder)service).getService();                
            }
        };
         
        bindService(new Intent(this, ServiceMqtt.class), mqttConnection, Context.BIND_AUTO_CREATE);
        
    }

    abstract public GeocodableLocation getLastKnownLocation();

    abstract protected void handlePreferences();
    
    abstract public void enableForegroundMode();

    abstract public void enableBackgroundMode();

    public static ServiceLocator getInstance(){
        return instance;
    }
    
    public void publishLastKnownLocation() {
        Log.v(TAG, "publishLastKnownLocation");
        lastPublish = new Date();

        StringBuilder payload = new StringBuilder();
        Date d = new Date();
        GeocodableLocation l = getLastKnownLocation();
        String topic = sharedPreferences.getString(Defaults.SETTINGS_KEY_TOPIC, Defaults.VALUE_TOPIC);

           
        if (topic == null) {
            changeState(Defaults.State.ServiceLocator.NOTOPIC);
            return;
        }
        
        if (l == null) {
            changeState(Defaults.State.ServiceLocator.NOLOCATION);
            return;
        }

        payload.append("{");
        payload.append("\"_type\": ").append("\"").append("location").append("\"");
        payload.append(", \"lat\": ").append("\"").append(l.getLatitude()).append("\"");
        payload.append(", \"lon\": ").append("\"").append(l.getLongitude()).append("\"");
        payload.append(", \"tst\": ").append("\"").append((int)(d.getTime()/1000)).append("\"");
        payload.append(", \"acc\": ").append("\"").append(Math.round(l.getLocation().getAccuracy() * 100) / 100.0d).append("m").append("\"");
        payload.append(", \"alt\": ").append("\"").append(l.getLocation().getAltitude()).append("\"");
        payload.append("}");

        ServiceMqtt.getInstance().publish(
                topic,
                payload.toString(),
                sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_RETAIN, Defaults.VALUE_RETAIN),
                Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_QOS, Defaults.VALUE_QOS))
                , 20, this, l);

    }

    @Override
    public void publishSuccessfull(Object extra) {
        Log.v(TAG, "publishSuccessfull");
        changeState(Defaults.State.ServiceLocator.INITIAL);
        EventBus.getDefault().postSticky(new Events.PublishSuccessfull(extra));       
    }

    public static Defaults.State.ServiceLocator getState() {
        return state;
    }
    public static String getStateAsString(){
        return stateAsString(getState());
    }
    
    public static String stateAsString(Defaults.State.ServiceLocator state) {
        return Defaults.State.toString(state);
    }

    private void changeState(Defaults.State.ServiceLocator newState) {
        Log.d(this.toString(), "ServiceLocator state changed to: " + newState);
        EventBus.getDefault().postSticky(new Events.StateChanged.ServiceLocator(newState));
        state = newState;
    }

    


    @Override
    public void publishFailed(Object extra) {
        changeState(Defaults.State.ServiceLocator.PUBLISHING_TIMEOUT);
    }

    @Override
    public void publishing(Object extra) {
        changeState(Defaults.State.ServiceLocator.PUBLISHING);
    }

    @Override
    public void publishWaiting(Object extra) {
        changeState(Defaults.State.ServiceLocator.PUBLISHING_WAITING);
    }


    private boolean isTickerState(Defaults.State.ServiceLocator s) {
        return s == Defaults.State.ServiceLocator.NOLOCATION 
                || s == Defaults.State.ServiceLocator.NOTOPIC
                || s == Defaults.State.ServiceLocator.PUBLISHING_TIMEOUT 
                || (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH, Defaults.VALUE_TICKER_ON_PUBLISH && 
                        (s == Defaults.State.ServiceLocator.PUBLISHING || s == Defaults.State.ServiceLocator.PUBLISHING_WAITING)));
    }

    public Date getLastPublishDate() {
        return lastPublish;
    }

    public boolean areBackgroundUpdatesEnabled() {
        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES,
                Defaults.VALUE_BACKGROUND_UPDATES);
    }
    
    public int getUpdateIntervall() {
        int ui;
        try{
           ui = Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL,
                Defaults.VALUE_BACKGROUND_UPDATES_INTERVAL));
        } catch (Exception e) {
            ui = 30;
        }
           
           return ui;
    }

    public int getUpdateIntervallInMiliseconds() {
        return getUpdateIntervall() * 60 * 1000;
    }
}
