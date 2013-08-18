
package st.alr.mqttitude;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.R;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {
    MenuItem publish;
    TextView location;
    TextView statusLocator;
    TextView statusLastupdate;
    TextView statusServer;
    private GoogleMap mMap;

    private TextView locationAddress;
    private TextView locationZipcode;
    private TextView gecoderUnavilableLatLong;
    private LinearLayout geocoderAvailable;
    private LinearLayout geocoderUnavailable;

    private Marker mMarker;
    private Circle mCircle;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_settings) {
            Intent intent1 = new Intent(this, ActivityPreferences.class);
            startActivity(intent1);
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

        geocoderAvailable = (LinearLayout) findViewById(R.id.geocoderAvailable);
        geocoderUnavailable = (LinearLayout) findViewById(R.id.geocoderUnavailable);

        locationAddress = (TextView) findViewById(R.id.locationAddress);
        locationZipcode = (TextView) findViewById(R.id.locationZipcode);
        gecoderUnavilableLatLong = (TextView) findViewById(R.id.geocoderUnavailableLatLon);

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

        mMarker = mMap.addMarker(new MarkerOptions().position(latlong).icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // if(l.getAccuracy() < 20) {
        // mCircle = mMap.addCircle(new
        // CircleOptions().center(latlong).radius(l.getAccuracy()).strokeColor(0x330072ff).fillColor(0x260072ff).strokeWidth(3));
        // }

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        gecoderUnavilableLatLong.setText(l.getLatitude() + " / " + l.getLongitude());

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses != null && addresses.size() > 0) {
            showGeocoderAvailable();
            
            Address a = addresses.get(0);

            locationAddress.setText(a.getAddressLine(0));
            locationZipcode.setText(a.getAddressLine(1));

        } else {
            showGeocoderUnavailable();
        }
        
        
    }

    private void showGeocoderAvailable() {
        
        geocoderUnavailable.setVisibility(View.GONE);
        if(!geocoderAvailable.isShown())
            geocoderAvailable.setVisibility(View.VISIBLE);
    }
    
    private void showGeocoderUnavailable() {        
        geocoderAvailable.setVisibility(View.GONE);
        if(!geocoderUnavailable.isShown())          
            geocoderUnavailable.setVisibility(View.VISIBLE);        
    }
    
    private void showLocationUnavailable(){
        showGeocoderUnavailable();
        gecoderUnavilableLatLong.setText(getString(R.string.na));
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
