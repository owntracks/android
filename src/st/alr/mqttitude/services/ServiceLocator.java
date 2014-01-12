
package st.alr.mqttitude.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import st.alr.mqttitude.App;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.db.WaypointDao.Properties;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.model.Report;
import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.MqttPublish;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, MqttPublish, 
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener,  LocationClient.OnRemoveGeofencesResultListener,  LocationClient.OnAddGeofencesResultListener {
    
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
    private List<Waypoint> waypoints;
    private WaypointDao waypointDao;
    

    public void onCreate(ServiceProxy p) {
       

                context = p;
                waypointDao = ServiceProxy.getServiceApplication().getWaypointDao();
                loadWaypoints();
        
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
    
    public void onFenceTransition(Intent intent) {
        int transitionType = LocationClient.getGeofenceTransition(intent);
        
        // Test that a valid transition was reported
        if ( (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)  || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) ) {
            List <Geofence> triggerList = LocationClient.getTriggeringGeofences(intent);

            for (int i = 0; i < triggerList.size(); i++) {
               
                Waypoint w = waypointDao.queryBuilder().where(Properties.GeofenceId.eq(triggerList.get(i).getRequestId())).limit(1).unique();

                Log.v(this.toString(), "Waypoint triggered " + w.getDescription() + " transition: " + transitionType);
                
                if(w != null)
                    publishGeofenceTransitionEvent(w, transitionType);
            }
        }          
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
        
        setupGeofences();
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
                
            } else if (intent.getAction().equals(Defaults.INTENT_ACTION_FENCE_TRANSITION)) {
                Log.v(this.toString(), "Geofence transition occured");
                onFenceTransition(intent);
                
            } else {
                Log.v(this.toString(), "Received unknown intent");
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
    
    public void publishGeofenceTransitionEvent(Waypoint w, int transition) {
        Report r = new Report(getLastKnownLocation());
        r.setTransition(transition);
        r.setWaypoint(w);
        publish(r);
        
    }
    
    public void publishLastKnownLocation() {
        publish(null);
    }
    
    public void publish(Report r) {
        lastPublish = new Date();        
        
        // Safety checks
        if(ServiceProxy.getServiceBroker() == null) {
            Log.e(this.toString(), "publishLastKnownLocation but ServiceMqtt not ready");
            return;
        }
        
        if (r == null && getLastKnownLocation() == null) {
            changeState(Defaults.State.ServiceLocator.NOLOCATION);
            return;
        }
        
        String topic = ActivityPreferences.getPubTopic(true);
        if (topic == null) {
            changeState(Defaults.State.ServiceLocator.NOTOPIC);
            return;
        }
        
        Report report; 
        if(r == null)
            report = new Report(getLastKnownLocation());
        else
            report = r;
        
                                  
        if(ActivityPreferences.includeBattery())
            report.setBattery(App.getBatteryLevel());

        ServiceProxy.getServiceBroker().publish(
                topic,
                report.toString(),
                sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_RETAIN, Defaults.VALUE_RETAIN),
                Integer.parseInt(sharedPreferences.getString(Defaults.SETTINGS_KEY_QOS, Defaults.VALUE_QOS))
                , 20, this, report.getLocation());

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
        
    public void loadWaypoints() {
        this.waypoints = waypointDao.loadAll();
    }
    
    public void onEvent(Events.WaypointAdded e) {

        if(!isWaypointWithGeofence(e.getWaypoint()))
            return;
        
        Log.v(this.toString(), "adding geofence");
        
        loadWaypoints();
        setupGeofences();
    }
    
    public void onEvent(Events.WaypointUpdated e) {
        
        if(!isWaypointWithGeofence(e.getWaypoint()))
            return;

        Log.v(this.toString(), "updating geofence");
        
        removeGeofence(e.getWaypoint());                
        loadWaypoints();
        setupGeofences();
    }

    
    private boolean isWaypointWithGeofence(Waypoint w) {
        return w.getRadius() != null && w.getRadius() > 0;
    }
    
    public void onEvent(Events.WaypointRemoved e) {
        if(!isWaypointWithGeofence(e.getWaypoint()))
            return;
        
        Log.v(this.toString(), "removing geofence");

        
        
        removeGeofence(e.getWaypoint());
        loadWaypoints();
        setupGeofences();
    }
    
    private void setupGeofences() {
        if(!ready)
            return;
        List<Geofence> fences = new ArrayList<Geofence>();

        
        
        for (Waypoint w : waypoints) {
            if(!isWaypointWithGeofence(w))
                continue;
            
            if(w.getGeofenceId() == null) {
                w.setGeofenceId(UUID.randomUUID().toString());
                waypointDao.update(w);
            } 
            
            int transitionType;
            switch (w.getTransitionType()) {
                case 0: 
                    transitionType = Geofence.GEOFENCE_TRANSITION_ENTER;
                    break;
                case 1:
                    transitionType = Geofence.GEOFENCE_TRANSITION_EXIT;
                    break;
                case 2: 
                    transitionType = Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_ENTER;
                    break; 
                default: 
                    transitionType = Geofence.GEOFENCE_TRANSITION_ENTER;
            }
            Log.v(this.toString() , "id " + w.getGeofenceId());
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(w.getGeofenceId())
                    .setTransitionTypes(transitionType)
                    .setCircularRegion(w.getLatitude(), w.getLongitude(), w.getRadius()).setExpirationDuration(Geofence.NEVER_EXPIRE).build();            
            
            fences.add(geofence);            
        }
        
        if(fences.size() == 0) {
            Log.v(this.toString(), "no geofences to add");
            return;
        }
        
        Log.v(this.toString(), "adding geofences");
        mLocationClient.addGeofences(fences, ServiceProxy.getPendingIntentForService(context,
                ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_FENCE_TRANSITION, null), this);


                        
        
    }
    
    private void removeGeofence(Waypoint w) {
        ArrayList<String> l = new ArrayList<String>();
        l.add(w.getGeofenceId());
        removeGeofences(l);      

        w.setGeofenceId(null);
        waypointDao.update(w);

    }
    
    private void removeGeofences(List<String> ids) {
        mLocationClient.removeGeofences(ids, this);
    }
    
    public void onEvent(Object event){}

    @Override
    public void onAddGeofencesResult(int arg0, String[] arg1) {
        if (LocationStatusCodes.SUCCESS == arg0) {
            for (int i = 0; i < arg1.length; i++) {
                Log.v(this.toString(), "geofence "+ arg1[i] +" added");

            }
            Toast.makeText(context, "Geofences added",Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(context, "Unable to add Geofence",Toast.LENGTH_SHORT).show();
            Log.v(this.toString(), "geofence adding failed");        }
        
        
    }

    @Override
    public void onRemoveGeofencesByPendingIntentResult(int arg0, PendingIntent arg1) {
        if (LocationStatusCodes.SUCCESS == arg0) {
            Log.v(this.toString(), "geofence removed");
        } else {
            Log.v(this.toString(), "geofence removing failed");        }

    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int arg0, String[] arg1) {
        if (LocationStatusCodes.SUCCESS == arg0) {
            for (int i = 0; i < arg1.length; i++) {
                Log.v(this.toString(), "geofence "+ arg1[i] +" removed");
            }
        } else {
            Log.v(this.toString(), "geofence removing failed");        }
        
    }
}
