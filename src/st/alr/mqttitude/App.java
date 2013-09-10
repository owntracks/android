
package st.alr.mqttitude;

import java.text.SimpleDateFormat;
import java.util.Date;

import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.FusedLocationLocator;
import st.alr.mqttitude.support.Locator;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.greenrobot.event.EventBus;

import com.bugsnag.android.Bugsnag;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class App extends Application {
    private static App instance;
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private NotificationManager notificationManager;
    private static NotificationCompat.Builder notificationBuilder;

    private Locator locator;
    private boolean even = false;
    private SimpleDateFormat dateFormater;
    
    @Override
    public void onCreate() {
        super.onCreate();
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        instance = this;
        
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");

        EventBus.getDefault().register(this);

        if (resp == ConnectionResult.SUCCESS) {
            locator = new FusedLocationLocator(this);
            Log.v(this.toString(), "Play  services version: " + GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE);
        } else {
            locator = new FusedLocationLocator(this);
            Log.e(this.toString(),  "play services not available and no other locator implemented yet ");
        }
        this.dateFormater = new SimpleDateFormat("y/M/d H:m:s", getResources().getConfiguration().locale);

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
        
        locator.start();

    }

    public String formatDate(Date d) {
        return dateFormater.format(d);
    }

    public static App getInstance() {
        return instance;
    }

    /**
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification() {
        Log.v(this.toString(), "handleNotification()");
        notificationManager.cancel(Defaults.NOTIFCATION_ID);

        if (notificationEnabled())
            createNotification();
    }
    private boolean notificationEnabled() {
        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED, Defaults.VALUE_NOTIFICATION_ENABLED);
    }

    private void createNotification() {
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());

        Intent resultIntent = new Intent(App.getInstance(), ActivityMain.class);
        android.support.v4.app.TaskStackBuilder stackBuilder = android.support.v4.app.TaskStackBuilder
                .create(this);
        stackBuilder.addParentStack(ActivityMain.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        updateNotification();
    }

    public void updateTicker(String text) {
        notificationBuilder.setTicker(text + ((even = even ? false : true) ? " " : ""));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

    public void updateNotification() {
        if(!notificationEnabled())
            return;
        
        
        String text = locator.getStateAsText();
        notificationBuilder.setContentTitle(getResources().getString(R.string.app_name));
        notificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setWhen(0);
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

//    public void onEventMainThread(Events.StateChanged e) {
//        updateNotification();
//    }
    public void onEventMainThread(Events.MqttConnectivityChanged e) {

    }

    public void onEvent(Events.LocationUpdated e) {
        if(e.getLocation() == null)
            return;
        
        Log.v(this.toString(), "LocationUpdated: " + e.getLocation().getLatitude() + ":"
                + e.getLocation().getLongitude());
    }

    public Locator getLocator(){
        return this.locator;
    } 
}
