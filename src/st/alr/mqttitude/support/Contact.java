package st.alr.mqttitude.support;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceApplication;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Contact {

    private int uid;
    private String name;
    private String topic;
    private GeocodableLocation location;
    private Bitmap userImage;
    private static final int userImageHeightScale = (int) convertDpToPixel(48);
    private static Bitmap defaultUserImage = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.noimage), userImageHeightScale, userImageHeightScale, true) ;    
    private static BitmapDescriptor defaultUserImageDescriptor  = BitmapDescriptorFactory.fromBitmap(defaultUserImage);  
    private Marker marker;
    private View view;
 

    public Contact(String topic) {
        this.topic = topic;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public Marker getMarker(){
        return marker;
    }

    public void updateMarkerPosition(){
        if(marker != null && location.getLatLng() != null)
            marker.setPosition(location.getLatLng());
        else
            Log.e(this.toString(), "update of marker position requested, but no marker set");
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
    public GeocodableLocation getLocation() {
        return location;
    }
    
    public void setLocation(GeocodableLocation location) {
        this.location = location;
        location.setTag(this.topic);// to find according contact once geocoder resolving returns
    }

  public String toString() {
        if(getName() != null)
            return name;
        else 
            return topic;
    }

    public void setUserImage(Bitmap image) {
         this.userImage = Bitmap.createScaledBitmap(image, userImageHeightScale, userImageHeightScale, true);
    }
    
    public Bitmap getUserImage() {
        return this.userImage != null ? this.userImage : defaultUserImage;
    }
    
    public BitmapDescriptor getUserImageDescriptor() {
        return this.userImage != null? BitmapDescriptorFactory.fromBitmap(getUserImage()) : defaultUserImageDescriptor;
    }
    public void setTopic(String topic) {
        this.topic = topic;
    }
    
    public static float convertDpToPixel(float dp){
        return dp * (App.getContext().getResources().getDisplayMetrics().densityDpi / 160f);
    }

    public String getTopic() {
        return topic;
    }

    
    
    
}