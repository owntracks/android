
package st.alr.mqttitude.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

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
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    protected Date lastPublish;
    private java.text.DateFormat lastPublishDateFormat;
    private Set<Defaults.State> state;
    protected final String TAG = this.toString();

    Locator(Context context) {
        this.context = context;
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

    abstract public void enableForegroundMode();

    abstract public void enableBackgroundMode();

    public void publishLastKnownLocation() {
        Log.v(TAG, "publishLastKnownLocation");
        lastPublish = new Date();

        Intent service = new Intent(context, ServiceMqtt.class);
        StringBuilder payload = new StringBuilder();
        Date d = new Date();
        Location l = getLastKnownLocation();
        String topic = sharedPreferences.getString(Defaults.SETTINGS_KEY_TOPIC, Defaults.VALUE_TOPIC);
        ServiceMqtt s = ServiceMqtt.getInstance();
        if(s == null) {
            return;
        }
        
        if (topic == null) {
            addState(State.NOTOPIC);
            return;
        }
        if (l == null) {
            this.addState(State.LocatingFail);
            return;
        }

         
        
        context.startService(service);

        payload.append("{");
        payload.append("\"type\": ").append("\"").append("_location").append("\"");
        payload.append("\"lat\": ").append("\"").append(l.getLatitude()).append("\"");
        payload.append(", \"lon\": ").append("\"").append(l.getLongitude()).append("\"");
        payload.append(", \"tst\": ").append("\"").append((int)(d.getTime()/1000)).append("\"");
        payload.append(", \"acc\": ").append("\"").append(Math.round(l.getAccuracy() * 100) / 100.0d).append("m").append("\"");
        payload.append(", \"alt\": ").append("\"").append(l.getAltitude()).append("\"");
        payload.append("}");

        ServiceMqtt.getInstance().publish(
                topic,
                payload.toString(),
                sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_RETAIN, Defaults.VALUE_RETAIN),
                Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_QOS, Defaults.VALUE_QOS))
                , 20, this);

    }

    public void publishSuccessfull() {
        Log.v(TAG, "publishSuccessfull");
        EventBus.getDefault().post(new Events.PublishSuccessfull());
        // This is a bit hacked as we append an empty space on every second
        // ticker update. Otherwise consecutive tickers with the same text would
        // not be shown
        if(isTickerOnPublishEnabled())
            App.getInstance().updateTicker(App.getInstance().getString(R.string.statePublished));
        this.resetState();
    }

    public Set<State> getState() {
        return this.state;
    }

    public String getStateAsText() {
        if (this.state.contains(State.NOTOPIC)) {
            return App.getInstance().getString(R.string.stateNotopic);
        }
        if (this.state.contains(State.PublishConnectionTimeout)) {
            return App.getInstance().getString(R.string.statePublishTimeout);
        }
        if (this.state.contains(State.LocatingFail)) {
            return App.getInstance().getString(R.string.stateLocatingFail);
        }

        if (this.state.contains(State.Publishing)) {
            return App.getInstance().getString(R.string.statePublishing);
        }
        if (this.state.contains(State.PublishConnectionWaiting)) {
            return App.getInstance().getString(R.string.stateWaiting);
        }
        return App.getInstance().getString(R.string.stateIdle);
    }

    public void publishFailed() {
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
        if (isTickerState(s) && isTickerOnPublishEnabled()) {
            App.getInstance().updateTicker(getStateAsText());
        }
        App.getInstance().updateNotification();
        EventBus.getDefault().post(new Events.StateChanged());
    }

    private boolean isTickerState(State s) {
        return s == Defaults.State.LocatingFail || s == Defaults.State.NOTOPIC
                || s == Defaults.State.PublishConnectionTimeout || s == Defaults.State.PublishConnectionWaiting || s == Defaults.State.Publishing;
    }

    protected void removeState(State s) {
        this.state.remove(s);
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
    
    public boolean isTickerOnPublishEnabled(){
        return  sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
                Defaults.VALUE_TICKER_ON_PUBLISH);
    }

    
    public int getUpdateIntervall() {
        return Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL,
                Defaults.VALUE_BACKGROUND_UPDATES_INTERVAL));
    }

    public int getUpdateIntervallInMiliseconds() {
        return getUpdateIntervall() * 60 * 1000;
    }

}
