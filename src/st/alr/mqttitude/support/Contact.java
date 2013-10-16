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

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class Contact {

    private int uid;
    private String name;
    private String topic;
    private GeocodableLocation location;
    private Color color;
    private Bitmap userImage;
    private static final int userImageHeightScale = (int) convertDpToPixel(48);
    private static Bitmap defaultUserImage = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.ic_launcher), userImageHeightScale, userImageHeightScale, true) ;    
    private static BitmapDescriptor defaultUserImageDescriptor  = BitmapDescriptorFactory.fromBitmap(defaultUserImage);  

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

    
    
    
}