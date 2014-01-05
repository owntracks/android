
package st.alr.mqttitude.services;

import java.util.Date;

import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.model.Report;
import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.MqttPublish;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, MqttPublish, 
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    
    private SharedPreferences sharedPreferences;
    private OnSharedPreferenceChangeListener preferencesChangedListener;
    private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
    private ServiceProxy context;

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private boolean ready = false;
    private boolean foreground = false;

    private GeocodableLocation lastKnownLocation;
    private Date lastPublish;
    

    

    public void onCreate(ServiceProxy p) {
       

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

                
        mLocationClient = new LocationClient(context, this, this);


        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting() && ServiceApplication.checkPlayServices())
            mLocationClient.connect();

    }

    public GeocodableLocation getLastKnownLocation() {
        return lastKnownLocation;
    }

    @Override
    public void onLocationChanged(Location arg0) {
        Log.v(this.toString(), "onLocationChanged");
        this.lastKnownLocation = new GeocodableLocation(arg0);

        EventBus.getDefault().postSticky(new Events.LocationUpdated(this.lastKnownLocation));

        if (shouldPublishLocation())
            publishLastKnownLocation();        
    }

    private boolean shouldPublishLocation() {
        Date now = new Date();
        if (lastPublish == null)
            return true;

        if (now.getTime() - lastPublish.getTime() > getUpdateIntervallInMiliseconds())
            return true;

        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(this.toString(), "Failed to connect");
    }

    @Override
    public void onConnected(Bundle arg0) {
        ready = true;

        Log.v(this.toString(), "Connected");
        setupLocationRequest();
        requestLocationUpdates();
    }

    @Override
    public void onDisconnected() {
        ready = false;
        ServiceApplication.checkPlayServices(); // show error notification if play services were disabled
    }

    private void setupBackgroundLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(getUpdateIntervallInMiliseconds());
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(500);
    }

    private void setupForegroundLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10 * 1000);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(50);
    }

    protected void handlePreferences() {
        setupLocationRequest();
        requestLocationUpdates();
    }

    private void disableLocationUpdates() {
        Log.v(this.toString(), "Disabling updates");
        if (mLocationClient != null) {
            mLocationClient.removeLocationUpdates(ServiceProxy.getPendingIntentForService(context,
                    ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_LOCATION_CHANGED, null, 0));
        }
    }

    private void requestLocationUpdates() {
        if (!ready) {
            Log.e(this.toString(), "requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }

        if (foreground || areBackgroundUpdatesEnabled()) {
//            locationIntent = ServiceProxy.getPendingIntentForService(context,
//                    ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_LOCATION_CHANGED, null);

            mLocationClient.requestLocationUpdates(mLocationRequest, ServiceProxy.getPendingIntentForService(context,
                    ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_LOCATION_CHANGED, null));

        } else {
            Log.d(this.toString(), "Location updates are disabled (not in foreground or background updates disabled)");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {            
            if (intent.getAction().equals(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN)) {
                publishLastKnownLocation();
            } else if (intent.getAction().equals(Defaults.INTENT_ACTION_LOCATION_CHANGED)) {
                Location location = intent.getParcelableExtra(LocationClient.KEY_LOCATION_CHANGED);

                if (location != null) 
                    onLocationChanged(location);
            }

        }

        return 0;
    }

    private void setupLocationRequest() {
        if(!ready)
            return;
        
        disableLocationUpdates();

        if (foreground)
            setupForegroundLocationRequest();
        else
            setupBackgroundLocationRequest();
    }

    public void enableForegroundMode() {
        Log.d(this.toString(), "enableForegroundMode");
        foreground = true;
        setupLocationRequest();
        requestLocationUpdates();
    }

    public void enableBackgroundMode() {
        Log.d(this.toString(), "enableBackgroundMode");
        foreground = false;
        setupLocationRequest();
        requestLocationUpdates();
    }

    @Override
    public void onDestroy() {
        Log.v(this.toString(), "onDestroy. Disabling location updates");
        disableLocationUpdates();
    }
 


    
    public void publishLastKnownLocation() {
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
        

        Report r = new Report(l);
        ServiceProxy.getServiceBroker().publish(
                topic,
                r.toString(),
                sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_RETAIN, Defaults.VALUE_RETAIN),
                Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_QOS, Defaults.VALUE_QOS))
                , 20, this, l);

    }

    @Override
    public void publishSuccessfull(Object extra) {
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
        
    public void onEvent(Object event){}
}
