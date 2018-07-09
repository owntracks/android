package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.Toasts;
import org.owntracks.android.ui.base.navigator.Navigator;

import java.io.FileOutputStream;
import java.io.InputStream;

import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class ConnectionParametersViewModel extends BaseDialogViewModel {

    boolean cleanSession;
    private String keepaliveText;
    private boolean keepaliveTextDirty;

    public ConnectionParametersViewModel(Preferences preferences) {
        super(preferences);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void load() {
        this.cleanSession = preferences.getCleanSession();
        this.keepaliveText = preferences.getKeepaliveWithHintSupport();
    }

    @Override
    public void save() {
        preferences.setCleanSession(cleanSession);

        if(keepaliveTextDirty) {
            try {
                preferences.setKeepalive(Integer.parseInt(keepaliveText));
            } catch (NumberFormatException e) {
                preferences.setKeepaliveDefault();
            }
        }
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public String getKeepaliveText() {
        return keepaliveText;
    }

    public void setKeepaliveText(String keepaliveText) {
        this.keepaliveText = keepaliveText;
        this.keepaliveTextDirty = true;
    }
}

