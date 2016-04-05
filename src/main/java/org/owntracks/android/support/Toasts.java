package org.owntracks.android.support;

import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceMessageMqtt;

public class Toasts {
    public static void showCurrentLocationNotAvailable(){
    }


    public static void showLocationPermissionNotAvailable(){
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.locationPermissionNotAvailable), Toast.LENGTH_SHORT).show();
    }

    public static void showUnableToCopyCertificateToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.unableToCopyCertificate), Toast.LENGTH_SHORT).show();

    }

    public static void showCopyCertificateSuccessToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.successCopyCertificate), Toast.LENGTH_SHORT).show();

    }


    private static Toast stateChangeToast;
    public static void showBrokerStateChange(ServiceMessageMqtt.State state) {
        if(stateChangeToast != null)
            stateChangeToast.cancel();

        int stringRes = 0;

        if (state == ServiceMessageMqtt.State.CONNECTED) {
            stringRes = R.string.snackbarConnected;
        //} else if (state == ServiceMessageMqtt.State.CONNECTING) {
        //    stringRes = R.string.snackbarConnecting;
        } else if (state == ServiceMessageMqtt.State.DISCONNECTED || state == ServiceMessageMqtt.State.DISCONNECTED_USERDISCONNECT) {
            stringRes = R.string.snackbarDisconnected;
        } else if (state == ServiceMessageMqtt.State.DISCONNECTED_ERROR) {
            stringRes = R.string.snackbarDisconnectedError;
        } else if (state == ServiceMessageMqtt.State.DISCONNECTED_CONFIGINCOMPLETE) {
            stringRes = R.string.snackbarConfigIncomplete;
        }

        if(stringRes!=0) {
            stateChangeToast = Toast.makeText(App.getContext(), stringRes, Toast.LENGTH_SHORT);
            stateChangeToast.show();
        }
    }

    public static void showWaypointRemovedToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.waypointRemoved), Toast.LENGTH_SHORT).show();
    }
}
