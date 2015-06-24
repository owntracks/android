package org.owntracks.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.monxalo.android.widget.SectionCursorAdapter;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.adapter.LoaderSectionCursorAdapter;
import org.owntracks.android.adapter.MessageAdapter;
import org.owntracks.android.db.Message;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.SimpleCursorLoader;

import de.greenrobot.event.EventBus;


public class ActivityMessages extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ActivityMessages";
    public static final String CURSOR_ORDER = String.format("%s ASC, %s DESC", MessageDao.Properties.Channel.columnName, MessageDao.Properties.Tst.columnName );

    private Cursor cursor;

    private Toolbar toolbar;
    private ListView listView;
    private int LOADER_ID = 1;
    private MessageAdapter listAdapter;
    private TextView messageListPlaceholder;

    protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v("ActivityMain", "ServiceProxy bound");
            }
        });

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pager);
        toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());


        final Context context = this;
        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                if (drawerItem == null)
                    return false;

                Log.v(TAG, "Drawer item clicked: " + drawerItem.getIdentifier());
                DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

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


        listAdapter = new MessageAdapter(this, R.layout.section, MessageDao.Properties.Channel.columnName);
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        this.messageListPlaceholder = (TextView) findViewById(R.id.placeholder);
        this.listView.setEmptyView(messageListPlaceholder);

        Log.v(TAG, "initLoader");
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_messages, menu);
    }

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
        Log.v(TAG, "onLoaderReset()");

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