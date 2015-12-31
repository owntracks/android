package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;
import com.google.android.gms.maps.model.LatLngBounds;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import de.greenrobot.dao.DaoException;
import de.greenrobot.dao.query.Query;
import de.greenrobot.event.EventBus;


public class ActivityWaypoint extends ActivityBase implements StaticHandlerInterface {
    private static final String TAG = "ActivityWaypoint";

    private static final int REQUEST_PLACE_PICKER = 19283;
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

    private EditText description;
    private EditText longitude;
    private EditText latitude;
    private EditText radius;
    private EditText ssid;

    private Switch share;
    private MenuItem saveButton;

    // Thanks Google for not providing a getter for the value of switches.
    private boolean shareValue = false;
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


        requiredForSave = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {  conditionallyEnableSaveButton(); }
        };

        setContentView(R.layout.activity_waypoint);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        this.description = (EditText) findViewById(R.id.description);
        this.latitude = (EditText) findViewById(R.id.latitude);
        this.longitude = (EditText) findViewById(R.id.longitude);
        this.radius = (EditText) findViewById(R.id.radius);
        this.ssid = (EditText) findViewById(R.id.ssid);


        this.share = (Switch) findViewById(R.id.share);
        this.share.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shareValue = isChecked;
            }
        });


        handler = new StaticHandler(this);
        this.dao = Dao.getWaypointDao();


        Bundle extras = getIntent().getExtras();
        ;


        if(getIntent().getExtras() == null || (this.waypoint = this.dao.loadByRowId(extras.getLong("keyId"))) == null) {
            this.waypoint = null;
        } else {
            this.description.setText(this.waypoint.getDescription());
            this.latitude.setText(this.waypoint.getGeofenceLatitude().toString());
            this.longitude.setText(this.waypoint.getGeofenceLongitude().toString());

            if (this.waypoint.getGeofenceRadius() != null && this.waypoint.getGeofenceRadius() > 0) {
                this.radius.setText(this.waypoint.getGeofenceRadius().toString());
            }

            this.ssid.setText(this.waypoint.getWifiSSID());

            // Shared waypoints are disabled in public mode to protect user's privacy
            findViewById(R.id.shareWrapper).setVisibility(Preferences.isModePublic() ? View.GONE : View.VISIBLE);

            this.share.setChecked(this.waypoint.getShared());

        }
    }
    private void conditionallyEnableSaveButton() {

        boolean enabled = false;
        try {
            enabled = (this.description.getText().toString().length() > 0)
                    && (this.latitude.getText().toString().length() > 0)
                    && (this.longitude.getText().toString().length() > 0);
        } catch (Exception e) {
            enabled = false; // invalid input or NumberFormatException result in no valid input
        }
        Log.v(TAG, "conditionallyEnableSaveButton: " +enabled);
        saveButton.setEnabled(enabled);
        saveButton.getIcon().setAlpha(enabled ? 255 : 130);

    }


    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null); // disable handler
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

        // Setup change listener that change enabled state of save button
        this.description.addTextChangedListener(requiredForSave);
        this.latitude.addTextChangedListener(requiredForSave);
        this.longitude.addTextChangedListener(requiredForSave);
        this.radius.addTextChangedListener(requiredForSave);

        // Check if we should initially enable or disable save button based on values set in onCreate
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
                useCurrentLocation();
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

            latitude.setText("" + place.getLatLng().latitude);
            longitude.setText(""+place.getLatLng().longitude);

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

    private void useCurrentLocation() {
        final Context c = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Location l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                if(l != null) {
                    ((ActivityWaypoint)c).latitude.setText(Double.toString((l.getLatitude())));
                    ((ActivityWaypoint)c).longitude.setText(Double.toString((l.getLongitude())));
                }
            }
        });
    }

    private void save() {
        Waypoint w;


        boolean update;
        if (this.waypoint == null) {
            w = new Waypoint();
            w.setModeId(Preferences.getModeId());
            update = false;
        } else {
            w = this.waypoint;
            update = true;
        }

        w.setDescription(this.description.getText().toString());
        try {
            w.setGeofenceLatitude(Double.parseDouble(this.latitude.getText().toString()));
            w.setGeofenceLongitude(Double.parseDouble(this.longitude.getText().toString()));
        } catch (NumberFormatException e) {
        }

        try {
            w.setGeofenceRadius(Integer.parseInt(this.radius.getText().toString()));
        } catch (NumberFormatException e) {
            w.setGeofenceRadius(null);
        }

        try {
            w.setWifiSSID(this.ssid.getText().toString());
        } catch (NumberFormatException e) {
        }

        if(!Preferences.isModePublic())
            w.setShared(shareValue);
        else
            w.setShared(false);


        if (update)
            update(w);
        else {
            w.setDate(new java.util.Date());
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
