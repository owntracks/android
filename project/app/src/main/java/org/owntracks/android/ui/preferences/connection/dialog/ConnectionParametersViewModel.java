package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Intent;

import org.owntracks.android.support.Preferences;

public class ConnectionParametersViewModel extends BaseDialogViewModel {

    private boolean cleanSession;
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
