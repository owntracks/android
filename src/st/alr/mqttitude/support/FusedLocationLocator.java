package st.alr.mqttitude.support;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import de.greenrobot.event.EventBus;

import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class FusedLocationLocator extends Locator implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener{
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private final int MINUTES_TO_MILISECONDS = 60*1000;
    private boolean ready = false;
    
    public FusedLocationLocator(Context context) {
        super(context);
        setupLocationRequest();

        mLocationClient = new LocationClient(context, this, this);
    }
    
    public void start() {
        if(!mLocationClient.isConnected() && !mLocationClient.isConnecting())
            mLocationClient.connect();
    }

    @Override
    public Location getLastKnownLocation() {
        if(ready)
            return mLocationClient.getLastLocation();
        else 
            return null;
    }

    @Override
    public void onLocationChanged(Location arg0) {
        Log.v(TAG, "FusedLocationLocator onLocationChanged");
        EventBus.getDefault().postSticky(new Events.LocationUpdated(mLocationClient.getLastLocation()));

        if(automaticUpdatesEnabled())
            publishLastKnownLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "FusedLocationLocator failed to connect");
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.v(TAG, "FusedLocationLocator connected");
        ready = true;
        requestAutomaticLocationUpdates();
    }

    @Override
    public void onDisconnected() {
        Log.v(TAG, "FusedLocationLocator disconnected");
        ready = false;
    }

    
    private void setupLocationRequest() {        
        Log.v(TAG, "setupLocationRequest. Interval: " + getUpdateIntervall());
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(getUpdateIntervallInMiliseconds());
        mLocationRequest.setFastestInterval(getUpdateIntervallInMiliseconds());
    }
    
    @Override
    protected void handlePreferences() {
        if(mLocationRequest != null) {
            mLocationClient.removeLocationUpdates(this);
        }
        
        setupLocationRequest();
        
        requestAutomaticLocationUpdates();
    }
    
    private boolean automaticUpdatesEnabled(){
        return sharedPreferences.getBoolean("automaticPublishes", false);
    }
    
    private void requestAutomaticLocationUpdates(){
        if(automaticUpdatesEnabled()) 
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        else 
            Log.e(TAG, "Location updates are disabled");
    }

}
