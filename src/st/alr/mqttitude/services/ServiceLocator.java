
package st.alr.mqttitude.services;

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.MqttPublish;
import st.alr.mqttitude.support.Defaults.State;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class ServiceLocator extends ServiceBindable implements MqttPublish {
    protected SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    protected Date lastPublish;
    private static Set<Defaults.State> state;
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
        state = EnumSet.of(Defaults.State.Idle);

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

    public void publishLastKnownLocation() {
        Log.v(TAG, "publishLastKnownLocation");
        lastPublish = new Date();

        StringBuilder payload = new StringBuilder();
        Date d = new Date();
        GeocodableLocation l = getLastKnownLocation();
        String topic = sharedPreferences.getString(Defaults.SETTINGS_KEY_TOPIC, Defaults.VALUE_TOPIC);

           
        if (topic == null) {
            addState(State.NOTOPIC);
            return;
        }
        
        if (l == null) {
            this.addState(State.LocatingFail);
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
        this.resetState();
        EventBus.getDefault().post(new Events.PublishSuccessfull(extra));       
    }

    public static Set<State> getState() {
        return state;
    }
    
    

    
    public static String getStateAsText() {
        int id = R.string.stateIdle;
        
        if(state != null) {

            if (state.contains(State.Publishing))
                id = R.string.statePublishing;
            else if (state.contains(State.PublishConnectionWaiting))
                id = R.string.stateWaiting;                        
            else if (state.contains(State.PublishConnectionTimeout))
                id = R.string.statePublishTimeout;
            else if (state.contains(State.LocatingFail))
                id = R.string.stateLocatingFail;    
            else if (state.contains(State.NOTOPIC))
                id = R.string.stateNotopic;
        }
        return App.getInstance().getString(id);
    }

    @Override
    public void publishFailed(Object extra) {
        Log.e(TAG, "publishTimeout");
        this.addState(State.PublishConnectionTimeout);
    }

    @Override
    public void publishing(Object extra) {
        Log.v(TAG, "publishing");
        this.addState(State.Publishing);
    }

    @Override
    public void publishWaiting(Object extra) {
        Log.v(TAG, "waiting for broker connection");
        this.addState(State.PublishConnectionWaiting);
    }

    protected void setStateTo(State s) {
        state.clear();
        state.add(s);
    }

    protected void addState(State s) {       
        state.add(s);
        if (isTickerState(s)) {
            App.getInstance().updateTicker(getStateAsText());
        }
        App.getInstance().updateNotification();
        EventBus.getDefault().post(new Events.StateChanged());
    }

    private boolean isTickerState(State s) {
        return s == Defaults.State.LocatingFail || s == Defaults.State.NOTOPIC
                || s == Defaults.State.PublishConnectionTimeout || s == Defaults.State.PublishConnectionWaiting || (s == Defaults.State.PublishConnectionWaiting && sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
                        Defaults.VALUE_TICKER_ON_PUBLISH));
    }

    protected void removeState(State s) {
        state.remove(s);
        EventBus.getDefault().post(new Events.StateChanged());
    }

    public void resetState() {
        this.setStateTo(State.Idle);
        EventBus.getDefault().post(new Events.StateChanged());
        App.getInstance().updateNotification();
    }

    public Date getLastPublishDate() {
        return lastPublish;
    }

    public boolean areBackgroundUpdatesEnabled() {
        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES,
                Defaults.VALUE_BACKGROUND_UPDATES);
    }
    

    
    public int getUpdateIntervall() {
        return Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL,
                Defaults.VALUE_BACKGROUND_UPDATES_INTERVAL));
    }

    public int getUpdateIntervallInMiliseconds() {
        return getUpdateIntervall() * 60 * 1000;
    }
}
