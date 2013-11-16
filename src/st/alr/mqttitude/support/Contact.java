package st.alr.mqttitude.support;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceApplication;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.Matrix;
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
    private static Bitmap markerBackground = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.markerbg), (int)convertDpToPixel(57), (int)convertDpToPixel(62), true) ;    

    private static BitmapDescriptor defaultUserImageDescriptor  = BitmapDescriptorFactory.fromBitmap(defaultUserImage); 
    private static BitmapDescriptor defaultUserMarkerDescriptor  = BitmapDescriptorFactory.fromBitmap(combineImages(markerBackground, defaultUserImage));  

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
    
    public BitmapDescriptor getMarkerImageDescriptor(){
        return this.userImage != null? BitmapDescriptorFactory.fromBitmap(combineImages(markerBackground, getUserImage())) : defaultUserMarkerDescriptor;
    }



    public static Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom 
        Bitmap cs = null; 

        int width, height = 0; 

          width = c.getWidth();
          height = c.getHeight(); 

        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 

        Canvas comboImage = new Canvas(cs); 

        comboImage.drawBitmap(c, 0f, 0f, null); 
        comboImage.drawBitmap(s, 8f, 8f, null); 

        // this is an extra bit I added, just incase you want to save the new image somewhere and then return the location 
        /*String tmpImg = String.valueOf(System.currentTimeMillis()) + ".png"; 

        OutputStream os = null; 
        try { 
          os = new FileOutputStream(loc + tmpImg); 
          cs.compress(CompressFormat.PNG, 100, os); 
        } catch(IOException e) { 
          Log.e("combineImages", "problem combining images", e); 
        }*/ 

        return cs; 
      } 

    
//    public static Bitmap get_ninepatch(int id,int x, int y, Context context){
//        // id is a resource id for a valid ninepatch
//        NinePatchDrawable bg =  (NinePatchDrawable) ServiceApplication.getInstance().getResources().getDrawable(id);
//        if (bg != null) {
//          bg.setBounds(0, 0, getWidth(), getHeight());
//          bg.draw(canvas);
//          }
//
//    }

}