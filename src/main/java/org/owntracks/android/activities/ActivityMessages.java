package org.owntracks.android.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.github.monxalo.android.widget.SectionCursorAdapter;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.adapter.MessageAdapter;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;

import de.greenrobot.event.EventBus;


public class ActivityMessages extends AppCompatActivity {
    private static final String TAG = "ActivityMessages";
    private Cursor cursor;

    private Toolbar toolbar;
    private ListView listView;

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


        listView = (ListView) findViewById(R.id.listView);

        String tstCol = MessageDao.Properties.Tst.columnName;
        String channelCol = MessageDao.Properties.Channel.columnName;


        String orderBy = channelCol + " ASC, " + tstCol + " DESC";

        cursor = App.getDb().query(App.getMessageDao().getTablename(), App.getMessageDao().getAllColumns(), null, null, null, null, orderBy);
        String[] from = { MessageDao.Properties.Title.columnName, MessageDao.Properties.Channel.columnName };
        int[] to = { R.id.title, R.id.subtitle };
        SectionCursorAdapter adapter = new MessageAdapter(this, cursor, R.layout.section, cursor.getColumnIndex(MessageDao.Properties.Channel.columnName));

        listView.setAdapter(adapter);

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


    public void onEvent(Events.MessageAdded e) {
    }
}