package org.owntracks.android.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.StatisticsProvider;

public class ActivityStatistics extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Context context = this;
        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        goToRoot();
                        return true;
                    case R.string.idPager:
                        Intent intent1 = new Intent(context, ActivityMessages.class);
                        startActivity(intent1);
                        return true;
                    case R.string.idWaypoints:
                        Intent intent = new Intent(context, ActivityWaypoints.class);
                        startActivity(intent);
                        return true;
                    case R.string.idSettings:
                        Intent inten2 = new Intent(context, ActivityPreferences.class);
                        startActivity(inten2);
                        return true;
                    case R.string.idStatistics:
                        return true;

                }
                return false;
            }
        };

        DrawerFactory.buildDrawer(this, toolbar, drawerListener, 2);


        set();
    }


    private void goToRoot() {
        Intent intent1 = new Intent(this, ActivityMain.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent1);
        finish();
    }

    private void set() {
        ((TextView)findViewById(R.id.appStart)).setText(App.formatDate(StatisticsProvider.getTime(this, StatisticsProvider.APP_START)));
        ((TextView)findViewById(R.id.reference)).setText(App.formatDate(StatisticsProvider.getTime(this, StatisticsProvider.REFERENCE)));
        ((TextView)findViewById(R.id.serviceProxyStart)).setText(App.formatDate(StatisticsProvider.getTime(this, StatisticsProvider.SERVICE_PROXY_START)));
        ((TextView)findViewById(R.id.serviceLocatorPlay)).setText(App.formatDate(StatisticsProvider.getTime(this, StatisticsProvider.SERVICE_LOCATOR_PLAY_CONNECTED)));
        ((TextView)findViewById(R.id.serviceLocatorOnLocationChangeDate)).setText(""+StatisticsProvider.getTime(this, StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE));
        ((TextView)findViewById(R.id.serviceLocatorOnLocationChanges)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_CHANGES));
        ((TextView)findViewById(R.id.serviceBrokerPInit)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_LOCATION_PUBLISH_INIT));
        ((TextView)findViewById(R.id.serviceBrokerPDrop)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS0_DROP));
        ((TextView)findViewById(R.id.serviceBrokerPQueue)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_LOCATION_PUBLISH_INIT_QOS12_QUEUE));
        ((TextView)findViewById(R.id.serviceBrokerPSuccess)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_LOCATION_PUBLISH_SUCCESS));
        ((TextView)findViewById(R.id.serviceBrokerConnects)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_CONNECTS));
        ((TextView)findViewById(R.id.serviceBrokerQueueLength)).setText(""+StatisticsProvider.getCounter(this, StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_statistics, menu);
        menu.findItem(R.id.refresh).setIcon(
                new IconDrawable(this, Iconify.IconValue.fa_refresh)
                        .colorRes(R.color.md_white_1000)
                        .actionBarSize());


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:     // If the user hits the toolbar back arrow, go back to ActivityMain, no matter where he came from (same as hitting back)
                goToRoot();
                return true;
            case R.id.refresh:
                set();
                return true;
            case R.id.clear:     // If the user hits the toolbar back arrow, go back to ActivityMain, no matter where he came from (same as hitting back)
                StatisticsProvider.setTime(this, StatisticsProvider.REFERENCE);
                StatisticsProvider.clearCounters(this);
                set();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }


}
