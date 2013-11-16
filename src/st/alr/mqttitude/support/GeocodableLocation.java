package st.alr.mqttitude.support;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class GeocodableLocation extends Location  implements Serializable{
    String geocoder; 
    LatLng latlng;   
    String tag; 
    
    
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public GeocodableLocation(Location location){
        this(location, null);
    }
    public GeocodableLocation(String provider){
        super(provider);
        this.geocoder = null;
    }
    public GeocodableLocation(Location location, String geocoder){
        super(location);
        this.geocoder = geocoder;
        if(location != null)
            this.latlng = new LatLng(location.getLatitude(), location.getLongitude());
    }
    
    public JSONObject toJsonObject(){
        JSONObject o = new JSONObject();
        try {
        o.put("_type", "location");
        o.put("lat", getLatitude());
        o.put("lon", getLongitude());
        o.put("tst", getTime());
        o.put("acc", getAccuracy());
        o.put("alt", getAltitude());
        o.put("vac", 0);
        o.put("dir", getBearing());
        o.put("vel", getSpeed());
        } catch (JSONException e) {
            return o;
        }
        return o;
    }

    public static GeocodableLocation fromJsonObject(JSONObject json){
        Double lat;
        Double lon;
        Float acc;
        Long tst;
        Double alt;

        try {
            String type = json.getString("_type");
            if(!type.equals("location"))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e("GeocodableLocation", "Unable to deserialize Location object from JSON");
            return null;            
        }

        try {lat = json.getDouble("lat"); } catch(Exception e) { lat = (double) 0; };
        try {lon = json.getDouble("lon");} catch(Exception e) { lon = (double) 0; };
        try {acc = Float.parseFloat(json.getString("acc")); } catch(Exception e) { acc = (float) 0; };
        try {tst = Long.parseLong(json.getString("tst")); } catch(Exception e) { tst = (long) 0; };
        try {alt = json.getDouble("alt"); } catch(Exception e) { alt = (double) 0; };

        GeocodableLocation l = new GeocodableLocation("mqttitude-deserialized");                    
        l.setLatitude(lat);
        l.setLongitude(lon);
        l.setAccuracy(acc);
        l.setTime(tst);
        l.setAltitude(alt);
        
        return l;
    }
    
    public String getGeocoder() {
        return geocoder;
    }

    public Location getLocation() {
        return this; // compatibility fix
    }
    
    @Override
    public void setLatitude(double latitude){
        super.setLatitude(latitude);
        this.latlng = new LatLng(latitude, getLongitude());
    }
    @Override
    public void setLongitude(double longitude){
        super.setLongitude(longitude);
        this.latlng = new LatLng(getLatitude(),longitude);

    }

    public void setGeocoder(String geocoder) {
        this.geocoder = geocoder;
    }

    @Override
    public String toString() {
        if(geocoder != null)
            return geocoder;
        else
            return toLatLonString();
    }
    public String toLatLonString(){
        return getLatitude() + " : " + getLongitude();
    }
    public LatLng getLatLng() {
        return latlng;
    }
    
    
    
}