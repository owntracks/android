
package st.alr.mqttitude.services;

import java.util.Date;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.MqttPublish;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import de.greenrobot.event.EventBus;

public abstract class ServiceLocator implements ProxyableService, MqttPublish {
    protected SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    protected Date lastPublish;
    private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
    private final String TAG = "ServiceLocator";
    private static ServiceLocator instance;
    protected ServiceProxy context;
    
    public void onCreate(ServiceProxy p)
    {

        instance = this;
        context = p;
        
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if(key.equals(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES) || key.equals(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL))
                    handlePreferences();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
       
                 
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
        String topic = ActivityPreferences.getPubTopic(true);

        if (topic == null) {
            changeState(Defaults.State.ServiceLocator.NOTOPIC);
            return;
        }
        
        if (l == null) {
            changeState(Defaults.State.ServiceLocator.NOLOCATION);
            return;
        }

        if(ServiceProxy.getServiceBroker() == null) {
            Log.e(this.toString(), "publishLastKnownLocation but ServiceMqtt not ready");
            return;
        }
        
        payload.append("{");
        payload.append("\"_type\": ").append("\"").append("location").append("\"");
        payload.append(", \"lat\": ").append("\"").append(l.getLatitude()).append("\"");
        payload.append(", \"lon\": ").append("\"").append(l.getLongitude()).append("\"");
        payload.append(", \"tst\": ").append("\"").append((int)(d.getTime()/1000)).append("\"");
        payload.append(", \"acc\": ").append("\"").append(Math.round(l.getLocation().getAccuracy() * 100) / 100.0d).append("\"");
        payload.append("}");

        ServiceProxy.getServiceBroker().publish(
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
    public static String getStateAsString(Context c){
        return stateAsString(getState(), c);
    }
    
    public static String stateAsString(Defaults.State.ServiceLocator state, Context c) {
        return Defaults.State.toString(state, c);
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
            ui = Integer.parseInt(Defaults.VALUE_BACKGROUND_UPDATES_INTERVAL);
        }
           
           return ui;
    }

    public int getUpdateIntervallInMiliseconds() {
        return getUpdateIntervall() * 60 * 1000;
    }
    
    public void onEvent(Events.Dummy e) {}
}
