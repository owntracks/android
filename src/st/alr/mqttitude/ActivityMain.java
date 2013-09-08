
package st.alr.mqttitude;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Events;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

public class ActivityMain extends android.support.v4.app.FragmentActivity {
    MenuItem publish;
    TextView location;
    TextView statusLocator;
    TextView statusLastupdate;
    TextView statusServer;
    private GoogleMap mMap;

    private TextView locationPrimary;
    private TextView locationMeta;
    private LinearLayout locationAvailable;
    private LinearLayout locationUnavailable;

    private Marker mMarker;
    private Circle mCircle;
    private Geocoder geocoder;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        Intent i = null;
        
        if (itemId == R.id.menu_settings) {
            i = new Intent(this, ActivityPreferences.class);
            startActivity(i);
            return true;
        }  else if (itemId == R.id.menu_status) {
                i = new Intent(this, ActivityStatus.class);
                startActivity(i);
                return true;
        } else if (itemId == R.id.menu_publish) {
            App.getInstance().getLocator().publishLastKnownLocation();
            return true;
        } else if (itemId == R.id.menu_share) {
            this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            if (mMap != null)
                setUpMap();
        }
    }

    private void setUpMap() {
        // Hide the zoom controls as the button panel will cover it.
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.setMyLocationEnabled(false);
        mMap.setTrafficEnabled(false);
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
    protected void onPause() {
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
    
        if (App.getInstance().isDebugBuild())            
                menu.findItem(R.id.menu_status).setVisible(true);
        
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
        geocoder = new Geocoder(this, Locale.getDefault());
        locationAvailable = (LinearLayout) findViewById(R.id.locationAvailable);
        locationUnavailable = (LinearLayout) findViewById(R.id.locationUnavailable);

        locationPrimary = (TextView) findViewById(R.id.locationPrimary);
        locationMeta = (TextView) findViewById(R.id.locationMeta);
        
        showLocationUnavailable();
        
        EventBus.getDefault().register(this);

    }

    public void onEvent(Events.LocationUpdated e) {
        setLocation(e.getLocation());
    }

    public void setLocation(Location l) {
       if(l == null) {
           showLocationUnavailable();
           return;
       } 
       
        LatLng latlong = new LatLng(l.getLatitude(), l.getLongitude());
        CameraUpdate center = CameraUpdateFactory.newLatLng(latlong);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

        if (mMarker != null)
            mMarker.remove();

        if (mCircle != null)
            mCircle.remove();

        
        mMarker = mMap.addMarker(new MarkerOptions().position(latlong).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

         if(l.getAccuracy() >= 50) {
                 mCircle = mMap.addCircle(new
                 CircleOptions().center(latlong).radius(l.getAccuracy()).strokeColor(0xff1082ac).fillColor(0x1c15bffe).strokeWidth(3));
         }

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        locationPrimary.setText(l.getLatitude() + " / " + l.getLongitude());
        locationMeta.setText(App.getInstance().formatDate(new Date()));
        showLocationAvailable();
        
        try {
            List<Address> addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {            
                Address a = addresses.get(0);
                locationPrimary.setText(a.getAddressLine(0));
            }
        } catch (IOException e) {
            // Geocoder information not available. LatLong is already shown and just not overwritten. Nothing to do here
        }

        
        
    }

    private void showLocationAvailable() {
        locationUnavailable.setVisibility(View.GONE);
        if(!locationAvailable.isShown())
            locationAvailable.setVisibility(View.VISIBLE);
    }

    private void showLocationUnavailable(){
        locationAvailable.setVisibility(View.GONE);
        if(!locationUnavailable.isShown())          
            locationUnavailable.setVisibility(View.VISIBLE);        
    }
    
    public void share(View view) {

        Location l = App.getInstance().getLocator().getLastKnownLocation();
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(
                Intent.EXTRA_TEXT,
                "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + ","
                        + Double.toString(l.getLongitude()));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.shareLocation)));

    }

    public void upload(View view) {
        App.getInstance().getLocator().publishLastKnownLocation();
    }
}
