package org.owntracks.android.support;

import android.content.Context;
import android.location.Address;

import org.owntracks.android.App;
import org.owntracks.android.injection.qualifier.AppContext;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class GeocoderGoogle implements Geocoder {
    private static android.location.Geocoder geocoder;


    @Override
    public String reverse(double latitude, double longitude) {
        if(!android.location.Geocoder.isPresent()) {
            Timber.e("geocoder is not present");
            return null;
        }

        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if ((addresses != null) && (addresses.size() > 0)) {
                StringBuilder g = new StringBuilder();
                Address a = addresses.get(0);
                if (a.getAddressLine(0) != null)
                    g.append(a.getAddressLine(0));
                return g.toString();
            } else {
                return "not available";
            }
        } catch (IOException e) {
            return null;
        }
    }

    GeocoderGoogle(@AppContext Context context){
        geocoder = new android.location.Geocoder(context, Locale.getDefault());
    }
}
