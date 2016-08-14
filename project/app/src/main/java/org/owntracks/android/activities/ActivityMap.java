package org.owntracks.android.activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentManager;
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
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mikepenz.materialdrawer.Drawer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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

import timber.log.Timber;

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
    private MapFragment mapView;
    private Bundle intentExtras;
    private MapLocationSource mapLocationSource;
    private BottomSheetBehavior bottomSheetBehavior;

    private int mode = ACTION_FOLLOW_DEVICE;
    private long zoom = ZOOM_LEVEL_STREET;
    private FusedContact activeContact;


    private boolean onCreate = false;
    private boolean onNewIntent = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("trace/start %s", System.currentTimeMillis());


        ActivityWelcome.runChecks(this);
        Timber.v("trace/checks end %s", System.currentTimeMillis());

        super.onCreate(savedInstanceState);
        this.onCreate = true;
        this.markers = new HashMap<>();
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_map);

        App.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                initMapDelayed();
            }
        }, 50);



        this.bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout);
        binding.contactPeek.contactRow.setOnClickListener(bottomSheetClickListener);
        binding.contactPeek.contactRow.setOnLongClickListener(bottomSheetLongClickListener);

        hideBottomSheet();


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
        Timber.v("trace/end %s", System.currentTimeMillis());

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

    private boolean hasMap() {
        return this.map != null ;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
        if(hasMap()) {
            redrawMap();
            if (intentExtras != null) {
                onHandleIntentExtras();
                intentExtras = null;
            }

        }
        EventBus.getDefault().register(this);

    }

    private void redrawMap() {
        clearMap();
        onAddInitialMarkers();
    }



    @Override
    public void onPause() {
        super.onPause();

        EventBus.getDefault().unregister(this);

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
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


    @Subscribe(threadMode = ThreadMode.MAIN)
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

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEventMainThread(Events.CurrentLocationUpdated e) {
        onDeviceLocationUpdated(e.getGeocodableLocation());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.ModeChanged e) {
        clearMap();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.EndpointStateChanged e) {
        if(e.getState() == ServiceMessage.EndpointState.ERROR_CONFIGURATION)
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

    private void updateContactMarker(@Nullable FusedContact c) {
        if (c == null || !c.hasLocation() || map == null)
            return;

        Marker m = markers.get(c.getId());

        if (m != null) {
            m.setPosition(c.getLatLng());
        } else {
            m = map.addMarker(new MarkerOptions().position(c.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            m.setTag(c);
            markers.put(c.getId(), m);
        }

        ContactImageProvider.setMarkerAsync(m, c);

    }

    private void onAddInitialMarkers() {
        for (Object o : App.getFusedContacts().entrySet()) {
            updateContactMarker((FusedContact) ((Map.Entry) o).getValue());
        }
    }


    private void initMapDelayed() {
        Timber.v("trace start %s", System.currentTimeMillis());

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment supportMapFragment =  SupportMapFragment.newInstance();
        fm.beginTransaction().replace(R.id.mapContainer, supportMapFragment).commit();

        supportMapFragment.getMapAsync(this);
        Timber.v("trace end %s", System.currentTimeMillis());

    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Timber.v("trace start %s", System.currentTimeMillis());
        this.map = googleMap;
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

        Timber.v("trace pre marker %s", System.currentTimeMillis());

        onAddInitialMarkers();
        onHandleIntentExtras();

        Timber.v("trace post %s", System.currentTimeMillis());

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.intentExtras = intent.getExtras();

    }

    private void onHandleIntentExtras() {
        Timber.v("trace start %s", System.currentTimeMillis());

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
        Timber.v("trace end %s", System.currentTimeMillis());

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
        actionSelectContact(FusedContact.class.cast(marker.getTag()));
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
            if(lastKnownLocation != null)
                this.listener.onLocationChanged(lastKnownLocation);
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
}
