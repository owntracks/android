package org.owntracks.android.support;

import android.content.Context;
import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;

public class Toasts {
    public static void showContactLocationNotAvailable(){
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
    }
    public static void showCurrentLocationNotAvailable(){
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.currentLocationNotAvailable), Toast.LENGTH_SHORT).show();
    }


    public static void showLocationPermissionNotAvailable(){
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.locationPermissionNotAvailable), Toast.LENGTH_SHORT).show();
    }

}
