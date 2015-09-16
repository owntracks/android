package org.owntracks.android.support;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityPreferencesConnection;
import org.owntracks.android.services.ServiceApplication;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;

public class SnackbarFactory {

    public interface SnackbarFactoryDelegate {
        View getSnackbarTargetView();
    }
    public static Snackbar make(SnackbarFactoryDelegate a, String text, int duration) {
        return Snackbar.make(a.getSnackbarTargetView(), text, duration);
    }

    public static Snackbar make(SnackbarFactoryDelegate a, int resID, int duration) {
        return Snackbar.make(a.getSnackbarTargetView(), resID, duration);
    }

    public static void show(Snackbar s) {
        ((TextView) s.getView().findViewById(android.support.design.R.id.snackbar_text)).setTextColor(Color.WHITE);
        s.show();
    }



}

