package org.owntracks.android.support;

import android.app.Service;
import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceMessage;
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
    public static void showEndpointStateChange(ServiceMessage.EndpointState state) {
        if(stateChangeToast != null)
            stateChangeToast.cancel();

        int stringRes = 0;

        if (state == ServiceMessage.EndpointState.CONNECTED) {
            stringRes = R.string.snackbarConnected;
        //} else if (state == ServiceMessageMqtt.EndpointState.CONNECTING) {
        //    stringRes = R.string.snackbarConnecting;
        } else if (state == ServiceMessage.EndpointState.DISCONNECTED || state == ServiceMessage.EndpointState.DISCONNECTED_USERDISCONNECT ) {
            stringRes = R.string.snackbarDisconnected;
        } else if (state == ServiceMessage.EndpointState.DISCONNECTED_ERROR) {
            stringRes = R.string.snackbarDisconnectedError;
        } else if (state == ServiceMessage.EndpointState.DISCONNECTED_CONFIGINCOMPLETE) {
            stringRes = R.string.snackbarConfigIncomplete;
        } else if (state == ServiceMessage.EndpointState.DISCONNECTED_DATADISABLED) {
            stringRes = R.string.connectivityDisconnectedDataDisabled;
        }

        if(stringRes!=0) {
            stateChangeToast = Toast.makeText(App.getContext(), stringRes, Toast.LENGTH_SHORT);
            stateChangeToast.show();
        }
    }

    public static void showWaypointRemovedToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.waypointRemoved), Toast.LENGTH_SHORT).show();
    }

    public static void showContactLocationNotAvailable() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
    }
}
