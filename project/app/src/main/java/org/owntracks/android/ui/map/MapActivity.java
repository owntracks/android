package org.owntracks.android.ui.map;

import android.arch.lifecycle.Observer;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntegerRes;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Runner;
import org.owntracks.android.support.widgets.BindingConversions;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;

import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import timber.log.Timber;

public class MapActivity extends BaseActivity<UiMapBinding, MapMvvm.ViewModel> implements MapMvvm.View, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, OnMapReadyCallback, Observer {
    public static final String BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID";
    private static final long ZOOM_LEVEL_STREET = 15;

    final WeakHashMap<String, Marker> mMarkers = new WeakHashMap<>();
    private GoogleMap mMap;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private boolean isMapReady = false;
    private Menu mMenu;

    @Inject
    protected Runner runner;

    @Inject
    protected ContactImageProvider contactImageProvider;

    @Inject
    protected GeocodingProvider geocodingProvider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (!requirementsChecker.areRequirementsMet()) {
            navigator.startActivity(WelcomeActivity.class);
            finish();
        }
        bindAndAttachContentView(R.layout.ui_map, savedInstanceState);

        setSupportToolbar(this.binding.toolbar, false, true);
        setDrawer(this.binding.toolbar);

        // Workaround for Google Maps crash on Android 6
        try {
            binding.mapView.onCreate(savedInstanceState);
        } catch (Exception e) {
            Timber.e("not showing map due to issue   ");
            isMapReady = false;
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

        App.getInstance().startBackgroundServiceCompat(this);

        viewModel.getContact().observe(this, this);
        viewModel.getBottomSheetHidden().observe(this, new Observer() {
            @Override
            public void onChanged(@Nullable Object o) {
                if(Boolean.class.cast(o)) {
                    setBottomSheetHidden();
                } else {
                    setBottomSheetCollapsed();
                }
            }
        });
        viewModel.getCenter().observe(this, new Observer() {
            @Override
            public void onChanged(@Nullable Object o) {
                if(o != null) {
                    updateCamera(LatLng.class.cast(o));
                }
            }
        });

    }

    @Override
    public void onChanged(@Nullable Object activeContact) {
        if (activeContact != null) {
            FusedContact c = FusedContact.class.cast(activeContact);
            Timber.v("for contact: %s", c.getId());

            binding.contactPeek.name.setText(c.getFusedName());
            if(c.hasLocation()) {
                contactImageProvider.setImageViewAsync(binding.contactPeek.image, c);
                geocodingProvider.resolve(c.getMessageLocation(), binding.contactPeek.location);
                BindingConversions.setRelativeTimeSpanString(binding.contactPeek.locationDate, c.getTst());
                binding.acc.setText(c.getFusedLocationAccuracy());
                binding.tid.setText(c.getTrackerId());
                binding.id.setText(c.getId());
            } else {
                binding.contactPeek.location.setText(R.string.na);
                binding.contactPeek.locationDate.setText(R.string.na);
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        try {
            if (binding.mapView != null)
                binding.mapView.onSaveInstanceState(bundle);
        } catch (Exception ignored) {
            isMapReady = false;
        }

    }



    @Override
    public void onDestroy() {
        try {
            if (binding.mapView != null)
                binding.mapView.onDestroy();
        } catch (Exception ignored) {
            isMapReady = false;
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.isMapReady = false;

        try {
            if (binding.mapView != null)
                binding.mapView.onResume();

            if (mMap == null) {
                this.isMapReady = false;
                initMapDelayed();
            } else {
                viewModel.onMapReady();
                this.isMapReady = true;
            }

        } catch (Exception e) {
            Timber.e("not showing map due to crash in Google Maps library");
            isMapReady = false;
        }
        handleIntentExtras(getIntent());
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (binding.mapView != null)
                binding.mapView.onPause();
        } catch (Exception e) {
            isMapReady = false;
        }
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
            isMapReady = false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntentExtras(intent);
        try {
            if (binding.mapView != null)
                binding.mapView.onLowMemory();
        } catch (Exception ignored){
            isMapReady = false;
        }
    }

    public void initMapDelayed() {
        isMapReady = false;

        //runner.postOnMainHandlerDelayed();
        runner.postOnMainHandlerDelayed(new Runnable() {
            @Override
            public void run() {
                initMap();
            }
        }, 500);
    }

    private void initMap() {
        isMapReady = false;
        try {
            binding.mapView.getMapAsync(this);
        } catch (Exception ignored) { }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map, menu);
        this.mMenu = menu;
        if (!viewModel.hasLocation())
            enableLocationMenus();
        else
            disableLocationMenus();

        updateMonitoringModeMenu();

        return true;
    }

    public void updateMonitoringModeMenu() {
        MenuItem item = this.mMenu.findItem(R.id.menu_monitoring);

        switch (preferences.getMonitoring()) {
            case 0:
                item.setIcon(R.drawable.ic_done);
                break;
            case 1:
                item.setIcon(R.drawable.ic_assignment_late_white_48dp);
                break;
            case 2:
                item.setIcon(R.drawable.ic_close);
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {
            viewModel.sendLocation();

            return true;
        } else if (itemId == R.id.menu_mylocation) {
            viewModel.onMenuCenterDeviceClicked();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.menu_monitoring) {
            stepMonitoringModeMenu();
        }
        return false;
    }

    private void stepMonitoringModeMenu() {
        int mode = preferences.getMonitoring();
        int newmode;
        if(mode == 0)  {
            newmode = 1;
            Toast.makeText(this, "significant location monitoring mode", Toast.LENGTH_SHORT).show();
        } else if (mode == 1)  {
            newmode = 2;
            Toast.makeText(this, "move monitoring mode", Toast.LENGTH_SHORT).show();
        } else  {
            newmode = 0;
            Toast.makeText(this, "manual monitoring mode", Toast.LENGTH_SHORT).show();
        }
        Timber.v("setting monitoring mode %s -> %s", mode, newmode);
        preferences.setMonitoring(newmode);
    }

    private void disableLocationMenus() {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.menu_mylocation).getIcon().setAlpha(130);
            this.mMenu.findItem(R.id.menu_report).getIcon().setAlpha(130);
        }
    }

    public void enableLocationMenus() {
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
        this.mMap.setLocationSource(viewModel.getMapLocationSource());
        this.mMap.setMyLocationEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.mMap.setOnMapClickListener(viewModel.getOnMapClickListener());
        this.mMap.setOnMarkerClickListener(viewModel.getOnMarkerClickListener());
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
        this.isMapReady = true;
        viewModel.onMapReady();
    }


    public void updateCamera(@NonNull LatLng latLng) {
        if(isMapReady)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL_STREET));
    }

    @Override
    public void clearMarkers() {
        if (isMapReady)
            mMap.clear();
        mMarkers.clear();
    }

    @Override
    public void removeMarker(@Nullable FusedContact contact) {
        if(contact == null)
            return;

        Marker m = mMarkers.get(contact.getId());
        if(m != null)
            m.remove();
    }

    @Override
    public void updateMarker(@Nullable FusedContact contact) {
        if (contact == null || !contact.hasLocation() || !isMapReady)
            return;

        Marker m = mMarkers.get(contact.getId());

        if (m != null) {
            m.setPosition(contact.getLatLng());
        } else {
            m = mMap.addMarker(new MarkerOptions().position(contact.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            m.setTag(contact.getId());
            mMarkers.put(contact.getId(), m);
        }

        contactImageProvider.setMarkerAsync(m, contact);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_navigate:

                FusedContact c = viewModel.getActiveContact();
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

    @Override
    public boolean onLongClick(View view) {
        viewModel.onBottomSheetLongClick();
        return true;
    }

    @Override
    public void setBottomSheetExpanded() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    // BOTTOM SHEET CALLBACKS
    @Override
    public void onClick(View view) {
        viewModel.onBottomSheetClick();
    }

    @Override
    public void setBottomSheetCollapsed() {
        Timber.v("vm contact: %s", binding.getVm().getActiveContact());
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void setBottomSheetHidden() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if(mMenu != null)
            mMenu.close();
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.START); //new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_contacts, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        if (preferences.getModeId() == MessageProcessorEndpointHttp.MODE_ID)
            popupMenu.getMenu().removeItem(R.id.menu_clear);
        popupMenu.show();
    }

}
