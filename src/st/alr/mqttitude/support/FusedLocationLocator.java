
package st.alr.mqttitude.support;

import java.util.Date;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import de.greenrobot.event.EventBus;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

public class FusedLocationLocator extends Locator implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private final int MINUTES_TO_MILISECONDS = 60 * 1000;
    private boolean ready = false;
    private boolean foreground = false;

    public FusedLocationLocator(Context context) {
        super(context);
        setupLocationRequest();

        mLocationClient = new LocationClient(context, this, this);
    }

    @Override
    public void start() {
        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting())
            mLocationClient.connect();
    }

    @Override
    public Location getLastKnownLocation() {
        if (ready)
            return mLocationClient.getLastLocation();
        else
            return null;
    }

    @Override
    public void onLocationChanged(Location arg0) {
        Log.v(TAG, "FusedLocationLocator onLocationChanged");
        EventBus.getDefault().postSticky(new Events.LocationUpdated(mLocationClient.getLastLocation()));

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
        Log.e(TAG, "FusedLocationLocator failed to connect");
    }

    @Override
    public void onConnected(Bundle arg0) {
        ready = true;

        Log.v(TAG, "FusedLocationLocator connected");
        requestLocationUpdates();
    }

    @Override
    public void onDisconnected() {
        ready = false;

        Log.v(TAG, "FusedLocationLocator disconnected");
        disableLocationUpdates();
    }

    private void setupBackgroundLocationRequest() {
        Log.v(TAG, "setupBackgroundLocationRequest. Interval: " + getUpdateIntervall());
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(getUpdateIntervallInMiliseconds());
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setSmallestDisplacement(500);
    }

    private void setupForegroundLocationRequest() {
        Log.v(TAG, "setupForegroundLocationRequest. Interval: " + 0.5);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1 * MINUTES_TO_MILISECONDS);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setSmallestDisplacement(100);
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
