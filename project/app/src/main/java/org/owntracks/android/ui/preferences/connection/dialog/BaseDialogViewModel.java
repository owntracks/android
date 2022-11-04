package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.DialogInterface;
import android.content.Intent;
import androidx.databinding.BaseObservable;

import org.owntracks.android.preferences.Preferences;

public abstract class BaseDialogViewModel extends BaseObservable implements DialogInterface.OnClickListener {
    final Preferences preferences;
    BaseDialogViewModel(Preferences preferences) {
        this.preferences = preferences;
        load();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            save();
        } else if(which == DialogInterface.BUTTON_NEGATIVE) {
            dialog.cancel();
        }


    }

    public abstract void onActivityResult(int requestCode, int resultCode, Intent data);
    abstract void load();
    abstract void save();
}
