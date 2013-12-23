
package st.alr.mqttitude.services;

import java.util.Date;

import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import de.greenrobot.event.EventBus;

public class ServiceLocatorFused extends ServiceLocator implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private boolean ready = false;
    private boolean foreground = false;
    private final String TAG = "ServiceLocatorFused";
    private GeocodableLocation lastKnownLocation;
    private PendingIntent locationIntent;

    public void onCreate(ServiceProxy p) {
        super.onCreate(p);
        Log.v(this.toString(), "onCreate");

        setupLocationRequest();
        mLocationClient = new LocationClient(context, this, this);

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
        mLocationRequest.setInterval(10 * 1000);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(50);
    }

    @Override
    protected void handlePreferences() {
        setupLocationRequest();
        requestLocationUpdates();
    }

    private void disableLocationUpdates() {
        if (ready && mLocationRequest != null && locationIntent != null) {
            mLocationClient.removeLocationUpdates(locationIntent);
            mLocationRequest = null;
        }
    }

    private void requestLocationUpdates() {
        if (!ready) {
            Log.e(TAG,
                    "requestLocationUpdates but not connected. Updates will be requested again once connected");
            return;
        }

        if (foreground || areBackgroundUpdatesEnabled()) {
            locationIntent = ServiceProxy.getPendingIntentForService(context,
                    ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_LOCATION_CHANGED, null);

            mLocationClient.requestLocationUpdates(mLocationRequest, locationIntent);

        } else {
            Log.d(TAG, "Location updates are disabled (not in foreground or background updates disabled)");
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

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub

    }

}
