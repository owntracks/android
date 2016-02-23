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

    public static void showStoragePemissionDenied() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.storageAccessDenied), Toast.LENGTH_SHORT).show();
    }

    public static void showUnableToCopyCertificateToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.unableToCopyCertificate), Toast.LENGTH_SHORT).show();

    }

    public static void showCopyCertificateSuccessToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.successCopyCertificate), Toast.LENGTH_SHORT).show();

    }
}
