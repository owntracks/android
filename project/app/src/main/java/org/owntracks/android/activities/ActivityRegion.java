package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.ActivityRegionBinding;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.support.SimpleTextChangeListener;
import org.owntracks.android.db.Waypoint;


@Deprecated
public class ActivityRegion extends ActivityBase  {
    private static final String TAG = "ActivityRegion";

    private static final int REQUEST_PLACE_PICKER = 19283;
    private static final int PERMISSION_REQUEST_USE_CURRENT = 1;
    private WaypointDao dao;
    private Waypoint waypoint;
    private boolean update = true;

    private MenuItem saveButton;
    private ActivityRegionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_waypoint);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_region);

        setSupportToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        this.dao = App.getDao().getWaypointDao();


        if (hasIntentExtras()) {
            this.waypoint = this.dao.loadByRowId(getIntent().getExtras().getLong("keyId"));
        }

        if(this.waypoint == null) {
            this.update = false;
            this.waypoint = new Waypoint();
            this.waypoint.setDefaults();
        }

        binding.setItem(this.waypoint);
        binding.shareWrapper.setVisibility(App.getPreferences().isModeMqttPublic() ? View.GONE : View.VISIBLE);
        setupListenerAndRequiredFields();
    }

    private void setupListenerAndRequiredFields() {
        TextWatcher requiredForSave = new SimpleTextChangeListener() {
            @Override
            public void onChanged(String s) {
                conditionallyEnableSaveButton();
            }
        };

        binding.description.addTextChangedListener(requiredForSave);
        binding.latitude.addTextChangedListener(requiredForSave);
        binding.longitude.addTextChangedListener(requiredForSave);

        binding.share.setChecked(this.waypoint.getShared());
    }

    private void conditionallyEnableSaveButton() {

        boolean enabled;
        try {

            float lat = Float.parseFloat(binding.latitude.getText().toString());
            float lon = Float.parseFloat(binding.longitude.getText().toString());

            enabled = (binding.description.getText().toString().length() > 0)
                    && (binding.latitude.getText().toString().length() > 0)
                    && (binding.longitude.getText().toString().length() > 0)
                    && ((lat <= 90) && (lat >= -90)) && ((lon <= 180) && (lon >= -180));

        } catch (Exception e) {
            enabled = false; // invalid input or NumberFormatException result in no valid input
        }
        Log.v(TAG, "conditionallyEnableSaveButton: " +enabled);
        if(saveButton != null) {
            saveButton.setEnabled(enabled);
            saveButton.getIcon().setAlpha(enabled ? 255 : 130);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void add(Waypoint w) {
        long id = this.dao.insert(w);
        Log.v(TAG, "added waypoint with id: " + id);
        App.getEventBus().post(w); // For ServiceLocator update
        //App.getEventBus().postSticky(new Events.WaypointAddedByUser(w)); // For UI update
    }

    private void update(Waypoint w) {
        this.dao.update(w);
        App.getEventBus().post(w); // For ServiceLocator update
        //App.getEventBus().postSticky(new Events.WaypointUpdatedByUser(w)); // For UI update
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
                save();
                finish();
                return true;
            case R.id.useCurrent:
                //runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USE_CURRENT);
                pickLocation();

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

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_PLACE_PICKER && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            Place place = PlacePicker.getPlace(this, data);

            final LatLng l = place.getLatLng();
            final CharSequence addr = place.getAddress();
            if(l != null) {
                binding.latitude.setText(Double.toString(l.latitude));
                binding.longitude.setText(Double.toString(l.longitude));
                Toast.makeText(App.getContext(), addr, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void  pickLocation() {
        try {

            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), REQUEST_PLACE_PICKER);

        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }

    }

    private void save() {
       Waypoint w = this.waypoint;


        if (!update) {
            w.setModeId(App.getPreferences().getModeId());
            w.setDate(new java.util.Date());
        }

        w.setDescription(binding.description.getText().toString());
        try {
            w.setGeofenceLatitude(Double.parseDouble(binding.latitude.getText().toString()));
            w.setGeofenceLongitude(Double.parseDouble(binding.longitude.getText().toString()));
        } catch (NumberFormatException ignored) { }

        try {
            w.setGeofenceRadius(Integer.parseInt(binding.radius.getText().toString()));
        } catch (NumberFormatException e) {
            w.setGeofenceRadius(null);
        }

        w.setBeaconUUID(binding.beaconUUID.getText().toString());
        try {

            w.setBeaconMinor(Integer.valueOf(binding.beaconMinor.getText().toString()));
        } catch (NumberFormatException e) {
            w.setBeaconMinor(null);
        }

        try {
            w.setBeaconMajor(Integer.valueOf(binding.beaconMajor.getText().toString()));
        } catch (NumberFormatException e) {
            w.setBeaconMajor(null);
        }

        if(!App.getPreferences().isModeMqttPublic())
            w.setShared(binding.share.isChecked());
        else
            w.setShared(false);


        if (update)
            update(w);
        else {
            add(w);
        }


    }

    // If the user hits back, go back to ActivityMain, no matter where he came from
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

    }
}
