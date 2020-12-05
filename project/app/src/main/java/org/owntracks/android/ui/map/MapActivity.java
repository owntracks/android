package org.owntracks.android.ui.map;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.R;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.databinding.UiMapBinding;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.services.LocationProcessor;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.geocoding.GeocoderProvider;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.widgets.BindingConversions;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.welcome.WelcomeActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;

public class MapActivity extends BaseActivity<UiMapBinding, MapMvvm.ViewModel> implements MapMvvm.View, View.OnClickListener, View.OnLongClickListener, PopupMenu.OnMenuItemClickListener, OnMapReadyCallback, Observer {
    public static final String BUNDLE_KEY_CONTACT_ID = "BUNDLE_KEY_CONTACT_ID";
    private static final long ZOOM_LEVEL_STREET = 15;
    private final int PERMISSIONS_REQUEST_CODE = 1;

    private final Map<String, Marker> markers = new HashMap<>();
    private GoogleMap googleMap;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private boolean isMapReady = false;
    private Menu mMenu;

    private FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationRepoUpdaterCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Timber.d("Foreground location result received: %s", locationResult);
            locationRepo.setCurrentLocation(locationResult.getLastLocation());
            super.onLocationResult(locationResult);
        }
    };

    @Inject
    LocationRepo locationRepo;

    @Inject
    RunThingsOnOtherThreads runThingsOnOtherThreads;

    @Inject
    ContactImageProvider contactImageProvider;

    @Inject
    EventBus eventBus;

    @Inject
    GeocoderProvider geocoderProvider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!preferences.isSetupCompleted()) {
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
            Timber.e(e, "Failed to bind map to view.");
            isMapReady = false;
        }
        this.bottomSheetBehavior = BottomSheetBehavior.from(this.binding.bottomSheetLayout);
        this.binding.contactPeek.contactRow.setOnClickListener(this);
        this.binding.contactPeek.contactRow.setOnLongClickListener(this);
        this.binding.moreButton.setOnClickListener(this::showPopupMenu);
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

        viewModel.getContact().observe(this, this);
        viewModel.getBottomSheetHidden().observe(this, o -> {
            if ((Boolean) o) {
                setBottomSheetHidden();
            } else {
                setBottomSheetCollapsed();
            }
        });
        viewModel.getCenter().observe(this, o -> {
            if (o != null) {
                updateCamera((LatLng) o);
            }
        });
        checkAndRequestLocationPermissions();
        Timber.v("starting BackgroundService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService((new Intent(this, BackgroundService.class)));
        } else {
            startService((new Intent(this, BackgroundService.class)));
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void checkAndRequestLocationPermissions() {
        if (!requirementsChecker.isPermissionCheckPassed()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Activity currentActivity = this;
                    new AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setMessage(R.string.permissions_description)
                            .setPositiveButton("OK", (dialog, which) ->
                                    ActivityCompat.requestPermissions(currentActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE)
                            )
                            .show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onChanged(@Nullable Object activeContact) {
        if (activeContact != null) {
            FusedContact c = (FusedContact) activeContact;
            Timber.v("for contact: %s", c.getId());

            binding.contactPeek.name.setText(c.getFusedName());
            if (c.hasLocation()) {
                ContactImageProvider.setImageViewAsync(binding.contactPeek.image, c);
                geocoderProvider.resolve(c.getMessageLocation(), binding.contactPeek.location);
                BindingConversions.setRelativeTimeSpanString(binding.contactPeek.locationDate, c.getTst());
                binding.acc.setText(String.format(Locale.getDefault(), "%s m", c.getFusedLocationAccuracy()));
                binding.tid.setText(c.getTrackerId());
                binding.id.setText(c.getId());
                if (viewModel.hasLocation()) {
                    binding.distance.setVisibility(View.VISIBLE);
                    binding.distanceLabel.setVisibility(View.VISIBLE);

                    float[] distance = new float[2];
                    Location.distanceBetween(viewModel.getCurrentLocation().latitude, viewModel.getCurrentLocation().longitude, c.getLatLng().latitude, c.getLatLng().longitude, distance);

                    binding.distance.setText(String.format(Locale.getDefault(), "%d m", Math.round(distance[0])));
                } else {
                    binding.distance.setVisibility(View.GONE);
                    binding.distanceLabel.setVisibility(View.GONE);

                }

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
            binding.mapView.onSaveInstanceState(bundle);
        } catch (Exception ignored) {
            isMapReady = false;
        }
    }

    @Override
    public void onDestroy() {
        try {
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
            binding.mapView.onResume();

            if (googleMap == null) {
                Timber.v("map not ready. Running initDelayed()");
                this.isMapReady = false;
                initMapDelayed();
            } else {
                Timber.v("map ready. Running onMapReady()");
                this.isMapReady = true;
                viewModel.onMapReady();
            }

        } catch (Exception e) {
            Timber.e(e, "Not showing map due to crash in Google Maps library");
            isMapReady = false;
        }
        handleIntentExtras(getIntent());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkAndRequestLocationPermissions();
        }

        fusedLocationClient.requestLocationUpdates(
                new LocationRequest()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(TimeUnit.SECONDS.toMillis(5)),
                locationRepoUpdaterCallback,
                null
        ).addOnCompleteListener(task ->
                Timber.i("Requested foreground location updates. isSuccessful: %s isCancelled: %s", task.isSuccessful(), task.isCanceled())
        );
        updateMonitoringModeMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            binding.mapView.onPause();
        } catch (Exception e) {
            isMapReady = false;
        }
        fusedLocationClient.removeLocationUpdates(locationRepoUpdaterCallback).addOnCompleteListener(task ->
                Timber.i("Removed foreground location updates. isSuccessful: %s isCancelled: %s", task.isSuccessful(), task.isCanceled())
        );
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
            binding.mapView.onLowMemory();
        } catch (Exception ignored) {
            isMapReady = false;
        }
    }

    private void initMapDelayed() {
        isMapReady = false;
        runThingsOnOtherThreads.postOnMainHandlerDelayed(this::initMap, 500);
    }

    private void initMap() {
        isMapReady = false;
        try {
            binding.mapView.getMapAsync(this);
        } catch (Exception ignored) {
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_map, menu);
        this.mMenu = menu;
        if (viewModel.hasLocation())
            enableLocationMenus();
        else
            disableLocationMenus();

        updateMonitoringModeMenu();

        return true;
    }

    public void updateMonitoringModeMenu() {
        if (this.mMenu == null) {
            return;
        }
        MenuItem item = this.mMenu.findItem(R.id.menu_monitoring);

        switch (preferences.getMonitoring()) {
            case LocationProcessor.MONITORING_QUIET:
                item.setIcon(R.drawable.ic_baseline_stop_36);
                item.setTitle(R.string.monitoring_quiet);
                break;
            case LocationProcessor.MONITORING_MANUAL:
                item.setIcon(R.drawable.ic_baseline_pause_36);
                item.setTitle(R.string.monitoring_manual);
                break;
            case LocationProcessor.MONITORING_SIGNIFICANT:
                item.setIcon(R.drawable.ic_baseline_play_arrow_36);
                item.setTitle(R.string.monitoring_significant);
                break;
            case LocationProcessor.MONITORING_MOVE:
                item.setIcon(R.drawable.ic_step_forward_2);
                item.setTitle(R.string.monitoring_move);
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
        preferences.setMonitoringNext();

        int newmode = preferences.getMonitoring();
        if (newmode == LocationProcessor.MONITORING_QUIET) {
            Toast.makeText(this, R.string.monitoring_quiet, Toast.LENGTH_SHORT).show();
        } else if (newmode == LocationProcessor.MONITORING_MANUAL) {
            Toast.makeText(this, R.string.monitoring_manual, Toast.LENGTH_SHORT).show();
        } else if (newmode == LocationProcessor.MONITORING_SIGNIFICANT) {
            Toast.makeText(this, R.string.monitoring_significant, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.monitoring_move, Toast.LENGTH_SHORT).show();
        }
    }

    private void disableLocationMenus() {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.menu_mylocation).setEnabled(false).getIcon().setAlpha(128);
            this.mMenu.findItem(R.id.menu_report).setEnabled(false).getIcon().setAlpha(128);
        }
    }

    public void enableLocationMenus() {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.menu_mylocation).setEnabled(true).getIcon().setAlpha(255);
            this.mMenu.findItem(R.id.menu_report).setEnabled(true).getIcon().setAlpha(255);
        }
    }

    // MAP CALLBACKS
    @SuppressWarnings("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setIndoorEnabled(false);
        this.googleMap.setLocationSource(viewModel.getMapLocationSource());
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.setOnMapClickListener(viewModel.getOnMapClickListener());
        this.googleMap.setOnMarkerClickListener(viewModel.getOnMarkerClickListener());
        this.googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
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


    private void updateCamera(@NonNull LatLng latLng) {
        if (isMapReady)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL_STREET));
    }

    @Override
    public void clearMarkers() {
        if (isMapReady)
            googleMap.clear();
        markers.clear();
    }

    @Override
    public void removeMarker(@Nullable FusedContact contact) {
        if (contact == null)
            return;

        Marker m = markers.get(contact.getId());
        if (m != null)
            m.remove();
    }

    @Override
    public void updateMarker(@Nullable FusedContact contact) {
        if (contact == null || !contact.hasLocation() || !isMapReady) {
            Timber.v("unable to update marker. null:%s, location:%s, mapReady:%s", contact == null, contact == null || contact.hasLocation(), isMapReady);
            return;
        }

        Timber.v("updating marker for contact: %s", contact.getId());
        Marker marker = markers.get(contact.getId());

        if (marker != null && marker.getTag() != null) {
            marker.setPosition(contact.getLatLng());
        } else {
            // If a marker has been removed, its tag will be null. Doing anything with it will make it explode
            if (marker != null) {
                markers.remove(contact.getId());
            }
            marker = googleMap.addMarker(new MarkerOptions().position(contact.getLatLng()).anchor(0.5f, 0.5f).visible(false));
            marker.setTag(contact.getId());
            markers.put(contact.getId(), marker);
        }

        contactImageProvider.setMarkerAsync(marker, contact);
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
        if (mMenu != null)
            mMenu.close();
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.START); //new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.menu_popup_contacts, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);
        if (preferences.getMode() == MessageProcessorEndpointHttp.MODE_ID)
            popupMenu.getMenu().removeItem(R.id.menu_clear);
        popupMenu.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            eventBus.postSticky(new Events.PermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION));
        }
    }
}
