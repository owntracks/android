package st.alr.mqttitude.support;

import java.io.InputStream;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceProxy;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;

public class Contact {

    private int uid;
    private String name;
    private String topic;
    private GeocodableLocation location;
    private Bitmap userImage;
    private static final int userImageHeightScale = (int) convertDpToPixel(48);

    public static Bitmap defaultUserImage = getRoundedShape(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(ServiceProxy.getInstance().getResources(), R.drawable.noimage), userImageHeightScale, userImageHeightScale, true)) ;    
    //private static Bitmap markerBackground = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.markerbg), (int)convertDpToPixel(57), (int)convertDpToPixel(62), true) ;    

   // private static BitmapDescriptor defaultUserImageDescriptor  = BitmapDescriptorFactory.fromBitmap(defaultUserImage); 
   // private static BitmapDescriptor defaultUserMarkerDescriptor  = BitmapDescriptorFactory.fromBitmap(combineImages(markerBackground, defaultUserImage));  

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

  @Override
public String toString() {
        if(getName() != null)
            return name;
        else 
            return topic;
    }

    public void setUserImage(Bitmap image) {
         if(image != null)
             this.userImage = getRoundedShape(Bitmap.createScaledBitmap(image, userImageHeightScale, userImageHeightScale, true));
    }
    
    public Bitmap getUserImage() {
        return this.userImage != null ? this.userImage : defaultUserImage;
    }
    
    public BitmapDescriptor getUserImageDescriptor() {
        return this.userImage != null? BitmapDescriptorFactory.fromBitmap(getUserImage()) : BitmapDescriptorFactory.fromBitmap(defaultUserImage);
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
        return this.userImage != null? BitmapDescriptorFactory.fromBitmap(getUserImage()) : BitmapDescriptorFactory.fromBitmap(defaultUserImage);
    }



//    public static Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom 
//        Bitmap cs = null; 
//
//        int width, height = 0; 
//
//          width = c.getWidth();
//          height = c.getHeight(); 
//
//        cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
//
//        Canvas comboImage = new Canvas(cs); 
//
//        comboImage.drawBitmap(c, 0f, 0f, null); 
//        comboImage.drawBitmap(s, 8f, 8f, null); 
//
//        // this is an extra bit I added, just incase you want to save the new image somewhere and then return the location 
//        /*String tmpImg = String.valueOf(System.currentTimeMillis()) + ".png"; 
//
//        OutputStream os = null; 
//        try { 
//          os = new FileOutputStream(loc + tmpImg); 
//          cs.compress(CompressFormat.PNG, 100, os); 
//        } catch(IOException e) { 
//          Log.e("combineImages", "problem combining images", e); 
//        }*/ 
//
//        return cs; 
//      } 

    
//    public static Bitmap get_ninepatch(int id,int x, int y, Context context){
//        // id is a resource id for a valid ninepatch
//        NinePatchDrawable bg =  (NinePatchDrawable) ServiceApplication.getInstance().getResources().getDrawable(id);
//        if (bg != null) {
//          bg.setBounds(0, 0, getWidth(), getHeight());
//          bg.draw(canvas);
//          }
//
//    }
    public static Bitmap resolveImage(ContentResolver cr, long id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        Log.v("loadContactPhoto", "using URI " + uri);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }
    

    
//    public static Bitmap getRoundedShape(Bitmap scaleBitmapImage) {
//        // TODO Auto-generated method stub
//         int targetWidth = 50;
//         int targetHeight = 50;
//         Bitmap targetBitmap = Bitmap.createBitmap(targetWidth, 
//                               targetHeight,Bitmap.Config.ARGB_8888);
//
//         Canvas canvas = new Canvas(targetBitmap);
//         Path path = new Path();
//         path.addCircle(((float) targetWidth - 1) / 2,
//         ((float) targetHeight - 1) / 2,
//         (Math.min(((float) targetWidth), 
//                   ((float) targetHeight)) / 2),
//             Path.Direction.CCW);
//
//         canvas.clipPath(path);
//         Bitmap sourceBitmap = scaleBitmapImage;
//         canvas.drawBitmap(sourceBitmap, 
//                                   new Rect(0, 0, sourceBitmap.getWidth(),
//         sourceBitmap.getHeight()), 
//                                   new Rect(0, 0, targetWidth,
//         targetHeight), null);
//         return targetBitmap;
//        }

    
    public static Bitmap getRoundedShape(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
            bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
     
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = bitmap.getWidth();
     
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
     
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
     
        return output;
      }



}