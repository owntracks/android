package org.owntracks.android.ui.region;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiRegionBinding;
import org.owntracks.android.ui.base.BaseActivity;

public class RegionActivity extends BaseActivity<UiRegionBinding, RegionMvvm.ViewModel> implements RegionMvvm.View{

    private static final int REQUEST_PLACE_PICKER = 19283;

    private MenuItem saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasEventBus(false);
        bindAndAttachContentView(R.layout.ui_region, savedInstanceState);
        setSupportToolbar(binding.toolbar);

        Bundle b = navigator.getExtrasBundle(getIntent());
        if(b != null) {
            viewModel.loadWaypoint(b.getLong("waypointId",0));
        }


        binding.description.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                conditionallyEnableSaveButton();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            Place place = PlacePicker.getPlace(this, data);

            final LatLng l = place.getLatLng();
            final CharSequence addr = place.getAddress();
            if (l != null) {
                viewModel.setLatLng(l.latitude, l.longitude);
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
