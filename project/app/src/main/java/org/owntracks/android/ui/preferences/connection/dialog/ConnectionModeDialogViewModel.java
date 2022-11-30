package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Intent;

import org.owntracks.android.R;
import org.owntracks.android.preferences.Preferences;
import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.services.MessageProcessorEndpointHttp;

public class ConnectionModeDialogViewModel extends BaseDialogViewModel {
    private int modeResId;

    public ConnectionModeDialogViewModel(Preferences preferences) {
        super(preferences);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void load() {
        this.modeResId = modeToResId(preferences.getMode());
    }

    @Override
    public void save() {
        preferences.setMode(resIdToMode(modeResId));
    }

    public int getMode() {
        return modeResId;
    }

    public void setMode(int mode) {
        this.modeResId = mode;
    }

    private int modeToResId(ConnectionMode mode) {
        switch (mode) {
            case HTTP:
                return R.id.radioModeHttpPrivate;
            case MQTT:
            default:
                return R.id.radioModeMqttPrivate;
        }
    }

    private ConnectionMode resIdToMode(int resId) {
        if (resId == R.id.radioModeHttpPrivate) {
            return ConnectionMode.HTTP;
        }
        return ConnectionMode.MQTT;
    }
}
