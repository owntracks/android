    
package st.alr.mqttitude;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.mqttitude.support.Defaults;
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

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        locator = new Locator(this);
        receiver = new UpdateReceiver();
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

    public static App getInstance() {
        return instance;
    }
    

    
    public Locator getLocator() {
        return locator;
    }
    
    
    public void onEvent(Events.LocationUpdated e) {
        Log.v(this.toString(), "LocationUpdated: " + e.getLocation().getLatitude() + ":" + e.getLocation().getLongitude());
    }

    public void updateLocation(final boolean publish) {
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

            updateLocation(true);
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
                .setContentText("Idle")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0);
      
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

    @Override
    public void publishSuccessfull() {
        updateTicker("Location published");
    }

    @Override
    public void publishFailed() {
        Log.v(this.toString(), "fail");
        updateTicker("Error: Location publish failed");
    }
    
}

