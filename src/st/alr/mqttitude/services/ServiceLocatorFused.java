
package st.alr.mqttitude.services;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.Date;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;

public class ServiceLocatorFused extends ServiceLocator implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private final int MINUTES_TO_MILISECONDS = 60 * 1000;
    private boolean ready = false;
    private boolean foreground = false;
    private final String TAG = "ServiceLocatorFused";
    private GeocodableLocation lastKnownLocation;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(this.toString(), "onCreate");

        setupLocationRequest();
        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public void onStartOnce(){
        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting())
            mLocationClient.connect();        
    }
    
    @Override
    public GeocodableLocation getLastKnownLocation() {
        return lastKnownLocation;
    }

    @Override
    public void onLocationChanged(Location arg0) {
        Log.v(TAG, "ServiceLocatorFused onLocationChanged");
        this.lastKnownLocation = new GeocodableLocation(arg0);
        
        EventBus.getDefault().postSticky(new Events.LocationUpdated(this.lastKnownLocation));

        if (shouldPublishLocation()) {
            Log.d(TAG, "should publish");
            publishLastKnownLocation();
        }
    }

    private boolean shouldPublishLocation() {
        Date now = new Date();
        if (lastPublish == null)
            return true;
        Log.v(TAG, "time: " + (now.getTime() - lastPublish.getTime()));
        Log.v(TAG, "interval: " + getUpdateIntervallInMiliseconds());

        if (now.getTime() - lastPublish.getTime() > getUpdateIntervallInMiliseconds())
            return true;

        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "ServiceLocatorFused failed to connect");
    }

    @Override
    public void onConnected(Bundle arg0) {
        ready = true;

        Log.v(TAG, "ServiceLocatorFused connected");
        requestLocationUpdates();
    }

    @Override
    public void onDisconnected() {
        ready = false;

        Log.v(TAG, "ServiceLocatorFused disconnected");
        disableLocationUpdates();
    }

    private void setupBackgroundLocationRequest() {
        Log.v(TAG, "setupBackgroundLocationRequest. Interval: " + getUpdateIntervall());
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(getUpdateIntervallInMiliseconds());
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(500);
    }

    private void setupForegroundLocationRequest() {
        Log.v(TAG, "setupForegroundLocationRequest. Interval: " + 10);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(10*1000);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(5);
    }

    @Override
    protected void handlePreferences() {
        setupLocationRequest();
        requestLocationUpdates();
    }

    private void disableLocationUpdates() {
        if (ready && mLocationRequest != null) {
            mLocationClient.removeLocationUpdates(this);
            mLocationRequest = null;
        }
    }

    private void requestLocationUpdates() {
        if (!ready) {
            Log.e(TAG,
                    "requestLocationUpdates but not connected. Updates will be requested again once connected");
            return;
        }

        if (foreground || areBackgroundUpdatesEnabled())
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        else
            Log.d(TAG,
                    "Location updates are disabled (not in foreground or background updates disabled)");
    }

    private void setupLocationRequest() {
        disableLocationUpdates();

        if (foreground)
            setupForegroundLocationRequest();
        else
            setupBackgroundLocationRequest();
    }

    @Override
    public void enableForegroundMode() {
        Log.d(TAG, "enableForegroundMode");
        foreground = true;
        setupLocationRequest();
        requestLocationUpdates();
    }

    @Override
    public void enableBackgroundMode() {
        foreground = false;
        Log.d(TAG, "enableBackgroundMode");

        setupLocationRequest();
        requestLocationUpdates();
    }

}
