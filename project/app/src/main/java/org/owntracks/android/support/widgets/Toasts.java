package org.owntracks.android.support.widgets;

import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessor;

public class Toasts {
    public static void showUnableToCopyCertificateToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.unableToCopyCertificate), Toast.LENGTH_SHORT).show();

    }

    public static void showCopyCertificateSuccessToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.successCopyCertificate), Toast.LENGTH_SHORT).show();

    }


    private static Toast stateChangeToast;
    public static void showEndpointStateChange(MessageProcessor.EndpointState state) {
        if(stateChangeToast != null)
            stateChangeToast.cancel();

        stateChangeToast = Toast.makeText(App.getContext(), state.getLabel(App.getContext()), Toast.LENGTH_SHORT);
        stateChangeToast.show();
    }

    public static void showWaypointRemovedToast() {
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.waypointRemoved), Toast.LENGTH_SHORT).show();
    }
}
