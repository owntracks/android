package org.owntracks.android.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.navigator.ActivityNavigator;
import org.owntracks.android.ui.waypoints.AdapterCursorLoader;
import org.owntracks.android.ui.waypoints.AdapterWaypoints;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.SimpleCursorLoader;
import org.owntracks.android.support.widgets.Toasts;



public class ActivityRegions extends ActivityBase implements LoaderManager.LoaderCallbacks<Cursor>, AdapterCursorLoader.OnViewHolderClickListener<AdapterWaypoints.ItemViewHolder> {
    private static final String TAG = "ActivityRegions";
    private static final String CURSOR_ORDER = String.format("%s ASC", WaypointDao.Properties.Description.columnName );
    private Toolbar toolbar;
    private org.owntracks.android.support.widgets.RecyclerView listView;
    private final int LOADER_ID = 1;
    private AdapterWaypoints listAdapter;
    private boolean actionMode;

    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v("ActivityMain", "ServiceProxy bound");
            }
        });

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_regions);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        new ActivityNavigator(this).attachDrawer(toolbar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        listAdapter = new AdapterWaypoints(this);
        listAdapter.setOnViewHolderClickListener(this);
        listView = (org.owntracks.android.support.widgets.RecyclerView) findViewById(R.id.listView);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listAdapter);
        listView.addItemDecoration(new DividerItemDecoration(listView.getContext(), layoutManager.getOrientation()));

        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setEmptyView(findViewById(R.id.placeholder));
        listView.setHasFixedSize(false);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    private void requery() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }


    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                return App.getDao().getWaypointDao().queryBuilder().where(WaypointDao.Properties.ModeId.eq(Preferences.getModeId())).buildCursor().query();
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
        App.getEventBus().unregister(this);
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


    private void remove(long id) {

        Waypoint w = App.getDao().getWaypointDao().loadByRowId(id);
        App.getDao().getWaypointDao().delete(w);
        App.getEventBus().post(new Events.WaypointRemoved(w));
        Toasts.showWaypointRemovedToast();
        if(mActionMode != null)
            mActionMode.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoints, menu);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.WaypointAdded e) {
        requery();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.WaypointTransition e) {
        requery();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.WaypointRemoved e) {
        requery();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
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
            case R.id.exportWaypointsService:
                //Dirty hack here
                ServiceProxy.runOrBind(this, new Runnable() {
                    @Override
                    public void run() {
                        if(ServiceProxy.getServiceLocator().publishWaypointsMessage()) {
                            Toast.makeText(getApplicationContext(), R.string.preferencesExportQueued, Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(getApplicationContext(), R.string.preferencesExportFailed, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
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
        App.getEventBus().register(this);
    }


    @Override
    public void onViewHolderClick(View rootView, AdapterWaypoints.ItemViewHolder viewHolder) {
        if(mActionMode == null) {
            Intent detailIntent = new Intent(this, ActivityRegion.class);
            detailIntent.putExtra("keyId", viewHolder.getItemId());
            startActivity(detailIntent);
        } else {
            selectActionModeItem(viewHolder);
        }
    }

    @Override
    public boolean onViewHolderLongClick(View rootView, AdapterWaypoints.ItemViewHolder viewHolder) {
        if(mActionMode != null)
            return false;

        mActionMode = startSupportActionMode( modeCallBack );
        Log.v(TAG, "startSupportActionMode " + viewHolder.getItemId());

        selectActionModeItem(viewHolder);
        return true;
    }

    private void selectActionModeItem(AdapterWaypoints.ItemViewHolder viewHolder) {
        deselectAllItems();
        modeCallBack.setItemId(viewHolder.getItemId());
        viewHolder.setSelected(true);

    }


    private ActionMode mActionMode;

    private interface ActionCallback extends ActionMode.Callback {
        void setItemId(long itemId);
    }


    // Define the callback when ActionMode is activated
    private ActionCallback modeCallBack = new ActionCallback() {
        long itemId;

        @Override
        public void setItemId(long itemId) {
            this.itemId = itemId;
        }

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle("Actions");
            mode.getMenuInflater().inflate(R.menu.activity_waypoints_actionmode, menu);
            return true;
        }

        // Called each time the action mode is shown.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // Fix statusbar color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.secondary, getTheme()));
            }
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.remove:
                    remove(itemId);

                    mode.finish(); // Action picked, so close the contextual menu
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null; // Clear current action mode
            deselectAllItems();
        }
    };

    private void deselectAllItems() {
        for (int i = 0; i < listAdapter.getItemCount(); i++) {
            RecyclerView.ViewHolder holder = listView.findViewHolderForLayoutPosition(i);

            if (holder != null) {
                ((AdapterCursorLoader.ClickableViewHolder)holder).setSelected(false);
            }
        }

    }


}
