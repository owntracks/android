    
package st.alr.mqttitude;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Defaults.State;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Locator;
import st.alr.mqttitude.support.LocatorCallback;
import st.alr.mqttitude.support.Events.MqttConnectivityChanged;
import st.alr.mqttitude.support.MqttPublish;
import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import de.greenrobot.event.EventBus;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class App extends Application implements MqttPublish{
    private static App instance;
    private Locator locator;
    private BroadcastReceiver receiver; 
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private NotificationManager notificationManager;
    private static NotificationCompat.Builder notificationBuilder;
    private Set<Defaults.State> state;
    private Location lastLocation;
    private Date lastUpdate;
    private java.text.DateFormat lastUpdateDateFormat;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        state = EnumSet.of(Defaults.State.Idle);
        locator = new Locator(this);
        receiver = new UpdateReceiver();
        lastUpdateDateFormat = new SimpleDateFormat("y/M/d H:m:s");


        registerReceiver(receiver, new IntentFilter(st.alr.mqttitude.support.Defaults.UPDATE_INTEND_ID));
        
        notificationManager = (NotificationManager) App.getInstance().getSystemService(
                Context.NOTIFICATION_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED))
                    handleNotification();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        handleNotification();

        scheduleNextUpdate();
        EventBus.getDefault().register(this);
    }

    public String getLocatorText() {
        if(this.state.contains(State.Publishing)) {
            Log.v(this.toString(), "1");
            return "Publishing";
        }
        if(this.state.contains(State.PublishConnectionTimeout)) {
            Log.v(this.toString(), "2");
            return "Error: Publish timeout";

        }
        if(this.state.contains(State.PublishConnectionWaiting)) {
            Log.v(this.toString(), "3");

            return "Wainting for connection";

        }
        if(this.state.contains(State.LocatingFail)) {
            Log.v(this.toString(), "4");

            return "Error: Unable to acqire location";
        }
        if(this.state.contains(State.Locating)) {
            Log.v(this.toString(), "5");

            return "Locating";
        }
        Log.v(this.toString(), "6");

        return "Idle";
    }
    
    public Location getLocation(){
        return lastLocation;
    }
    
    public static App getInstance() {
        return instance;
    }
    

    
    public Locator getLocator() {
        return locator;
    }
    
    

    
    
    public void publishLocation(final boolean publish) {
        locator.get(new LocatorCallback() {
            
            @Override
            public void onLocationRespone(Location location) {
                EventBus.getDefault().postSticky(new Events.LocationUpdated(location));
                if(publish) {
                    Intent service = new Intent(App.getInstance(), ServiceMqtt.class);
                    startService(service);                    

                    ServiceMqtt.getInstance().publishWithTimeout(PreferenceManager.getDefaultSharedPreferences(App.getInstance()).getString("location_topic", null), location.getLatitude()+":"+location.getLongitude(), true, 20, App.getInstance());
                }
                    
            }
        });
    }
    
    
    private void scheduleNextUpdate()
    {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(st.alr.mqttitude.support.Defaults.UPDATE_INTEND_ID), PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar wakeUpTime = Calendar.getInstance();
        wakeUpTime.add(Calendar.MINUTE, Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("updateIntervall", st.alr.mqttitude.support.Defaults.VALUE_UPDATE_INTERVAL)));

        AlarmManager aMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), pendingIntent);
    }

    private class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(Defaults.UPDATE_INTEND_ID)){

            publishLocation(true);
            scheduleNextUpdate();
            }
        }
        
    }
    
    
    
    /**
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification() {
        Log.v(this.toString(), "handleNotification()");
        notificationManager.cancel(Defaults.NOTIFCATION_ID);

        if (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED, Defaults.VALUE_NOTIFICATION_ENABLED))
            createNotification();
    }

    private void createNotification() {
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());

        Intent resultIntent = new Intent(App.getInstance(), ActivityMain.class);
        android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ActivityMain.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        updateNotification();
    }
    
    public void updateTicker(String text) {
     notificationBuilder.setTicker(text);   
     notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

    public void updateNotification() {
        notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
        notificationBuilder
                .setSmallIcon(R.drawable.notification)
                .setOngoing(true)
                .setContentText(getLocatorText())
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0);
      
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

    public void onEventMainThread(Events.StateChanged e) {
        updateNotification();
    }
    
    public void onEvent(Events.LocationUpdated e) {
        this.lastLocation = e.getLocation();
        Log.v(this.toString(), "LocationUpdated: " + e.getLocation().getLatitude() + ":" + e.getLocation().getLongitude());
    }
    
    @Override
    public void publishSuccessfull() {
        Log.v(this.toString(), "publishSuccessfull");
        lastUpdate = new Date();
        EventBus.getDefault().post(new Events.PublishSuccessfull());
        this.resetState(); // Go back to idle;
    }
    


    public void setStateTo(State s) {
        this.state.clear();
        this.state.add(s);
    }
    
    public void addState(State s){
        this.state.add(s);
        EventBus.getDefault().post(new Events.StateChanged());
    }
    public void removeState(State s){
        this.state.remove(s);
        EventBus.getDefault().post(new Events.StateChanged());
    }
    public void resetState(){
        this.setStateTo(State.Idle);
        EventBus.getDefault().post(new Events.StateChanged());
    }

    public String getLastupdateText() {
        if(lastUpdate != null)
            return lastUpdateDateFormat.format(lastUpdate);
        else 
            return getResources().getString(R.string.na);
    }

    @Override
    public void publishTimeout() {
        this.addState(State.PublishConnectionTimeout);        
    }

    @Override
    public void publishing() {
        this.addState(State.Publishing);

    }

    @Override
    public void waiting() {
        this.addState(State.PublishConnectionWaiting);
    }
}

