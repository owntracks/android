package org.owntracks.android;

import android.content.Context;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.adapter.WaypointAdapter;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import java.sql.Date;
import java.util.ArrayList;

import de.greenrobot.dao.DaoException;
import de.greenrobot.dao.query.Query;
import de.greenrobot.event.EventBus;


public class ActivityWaypoint extends ActionBarActivity implements StaticHandlerInterface {
    private WaypointDao dao;
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
    private LinearLayout geofenceSettings;
    private Switch enter;
    private Switch leave;
    private Switch share;
    private MenuItem saveButton;

    // Thanks Google for not providing a getter for the value of switches.
    private boolean enterValue = false;
    private boolean leaveValue = false;
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


        TextWatcher requiredForSave = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {  conditionallyEnableSaveButton(); }
        };

        TextWatcher requiredForGeofence = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start,  int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start,  int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                conditionallyShowGeofenceSettings();
            }
        };

        setContentView(R.layout.activity_waypoint);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        this.description = (EditText) findViewById(R.id.description);
        this.description.addTextChangedListener(requiredForSave);
        this.latitude = (EditText) findViewById(R.id.latitude);
        this.latitude.addTextChangedListener(requiredForSave);
        this.longitude = (EditText) findViewById(R.id.longitude);
        this.longitude.addTextChangedListener(requiredForSave);
        this.radius = (EditText) findViewById(R.id.radius);
        this.radius.addTextChangedListener(requiredForGeofence);

        this.geofenceSettings = (LinearLayout) findViewById(R.id.waypointGeofenceSettings);
        this.enter = (Switch) findViewById(R.id.enter);
        this.enter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enterValue = isChecked;

            }
        });
        this.leave = (Switch) findViewById(R.id.leave);
        this.leave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                leaveValue = isChecked;
            }
        });
        this.share = (Switch) findViewById(R.id.share);
        this.share.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shareValue = isChecked;
            }
        });


        handler = new StaticHandler(this);
        this.dao = App.getWaypointDao();






        Bundle extras = getIntent().getExtras();
        if(extras == null || extras.getString("keyId") == null) {
            this.waypoint = new Waypoint();
        } else {
            Query query = this.dao.queryBuilder().where( WaypointDao.Properties.Id.eq(extras.getString("keyId"))).build();
            try {
                this.waypoint = (Waypoint) query.unique();

                this.description.setText(this.waypoint.getDescription());
                this.latitude.setText(this.waypoint.getLatitude().toString());
                this.longitude.setText(this.waypoint.getLongitude().toString());

                if (this.waypoint.getRadius() != null && this.waypoint.getRadius() > 0) {
                    this.radius.setText(this.waypoint.getRadius().toString());

                    switch (this.waypoint.getTransitionType()) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            this.enter.setChecked(true);
                            this.enterValue = true;
                            this.leave.setChecked(false);
                            this.leaveValue = false;
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            this.enter.setChecked(false);
                            this.enterValue = false;
                            this.leave.setChecked(true);
                            this.leaveValue = true;
                            break;
                        default:
                            this.enter.setChecked(true);
                            this.enterValue = true;
                            this.leave.setChecked(true);
                            this.leaveValue = true;
                            break;
                    }

                } else {
                    this.geofenceSettings.setVisibility(View.GONE);
                    this.enter.setChecked(false);
                    this.leave.setChecked(false);
                }

                this.share.setChecked(this.waypoint.getShared());

            } catch(DaoException e) { // No result found or id not unique (both shouldn't happen)
                finish();
                return;
            }

            conditionallyShowGeofenceSettings();
            conditionallyEnableSaveButton();
        }




    }
    private void conditionallyEnableSaveButton() {


        if ((this.description.getText().toString().length() > 0)
                && (this.latitude.getText().toString().length() > 0)
                && (this.longitude.getText().toString().length() > 0)
                && ((this.radius.getText().toString().length() > 0) && (Float.parseFloat(this.latitude.getText().toString()) > 0)) && (enterValue || leaveValue)
           )
            saveButton.setEnabled(true);
        else
            saveButton.setEnabled(false);
    }

    private void conditionallyShowGeofenceSettings() {
        boolean visible;
        try {
            visible = (this.radius.getText().toString().length() > 0) && (Float.parseFloat(this.radius.getText().toString()) > 0);
        } catch (Exception e) {
            visible = false;
        }

        this.geofenceSettings.setVisibility(visible ? View.VISIBLE : View.GONE);

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
        this.dao.insert(w);
        EventBus.getDefault().post(new Events.WaypointAdded(w));
    }

    protected void update(Waypoint w) {
        this.dao.update(w);
        EventBus.getDefault().post(new Events.WaypointUpdated(w));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoint, menu);
        this.saveButton = menu.findItem(R.id.save);
        this.saveButton.setEnabled(this.waypoint != null);
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void useCurrentLocation() {
        final Context c = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
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
            update = true;
        } else {
            w = this.waypoint;
            update = false;
        }

        w.setDescription(this.description.getText().toString());
        try {
            w.setLatitude(Double.parseDouble(this.latitude.getText().toString()));
            w.setLongitude(Double.parseDouble(this.longitude.getText().toString()));
        } catch (NumberFormatException e) {
        }

        try {
            w.setRadius(Float.parseFloat(this.radius.getText().toString()));
        } catch (NumberFormatException e) {
            w.setRadius(null);
        }

        w.setShared(shareValue);

        if(this.enterValue && !this.leaveValue) {
            w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER);
        } else if(!this.enterValue && this.leaveValue) {
            w.setTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);
        } else if(this.enterValue && this.leaveValue) {
            w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
        } else {

        }
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
