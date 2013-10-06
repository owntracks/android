package st.alr.mqttitude.support;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceApplication;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class Friend {

    private int uid;
    private String name;
    private String mqqtUsername;
    private GeocodableLocation location;
    private Color color;
    private Bitmap userImage;
    private static Bitmap defaultUserImage = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.ic_launcher);    
    private static BitmapDescriptor defaultUserImageDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher);  

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

    public void setUserImage(Bitmap image) {
        this.userImage = image;
    }
    
    public Bitmap getUserImage() {
        return this.userImage != null ? this.userImage : defaultUserImage;
    }
    
    public BitmapDescriptor getUserImageDescriptor() {
        return this.userImage != null? BitmapDescriptorFactory.fromBitmap(getUserImage()) : defaultUserImageDescriptor;
    }
    
    
    
    
}