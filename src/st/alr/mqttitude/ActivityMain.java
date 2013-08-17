
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
import st.alr.mqttitude.support.Events.MqttConnectivityChanged;
import st.alr.mqttitude.R;
import st.alr.mqttitude.R.menu;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
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

        locationAddress = (TextView) findViewById(R.id.locationAddress);
        locationLatlong = (TextView) findViewById(R.id.locationLatlong);
        locationAccuracy = (TextView) findViewById(R.id.locationAccuracy);
        
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

        locationLatlong.setText(l.getLatitude() + " / " + l.getLongitude());
        locationAccuracy.setText("±" + Math.round(l.getAccuracy()*100)/100.0d+"m");
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        StringBuffer addressBuffer = new StringBuffer();
        try {
            addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if(addresses != null && addresses.size() > 0) {
            Address a = addresses.get(0);

            if(a.getAddressLine(0) != null) {
                addressBuffer.append(a.getAddressLine(0));
                addressBuffer.append(" - ");
            }
            if(a.getAddressLine(0) != null) 
                addressBuffer.append(a.getAddressLine(1));

            
            locationAddress.setText(addressBuffer.toString());
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
