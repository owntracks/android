
package st.alr.mqttitude.services;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceMqtt.LocalBinder;
import st.alr.mqttitude.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.MqttPublish;
import st.alr.mqttitude.support.Defaults.State;
import st.alr.mqttitude.support.Events.PublishSuccessfull;
import st.alr.mqttitude.support.Events.StateChanged;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.net.Proxy;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class ServiceLocator extends Service implements MqttPublish {
    protected Context context;
    protected SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    protected Date lastPublish;
    private java.text.DateFormat lastPublishDateFormat;
    private Set<Defaults.State> state;
    protected final String TAG = this.toString();

    @Override
    public void onCreate()
    {
        super.onCreate();
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
        payload.append("\"_type\": ").append("\"").append("location").append("\"");
        payload.append(", \"lat\": ").append("\"").append(l.getLatitude()).append("\"");
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
        if(this.state == null)
            return App.getInstance().getString(R.string.stateIdle);
                    
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


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
      //TODO do something useful
      return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocatorBinder();
    }
    
    public class LocatorBinder extends Binder
    {
            public ServiceLocator getService ()
            {
                    return ServiceLocator.this;
            }
    }

    // This is how activities get hold of the service implementation as an interface!!!
    // All invocations will be executed async in the UI thread!!!
    public static ServiceLocator getAsyncService(final Context context) {
        final Intent intent = new Intent(context, ServiceLocator.class);

        // Unmark the following if you want the service to keep on leaving even after the passed activity was destroyed.
        context.startService(intent);

        ServiceLocator proxy = (ServiceLocator) Proxy.newProxyInstance(context.getClassLoader(), new Class<?>[] { ServiceLocator.class }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }

                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        // this is the implementation itself!!!
                        ServiceLocator mainInterface = ((LocatorBinder) service).getService();
                        try {
                            method.invoke(mainInterface, args);
                        } catch (Exception e) {
                            // Alternatively, you can notify the service's error mechanism
                            Log.e("", "Error invoking service method: " + method.getName(), e);
                        }
                    }
                };
                // This is how you bind async the connection to the service.
                // The service will be created if it was not yet created.
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE);

                return null;            }
        });

        return proxy;
    }

}
