
package st.alr.mqttitude;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Events.LocationUpdated;
import st.alr.mqttitude.support.Events.MqttConnectivityChanged;
import st.alr.mqttitude.R;
import st.alr.mqttitude.R.menu;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.LayerDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {
    MenuItem publish;
    TextView location;
    TextView statusLocator;
    TextView statusLastupdate;
    TextView statusServer;
    private GoogleMap mMap;

    private ShareActionProvider mShareActionProvider;
    private TextView locationAccuracy;
    private TextView locationLatlong;
    private TextView locationAddress;
    private TextView locationZipcode;
    private Marker mMarker;
    private Circle mCircle;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_settings) {
            Intent intent1 = new Intent(this, ActivityPreferences.class);
            startActivity(intent1);
            return true;
        }
            //        } else if (itemId == R.id.menu_publish) {
//            App.getInstance().getLocator().publishLastKnownLocation();
//            return true;
//        } else if (itemId == R.id.menu_share) {
//            if (mShareActionProvider != null) {
//
//                
// //                   mShareActionProvider.setShareIntent(shareIntent);
//            }
//            Location l = App.getInstance().getLocator().getLastKnownLocation();
//            if(l != null){
//                Intent sendIntent = new Intent();
//                sendIntent.setAction(Intent.ACTION_SEND);
//                sendIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + "," + Double.toString(l.getLongitude()));
//                sendIntent.setType("text/plain");
//                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.shareLocation)));
//      }
//            return true;
//        } else {
            return super.onOptionsItemSelected(item);
//        }
   }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        // Hide the zoom controls as the button panel will cover it.
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setMyLocationEnabled(false);
        mMap.setTrafficEnabled(false);
       
        
        // Add lots of markers to the map.
//        addMarkersToMap();

        // Setting an info window adapter allows us to change the both the contents and look of the
        // info window.
//        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Set listeners for marker events.  See the bottom of this class for their behavior.
//        mMap.setOnMarkerClickListener(this);
//        mMap.setOnInfoWindowClickListener(this);
//        mMap.setOnMarkerDragListener(this);

//        // Pan to see all markers in view.
//        // Cannot zoom to bounds until the map has a size.
//        final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
//        if (mapView.getViewTreeObserver().isAlive()) {
//            mapView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
//                @SuppressWarnings("deprecation") // We use the new method when supported
//                @SuppressLint("NewApi") // We check which build version we are using.
//                @Override
//                public void onGlobalLayout() {
//                    LatLngBounds bounds = new LatLngBounds.Builder()
//                            .include(PERTH)
//                            .include(SYDNEY)
//                            .include(ADELAIDE)
//                            .include(BRISBANE)
//                            .include(MELBOURNE)
//                            .build();
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
//                      mapView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                    } else {
//                      mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                    }
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
//                }
//            });
//        }
        

    }
    
    @Override
    protected void onStart() {
        super.onStart();

        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        App.getInstance().getLocator().enableForegroundMode();
    }

    @Override
    protected void onPause(){
        App.getInstance().getLocator().enableBackgroundMode();
        super.onPause();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
//        // Locate MenuItem with ShareActionProvider
//        MenuItem item = menu.findItem(R.id.menu_share);

//        // Fetch and store ShareActionProvider
//        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        return true;
    }

    /**
     * @category START
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();

        
        final LinearLayout draggableView = (LinearLayout) findViewById(R.id.draggableView);
        final LinearLayout expandablePart = (LinearLayout) findViewById(R.id.expandablePart);
        
        final LinearLayout visiblePart = (LinearLayout) findViewById(R.id.visiblePart);
        ViewTreeObserver vto = visiblePart.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)draggableView.getLayoutParams();
                Log.v(this.toString(), "height of expandable part: " + expandablePart.getHeight());
                params.setMargins(0, 0, 0, -expandablePart.getHeight()); //substitute parameters for left, top, right, bottom
                draggableView.setLayoutParams(params);

                ViewTreeObserver obs = visiblePart.getViewTreeObserver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    obs.removeOnGlobalLayoutListener(this);
                } else {
                    obs.removeGlobalOnLayoutListener(this);
                }
            }

        });
        
        
//        view.setLayoutParams(...);  // set to (x, y)
//        
//     // then animate the view translating from (0, 0)
//     TranslationAnimation ta = new TranslateAnimation(-x, -y, 0, 0);
//     ta.setDuration(1000);
//     view.startAnimation(ta);

//        visiblePart.setOnTouchListener(new OnTouchListener()
//        {
//            PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
//            PointF StartPT = new PointF(); // Record Start Position of 'img'
//            int minMargin = -expandablePart.getHeight();
//            int maxMargin = 0;
//            
//            @Override
//            public boolean onTouch(View v, MotionEvent event)
//            {
//                int eid = event.getAction();
//                switch (eid)
//                {
//                    case MotionEvent.ACTION_MOVE :
//                      //  Log.v(this.toString(), "MOVE");
//                        
//                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)draggableView.getLayoutParams();
////                        Log.v(this.toString(), "bottom margin: " + );
//
//                        
//                        //params.setMargins(0, 0, 0, (int) -(expandablePart.getHeight() - (event.getY() -DownPT.y))); //substitute parameters for left, top, right, bottom
//                        //draggableView.setLayoutParams(params);
////                        
//                        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams)draggableView.getLayoutParams();
//                        p.setMargins(0, 0, 0, (int) Math.min(-expandablePart.getHeight() + Math.max(DownPT.y - event.getY(),0), 0)); //substitute parameters for left, top, right, bottom
//                        draggableView.setLayoutParams(p);
// 
//                        //                        PointF mv = new PointF( event.getX() - DownPT.x, event.getY() - DownPT.y);
////                        img.setX((int)(StartPT.x+mv.x));
////                        img.setY((int)(StartPT.y+mv.y));
////                        StartPT = new PointF  ( img.getX(), img.getY() );
//                        break;
//                    case MotionEvent.ACTION_DOWN :
//                      //  Log.v(this.toString(), "DOWN");
////
//                        DownPT.x = event.getX();
//                        DownPT.y = event.getY();
////                        StartPT = new PointF( img.getX(), img.getY() );
//                        break;
//                    case MotionEvent.ACTION_UP :
//                     //   Log.v(this.toString(), "UP");
//
//                        // Nothing have to do
//                        break;
//                    default :
//                        break;
//                }
//                return true;
//            }
//
//        });


        
        locationAddress = (TextView) findViewById(R.id.locationAddress);
      //  locationLatlong = (TextView) findViewById(R.id.locationLatlong);
        locationZipcode = (TextView) findViewById(R.id.locationZipcode);
//        statusLocator = (TextView) findViewById(R.id.locatorSubtitle);
//        statusLastupdate = (TextView) findViewById(R.id.lastupdateSubtitle);
//        statusServer = (TextView) findViewById(R.id.brokerSubtitle);
//
//        setLocatorStatus();
//        setLastupdateStatus();
//        setBrokerStatus();
        
        EventBus.getDefault().register(this);

    }

    
    
    public void onEvent(Events.LocationUpdated e) {
        setLocation(e.getLocation());
    }
    public void onEventMainThread(Events.StateChanged e) {
        setLocatorStatus();
    }
    public void onEventMainThread(Events.PublishSuccessfull e) {
        setLastupdateStatus();
    }
    public void onEventMainThread(Events.MqttConnectivityChanged e) {
        Log.v(this.toString(), "connectivity changed");
        setBrokerStatus();
    }
    
    public void setLocation(Location l){
        LatLng latlong = new LatLng(l.getLatitude(), l.getLongitude());
        CameraUpdate center= CameraUpdateFactory.newLatLng(latlong);
        CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
        
        if(mMarker != null)
          mMarker.remove();  

        if(mCircle != null)
            mCircle.remove();  

        mMarker = mMap.addMarker(new MarkerOptions().position(latlong).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        
//        if(l.getAccuracy() < 20) {
//            mCircle = mMap.addCircle(new CircleOptions().center(latlong).radius(l.getAccuracy()).strokeColor(0x330072ff).fillColor(0x260072ff).strokeWidth(3));
//        }
        
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

       // locationLatlong.setText(l.getLatitude() + " / " + l.getLongitude());
       // locationAccuracy.setText("±" + Math.round(l.getAccuracy()*100)/100.0d+"m");
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(addresses != null && addresses.size() > 0) {
            Address a = addresses.get(0);

            locationAddress.setText(a.getAddressLine(0));
            locationZipcode.setText(a.getAddressLine(1));

        }
    }
    
    public void setLocatorStatus(){
//        statusLocator.setText(App.getInstance().getLocator().getStateAsText());
    }
    public void setBrokerStatus() {
//        statusServer.setText(ServiceMqtt.getConnectivityText());
    }
    public void setLastupdateStatus(){
//        statusLastupdate.setText(App.getInstance().getLocator().getLastupdateText());
    }
    
    public void share(View view) {
        
       Location l = App.getInstance().getLocator().getLastKnownLocation();
      Intent sendIntent = new Intent(); 
      sendIntent.setAction(Intent.ACTION_SEND);
      sendIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + "," + Double.toString(l.getLongitude()));
      sendIntent.setType("text/plain");
      startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.shareLocation)));

    }
    public void upload(View view) {
        App.getInstance().getLocator().publishLastKnownLocation();
    }
}
