package org.owntracks.android.activities;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mikepenz.materialdrawer.Drawer;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.databinding.ActivityMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.Events;

import java.util.HashMap;

public class ActivityMap extends ActivityBase implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    private static final String TAG = "ActivityMap";

    public static final String INTENT_KEY_TOPIC = "t";
    public static final String INTENT_KEY_ACTION = "a";

    private static final long ZOOM_LEVEL_WOLRD = 1;
    private static final long ZOOM_LEVEL_CONTINENT = 5;
    private static final long ZOOM_LEVEL_CITY = 10;
    private static final long ZOOM_LEVEL_STREET = 15;
    private static final long ZOOM_LEVEL_BUILDING = 20;

    private static final int PERMISSION_REQUEST_USER_LOCATION = 2;


    public static final int ACTION_FOLLOW_DEVICE = 0;
    public static final int ACTION_FOLLOW_CONTACT = 1;


    private GoogleMap map;
    private ActivityMapBinding binding;
    private HashMap<String, Marker> markers;
    private MapView mapView;
    private Bundle intentExtras;
    private MapLocationSource mapLocationSource;
    private int mode = ACTION_FOLLOW_DEVICE;
    private FusedContact followedContact;
    private BottomSheetBehavior bottomSheetBehavior;
    private FusedContact selectedContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.markers = new HashMap<>();
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_map);
        this.mapView = binding.mapView;
        this.mapView.onCreate(savedInstanceState);
        this.mapView.getMapAsync(this);
        this.bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetLayout);
        this.bottomSheetBehavior.setBottomSheetCallback(bottomSheetCallback);
        binding.bottomSheet.bottomSheetLayout.setOnClickListener(bottomSheetClickListener);
        this.bottomSheetBehavior.setPeekHeight(0);
        runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USER_LOCATION);


        toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Drawer drawer = DrawerProvider.buildDrawer(this, toolbar);


        this.intentExtras = getIntent().getExtras();
        this.mapLocationSource = new MapLocationSource();
        

    }

    @Override
    public void onStart() {

        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        this.mapView.onResume();
        de.greenrobot.event.EventBus.getDefault().registerSticky(this);
    }



    @Override
    public void onPause() {
        super.onPause();
        this.mapView.onPause();
        de.greenrobot.event.EventBus.getDefault().unregister(this);

    }

    @Override
    public void onStop() {

        super.onStop();
    }

    @Override
    public void onDestroy() {
        this.mapView.onDestroy();

        super.onDestroy();
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
        } else if (itemId == R.id.menu_mylocation) {
            actionCenterDevice();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;

    }


    @SuppressWarnings("unused")
    public void onEvent(Events.ContactUpdated e) {
        updateContactLocation(e.getContact());

        if (e.getContact() == followedContact)
            actionCenterContact(e.getContact());
    }

    @SuppressWarnings("unused")
    public void onEvent(Events.ContactAdded e) {
        updateContactLocation(e.getContact());
    }

    @SuppressWarnings("unused")
    public void onEvent(Events.CurrentLocationUpdated e) {
        onDeviceLocationUpdated(e.getGeocodableLocation());
    }

    private void onDeviceLocationUpdated(GeocodableLocation l) {
        this.mapLocationSource.updateWithLocation(l);

        if (mode == ACTION_FOLLOW_DEVICE && this.map != null) {
            centerMap(l.getLatLng());
        }

    }


    private void updateContactLocation(FusedContact c) {
        Log.v(TAG, "updateContactLocation: " + c.getTopic() + " hasLocation: " + c.hasLocation());
        if (!c.hasLocation())
            return;

        //removeContactMarker(c);
        Marker m = markers.get(c.getTopic());

        if (m != null) {
            m.setPosition(c.getLatLng());
        } else {
            m = map.addMarker(new MarkerOptions().snippet(c.getTopic()).position(c.getLatLng()).anchor(0.5f, 0.5f));
            markers.put(c.getTopic(), m);
        }

        ContactImageProvider.setMarkerAsync(m, c);

    }


    @SuppressWarnings("MissingPermission")
    // Map uses custom provider that handles missing permissions
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.v(TAG, "onMapReady()");

        this.map = googleMap;
        this.map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        this.map.setIndoorEnabled(false);
        this.map.setLocationSource(this.mapLocationSource);
        this.map.setMyLocationEnabled(true);
        this.map.setOnMapClickListener(this);
        this.map.setOnMarkerClickListener(this);
        this.map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });

        // Load all contacts
        Log.v(TAG, "Populating map with # markers: " + App.getFusedContacts().size());
        for (FusedContact contact : App.getFusedContacts().values()) {
            updateContactLocation(contact);
        }

        onHandleIntentExtras();
    }


    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.intentExtras = intent.getExtras();
        if(map != null)
            onHandleIntentExtras();

    }

    private void onHandleIntentExtras() {
        if(intentExtras == null) {
            actionCenterDevice();
        } else {
            int action = intentExtras.getInt(INTENT_KEY_ACTION);
            switch (action) {
                case ACTION_FOLLOW_CONTACT:
                    Log.v(TAG, "action: ACTION_FOLLOW_CONTACT:" + action);
                    Log.v(TAG, "topic:" + intentExtras.getString(INTENT_KEY_TOPIC));
                    FusedContact c = App.getFusedContact(intentExtras.getString(INTENT_KEY_TOPIC));
                    Log.v(TAG, "contact: " + c);
                    actionCenterContact(c);
                    break;
                case ACTION_FOLLOW_DEVICE:
                    actionCenterDevice();
                    break;
            }
        }
    }

    private void actionCenterDevice() {
        this.mode = ACTION_FOLLOW_DEVICE;
    }

    private void actionCenterContact(FusedContact c) {
        this.mode = ACTION_FOLLOW_CONTACT;

        updateContactLocation(c);
        binding.setVariable(BR.item, c);
        centerMap(c.getLatLng());
    }

    private void centerMap(LatLng latLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL_STREET));

    }

    @Override
    public void onMapClick(LatLng latLng) {
        deselectContact();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.v(TAG, "onMarkerClick");
        actionSelectContact(App.getFusedContact(marker.getSnippet()));
        return true;
    }

    private void actionSelectContact(FusedContact fusedContact) {
        if(selectedContact == fusedContact)
            return;
        else
            selectedContact = fusedContact;

        binding.bottomSheet.setItem(fusedContact);
        binding.bottomSheet.contactPeek.setItem(fusedContact);
        // bottomSheetBehavior.setState(State.HIDDEN) doesn't work due to a bug in the support library
        // Set an initial height of 0 px to hide it instead and set it to 76dp on click instead.
        // setPeekHeight takes real px as a value. We convert the appropriate value from 76dp
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }

    private void actionFollowContact(FusedContact fusedContact) {
        actionSelectContact(fusedContact);
        centerMap(fusedContact.getLatLng());
        this.mode = ACTION_FOLLOW_CONTACT;

    }


    private void deselectContact() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomSheet.setItem(null);
        selectedContact = null;
    }

    View.OnClickListener bottomSheetClickListener = new View.OnClickListener() {
        // On click on the bottom sheet itself
        @Override
        public void onClick(View v) {
            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    };


    BottomSheetBehavior.BottomSheetCallback bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {

        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {

        }
    };




    private static class MapLocationSource implements LocationSource {
        private static final String TAG = "MapLocationSource";
        private OnLocationChangedListener listener;
        private GeocodableLocation lastKnownLocation;

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            Log.v(TAG, "activate()");
            listener = onLocationChangedListener;
        }

        @Override
        public void deactivate() {
            Log.v(TAG, "deactivate()");

            listener = null;
        }

        public void updateWithLocation(GeocodableLocation l) {
            Log.v(TAG, "updateWithLocation() " + l.getLatitude() + " " + l.getLongitude());

            this.lastKnownLocation = l;
            if(listener != null)
                this.listener.onLocationChanged(l);
        }

        public GeocodableLocation getLastKnownLocation() {
            return this.lastKnownLocation;
        }
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
