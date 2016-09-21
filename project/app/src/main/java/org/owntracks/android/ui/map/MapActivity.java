package org.owntracks.android.ui.map;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.databinding.UiActivityMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceLocator;
import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseActivity;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.ui.base.navigator.Navigator;

import java.util.WeakHashMap;

import javax.inject.Inject;
import javax.inject.Provider;

import timber.log.Timber;

public class MapActivity extends BaseActivity<UiActivityMapBinding, MapMvvm.ViewModel> implements MapMvvm.View, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {

    private static final long ZOOM_LEVEL_WOLRD = 1;
    private static final long ZOOM_LEVEL_CONTINENT = 5;
    private static final long ZOOM_LEVEL_CITY = 10;
    private static final long ZOOM_LEVEL_STREET = 15;
    private static final long ZOOM_LEVEL_BUILDING = 20;
    public static final String BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID";



    @Inject
    protected Provider<Navigator> navigator;

    WeakHashMap<String, Marker> mMarkers = new WeakHashMap <>();

    private GoogleMap mMap;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private MapLocationSource mMapLocationSource;


    private static boolean FLAG_STATE_MAP_READY = false;
    private static boolean FLAG_STATE_LOCATION_READY = false;

    private static boolean FLAG_DATA_UPDATED_DEVICE = false;
    private static boolean FLAG_DATA_UPDATED_CONTACT_ACTIVE = false;
    private static boolean FLAG_DATA_UPDATED_CONTACT_ALL = false;

    private static final int FLAG_ACTION_MODE_FREE = 0;
    private static final int FLAG_ACTION_MODE_DEVICE = 1;
    private static final int FLAG_ACTION_MODE_CONTACT = 2;
    private static int FLAG_ACTION_MODE = FLAG_ACTION_MODE_DEVICE;
    private Menu mMenu;

    // EVENT ENGINE ACTIONS
    private void queueActionModeDevice() {
        FLAG_ACTION_MODE = FLAG_ACTION_MODE_DEVICE;
        FLAG_DATA_UPDATED_DEVICE = true;  // misuse data update flag to center if ready
        executePendingActions();
    }

    private void queueActionModeContact(boolean center) {
        FLAG_ACTION_MODE = FLAG_ACTION_MODE_CONTACT;
        FLAG_DATA_UPDATED_CONTACT_ACTIVE = center;
        executePendingActions();
    }

    private void queueActionModeFree() {
        FLAG_ACTION_MODE = FLAG_ACTION_MODE_FREE;
        executePendingActions();
    }


    private void queueActionMapUpdate() {
        FLAG_DATA_UPDATED_CONTACT_ALL = true;
        executePendingActions();
    }


    private void executePendingActions() {
        if(!FLAG_STATE_MAP_READY) {
            return;
        }

        // MAP NEEDS UPDATE. HANDLE BEFORE VIEW UPDATES
        if(FLAG_DATA_UPDATED_CONTACT_ALL) {
            FLAG_DATA_UPDATED_CONTACT_ALL = false;
            doUpdateMarkerAll();
        }
        // DEVICE OR ACTIVE CONTACT UPDATED. UPDATE VIEW
        if(FLAG_STATE_LOCATION_READY && FLAG_DATA_UPDATED_DEVICE && FLAG_ACTION_MODE == FLAG_ACTION_MODE_DEVICE) {
            FLAG_DATA_UPDATED_DEVICE = false;
            doCenterDevice();
        } else if (FLAG_DATA_UPDATED_CONTACT_ACTIVE && (FLAG_ACTION_MODE == FLAG_ACTION_MODE_CONTACT)) {
            FLAG_DATA_UPDATED_CONTACT_ACTIVE = false;
            doCenterContact();
        }




    }


    // EVENT ENGINE STATE CALLBACKS
    private void onLocationSourceUpdated() {
        FLAG_STATE_LOCATION_READY = true;
        FLAG_DATA_UPDATED_DEVICE = true;
        executePendingActions();
        enableLocationMenus();
    }

    private void onStateMapReady() {
        FLAG_STATE_MAP_READY = true;
        FLAG_DATA_UPDATED_CONTACT_ALL = true;
        executePendingActions();
    }


    private void onActiveContactUpdated() {
        FLAG_DATA_UPDATED_CONTACT_ACTIVE = true;
        executePendingActions();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        ActivityWelcome.runChecks(this);

        activityComponent().inject(this);
        setAndBindContentView(R.layout.ui_activity_map, savedInstanceState);

        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);

        this.mMapLocationSource = new MapLocationSource();

        App.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                initMapDelayed();
            }
        }, 500);

        this.bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout);
        binding.contactPeek.contactRow.setOnClickListener(this);
        binding.contactPeek.contactRow.setOnLongClickListener(this);
        binding.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);

            }
        });

        setBottomSheetHidden();


    }
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentExtras(intent);
    }




    private void handleIntentExtras(Intent intent) {
        Timber.v("handleIntentExtras");

        Bundle b = getExtrasBundle(intent);
        if(b != null) {
            Timber.v("intent has extras from navigator");
            String contactId = b.getString(BUNDLE_KEY_CONTACT_ID);
            if(contactId != null) {
                viewModel.restore(contactId);
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        queueActionMapUpdate();
        handleIntentExtras(getIntent());

    }
    public void initMapDelayed() {
        Timber.v("trace start %s", System.currentTimeMillis());

        FragmentManager fm = getSupportFragmentManager();
        SupportMapFragment supportMapFragment =  SupportMapFragment.newInstance();
        fm.beginTransaction().replace(R.id.mapContainer, supportMapFragment).commit();

        supportMapFragment.getMapAsync(this);
        Timber.v("trace end %s", System.currentTimeMillis());

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map, menu);
        this.mMenu = menu;
        if(!FLAG_STATE_LOCATION_READY)
            disableLocationMenus();
        return true;
    }

    private void disableLocationMenus() {
        this.mMenu.findItem(R.id.menu_mylocation).getIcon().setAlpha(130);
        this.mMenu.findItem(R.id.menu_report).getIcon().setAlpha(130);
    }
    private void enableLocationMenus() {
        this.mMenu.findItem(R.id.menu_mylocation).getIcon().setAlpha(255);
        this.mMenu.findItem(R.id.menu_report).getIcon().setAlpha(255);
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
            viewModel.onMenuCenterDeviceClicked();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    // MAP CALLBACKS
    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Timber.v("onMapReady");

        this.mMap = googleMap;
        this.mMap.setIndoorEnabled(false);
        this.mMap.setLocationSource(mMapLocationSource);
        this.mMap.setMyLocationEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.mMap.setOnMapClickListener(this);
        this.mMap.setOnMarkerClickListener(this);
        this.mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                return null;
            }
        });
        onStateMapReady();
        viewModel.onMapReady();

    }

    @Override
    public void onMapClick(LatLng latLng) {
        queueActionModeFree();
        viewModel.onMapClick();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if(marker.getTag() != null) {
            viewModel.onMarkerClick(String.class.cast(marker.getTag()));
        }
        return true;
    }

    // MAP INTERACTION
    private void doCenterDevice() {
        doUpdateCamera(mMapLocationSource.getLatLng(), ZOOM_LEVEL_STREET);

    }

    private void doCenterContact() {
        doUpdateCamera(viewModel.getContact().getLatLng(), ZOOM_LEVEL_STREET);
    }

    private void doUpdateCamera(LatLng latLng, long zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    long repoRevision = -1;
    private void doUpdateMarkerAll() {
        long newRepoRevision = viewModel.getContactsRevision();

        if(repoRevision < newRepoRevision) {
            Timber.v("repoRevision:%s, newRepoRevision:%s => updating marker", repoRevision, newRepoRevision);
            addMarker();
        } else if(repoRevision > newRepoRevision) {
            Timber.v("repoRevision:%s, newRepoRevision:%s => reinitializing marker", repoRevision, newRepoRevision);
            clearMarker();
            addMarker();
        }
        repoRevision = newRepoRevision;
    }

    private void addMarker() {
        for (Object c : viewModel.getContacts()) {
            doUpdateMarkerSingle(FusedContact.class.cast(c));
        }
        repoRevision = viewModel.getContactsRevision();;

    }

    private void doUpdateMarkerSingle(@NonNull FusedContact contact) {
        Timber.v("updating single id:%s", contact.getId());

        if (!contact.hasLocation() || mMap == null)
            return;

        Marker m = mMarkers.get(contact.getId());

        if (m != null) {
            m.setPosition(contact.getLatLng());
        } else {
            m = mMap.addMarker(new MarkerOptions().position(contact.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            m.setTag(contact.getId());
            mMarkers.put(contact.getId(), m);
        }

        ContactImageProvider.setMarkerAsync(m, contact);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_navigate:
                FusedContact c = viewModel.getContact();
                if(c != null && c.hasLocation()) {
                    LatLng l = c.getLatLng();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + l.latitude + "," + l.longitude));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
                }

                return true;

            default:
                return false;
        }
    }


    public class MapLocationSource implements LocationSource {
        LocationSource.OnLocationChangedListener mListener;
        GeocodableLocation mLocation;

        public MapLocationSource() {
            super();
            App.getEventBus().register(this);
        }

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            mListener = onLocationChangedListener;
            if(mLocation != null)
                this.mListener.onLocationChanged(mLocation);
        }

        @Override
        public void deactivate() {
            mListener = null;
            App.getEventBus().unregister(this);
        }

        @Subscribe(threadMode = ThreadMode.MAIN, priority = 1)
        public void update(Events.CurrentLocationUpdated l) {
            Timber.v("update to location: %s", l.getLocation().toLatLonString());
            this.mLocation = l.getLocation();
            if(mListener != null)
                this.mListener.onLocationChanged(this.mLocation);
            onLocationSourceUpdated();
        }

        public boolean isAvailable() {
            return mLocation != null;
        }

        public GeocodableLocation getLocation() {
            return mLocation;
        }
        public LatLng getLatLng() {
            return mLocation.getLatLng();
        }

    };

    // BOTTOM SHEET CALLBACKS
    @Override
    public void onClick(View view) {
        viewModel.onBottomSheetClick();
    }

    @Override
    public boolean onLongClick(View view) {
        viewModel.onBottomSheetLongClick();
        return true;
    }

    @Override
    public void setBottomSheetExpanded() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void setBottomSheetCollapsed() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }

    @Override
    public void setBottomSheetHidden() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void contactUpdate(FusedContact c) {
        doUpdateMarkerSingle(c);
        repoRevision = viewModel.getContactsRevision();;
    }

    @Override
    public void contactUpdateActive() {
        onActiveContactUpdated();
        doUpdateMarkerSingle(viewModel.getContact());
        repoRevision = viewModel.getContactsRevision();;


    }

    @Override
    public void setModeDevice() {
        queueActionModeDevice();
    }

    @Override
    public void clearMarker() {
        if(FLAG_STATE_MAP_READY)
            mMap.clear();
        mMarkers.clear();
    }


    @Override
    public void setModeContact(boolean center) {
        queueActionModeContact(center);
    }


    private void showPopupMenu(View v) {


        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.START ); //new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_contacts, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

}
