package org.owntracks.android.ui.map;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.ui.base.BaseActivity;

import java.util.WeakHashMap;

import timber.log.Timber;

public class MapActivity extends BaseActivity<UiMapBinding, MapMvvm.ViewModel> implements MapMvvm.View, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener {
    public static final String BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID";
    private static final long ZOOM_LEVEL_STREET = 15;
    private static final int FLAG_ACTION_MODE_FREE = 0;
    private static final int FLAG_ACTION_MODE_DEVICE = 1;
    private static final int FLAG_ACTION_MODE_CONTACT = 2;

    final WeakHashMap<String, Marker> mMarkers = new WeakHashMap<>();
    long repoRevision = -1;
    private GoogleMap mMap;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private MapLocationSource mMapLocationSource;
    private boolean flagStateMapReady = false;
    private boolean flagStateLocationReady = false;
    private boolean flagRefreshDevice = false;
    private boolean flagRefreshContactActive = false;
    private boolean flagRefreshContactAll = false;
    private boolean flagRefreshAll = false;
    private int mode = FLAG_ACTION_MODE_DEVICE;
    private Menu mMenu;

    // EVENT ENGINE ACTIONS
    private void queueActionModeDevice() {
        mode = FLAG_ACTION_MODE_DEVICE;
        flagRefreshDevice = true;  // misuse data update flag to center if ready
        executePendingActions();
    }

    private void queueActionModeContact(boolean center) {
        mode = FLAG_ACTION_MODE_CONTACT;
        flagRefreshContactActive = center;
        executePendingActions();
    }

    private void queueActionModeFree() {
        Timber.v("queueing free mode");
        mode = FLAG_ACTION_MODE_FREE;
        executePendingActions();
    }

    private void queueActionMapUpdate() {
        flagRefreshAll = true;
        executePendingActions();
    }

    private void executePendingActions() {
        Timber.v("flag flagStateLocationReady: %s", flagStateLocationReady);
        Timber.v("flag flagStateMapReady: %s", flagStateMapReady);
        Timber.v("flag flagRefreshDevice: %s", flagRefreshDevice);
        Timber.v("flag flagRefreshContactActive: %s", flagRefreshContactActive);
        Timber.v("flag flagRefreshContactAll: %s", flagRefreshContactAll);
        Timber.v("flag flagRefreshAll: %s", flagRefreshAll);
        Timber.v("flag int mode: %s", mode);


        if (!flagStateMapReady) {
            return;
        }

        // MAP NEEDS UPDATE. HANDLE BEFORE VIEW UPDATES
        if (flagRefreshContactAll) {
            flagRefreshContactAll = false;
            doUpdateMarkerAll();
        }
        // DEVICE OR ACTIVE CONTACT UPDATED. UPDATE VIEW
        if (flagStateLocationReady && flagRefreshDevice && mode == FLAG_ACTION_MODE_DEVICE) {
            flagRefreshDevice = false;
            doCenterDevice();
        } else if (flagRefreshContactActive && (mode == FLAG_ACTION_MODE_CONTACT)) {
            flagRefreshContactActive = false;
            doCenterContact();
        } else if (flagRefreshAll) {
            flagRefreshAll = false;

            doUpdateMarkerAll();
            if (flagStateLocationReady && mode == FLAG_ACTION_MODE_DEVICE) {
                doCenterDevice();
            } else if (mode == FLAG_ACTION_MODE_CONTACT) {
                doCenterContact();
            }
        }
    }

    // EVENT ENGINE STATE CALLBACKS
    public void onLocationSourceUpdated() {
        Timber.v("onLocationSourceUpdated");

        flagStateLocationReady = true;
        flagRefreshDevice = true;
        executePendingActions();
        enableLocationMenus();
    }

    private void onStateMapReady() {
        flagStateMapReady = true;
        flagRefreshContactAll = true;
        executePendingActions();
    }

    private void onActiveContactUpdated() {
        flagRefreshContactActive = true;
        executePendingActions();
    }

    private void onActiveContactRemoved() {
        flagRefreshContactAll = true;
        executePendingActions();
    }

    private void onContactRemoved() {
        flagRefreshContactAll = true;
        executePendingActions();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.v("onCreate");
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);

        assertRequirements();
        bindAndAttachContentView(R.layout.ui_map, savedInstanceState);

        setSupportToolbar(this.binding.toolbar, false, true);
        setDrawer(this.binding.toolbar);

        this.mMapLocationSource = new MapLocationSource();

        // Workaround for Google Maps crash on Android 6
        try {
            binding.mapView.onCreate(savedInstanceState);
        } catch (Exception e) {
            Timber.e("not showing map due to issue https://issuetracker.google.com/issues/35827842");
            flagStateMapReady = false;
        }
        this.bottomSheetBehavior = BottomSheetBehavior.from(this.binding.bottomSheetLayout);
        this.binding.contactPeek.contactRow.setOnClickListener(this);
        this.binding.contactPeek.contactRow.setOnLongClickListener(this);
        this.binding.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);

            }
        });
        setBottomSheetHidden();

        AppBarLayout appBarLayout = this.binding.appBarLayout;
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
        AppBarLayout.Behavior behavior = new AppBarLayout.Behavior();
        behavior.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
            @Override
            public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
                return false;
            }
        });
        params.setBehavior(behavior);

        App.startBackgroundServiceCompat(getApplicationContext(), null);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        try {
            if (binding.mapView != null)
                binding.mapView.onSaveInstanceState(bundle);
        } catch (Exception ignored) {
            flagStateMapReady = false;
        }

    }

    @Override
    public void onDestroy() {
        try {
            if (binding.mapView != null)
                binding.mapView.onDestroy();
        } catch (Exception ignored) {
            flagStateMapReady = false;
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            if (binding.mapView != null)
                binding.mapView.onResume();

            if (mMap == null)
                initMapDelayed();

            queueActionMapUpdate();

        } catch (Exception e) {
            Timber.e("not showing map due to crash in Google Maps library");
            flagStateMapReady = false;
        }
        handleIntentExtras(getIntent());
        executePendingActions();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (binding.mapView != null)
                binding.mapView.onPause();
        } catch (Exception e) {
            flagStateMapReady = false;
        }
        // Save current repo state so we ca apply updates to contacts on resume
        repoRevision = viewModel.getContactsRevision();
    }

    private void handleIntentExtras(Intent intent) {
        Timber.v("handleIntentExtras");

        Bundle b = navigator.getExtrasBundle(intent);
        if (b != null) {
            Timber.v("intent has extras from drawerProvider");
            String contactId = b.getString(BUNDLE_KEY_CONTACT_ID);
            if (contactId != null) {
                viewModel.restore(contactId);
            }
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        try {
            if (binding.mapView != null)
                binding.mapView.onLowMemory();
        } catch (Exception ignored) {
            flagStateMapReady = false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentExtras(intent);
    }

    public void initMapDelayed() {
        flagStateMapReady = false;
        //flagStateLocationReady = false;

        App.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                initMap();
            }
        }, 500);
    }

    private void initMap() {
        flagStateMapReady = false;
        binding.mapView.getMapAsync(this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map, menu);
        this.mMenu = menu;
        if (!flagStateLocationReady)
            disableLocationMenus();
        else
            enableLocationMenus();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {

            App.startBackgroundServiceCompat(this, BackgroundService.INTENT_ACTION_SEND_LOCATION_USER);

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

    private void disableLocationMenus() {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.menu_mylocation).getIcon().setAlpha(130);
            this.mMenu.findItem(R.id.menu_report).getIcon().setAlpha(130);
        }
    }

    private void enableLocationMenus() {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.menu_mylocation).getIcon().setAlpha(255);
            this.mMenu.findItem(R.id.menu_report).getIcon().setAlpha(255);
        }
    }

    // MAP CALLBACKS
    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Timber.v("onMapReady");

        this.mMap = googleMap;
        this.mMap.setIndoorEnabled(false);
        this.mMap.setLocationSource(getMapLocationSource());
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
    }

    @Override
    public void onMapClick(LatLng latLng) {
        queueActionModeFree();
        viewModel.onMapClick();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTag() != null) {
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

    private void doUpdateMarkerAll() {
        Timber.v("repoRevision:%s", repoRevision);
        long newRepoRevision = viewModel.getContactsRevision();

        if (repoRevision < newRepoRevision) {
            Timber.v("repoRevision:%s, newRepoRevision:%s => updating marker", repoRevision, newRepoRevision);
            addMarker();
        } else if (repoRevision > newRepoRevision) {
            Timber.v("repoRevision:%s, newRepoRevision:%s => reinitializing marker", repoRevision, newRepoRevision);
            clearMarker();
            addMarker();
        } else {
            Timber.v("no update");
        }
        repoRevision = newRepoRevision;
    }

    private void addMarker() {
        for (Object c : viewModel.getContacts()) {
            doUpdateMarkerSingle(FusedContact.class.cast(c));
        }
        repoRevision = viewModel.getContactsRevision();

    }

    private void doUpdateMarkerSingle(@Nullable FusedContact contact) {
        if (contact == null || !contact.hasLocation() || mMap == null)
            return;

        Marker m = mMarkers.get(contact.getId());

        if (m != null) {
            m.setPosition(contact.getLatLng());
        } else {
            m = mMap.addMarker(new MarkerOptions().position(contact.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            m.setTag(contact.getId());
            mMarkers.put(contact.getId(), m);
        }

        App.getContactImageProvider().setMarkerAsync(m, contact);

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_navigate:
                FusedContact c = viewModel.getContact();
                if (c != null && c.hasLocation()) {
                    try {
                        LatLng l = c.getLatLng();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + l.latitude + "," + l.longitude));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(this, getString(R.string.noNavigationApp), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
                }

                return true;
            case R.id.menu_clear:
                viewModel.onClearContactClicked();
            default:
                return false;
        }
    }

    public LocationSource getMapLocationSource() {
        if (mMapLocationSource == null)
            mMapLocationSource = new MapLocationSource();

        return mMapLocationSource;
    }

    @Override
    public boolean onLongClick(View view) {
        viewModel.onBottomSheetLongClick();
        return true;
    }

    @Override
    public void setBottomSheetExpanded() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }    // BOTTOM SHEET CALLBACKS
    @Override
    public void onClick(View view) {
        viewModel.onBottomSheetClick();
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
        Timber.v("id:%s", c.getId());
        doUpdateMarkerSingle(c);
    }

    @Override
    public void contactUpdateActive() {
        Timber.v("");
        onActiveContactUpdated();
        doUpdateMarkerSingle(viewModel.getContact());
    }

    @Override
    public void contactRemove(FusedContact c) {
        doUpdateMarkerAll();
        //if(mode==FLAG_ACTION_MODE_FREE && c==viewModel.getContact())
        //    queueActionModeFree();

    }

    @Override
    public void setModeContact(boolean center) {
        queueActionModeContact(center);
    }

    @Override
    public void setModeDevice() {
        queueActionModeDevice();
    }

    @Override
    public void setModeFree() {
        queueActionModeFree();
    }

    @Override
    public void clearMarker() {
        if (flagStateMapReady)
            mMap.clear();
        mMarkers.clear();
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.START); //new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_contacts, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        if (App.getPreferences().getModeId() == App.MODE_ID_HTTP_PRIVATE)
            popupMenu.getMenu().removeItem(R.id.menu_clear);
        popupMenu.show();
    }

    public class MapLocationSource implements LocationSource {
        LocationSource.OnLocationChangedListener mListener;
        Location mLocation;

        MapLocationSource() {
            super();
            App.getEventBus().register(this);
        }

        @Override
        public void activate(OnLocationChangedListener onLocationChangedListener) {
            Timber.v("location source activated");
            mListener = onLocationChangedListener;
            if (mLocation != null)
                this.mListener.onLocationChanged(mLocation);
        }

        @Override
        public void deactivate() {
            mListener = null;
            App.getEventBus().unregister(this);
        }

        @Subscribe(threadMode = ThreadMode.MAIN, priority = 1, sticky = true)
        public void update(Location l) {
            Timber.v("location source updated");

            this.mLocation = l;
            if (mListener != null)
                this.mListener.onLocationChanged(this.mLocation);
            onLocationSourceUpdated();
        }

        LatLng getLatLng() {
            return new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        }
    }
}
