package org.owntracks.android.ui.region;

import android.app.Activity;
import android.content.Intent;
import android.databinding.Observable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiRegionBinding;
import org.owntracks.android.support.SimpleTextChangeListener;
import org.owntracks.android.support.widgets.Toasts;
import org.owntracks.android.ui.base.BaseActivity;


public class RegionActivity extends BaseActivity<UiRegionBinding, RegionMvvm.ViewModel> implements RegionMvvm.View {
    private static final int REQUEST_PLACE_PICKER = 19283;

    private MenuItem saveButton;
    private final Observable.OnPropertyChangedCallback waypointPropertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable observable, int i) {
            conditionallyEnableSaveButton();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setHasEventBus(false);
        bindAndAttachContentView(R.layout.ui_region, savedInstanceState);
        setSupportToolbar(binding.toolbar);

        Bundle extras = navigator.getExtrasBundle(getIntent());

        viewModel.setWaypoint(extras.getLong("keyId", -1));
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.getVm().getWaypoint().addOnPropertyChangedCallback(waypointPropertyChangedCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.getVm().getWaypoint().removeOnPropertyChangedCallback(waypointPropertyChangedCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            Place place = PlacePicker.getPlace(this, data);

            final LatLng l = place.getLatLng();
            final CharSequence addr = place.getAddress();
            if(l != null) {
                viewModel.getWaypoint().setGeofenceLatitude(l.latitude);
                viewModel.getWaypoint().setGeofenceLongitude(l.longitude);
                Toast.makeText(this, addr, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoint, menu);
        this.saveButton = menu.findItem(R.id.save);
        conditionallyEnableSaveButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                viewModel.saveWaypoint();
                finish();
                return true;
            case R.id.pick:
                pickLocation();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pickLocation() {
        try {

            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), REQUEST_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Toast.makeText(this, R.string.placePickerNotAvailable, Toast.LENGTH_SHORT).show();
        }
    }

    public void conditionallyEnableSaveButton() {
        if(saveButton != null) {
            saveButton.setEnabled(viewModel.canSaveWaypoint());
            saveButton.getIcon().setAlpha(viewModel.canSaveWaypoint() ? 255 : 130);
        }
    }
}
