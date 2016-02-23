package org.owntracks.android.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mikepenz.materialdrawer.Drawer;

import org.owntracks.android.R;
import org.owntracks.android.adapter.AdapterCursorLoader;
import org.owntracks.android.adapter.AdapterWaypoints;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DividerItemDecoration;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.SimpleCursorLoader;

import de.greenrobot.event.EventBus;


public class ActivityRegions extends ActivityBase implements LoaderManager.LoaderCallbacks<Cursor>, AdapterCursorLoader.OnViewHolderClickListener<AdapterWaypoints.ItemViewHolder> {
    private static final String TAG = "ActivityRegions";
    public static final String CURSOR_ORDER = String.format("%s ASC", WaypointDao.Properties.Description.columnName );
    private Toolbar toolbar;
    private org.owntracks.android.support.RecyclerView listView;
    private int LOADER_ID = 1;
    private AdapterWaypoints listAdapter;
    private Drawer drawer;

    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v("ActivityMain", "ServiceProxy bound");
            }
        });

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_waypoints);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        drawer = DrawerProvider.buildDrawer(this, toolbar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        //layoutManager.scrollToPosition(0);

        listAdapter = new AdapterWaypoints(this);
        listAdapter.setOnViewHolderClickListener(this);
        listView = (org.owntracks.android.support.RecyclerView) findViewById(R.id.listView);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listAdapter);
        listView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setEmptyView(findViewById(R.id.placeholder));
        listView.setHasFixedSize(false);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!(viewHolder instanceof AdapterWaypoints.ItemViewHolder)) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                remove(viewHolder.getItemId());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listView);

    }

    public void requery() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                return Dao.getDb().query(Dao.getWaypointDao().getTablename(), Dao.getWaypointDao().getAllColumns(), null, null, null, null, CURSOR_ORDER);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        listAdapter.swapCursor(data);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        listAdapter.swapCursor(null);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.closeServiceConnection();

            }
        });
        super.onDestroy();
    }


    protected void remove(long id) {
        Waypoint w = Dao.getWaypointDao().loadByRowId(id);
        Dao.getWaypointDao().delete(w);
        EventBus.getDefault().post(new Events.WaypointRemoved(w));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoints, menu);
        return true;
    }


    public void onEventMainThread(Events.WaypointAdded e) {
        requery();
    }

    public void onEventMainThread(Events.WaypointTransition e) {
        requery();
    }

    public void onEventMainThread(Events.WaypointRemoved e) {
        requery();
    }
    public void onEventMainThread(Events.WaypointUpdated e) {
        requery();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent detailIntent = new Intent(this, ActivityRegion.class);
                startActivity(detailIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        this.listView.removeAllViews(); // Clear out all views to prevent zombie view causing https://github.com/owntracks/android/issues/248
        registerForContextMenu(this.listView);
        requery();
        EventBus.getDefault().register(this);
    }


    @Override
    public void onViewHolderClick(View rootView, AdapterWaypoints.ItemViewHolder viewHolder) {

        Intent detailIntent = new Intent(this, ActivityRegion.class);
        detailIntent.putExtra("keyId", viewHolder.getItemId());
        startActivity(detailIntent);

    }
}
