package org.owntracks.android.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.adapter.WaypointAdapter;
import org.owntracks.android.db.ContactLink;
import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import java.util.ArrayList;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;


public class ActivityWaypoints extends ActionBarActivity implements StaticHandlerInterface {
    private ListView listView;
    private WaypointAdapter listAdapter;
    private WaypointDao dao;
    private Handler handler;
    private TextView waypointListPlaceholder;
    private static final int MENU_WAYPOINT_REMOVE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                //Log.v("ActivityWaypoints", "ServiceProxy bound");
            }
        });


        setContentView(R.layout.activity_waypoints);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Context context = this;
        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                if (drawerItem == null)
                    return;

                Log.v(this.toString(), "Drawer item clicked: " + drawerItem.getIdentifier());
                DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        goToRoot();
                        break;
                    case R.string.idWaypoints:
                        break;
                    case R.string.idSettings:
                        Intent intent = new Intent(context, ActivityPreferences.class);
                        startActivity(intent);
                        break;

                }
            }
        };

        DrawerFactory.buildDrawer(this, toolbar, drawerListener, 1);


        this.dao = App.getWaypointDao();
        this.handler = new StaticHandler(this);
        this.listAdapter = new WaypointAdapter(this, new ArrayList<>(this.dao.loadAll()));

        this.listView = (ListView) findViewById(R.id.waypoints);
        this.listView.setAdapter(this.listAdapter);

        this.waypointListPlaceholder = (TextView) findViewById(R.id.waypointListPlaceholder);
        this.listView.setEmptyView(waypointListPlaceholder);

        this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listAdapter.getItemViewType(position) == listAdapter.ROW_TYPE_HEADER)
                    return;

                Intent detailIntent = new Intent(context, ActivityWaypoint.class);
                detailIntent.putExtra("keyId", Long.toString(((Waypoint) listAdapter.getItem(position)).getId()));

                startActivity(detailIntent);
            }
        });
    }


    private void requestWaypointGeocoder(Waypoint w, boolean force) {
        if (w.getGeocoder() == null || force) {

            GeocodableLocation l = new GeocodableLocation("Waypoint");
            l.setLatitude(w.getLatitude());
            l.setLongitude(w.getLongitude());
            l.setExtra(w);
            (new ReverseGeocodingTask(this, handler)).execute(l);
        }


    }

    public void handleHandlerMessage(Message msg) {
        if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && ((GeocodableLocation)msg.obj).getExtra() instanceof  Waypoint) {

            // Gets the geocoder from the returned array of [Geocoder, Waypoint] and assigns the geocoder to the waypoint
            // The Geocoder will not change unless the location of the waypoint is updated. Therefore it is stored in the DAO and only overwritten when the waypoint is updated by the user
            Waypoint w = (Waypoint) ((GeocodableLocation)msg.obj).getExtra();
            w.setGeocoder(((GeocodableLocation)msg.obj).getGeocoder());
            this.dao.update(w);
            this.listAdapter.updateItem(w);
            this.listAdapter.notifyDataSetChanged();
        }
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

    protected void remove(Waypoint w) {
        this.dao.delete(w);
        Log.v(this.toString(), "Waypoint " +w + " removed ");
        EventBus.getDefault().post(new Events.WaypointRemoved(w));
    }

    public WaypointAdapter getListAdapter() {
        return this.listAdapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoints, menu);
        return true;
    }

    private void goToRoot() {
        Intent intent1 = new Intent(this, ActivityMain.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent1);
        finish();
    }





//    // These get posted by ActivityWaypoint with a sticky flag, so they're received when this activity
//    // gains focus to update the UI
//    // WaypointAdded and WaypointUpdated are not posted sticky and only handled by ServiceLocator to do the acutal work
//    public void onEventMainThread(Events.WaypointAddedByUser e) {
//        EventBus.getDefault().removeStickyEvent(e);
//        this.listAdapter.addItem(e.getWaypoint());
//        this.listAdapter.notifyDataSetChanged();
//        requestWaypointGeocoder(e.getWaypoint(), true); // Resolve Geocoder for Waypoint coordinates and overwrite exisitng Geocoder
//    }
//    public void onEventMainThread(Events.WaypointUpdatedByUser e) {
//        EventBus.getDefault().removeStickyEvent(e);
//        this.listAdapter.updateItem(e.getWaypoint());
//        this.listAdapter.notifyDataSetChanged();
//        requestWaypointGeocoder(e.getWaypoint(), true); // Resolve Geocoder for Waypoint coordinates and overwrite exisitng Geocoder
//    }

    public void onEventMainThread(Events.WaypointAdded e) {
        this.listAdapter.addItem(e.getWaypoint());
        this.listAdapter.notifyDataSetChanged();
    }
    public void onEventMainThread(Events.WaypointRemoved e) {
        this.listAdapter.removeItem(e.getWaypoint());
        this.listAdapter.notifyDataSetChanged();

    }
    public void onEventMainThread(Events.WaypointUpdated e) {
        this.listAdapter.updateItem(e.getWaypoint());
        this.listAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent detailIntent = new Intent(this, ActivityWaypoint.class);
                startActivity(detailIntent);
                return true;
      default:
                return super.onOptionsItemSelected(item);
        }

    }

    // If the user hits back, go back to ActivityMain, no matter where he came from
    @Override
    public void onBackPressed() {
        goToRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        listAdapter.clear();
        listAdapter.set(new ArrayList<>(this.dao.queryBuilder().where(WaypointDao.Properties.ModeId.eq(Preferences.getModeId())).build().list()));

        registerForContextMenu(this.listView);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        unregisterForContextMenu(this.listView);
        EventBus.getDefault().unregister(this);
        super.onPause();

    }

        @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.waypoints) {
            menu.add(Menu.NONE, MENU_WAYPOINT_REMOVE, 1,getString(R.string.waypointRemove));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.v("menu", "position: " + info.position);
        switch (item.getItemId()) {
            case MENU_WAYPOINT_REMOVE:
                remove( (Waypoint) this.listAdapter.getItem(info.position));
                break;
        }
        return true;
    }


}
