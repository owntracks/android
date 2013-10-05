package st.alr.mqttitude.support;

import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;

public class Friend {

    private int uid;
    private String name;
    private String mqqtUsername;
    private GeocodableLocation location;
    private BitmapDescriptor markerImage;
    private BitmapDescriptor staleMarkerImage;
    private Color color;
    
    public Friend(){
            
    }
    
    public int getUid() {
        return uid;
    }
    public void setUid(int uid) {
        this.uid = uid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getMqqtUsername() {
        return mqqtUsername;
    }
    public void setMqqtUsername(String mqqtUsername) {
        this.mqqtUsername = mqqtUsername;
    }
    public GeocodableLocation getLocation() {
        return location;
    }

    
    public void setLocation(GeocodableLocation location) {
        this.location = location;
    }

    public BitmapDescriptor getMarkerImage() {
        return markerImage;
    }
    public void setMarkerImage(BitmapDescriptor markerImage) {
        this.markerImage = markerImage;
    }
    public BitmapDescriptor getStaleMarkerImage() {
        return staleMarkerImage;
    }
    public void setStaleMarkerImage(BitmapDescriptor staleMarkerImage) {
        this.staleMarkerImage = staleMarkerImage;
    }
    public Color getColor() {
        return color;
    }
    public void setColor(Color color) {
        this.color = color;
    }
    public String toString() {
        if(getName() != null)
            return name;
        else 
            return mqqtUsername;
    }
    
    
    
    
}