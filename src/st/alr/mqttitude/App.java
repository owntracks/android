
package st.alr.mqttitude;

import java.text.SimpleDateFormat;
import java.util.Date;

import st.alr.mqttitude.services.ServiceLocator;
import st.alr.mqttitude.services.ServiceLocatorFused;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
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
    private static Class<?> locatorClass;
    private GeocodableLocation lastPublishedLocation;
    private Date lastPublishedLocationTime;

    private boolean even = false;
    private SimpleDateFormat dateFormater;
    private Handler handler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        int resp = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        instance = this;
        
        Bugsnag.register(this, Defaults.BUGSNAG_API_KEY);
        Bugsnag.setNotifyReleaseStages("production", "testing");

        handler = new Handler() {
            public void handleMessage(Message msg) {
                onHandlerMessage(msg);
            }
        };

        
        EventBus.getDefault().register(this);

        Intent serviceLocator = null;
        if (resp == ConnectionResult.SUCCESS) {
            Log.v(this.toString(), "Play  services version: " + GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE);
            locatorClass = ServiceLocatorFused.class;
        } else {
            // TODO: implement fallback locator
            Log.e(this.toString(),  "play services not available and no other locator implemented yet ");
            locatorClass = ServiceLocatorFused.class;
        }

        serviceLocator = new Intent(this, getServiceLocatorClass());
        
        this.dateFormater = new SimpleDateFormat("y/M/d H:m:s", getResources().getConfiguration().locale);

        notificationManager = (NotificationManager) App.getInstance().getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(App.getInstance());

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
        
        Log.v(this.toString(), "Starting MQTT service ");
        startService(new Intent(this, ServiceMqtt.class)); // Service remains running after binds by activity

        Log.v(this.toString(), "Starting locator service " + getServiceLocatorClass().toString());
        startService(serviceLocator); // Service remains running after binds by activity
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

        // if the notification is not enabled, the ticker will create an empty one that we get rid of
        if(!notificationEnabled())
            notificationManager.cancel(Defaults.NOTIFCATION_ID);
    }

    public void updateNotification() {
        if(!notificationEnabled())
            return;
        
        String title = null; 
        String subtitle = null;
        long time = 0; 
        
        
        if(lastPublishedLocation != null && sharedPreferences.getBoolean("notificationLocation", true)) {
            time = lastPublishedLocation.getLocation().getTime();

            if(lastPublishedLocation.getGeocoder() != null && sharedPreferences.getBoolean("notificationGeocoder", false)) {
                title = lastPublishedLocation.getGeocoder();
            } else {
                title = lastPublishedLocation.getLatitude() + ":" + lastPublishedLocation.getLongitude();
            }
        } else {
            title = getString(R.string.app_name);
        }
        
        subtitle = ServiceLocator.getStateAsText() + " | " + ServiceMqtt.getConnectivityText();

        notificationBuilder.setContentTitle(title);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(subtitle)
                .setPriority(NotificationCompat.PRIORITY_MIN);
        if(time != 0)
            notificationBuilder.setWhen(lastPublishedLocationTime.getTime());
        
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
    }

//    public void onEventMainThread(Events.StateChanged e) {
//        updateNotification();
//    }
    public void onEventMainThread(Events.MqttConnectivityChanged e) {
        updateNotification();
    }

    public void onEventMainThread(Events.StateChanged e) {
        updateNotification();
    }

    
    private void onHandlerMessage(Message msg) {
        switch (msg.what) {
            case ReverseGeocodingTask.GEOCODER_RESULT:
                geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
                break;
        }        
    }
    
    private void geocoderAvailableForLocation(GeocodableLocation l) {
        if(l == lastPublishedLocation) {
            Log.v(this.toString(), "geocoder now available for lastPublishedLocation");
            updateNotification();

        } else {
            Log.v(this.toString(), "geocoder now available for an old location");           
        }
    }


    public void onEvent(Events.PublishSuccessfull e) {
        Log.v(this.toString(), "Publish successful");
        if(e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
            GeocodableLocation l = (GeocodableLocation) e.getExtra();
            
            this.lastPublishedLocation = l;
            this.lastPublishedLocationTime = e.getDate();
            
            if(sharedPreferences.getBoolean("notificationGeocoder", false))
                (new ReverseGeocodingTask(this, handler)).execute(new GeocodableLocation[] {l});
    
            updateNotification();
    
            // This is a bit hacked as we append an empty space on every second
            // ticker update. Otherwise consecutive tickers with the same text would
            // not be shown
            if(sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
                    Defaults.VALUE_TICKER_ON_PUBLISH))
                updateTicker(getString(R.string.statePublished));
    
            }
    }



    public void onEvent(Events.LocationUpdated e) {
        if(e.getGeocodableLocation() == null)
            return;
        
        Log.v(this.toString(), "LocationUpdated: " + e.getGeocodableLocation().getLatitude() + ":"
                + e.getGeocodableLocation().getLongitude());
    }
    
    public boolean isDebugBuild(){
        return 0 != ( getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE );
    }

    public static Class<?> getServiceLocatorClass() {
        return locatorClass;
    }
}
