package org.owntracks.android.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.StatisticsProvider;

public class ActivityStatus extends ActivityBase {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setupSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        set();

    }



    private void set() {
        ((TextView)findViewById(R.id.permissionLocation)).setText( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied");
        ((TextView)findViewById(R.id.appStart)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.APP_START)));
        ((TextView)findViewById(R.id.appStart)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.APP_START)));
        ((TextView)findViewById(R.id.backendStatus)).setText(String.format("%s %s", App.formatDate(StatisticsProvider.getTime(StatisticsProvider.BACKEND_LAST_MESSAGE_TST)), StatisticsProvider.getString(StatisticsProvider.BACKEND_LAST_MESSAGE)));


        ((TextView)findViewById(R.id.serviceLocatorOnLocationChangeDate)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE)));
        ((TextView)findViewById(R.id.serviceBrokerQueueLength)).setText(Integer.toString(StatisticsProvider.getInt(StatisticsProvider.SERVICE_BROKER_QUEUE_LENGTH)));


        ((TextView)findViewById(R.id.serviceProxyStart)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.SERVICE_PROXY_START)));
        ((TextView)findViewById(R.id.serviceLocatorPlay)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.SERVICE_LOCATOR_PLAY_CONNECTED)));
        ((TextView)findViewById(R.id.playServicesStatus)).setText(getPlayServicesStatus());


    }

    private String getPlayServicesStatus() {
        String status;
        int playAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        switch (playAvailable) {
            case ConnectionResult.SERVICE_MISSING:
                status = "Not available";
                break;

            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                status = "Update required";
                break;

            case ConnectionResult.SERVICE_DISABLED:
            case ConnectionResult.SERVICE_INVALID:
                status = "Inactive";
                break;

            default:
                status = "Available ("+ GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + ")";
                break;
        }

        return status;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                set();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }




}
