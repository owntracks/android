package org.owntracks.android.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.owntracks.android.support.unfree.GoogleApiAvailability;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.StatisticsProvider;

public class ActivityStatus extends ActivityBase {


    private TextView backendStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        setSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        backendStatus = (TextView) findViewById(R.id.backendStatus);


        set();

    }



    private void set() {
        ((TextView)findViewById(R.id.permissionLocation)).setText( (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ? "granted" : "denied");
        ((TextView)findViewById(R.id.appStart)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.APP_START)));


        ((TextView)findViewById(R.id.serviceLocatorOnLocationChangeDate)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE)));
        ((TextView)findViewById(R.id.serviceBrokerQueueLength)).setText(Integer.toString(StatisticsProvider.getInt(StatisticsProvider.SERVICE_MESSAGE_QUEUE_LENGTH)));


        ((TextView)findViewById(R.id.serviceProxyStart)).setText(App.formatDate(StatisticsProvider.getTime(StatisticsProvider.SERVICE_PROXY_START)));
    }

    private String getPlayServicesStatus() {
        String status;
        int playAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        switch (playAvailable) {
            case GoogleApiAvailability.SERVICE_MISSING:
            case GoogleApiAvailability.API_UNAVAILABLE:
                status = "Not available";
                break;

            case GoogleApiAvailability.SERVICE_VERSION_UPDATE_REQUIRED:
                status = "Update required";
                break;

            case GoogleApiAvailability.SERVICE_DISABLED:
            case GoogleApiAvailability.SERVICE_INVALID:
                status = "Inactive";
                break;

            default:
                status = "Available ("+ GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + ")";
                break;
        }

        if (GoogleApiAvailability.getInstance().isWrapper()){
            status+=" (wrapped)";
        }

        return status;
    }

    private ServiceMessage.EndpointState previousState;

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(Events.EndpointStateChanged e) {
        if (!(previousState == ServiceMessage.EndpointState.ERROR && e.getExtra() == ServiceMessage.EndpointState.DISCONNECTED)) {
            if (e.getState() == ServiceMessage.EndpointState.ERROR && e.getExtra() != null && e.getExtra() instanceof Exception)
                backendStatus.setText(String.format("%s %s: %s", App.formatDate(e.getDate()), e.getState().getLabel(this), Exception.class.cast(e.getExtra()).getCause().getMessage()));
            else
                backendStatus.setText(String.format("%s %s", App.formatDate(e.getDate()), e.getState().getLabel(this)));
        }
        previousState = e.getState();

    }

    @Override
    public void onPause() {
        App.getEventBus().unregister(this);
        super.onPause();
    }
    @Override
    public void onStart() {
        super.onStart();
        App.getEventBus().register(this);
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
