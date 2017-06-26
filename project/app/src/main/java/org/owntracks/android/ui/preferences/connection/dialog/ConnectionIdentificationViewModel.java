package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Intent;

import org.owntracks.android.support.Preferences;

import timber.log.Timber;

public class ConnectionIdentificationViewModel extends BaseDialogViewModel {
    boolean authentication;
    private String username;
    private boolean usernameDirty;
    private String password;
    private boolean passwordDirty;
    private String deviceId;
    private boolean deviceIdDirty;
    private String trackerId;
    private boolean trackerIdDirty;

    public ConnectionIdentificationViewModel(Preferences preferences) {
        super(preferences);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void load() {
        this.authentication = preferences.getAuth();
        this.username = preferences.getUsername();
        this.password = preferences.getPassword();
        this.deviceId = preferences.getDeviceId(false);
        this.trackerId = preferences.getTrackerId(false);
    }

    @Override
    public void save() {
        preferences.setAuth(authentication);

        if(usernameDirty)
            preferences.setUsername(username);

        if(passwordDirty)
            preferences.setPassword(password);

        if(deviceIdDirty)
            preferences.setDeviceId(deviceId);

        if(trackerIdDirty)
            preferences.setTrackerId(trackerId);
    }

    public boolean isAuthentication() {
        return authentication;
    }

    public void setAuthentication(boolean authentication) {
        this.authentication = authentication;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.usernameDirty = true;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.passwordDirty = true;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        this.deviceIdDirty = true;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
        this.trackerIdDirty = true;
    }
}

