package st.alr.mqttitude.support;

import java.util.Date;

import android.location.Location;
import android.util.Log;

public class GeocodableLocation {
    String geocoder; 
    Location location;
    public GeocodableLocation(Location location){
        this(location, null);
    }
    public GeocodableLocation(Location location, String geocoder){
        this.geocoder = geocoder;
        this.location = location;
    }

    public String getGeocoder() {
        return geocoder;
    }

    public Location getLocation() {
        return location;
    }

    public void setGeocoder(String geocoder) {
        Log.v("Geocodable location", "setting geocoder" );
        this.geocoder = geocoder;
    }

    public double getLatitude() {
        return location.getLatitude();
    }

    public double getLongitude() {
        return location.getLongitude();
    }
    public String toString() {
        if(geocoder != null)
            return geocoder;
        else
            return toLatLonString();
    }
    public String toLatLonString(){
        return getLatitude() + " : " + getLongitude();

    }
    
}