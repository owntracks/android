package st.alr.mqttitude.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.prefs.PreferenceChangeListener;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults.State;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class Locator implements MqttPublish {
    protected Context context;
    protected SharedPreferences sharedPreferences;
    private  OnSharedPreferenceChangeListener preferencesChangedListener;
    private Date lastPublish;
    private java.text.DateFormat lastPublishDateFormat;
    private Set<Defaults.State> state;
    protected final String TAG = this.toString();


    Locator (Context context) {
        this.context = context;
        this.lastPublishDateFormat =  new SimpleDateFormat("y/M/d H:m:s");
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.state = EnumSet.of(Defaults.State.Idle);
        
        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                    handlePreferences();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
    }
    
    abstract public Location getLastKnownLocation();    
    abstract protected void handlePreferences();
    abstract public void start();
    
    public void publishLastKnownLocation() {
        Log.v(TAG, "publishLastKnownLocation");

        Location l = getLastKnownLocation();
        if(l != null) {
            Intent service = new Intent(context, ServiceMqtt.class);
            context.startService(service);        
            String topic = sharedPreferences.getString("location_topic", null);
            
            if(topic == null) {
               addState(State.NOTOPIC);
               return;
            }
            
            ServiceMqtt.getInstance().publishWithTimeout(topic, l.getLatitude() + ":" + l.getLongitude(), true, 20, this);
        } else {
            this.addState(State.LocatingFail);
        }
    }

    
    public void publishSuccessfull() {
        Log.v(TAG, "publishSuccessfull");
        lastPublish = new Date();
        EventBus.getDefault().post(new Events.PublishSuccessfull());
        this.resetState();
    }

        
    public Set<State> getState() {
        return this.state;
    }
    
    public String getStateAsText() {        
        if (this.state.contains(State.NOTOPIC)) {
            return "Error: No topic set";
        }  
        if (this.state.contains(State.Publishing)) {
            return "Publishing";
        }
        if (this.state.contains(State.PublishConnectionTimeout)) {
            return "Error: Publish timeout";
        }
        if (this.state.contains(State.PublishConnectionWaiting)) {
            return "Wainting for connection";
        }
        if (this.state.contains(State.LocatingFail)) {
            return "Error: Unable to acqire location";
        }
        if (this.state.contains(State.Locating)) {
            return "Locating";
        }
        return "Idle";
    }

    public void publishTimeout() {
        Log.e(TAG, "publishTimeout");
        this.addState(State.PublishConnectionTimeout);
    }

    public void publishing() {
        Log.v(TAG, "publishing");
        this.addState(State.Publishing);
    }

    public void publishWaiting() {
        Log.v(TAG, "waiting for broker connection");
        this.addState(State.PublishConnectionWaiting);
    }
    
    protected void setStateTo(State s) {
        this.state.clear();
        this.state.add(s);
    }

    protected void addState(State s) {
        this.state.add(s);
        EventBus.getDefault().post(new Events.StateChanged());
    }

    protected void removeState(State s) {
        this.state.remove(s);
        EventBus.getDefault().post(new Events.StateChanged());
    }

    public void resetState() {
        this.setStateTo(State.Idle);
        EventBus.getDefault().post(new Events.StateChanged());
    }

    public String getLastupdateText() {
        if (lastPublish != null)
            return lastPublishDateFormat.format(lastPublish);
        else
            return context.getResources().getString(R.string.na);
    }
    
    public int getUpdateIntervall(){
        return Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_UPDATE_INTERVAL, Defaults.VALUE_UPDATE_INTERVAL));
    }
    
    public int getUpdateIntervallInMiliseconds(){
        return getUpdateIntervall()*60*1000;
    }
   
}
