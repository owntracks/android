package org.owntracks.android.wrapper;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;

import timber.log.Timber;

public class GoogleApiAvailabilityWrapper extends GoogleApiAvailability {
    public static int GOOGLE_PLAY_SERVICES_VERSION_CODE;

    private static com.google.android.gms.common.GoogleApiAvailability wrappedInstance;

    protected GoogleApiAvailabilityWrapper() {
    }

    public static GoogleApiAvailabilityWrapper getInstance() {
        if (instance == null) {
            instance = new GoogleApiAvailabilityWrapper();
            wrappedInstance = com.google.android.gms.common.GoogleApiAvailability.getInstance();
            GOOGLE_PLAY_SERVICES_VERSION_CODE = com.google.android.gms.common.GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;
        }
        return (GoogleApiAvailabilityWrapper) instance;
    }

    @Override
    public boolean isWrapper() {
        return true;
    }

    @Override
    public int isGooglePlayServicesAvailable(Context context) {
        if(wrappedInstance.getOpenSourceSoftwareLicenseInfo(context)==null){
            //this way, we don't get a Warning in the Logs GooglePlayServicesUtil: Cannot find Google Play services package name.
            return SERVICE_MISSING;
        } else {
            return wrappedInstance.isGooglePlayServicesAvailable(context);
        }
    }

    @Override
    public final boolean isUserResolvableError(int errorCode) {
        Timber.d("error code is %s", errorCode);

        boolean resolvable = wrappedInstance.isUserResolvableError(errorCode);

        if (!resolvable) {
            resolvable = super.isUserResolvableError(errorCode);
        }
        return resolvable;
    }

    @Override
    public PendingIntent getErrorResolutionPendingIntent(Context context, int errorCode, int requestCode) {
        return wrappedInstance.getErrorResolutionPendingIntent(context, errorCode, requestCode);
    }

    @Override
    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode) {
        final Dialog errorDialog;
        final Dialog overrideDialog = super.getErrorDialog(activity, errorCode, requestCode);

        if (errorCode == SERVICE_MISSING || errorCode == GoogleApiAvailability.SERVICE_INVALID || errorCode == API_UNAVAILABLE) { // usual case, if no play services installed
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
}
