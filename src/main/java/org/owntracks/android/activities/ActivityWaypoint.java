package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.ActivityWaypointBindingBinding;
import org.owntracks.android.db.Dao;
import org.owntracks.android.support.SimpleTextChangeListener;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StaticHandlerInterface;
import org.owntracks.android.support.Toasts;

import de.greenrobot.event.EventBus;


public class ActivityWaypoint extends ActivityBase implements StaticHandlerInterface {
    private static final String TAG = "ActivityWaypoint";

    private static final int REQUEST_PLACE_PICKER = 19283;
    private static final int PERMISSION_REQUEST_USE_CURRENT = 1;
    private WaypointDao dao;
    private TextWatcher requiredForSave;
    private GeocodableLocation currentLocation;
    private Handler handler;
    private TextView waypointListPlaceholder;
    private int waypointLocalLastIdx = 0;
    private int waypointLocalMonitoringLastIdx = 0;
    private boolean localHeaderAdded = false;
    private boolean localMonitoringHeaderAdded = false;

    private static final Integer WAYPOINT_TYPE_LOCAL = 0;
    private static final Integer WAYPOINT_TYPE_LOCAL_MONITORING = 2;


    private Waypoint waypoint;
    boolean update = true;


    private Switch share;
    private MenuItem saveButton;

    // Thanks Google for not providing a getter for the value of switches.
    private boolean shareValue = false;
    private ActivityWaypointBindingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v("ActivityWaypoints", "ServiceProxy bound");
            }
        });


        setContentView(R.layout.activity_waypoint);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_waypoint_binding);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        this.dao = Dao.getWaypointDao();


        if (hasIntentExtras()) {
            this.waypoint = this.dao.loadByRowId(getIntent().getExtras().getLong("keyId"));
        }

        if(this.waypoint == null) {
            this.update = false;
            this.waypoint = new Waypoint();
        }

        binding.setItem(this.waypoint);
        binding.shareWrapper.setVisibility(Preferences.isModePublic() ? View.GONE : View.VISIBLE);

        setupRequiredFields();
    }

    private void setupRequiredFields() {
        requiredForSave = new SimpleTextChangeListener() {
            @Override
            public void onChanged(String s) {
                conditionallyEnableSaveButton();
            }
        };

        binding.description.addTextChangedListener(requiredForSave);
        binding.latitude.addTextChangedListener(requiredForSave);
        binding.longitude.addTextChangedListener(requiredForSave);

    }

    private void conditionallyEnableSaveButton() {

        boolean enabled = false;
        try {
            enabled = (binding.description.getText().toString().length() > 0)
                    && (binding.description.getText().toString().length() > 0)
                    && (binding.description.getText().toString().length() > 0);

        } catch (Exception e) {
            enabled = false; // invalid input or NumberFormatException result in no valid input
        }
        Log.v(TAG, "conditionallyEnableSaveButton: " +enabled);
        saveButton.setEnabled(enabled);
        saveButton.getIcon().setAlpha(enabled ? 255 : 130);

    }


    @Override
    public void onDestroy() {
       // handler.removeCallbacksAndMessages(null); // disable handler
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.closeServiceConnection();

            }
        });
        super.onDestroy();
    }


    protected void add(Waypoint w) {
        long id = this.dao.insert(w);
        Log.v(TAG, "added waypoint with id: " + id);
        EventBus.getDefault().post(new Events.WaypointAdded(w)); // For ServiceLocator update
        //EventBus.getDefault().postSticky(new Events.WaypointAddedByUser(w)); // For UI update
    }

    protected void update(Waypoint w) {
        this.dao.update(w);
        EventBus.getDefault().post(new Events.WaypointUpdated(w)); // For ServiceLocator update
        //EventBus.getDefault().postSticky(new Events.WaypointUpdatedByUser(w)); // For UI update
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
                runActionWithLocationPermissionCheck(PERMISSION_REQUEST_USE_CURRENT);

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

        if (requestCode == REQUEST_PLACE_PICKER
                && resultCode == Activity.RESULT_OK) {

            // The user has selected a place. Extract the name and address.
            final Place place = PlacePicker.getPlace(data, this);

            final CharSequence name = place.getName();
            final CharSequence address = place.getAddress();
            String attributions = PlacePicker.getAttributions(data);
            if (attributions == null) {
                attributions = "";
            }


            //mViewName.setText(name);
            //mViewAddress.setText(address);
            //mViewAttributions.setText(Html.fromHtml(attributions));

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // not used yet
    private void pickLocation() {
        try {
            PlacePicker.IntentBuilder intentBuilder =  new PlacePicker.IntentBuilder();

            Intent intent = intentBuilder.build(this);
            // Start the intent by requesting a result,
            // identified by a request code.
            startActivityForResult(intent, REQUEST_PLACE_PICKER);

        } catch (GooglePlayServicesRepairableException e) {
            // ...
        } catch (GooglePlayServicesNotAvailableException e) {
            // ...
        }

    }



    protected  void onRunActionWithPermissionCheck(int action, boolean granted) {
        switch (action) {
            case PERMISSION_REQUEST_USE_CURRENT:
                Log.v(TAG, "request code: PERMISSION_REQUEST_REPORT_LOCATION");
                if (granted) {
                    ServiceProxy.runOrBind(this, new Runnable() {

                        @Override
                        public void run() {
                            Location l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                            if(l != null) {
                                waypoint.setGeofenceLatitude(l.getLatitude());
                                waypoint.setGeofenceLongitude(l.getLongitude());
                            } else {

                                Toasts.showCurrentLocationNotAvailable();
                            }
                        }
                    });
                } else {
                    Toasts.showLocationPermissionNotAvailable();
                }
                return;

        }
    }



    private void save() {
       Waypoint w = this.waypoint;


        if (!update) {
            w.setModeId(Preferences.getModeId());
            w.setDate(new java.util.Date());
        }

        w.setDescription(binding.description.getText().toString());
        try {
            w.setGeofenceLatitude(Double.parseDouble(binding.latitude.getText().toString()));
            w.setGeofenceLongitude(Double.parseDouble(binding.longitude.getText().toString()));
        } catch (NumberFormatException e) {

        }

        try {
            w.setGeofenceRadius(Integer.parseInt(binding.radius.getText().toString()));
        } catch (NumberFormatException e) {
            w.setGeofenceRadius(null);
        }

        w.setBeaconUUID(binding.beaconUUID.getText().toString());
        try {
            w.setBeaconMinor(Integer.valueOf(binding.beaconMinor.getText().toString()));
        } catch (NumberFormatException e) {
            w.setBeaconMinor(0);
        }

        try {
            w.setBeaconMajor(Integer.valueOf(binding.beaconMajor.getText().toString()));
        } catch (NumberFormatException e) {
            w.setBeaconMajor(0);
        }

        if(!Preferences.isModePublic())
            w.setShared(shareValue);
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

    @Override
    public void handleHandlerMessage(Message msg) {

    }
}
