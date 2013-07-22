
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
import android.widget.TextView;
import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {
    MenuItem publish;
    TextView latitude;
    TextView longitude;
    TextView connectivity;
    TextView lastUpdate;
    TextView status;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent1 = new Intent(this, ActivityPreferences.class);
                startActivity(intent1);
                return true;
            case R.id.menu_update:
                App.getInstance().updateLocation(true);                
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    
    
    public void onEventMainThread(MqttConnectivityChanged event) {
        updateViewVisibility();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);
    }

    private void updateViewVisibility() {
        if(publish == null)
            return;
        
        if (ServiceMqtt.getConnectivity() == MQTT_CONNECTIVITY.CONNECTED) {
            publish.setEnabled(true);
        } else {
            publish.setEnabled(false);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViewVisibility();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
       publish = menu.getItem(0);
       updateViewVisibility();
        return true;
    }

    /**
     * @category START
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        longitude = (TextView) findViewById(R.id.longitude);
        latitude = (TextView) findViewById(R.id.latitude);
        EventBus.getDefault().register(this);

        App.getInstance().updateLocation(false);                

    }
    
    public void onEvent(Events.LocationUpdated e) {
        longitude.setText(e.getLocation().getLongitude()+"");
        latitude.setText(e.getLocation().getLatitude()+"");
    }

    
    public void update(View view) {
        App.getInstance().updateLocation(false);        
    }
    public void publish(View view) {
        App.getInstance().updateLocation(true);
    }
}
