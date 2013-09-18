
package st.alr.mqttitude;

import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import de.greenrobot.event.EventBus;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class ActivityStatus extends Activity {
    private TextView locatorStatus;
    private TextView locatorCurLatLon;
    private TextView locatorCurAccuracy;
    private TextView locatorCurLatLonTime;

    private TextView locatorLastPubLatLon;
    private TextView locatorLastPubAccuracy;
    private TextView locatorLastPubLatLonTime;

    private TextView brokerStatus;
    private TextView brokerError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        locatorStatus = (TextView) findViewById(R.id.locatorStatus);
        locatorCurLatLon = (TextView) findViewById(R.id.locatorCurLatLon);
        locatorCurAccuracy = (TextView) findViewById(R.id.locatorCurAccuracy);
        locatorCurLatLonTime = (TextView) findViewById(R.id.locatorCurLatLonTime);

        locatorLastPubLatLon = (TextView) findViewById(R.id.locatorLastPubLatLon);
        locatorLastPubAccuracy = (TextView) findViewById(R.id.locatorLastPubAccuracy);
        locatorLastPubLatLonTime = (TextView) findViewById(R.id.locatorLastPubLatLonTime);

        brokerStatus = (TextView) findViewById(R.id.brokerStatus);
        brokerError = (TextView) findViewById(R.id.brokerError);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_status, menu);
        return true;
    }

    public void onEventMainThread(Events.LocationUpdated e) {
        locatorCurLatLon.setText(e.getGeocodableLocation().toLatLonString());
        locatorCurAccuracy.setText("±" + e.getGeocodableLocation().getLocation().getAccuracy()+"m");
        locatorCurLatLonTime.setText(App.getInstance().formatDate(e.getDate()));
    }

    
    
    public void onEventMainThread(Events.PublishSuccessfull e) {
        if(e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
            GeocodableLocation l = (GeocodableLocation)e.getExtra();
            locatorLastPubLatLon.setText(l.toLatLonString());
            locatorLastPubAccuracy.setText("±" + l.getLocation().getAccuracy()+"m");
            locatorLastPubLatLonTime.setText(App.getInstance().formatDate(e.getDate()));            
        }
    }

    public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
       locatorStatus.setText(Defaults.State.toString(e.getState()));
    }

    public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
        brokerStatus.setText(Defaults.State.toString(e.getState()));
        if(e.getExtra() != null && e.getExtra() instanceof Exception && e.getExtra().getClass() != null) {
            brokerError.setText( ((Exception)e.getExtra()).getCause().getLocalizedMessage());
        } else {
            brokerError.setText(getString(R.string.na));
        }
    }
    

}
