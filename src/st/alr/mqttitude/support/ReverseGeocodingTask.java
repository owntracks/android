package st.alr.mqttitude.support;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

// AsyncTask encapsulating the reverse-geocoding API
public class ReverseGeocodingTask extends AsyncTask<GeocodableLocation, Void, Void> {
    
    Context mContext;
    Handler mHandler;
    public static final int GEOCODER_RESULT = 3452;
    public static final int GEOCODER_NORESULT = 3453;

    public ReverseGeocodingTask(Context context, Handler handler) {
        super();
        mContext = context;
        mHandler = handler;
    }

    @Override
    protected Void doInBackground(GeocodableLocation... params) {
        Log.v(this.toString(), "Doing reverse Geocoder lookup of location");
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        GeocodableLocation l = (GeocodableLocation)params[0];
        int r = GEOCODER_NORESULT;
        
        // Return right away if there is already geocoder information available
        if(l.getGeocoder() == null) {             
            try {
                List<Address> addresses = geocoder.getFromLocation(l.getLocation().getLatitude(), l.getLocation().getLongitude(), 1);
                Log.v(this.toString(), "Geocoder result: " + addresses.get(0).getAddressLine(0));
                if (addresses != null && addresses.size() > 0) {            
                    l.setGeocoder(addresses.get(0).getAddressLine(0));
                    r = GEOCODER_RESULT;
                }
            } catch (IOException e) {
                Log.v(this.toString(), "Geocoder returned no result");
                r = GEOCODER_NORESULT;
            }
        } else {
            Log.v(this.toString(), "Geocoder already has result: " + l.getGeocoder());
            r = GEOCODER_RESULT;
        }
       Message.obtain(mHandler, r, l).sendToTarget();

       return null;
    }
    

}