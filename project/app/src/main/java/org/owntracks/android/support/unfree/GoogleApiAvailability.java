package org.owntracks.android.support.unfree;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.support.Preferences;

import timber.log.Timber;

public class GoogleApiAvailability {

    public static final int SUCCESS = 0;
    public static final int SERVICE_MISSING = 1;
    public static final int SERVICE_VERSION_UPDATE_REQUIRED = 2;
    public static final int SERVICE_DISABLED = 3;
    public static final int SERVICE_INVALID = 9;
    public static final int API_UNAVAILABLE = 16;

    public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = 0;

    protected static GoogleApiAvailability instance;
    private GoogleApiAvailabilityResponder responder;

    protected GoogleApiAvailability(){
    }

    public static GoogleApiAvailability getInstance() {
        if (instance == null) {
            try {
                Class.forName("com.google.android.gms.common.GoogleApiAvailability", false, ActivityWelcome.class.getClassLoader());
                instance = GoogleApiAvailabilityWrapper.getInstance();
            } catch(ClassNotFoundException e) {
                instance = new GoogleApiAvailability(); // untested edge case if not compiled in as binary dependency
            }
        }
        return instance;
    }

    public boolean isWrapper() {
        return false;
    }

    public int isGooglePlayServicesAvailable(Context context) {
        return SERVICE_MISSING;
    }

    public boolean isUserResolvableError(int errorCode) {
        Timber.d("%s",errorCode);
        // Always resolvable (by overriding)
        return true;
    }

    public PendingIntent getErrorResolutionPendingIntent(Context context, int errorCode, int requestCode) {
        return null;
    }

    public Dialog getErrorDialog(Activity activity, int errorCode, int requestCode) {
        return getOverrideDialog(activity, errorCode, requestCode);
    }

    public Dialog getOverrideDialog(Activity activity, int errorCode, int requestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setMessage(R.string.play_override_question);

        builder.setPositiveButton(R.string.play_override_continue, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Preferences.setPlayOverride(true);
                if(responder != null) {
                    responder.onPlayServicesAvailable();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // do nothing
            }
        });

        return builder.create();
    }

    protected void tryErrorResolution(final Activity activity, final int resultCode, final int requestCode){
        PendingIntent p = getErrorResolutionPendingIntent(activity, resultCode, requestCode);
        try {
            if(p != null) {
                p.send();
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void provisionRecoveryButton(Button button, final Activity activity, final int errorCode, final int requestCode, GoogleApiAvailabilityResponder responder) {
        getInstance().setResponder(responder);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInstance().getErrorDialog(activity, errorCode, requestCode).show();
                getInstance().tryErrorResolution(activity, errorCode, requestCode);
            }
        });
    }

    public static boolean checkPlayServices(Context context) {
        return checkPlayServices(context, false);
    }

    public static boolean checkPlayServices(Context context, boolean acceptOverride) {
        boolean playAvailable = (getInstance().isGooglePlayServicesAvailable(context) == GoogleApiAvailability.SUCCESS);
        boolean playOverride = Preferences.getPlayOverride();

        return playAvailable || (acceptOverride && playOverride);
    }

    public static boolean checkPlayServices(GoogleApiAvailabilityResponder responder) {
        boolean playAvailable = checkPlayServices(responder.getContext(), true);

        if(playAvailable) {
            responder.onPlayServicesAvailable();
        } else {
            int resultCode = getInstance().isGooglePlayServicesAvailable(responder.getContext());

            if(getInstance().isUserResolvableError(resultCode)) {
                responder.onPlayServicesUnavailableRecoverable(resultCode);
            } else {
                responder.onPlayServicesUnavailableNotRecoverable(resultCode);
            }
        }
        return playAvailable;
    }

    public void setResponder(GoogleApiAvailabilityResponder responder){
        this.responder=responder;
    }
}
