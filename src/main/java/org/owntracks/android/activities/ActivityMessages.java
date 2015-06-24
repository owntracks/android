package org.owntracks.android.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.adapter.MessageAdapter;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DividerItemDecoration;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.SimpleCursorLoader;

import de.greenrobot.event.EventBus;


public class ActivityMessages extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ActivityMessages";
    public static final String CURSOR_ORDER = String.format("%s DESC", MessageDao.Properties.Tst.columnName );

    private Cursor cursor;

    private Toolbar toolbar;
    private org.owntracks.android.support.RecyclerView listView;
    private int LOADER_ID = 1;
    private MessageAdapter listAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v("ActivityMain", "ServiceProxy bound");
            }
        });

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_messages);
        toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());


        final Context context = this;
        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                if (drawerItem == null)
                    return false;

                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        goToRoot();
                        return true;
                    case R.string.idPager:
                        return true;
                    case R.string.idWaypoints:
                        Intent intent2 = new Intent(context, ActivityWaypoints.class);
                        startActivity(intent2);
                        return true;
                    case R.string.idSettings:
                        Intent intent = new Intent(context, ActivityPreferences.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            }
        };


        DrawerFactory.buildDrawer(this, toolbar, drawerListener, 1);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);

        listAdapter = new MessageAdapter(this);
        listView = (org.owntracks.android.support.RecyclerView) findViewById(R.id.listView);
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(listAdapter);
        listView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setEmptyView(findViewById(R.id.messageListPlaceholder));

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!(viewHolder instanceof MessageAdapter.ItemViewHolder)) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                Log.v(TAG, "onSwiped: " +viewHolder.getItemId() + " " + swipeDir);
                if(swipeDir == ItemTouchHelper.LEFT) {
                    Log.v(TAG, "deleting message " + ((MessageAdapter.ItemViewHolder) viewHolder).objectId);
                    App.getMessageDao().deleteByKey(((MessageAdapter.ItemViewHolder) viewHolder).objectId);
                    listAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                }


            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(listView);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_messages, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.remove:
                Log.v(TAG, "removing all messages");
                App.getMessageDao().deleteAll();
                //listAdapter.notifyItemRangeRemoved(0,listAdapter.getItemCount());
                getSupportLoaderManager().restartLoader(LOADER_ID, null, this);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
//        if (v.getId()==R.id.listView) {
//            menu.add(Menu.NONE, MENU_WAYPOINT_REMOVE, 1,getString(R.string.remove));
//        }
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item)
//    {
//        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
//        Log.v("menu", "position: " + info.position);
//        switch (item.getItemId()) {
//            case MENU_WAYPOINT_REMOVE:
//                remove( (Waypoint) this.listAdapter.getItem(info.position));
//                break;
//        }
//        return true;
//    }

    private void goToRoot() {
        Intent intent1 = new Intent(this, ActivityMain.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent1);
        finish();
    }
    @Override
    public void onBackPressed() {
        goToRoot();
    }



    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SimpleCursorLoader(this) {
            @Override
            public Cursor loadInBackground() {
                return App.getDb().query(App.getMessageDao().getTablename(), App.getMessageDao().getAllColumns(), null, null, null, null, CURSOR_ORDER);
            }
        };
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        listAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
        listAdapter.swapCursor(null);
    }
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(Events.MessageAdded e){

        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

}