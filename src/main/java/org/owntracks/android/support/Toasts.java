package org.owntracks.android.support;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferencesConnection;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;

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
    public static void showBrokerStateChange(ServiceBroker.State state) {
        if(stateChangeToast != null)
            stateChangeToast.cancel();

        int stringRes = 0;

        if (state == ServiceBroker.State.CONNECTED) {
            stringRes = R.string.snackbarConnected;
        //} else if (state == ServiceBroker.State.CONNECTING) {
        //    stringRes = R.string.snackbarConnecting;
        } else if (state == ServiceBroker.State.DISCONNECTED || state == ServiceBroker.State.DISCONNECTED_USERDISCONNECT) {
            stringRes = R.string.snackbarDisconnected;
        } else if (state == ServiceBroker.State.DISCONNECTED_ERROR) {
            stringRes = R.string.snackbarDisconnectedError;
        } else if (state == ServiceBroker.State.DISCONNECTED_CONFIGINCOMPLETE) {
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

    public static void showContactLocationNotAvailable() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
    }
}
