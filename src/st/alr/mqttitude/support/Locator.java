
package st.alr.mqttitude.support;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class Locator
{
    private Listener locationListener;
    private LocationManager locationManager;
    private Context context;
    LocatorCallback callback;

    public Locator(Context context) {
        locationListener = new Listener();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    
    public void get(LocatorCallback cb) {
        Log.v(this.toString(), "get");
        this.callback =  cb;
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    public void stop() {
        locationManager.removeUpdates(locationListener);
        Log.v("stopLocationListener", "locationManager stopped");
    }

    private class Listener implements LocationListener {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(this.toString(), "position receiver onStatusChanged");

            try {
                String strStatus = "";
                switch (status) {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        strStatus = "GPS_EVENT_FIRST_FIX";
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        strStatus = "GPS_EVENT_SATELLITE_STATUS";
                        break;
                    case GpsStatus.GPS_EVENT_STARTED:
                        strStatus = "GPS_EVENT_STARTED";
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        strStatus = "GPS_EVENT_STOPPED";
                        break;
                    default:
                        strStatus = String.valueOf(status);
                        break;
                }
                Log.v(this.toString(), "onStatusChanged: " + strStatus);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            try {
                if(callback != null)
                    callback.onLocationRespone(location);
           
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                locationManager.removeUpdates(this);
                callback = null;
            }
        }
    }
}
