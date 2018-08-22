package org.owntracks.android.ui.preferences.connection.dialog;

import android.content.Intent;
import android.view.View;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.MaterialDialog;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;
import org.owntracks.android.support.Preferences;

import timber.log.Timber;

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
        this.modeResId = modeToResId(preferences.getModeId());
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

    private int modeToResId(int mode) {
        switch (mode) {
            case MessageProcessorEndpointHttp.MODE_ID:
                return R.id.radioModeHttpPrivate;
            case MessageProcessorEndpointMqtt.MODE_ID:
            default:
                return R.id.radioModeMqttPrivate;
        }
    }

    private int resIdToMode(int resId) {
        switch (resId) {
            case R.id.radioModeHttpPrivate:
                return MessageProcessorEndpointHttp.MODE_ID;
            case R.id.radioModeMqttPrivate:
            default:
                return MessageProcessorEndpointMqtt.MODE_ID;
        }
    }
}

