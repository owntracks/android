package org.owntracks.android.ui.regions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiRegionsBinding;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.SimpleCursorLoader;
import org.owntracks.android.support.widgets.Toasts;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.region.RegionActivity;

import static org.owntracks.android.ui.base.navigator.Navigator.EXTRA_ARGS;


public class RegionsActivity extends BaseActivity<UiRegionsBinding, RegionsMvvm.ViewModel> implements RegionsMvvm.View, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String CURSOR_ORDER = String.format("%s ASC", WaypointDao.Properties.Description.columnName );
    private final int LOADER_ID = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        setHasEventBus(false);
        bindAndAttachContentView(R.layout.ui_regions, savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

       // listAdapter = new AdapterWaypoints(this);
        // listAdapter.setOnViewHolderClickListener(this);
        //binding.listView.setLayoutManager(layoutManager);
        //  binding.listView.setAdapter(listAdapter);
       // binding.listView.addItemDecoration(new DividerItemDecoration(this, layoutManager.getOrientation()));

        //binding.listView.setItemAnimator(new DefaultItemAnimator());
        // binding.listView.setEmptyView(findViewById(R.id.placeholder));
        //binding.listView.setHasFixedSize(false);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    @NonNull
    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ContactsLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        //    listAdapter.swapCursor(data);
    }

    public static class ContactsLoader extends SimpleCursorLoader {
        ContactsLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            return App.getDao().getWaypointDao().queryBuilder().where(WaypointDao.Properties.ModeId.eq(App.getPreferences().getModeId())).buildCursor().query();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

        //listAdapter.swapCursor(null);
    }



    private void requery() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }
    private void remove(long id) {

        Waypoint w = App.getDao().getWaypointDao().loadByRowId(id);
        App.getDao().getWaypointDao().delete(w);
        w.setDeleted(true);
        App.getEventBus().post(w);

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
    public void onEventMainThread(Waypoint e) {
        requery();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.WaypointTransition e) {
        requery();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                Intent detailIntent = new Intent(this, RegionActivity.class);
                startActivity(detailIntent);
                return true;
            case R.id.exportWaypointsService:
                MessageWaypoints m = new MessageWaypoints();
                MessageWaypointCollection waypoints = App.getPreferences().waypointsToJSON();
                if(waypoints == null)
                    return false;
                m.setWaypoints(waypoints);

                App.getMessageProcessor().sendMessage(m);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        //binding.listView.removeAllViews(); // Clear out all views to prevent zombie view causing https://github.com/owntracks/android/issues/248
        //registerForContextMenu(binding.listView);
        requery();
    }


    // @Override
    //  public void onViewHolderClick(View rootView, AdapterWaypoints.ItemViewHolder viewHolder) {
    //     if(mActionMode == null) {
    //         Intent detailIntent = new Intent(this, RegionActivity.class);
    //        detailIntent.putExtra("keyId", viewHolder.getItemId());
    //        Bundle b = new Bundle();
    //       b.putLong("keyId", viewHolder.getItemId());
    //        detailIntent.putExtra(EXTRA_ARGS, b);
    //        startActivity(detailIntent);
    //    } else {
        //        selectActionModeItem(viewHolder);
    //   }
    //}

    /*@Override
    public boolean onViewHolderLongClick(View rootView, AdapterWaypoints.ItemViewHolder viewHolder) {
        if(mActionMode != null)
            return false;

        mActionMode = startSupportActionMode( modeCallBack );

        selectActionModeItem(viewHolder);
        return true;
    }*/

    /*private void selectActionModeItem(AdapterWaypoints.ItemViewHolder viewHolder) {
        deselectAllItems();
        modeCallBack.setItemId(viewHolder.getItemId());
        viewHolder.setSelected(true);

    }*/


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
      /*  for (int i = 0; i < listAdapter.getItemCount(); i++) {
            android.support.v7.widget.RecyclerView.ViewHolder holder = binding.listView.findViewHolderForLayoutPosition(i);

            if (holder != null) {
                ((AdapterCursorLoader.ClickableViewHolder)holder).setSelected(false);
            }
        }*/
    }
}
