package org.owntracks.android.wrapper;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;

import timber.log.Timber;

public class GoogleApiAvailabilityWrapper extends GoogleApiAvailability {
    private static com.google.android.gms.common.GoogleApiAvailability wrappedInstance;

    @Override
    public int isGooglePlayServicesAvailable(Context context) {
        return wrappedInstance.isGooglePlayServicesAvailable(context);
    }

    public static GoogleApiAvailabilityWrapper getInstance() {
        if (instance == null) {
            instance = new GoogleApiAvailabilityWrapper();
            wrappedInstance = com.google.android.gms.common.GoogleApiAvailability.getInstance();
        }
        return (GoogleApiAvailabilityWrapper) instance;
    }

    @Override
    public final boolean isUserResolvableError(int errorCode) {
        Timber.d("%s", errorCode);

        boolean resolvable=wrappedInstance.isUserResolvableError(errorCode);

        if(!resolvable){
            resolvable=super.isUserResolvableError(errorCode);
        }
        return resolvable;
    }

    @Override
    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode) {
        final Dialog errorDialog;
        final Dialog overrideDialog = GoogleApiAvailabilityWrapper.super.getErrorDialog(activity, errorCode, requestCode);

        if (errorCode == GoogleApiAvailability.SERVICE_INVALID){ // usual case, if no play services installed
            errorDialog = overrideDialog;
        } else {
            errorDialog = wrappedInstance.getErrorDialog(activity, errorCode, requestCode, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) { // untested edge case (if choosing cancel on offered fix)
                            overrideDialog.show();
                        }
                    }
            );
        }

        return errorDialog;
    }

    @Override
    public PendingIntent getErrorResolutionPendingIntent(Context context, int errorCode, int requestCode) {
        return wrappedInstance.getErrorResolutionPendingIntent(context, errorCode, requestCode);
    }

    @Override
    public boolean isWrapper(){
        return true;
    }
}
