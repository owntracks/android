
package st.alr.mqttitude;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.services.ServiceMqtt.MQTT_CONNECTIVITY;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Locator;
import st.alr.mqttitude.support.LocatorCallback;
import st.alr.mqttitude.support.Events.MqttConnectivityChanged;
import st.alr.mqttitude.R;
import st.alr.mqttitude.R.menu;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {
    MenuItem publish;
    TextView location;
    TextView statusLocator;
    TextView statusLastupdate;
    TextView statusServer;
    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent1 = new Intent(this, ActivityPreferences.class);
                startActivity(intent1);
                return true;
            case R.id.menu_publish:
                App.getInstance().publishLocation(true);
                return true;
            case R.id.menu_share:
            
                if (mShareActionProvider != null) {

                    
 //                   mShareActionProvider.setShareIntent(shareIntent);
                }
                Location l = App.getInstance().getLocation();
                if(l != null){
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + "," + Double.toString(l.getLongitude()));
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.shareLocation)));
        }
                    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    @Override
    protected void onStart() {
        super.onStart();

        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        
        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        return true;
    }

    /**
     * @category START
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        location = (TextView) findViewById(R.id.currentLocation);
        statusLocator = (TextView) findViewById(R.id.locatorSubtitle);
        statusLastupdate = (TextView) findViewById(R.id.lastupdateSubtitle);
        statusServer = (TextView) findViewById(R.id.brokerSubtitle);

        setLocatorStatus();
        setLastupdateStatus();
        setBrokerStatus();
        
        EventBus.getDefault().register(this);
        App.getInstance().publishLocation(false);
    }

    public void onEvent(Events.LocationUpdated e) {
        setLocation(e.getLocation());
    }
    public void onEventMainThread(Events.StateChanged e) {
        setLocatorStatus();
    }
    public void onEventMainThread(Events.PublishSuccessfull e) {
        setLastupdateStatus();
    }
    public void onEventMainThread(Events.MqttConnectivityChanged e) {
        Log.v(this.toString(), "connectivity changed");
        setBrokerStatus();
    }
    
    public void setLocation(Location l){
        
        if(l != null)
            location.setText("Lat: " + l.getLatitude() + ", Long: " + l.getLongitude());
        else 
            location.setText(getResources().getString(R.string.na));
    }
    
    public void setLocatorStatus(){
        statusLocator.setText(App.getInstance().getLocatorText());
    }
    public void setBrokerStatus() {
        statusServer.setText(ServiceMqtt.getConnectivityText());
    }
    public void setLastupdateStatus(){
        statusLastupdate.setText(App.getInstance().getLastupdateText());
    }
    
    public void publish(View view) {
        App.getInstance().publishLocation(false);
    }
}
