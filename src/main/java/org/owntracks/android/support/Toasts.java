package org.owntracks.android.support;

import android.content.Context;
import android.widget.Toast;

import org.owntracks.android.App;
import org.owntracks.android.R;

public class Toasts {
    public static void showLocationNotAvailable(){
        Toast.makeText(App.getContext(), App.getContext().getString(R.string.contactLocationUnknown), Toast.LENGTH_SHORT).show();
    }
}
