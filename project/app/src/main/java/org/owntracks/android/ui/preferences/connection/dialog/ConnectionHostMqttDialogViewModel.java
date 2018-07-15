package org.owntracks.android.ui.preferences.connection.dialog;
import android.content.Intent;

import org.owntracks.android.support.Preferences;
import timber.log.Timber;

public class ConnectionHostMqttDialogViewModel extends BaseDialogViewModel {
    private String host;
    private boolean hostDirty;
    private String port;
    private boolean portDirty;
    private boolean ws;
    private boolean wsDirty;

    public ConnectionHostMqttDialogViewModel(Preferences preferences) {
        super(preferences);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void load() {
        this.host = preferences.getHost();
        this.port = preferences.getPortWithHintSupport();
        this.ws = preferences.getWs();
    }

    @Override
    public void save() {
        Timber.v("saving host:%s, port:%s, ws:%s", host, port, ws);
        if(hostDirty) {
            preferences.setHost(host);
        }

        if(portDirty) {
            try {
                preferences.setPort(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                preferences.setPortDefault();
            }
        }

        if(wsDirty) {
            preferences.setWs(ws);
        }
    }

    public String getHostText() {
        return preferences.getHost();
    }

    public void setHostText(String host) {
        this.host = host;
        this.hostDirty = true;
    }

    public String getPortText() {
        return port;
    }

    public void setPortText(String port) {
        this.port = port;
        this.portDirty = true;
    }

    public boolean isWs() {
        return ws;
    }

    public void setWs(boolean ws) {
        this.ws = ws;
        this.wsDirty = true;
    }
}

