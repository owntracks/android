package org.owntracks.android.activities;

import android.Manifest;
import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.mapbox.mapboxsdk.overlay.Marker;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.views.MapView;
import com.mikepenz.materialdrawer.Drawer;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.databinding.ActivityMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Toasts;

import java.util.HashMap;

public class ActivityMap extends ActivityBase {
    public static final String KEY_TOPIC = "t";
    public static final String KEY_ACTION = "a";
    public static final int KEY_ACTION_VALUE_CENTER_CONTACT = 1;
    public static final int KEY_ACTION_VALUE_CENTER_CURRENT= 2;

    private static final long ZOOM_LEVEL_COUNTRY = 6;
    private static final long ZOOM_LEVEL_CITY = 11;
    private static final long ZOOM_LEVEL_NEIGHBORHOOD = 17;
    private static final String TAG = "ActivityMap";
    private static final int PERMISSION_REQUEST_DISK_CACHE = 1 ;
    private static final int PERMISSION_REQUEST_USER_LOCATION = 2 ;


    private MapView mapView;
    private ActivityMapBinding binding;
    private HashMap<String, Marker> markers;
    private Toolbar toolbar;
    private Drawer drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.markers = new HashMap<>();
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_map);
        this.mapView = binding.mapView;
        this.mapView.setAccessToken(getString(R.string.MAPBOX_API_KEY));
        this.mapView.setTileSource(new MapboxTileLayer(getString(R.string.MAPBOX_TILE_LAYER)));


        runActionWithPermissionCheck(PERMISSION_REQUEST_DISK_CACHE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USER_LOCATION);


        toolbar =(Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        drawer = DrawerFactory.buildDrawerV2(this, toolbar, new DrawerFactory.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick() {
                drawer.closeDrawer();
                return false;
            }
        });

        // Disable drawer indicator if we're not showing the root fragment
        // Instead, this shows the back arrow and calls the drawer navigation listener where we can handle back
        // or show the drawer manually
        if(drawer != null && drawer.getActionBarDrawerToggle() != null)
            drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);



        Bundle b = getIntent().getExtras();
        if(b != null) {
            int action = b.getInt(KEY_ACTION);
            switch (action) {
                case KEY_ACTION_VALUE_CENTER_CONTACT:
                    FusedContact contact = App.getFusedContact(b.getString(KEY_TOPIC));
                    updateContactLocation(contact);
                    binding.setVariable(BR.item, contact);
                    this.mapView.setZoom(ZOOM_LEVEL_NEIGHBORHOOD);
                    this.mapView.setCenter(contact.getLatLng(), true);
                    break;

                case KEY_ACTION_VALUE_CENTER_CURRENT:
                    //centerDeviceLocation();
                    break;
            }
        }




    }


    protected  void onRunActionWithPermissionCheck(int action, boolean granted) {
        switch (action) {
            case PERMISSION_REQUEST_DISK_CACHE:
                Log.v(TAG, "request code: PERMISSION_REQUEST_REPORT_LOCATION");
                if (granted) {
                    this.mapView.setDiskCacheEnabled(true);
                } else {
                    this.mapView.setDiskCacheEnabled(false);
                }
                return;

            case PERMISSION_REQUEST_USER_LOCATION:
                if (granted) {
                    this.mapView.setUserLocationEnabled(true);
                } else {
                    this.mapView.setUserLocationEnabled(false);
                }

        }
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fragment_contacts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {

            return true;
        } else if( itemId == R.id.menu_mylocation) {
            return true;
        } else if( itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;

    }

    public void updateContactLocation(FusedContact c) {
        //removeContactMarker(c);
        com.mapbox.mapboxsdk.overlay.Marker m = markers.get(c.getTopic());
        if(m != null) {
            m.setPoint(c.getLatLng());
            m.updateDrawingPosition();
        } else {
            m = new com.mapbox.mapboxsdk.overlay.Marker(mapView, "", "", c.getLatLng());
            m.setAnchor(new PointF(0.5F, 0.5F));
            m.setRelatedObject(c);
            markers.put(c.getTopic(), m);
        }
        ContactImageProvider.setMarkerAsync(m, c);
        mapView.addMarker(m);


    }


/*    public void onEventMainThread(Events.CurrentLocationUpdated e) {

        if(currentLocationMarker != null) {
            this.currentLocationMarker.setPoint(e.getGeocodableLocation().getLatLng());
            this.currentLocationMarker.updateDrawingPosition();


        } else {
            this.currentLocationMarker = new com.mapbox.mapboxsdk.overlay.Marker("", "", e.getGeocodableLocation().getLatLng());
            Drawable markerDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.current_location_marker);
            Bitmap bitmap = Bitmap.createBitmap(markerDrawable.getIntrinsicWidth(), markerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            markerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            markerDrawable.draw(canvas);

            this.currentLocationMarker.setMarker(new BitmapDrawable(getActivity().getResources(), bitmap));
            this.currentLocationMarker.setAnchor(new PointF(0.5f, 0.5f));
            this.mapView.addMarker(this.currentLocationMarker);
        }


        if (isFollowingCurrentLocation())
            selectCurrentLocation(SELECT_CENTER_AND_ZOOM, true, ZOOM_LEVEL_NEIGHBORHOOD);
    }


    private void centerContact(FusedContact contact) {

    }

    private void centerDeviceLocation() {

    }*/
}
