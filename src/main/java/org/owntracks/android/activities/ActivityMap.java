package org.owntracks.android.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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
import org.owntracks.android.R;
import org.owntracks.android.databinding.ActivityMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceLocator;
import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Toasts;

import java.util.HashMap;
import java.util.Map;

public class ActivityMap extends ActivityBase implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "ActivityMap";

    public static final String INTENT_KEY_TOPIC = "t";
    public static final String INTENT_KEY_ACTION = "a";

    private static final long ZOOM_LEVEL_WOLRD = 1;
    private static final long ZOOM_LEVEL_CONTINENT = 5;
    private static final long ZOOM_LEVEL_CITY = 10;
    private static final long ZOOM_LEVEL_STREET = 15;
    private static final long ZOOM_LEVEL_BUILDING = 20;

    private static final int PERMISSION_REQUEST_USER_LOCATION = 2;


    private static final int ACTION_FREE_ROAM = 0;
    public static final int ACTION_FOLLOW_DEVICE = 1;
    public static final int ACTION_FOLLOW_CONTACT = 2;
    public static final int ACTION_SELECT_CONTACT = 3;




    private GoogleMap map;
    private ActivityMapBinding binding;
    private HashMap<String, Marker> markers;
    private MapView mapView;
    private Bundle intentExtras;
    private MapLocationSource mapLocationSource;
    private BottomSheetBehavior bottomSheetBehavior;

    private int mode = ACTION_FOLLOW_DEVICE;
    private long zoom = ZOOM_LEVEL_STREET;
    private FusedContact activeContact;
    private FloatingActionButton fab;


    private boolean onCreate = false;
    private boolean onNewIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.onCreate = true;
        this.markers = new HashMap<>();
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_map);

        this.mapView = binding.mapView;
        this.mapView.requestTransparentRegion(this.mapView);

        this.mapView.onCreate(savedInstanceState);
        this.mapView.getMapAsync(this);
        //this.fab = binding.fab;
        this.bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout);
        binding.contactPeek.contactRow.setOnClickListener(bottomSheetClickListener);
        binding.contactPeek.contactRow.setOnLongClickListener(bottomSheetLongClickListener);

        hideBottomSheet();

        runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USER_LOCATION);

        toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Drawer drawer = DrawerProvider.buildDrawer(this, toolbar);


        this.intentExtras = getIntent().getExtras();
        this.mapLocationSource = new MapLocationSource();

        binding.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);

            }
        });
    }

    private void showPopupMenu(View v) {


        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.START ); //new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_contacts, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }


    private void actionNavigateToSelectedContact() {
        if(activeContact != null && activeContact.hasLocation()) {
            LatLng l = activeContact.getLatLng();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + l.latitude + "," + l.longitude));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    public void onStart() {

        super.onStart();

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        this.mapView.onResume();


        redrawMap();
        if(intentExtras != null) {
            onHandleIntentExtras();
            intentExtras = null;
        }

        de.greenrobot.event.EventBus.getDefault().register(this);

    }

    private void redrawMap() {
        clearMap();
        onAddInitialMarkers();
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
        inflater.inflate(R.menu.activity_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {


            PendingIntent p  = ServiceProxy.getBroadcastIntentForService(this, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null);
            try {
                p.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
            return true;
        } else if (itemId == R.id.menu_mylocation) {
            actionFollowDevice();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;

    }


    @SuppressWarnings("unused")
    public void onEventMainThread(FusedContact e) {
        if (e == activeContact) {
            if(mode == ACTION_FOLLOW_CONTACT)
                actionFollowContact(e);
            else if(mode == ACTION_SELECT_CONTACT) {
                actionSelectContact(e);
            }
        } else {
            updateContactMarker(e);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.CurrentLocationUpdated e) {
        onDeviceLocationUpdated(e.getGeocodableLocation());
    }


    @SuppressWarnings("unused")
    public void onEventMainThread(Events.ModeChanged e) {
        redrawMap();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Events.EndpointStateChanged e) {
        if(e.getState() == ServiceMessage.EndpointState.DISCONNECTED_CONFIGINCOMPLETE)
            Toasts.showEndpointNotConfigured();
    }


    private void clearMap() {
        Log.v(TAG, "clearMap");
        if(map != null) {
            hideBottomSheet();
            this.map.clear();
            this.markers.clear();
        }
    }


    /*private void addContact(FusedContact c) {
        c.addOnPropertyChangedCallback(contactChangedCallback);
        updateContactMarker(c);
    }*/

/*
    private Observable.OnPropertyChangedCallback contactChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(final Observable observable, int i) {
            Log.v(TAG, "onPropertyChanged " + observable);


            if(observable instanceof FusedContact) {
                if(Looper.myLooper() == Looper.getMainLooper())
                    updateContactMarker((FusedContact)observable);
                else {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            updateContactMarker((FusedContact) observable);
                        }
                    });
                }
            }

        }
    };

*/



    private void updateContactMarker(@Nullable FusedContact c) {
        if (c == null || !c.hasLocation() || map == null)
            return;

        Marker m = markers.get(c.getId());

        if (m != null) {
            m.setPosition(c.getLatLng());
        } else {
            m = map.addMarker(new MarkerOptions().snippet(c.getId()).position(c.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            markers.put(c.getId(), m);
        }

        ContactImageProvider.setMarkerAsync(m, c);

    }

    private void onAddInitialMarkers() {
        for (Object o : App.getFusedContacts().entrySet()) {
            updateContactMarker((FusedContact) ((Map.Entry) o).getValue());
        }
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
        this.map.getUiSettings().setMyLocationButtonEnabled(false);
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

        onAddInitialMarkers();
        onHandleIntentExtras();
    }


    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.intentExtras = intent.getExtras();

    }

    private void onHandleIntentExtras() {
        Log.v(TAG, "onHandleIntentExtras: " + intentExtras);

        if(intentExtras == null) {
            actionFollowDevice();
        } else {
            int action = intentExtras.getInt(INTENT_KEY_ACTION);
            switch (action) {
                case ACTION_FOLLOW_CONTACT:
                    Log.v(TAG, "action: ACTION_FOLLOW_CONTACT:" + action);
                    Log.v(TAG, "topic:" + intentExtras.getString(INTENT_KEY_TOPIC));
                    FusedContact c = App.getFusedContact(intentExtras.getString(INTENT_KEY_TOPIC));
                    Log.v(TAG, "contact: " + c);
                    actionFollowContact(c);
                    break;
                case ACTION_FOLLOW_DEVICE:
                    actionFollowDevice();
                    break;
            }
            intentExtras = null;
        }
    }
    private void onDeviceLocationUpdated(GeocodableLocation l) {
        this.mapLocationSource.updateWithLocation(l);

        if (mode == ACTION_FOLLOW_DEVICE && this.map != null) {
            centerDevice();
        }

    }



    private void centerDevice() {
        if(!mapLocationSource.hasLocation())
            return;
        centerMap(this.mapLocationSource.getLastKnownLocation().getLatLng());
    }



    private void centerMap(LatLng latLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void actionFollowContact(@Nullable FusedContact c) {
        if(c == null || !c.hasLocation())
            return;

        actionSelectContact(c);
        Log.v(TAG, "actionFollowContact()");

        this.mode = ACTION_FOLLOW_CONTACT;
        centerMap(c.getLatLng());
    }



    private void actionFollowDevice() {
        this.mode = ACTION_FOLLOW_DEVICE;

        deselectContact();

        if(!mapLocationSource.hasLocation()) {
            Toasts.showCurrentLocationNotAvailable();
            return;
        }

        centerDevice();
    }




    @Override
    public void onMapClick(LatLng latLng) {
        deselectContact();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        actionSelectContact(App.getFusedContact(marker.getSnippet()));
        return true;
    }

    private void actionSelectContact(@Nullable FusedContact fusedContact) {
        Log.v(TAG, "actionSelectContact()");

        if(fusedContact == null)
            return;

        this.mode = ACTION_SELECT_CONTACT;
        if(activeContact == fusedContact)
            return;

        activeContact = fusedContact;

        updateContactMarker(fusedContact);
        binding.setItem(fusedContact);
        binding.contactPeek.setItem(fusedContact);

        collapseBottomSheet();
    }


    private void updateBottomSheetContact(FusedContact fusedContact) {
    }



    private void deselectContact() {
        hideBottomSheet();
        binding.setItem(null);
        activeContact = null;
    }


    View.OnClickListener bottomSheetClickListener = new View.OnClickListener() {
        // On click on the bottom sheet itself
        @Override
        public void onClick(View v) {
            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)
                expandBottomSheet();
            else if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                collapseBottomSheet();

        }
    };

    View.OnLongClickListener bottomSheetLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            actionFollowContact(activeContact);

            if(bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                collapseBottomSheet();

            return true;
        }
    };

    private void expandBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void collapseBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);


    }

    private void hideBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

    }



    // Popup menu click
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_navigate:
                actionNavigateToSelectedContact();
                return true;

            default:
                return false;
        }

    }



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

        public boolean hasLocation() {
            return this.lastKnownLocation != null;
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
