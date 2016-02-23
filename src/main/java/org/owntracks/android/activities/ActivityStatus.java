package org.owntracks.android.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.joanzapata.android.iconify.IconDrawable;
import com.joanzapata.android.iconify.Iconify;
import com.mikepenz.materialdrawer.Drawer;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.DrawerProvider;
import org.owntracks.android.support.StatisticsProvider;

public class ActivityStatus extends ActivityBase {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        setupSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }



    private void set() {
        ((TextView)findViewById(R.id.permissionLocation)).setText( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied");
        ((TextView)findViewById(R.id.permissionStorage)).setText( (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied");

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
        switch (item.getItemId()) {
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
