package org.owntracks.android.ui.welcome.mode;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.BindingConversions;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


@PerFragment
public class ModeFragmentViewModel extends BaseViewModel<ModeFragmentMvvm.View> implements ModeFragmentMvvm.ViewModel<ModeFragmentMvvm.View> {


    private final Preferences preferences;

    private int checkedButtonId = R.id.radioModeMqttPublic;

    @Inject
    public ModeFragmentViewModel(Preferences preferences) {
        this.preferences = preferences; 
    }

    public void attachView(@NonNull ModeFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        switch (preferences.getModeId()) {
            case App.MODE_ID_HTTP_PRIVATE:
                setCheckedButton(R.id.radioModeHttpPrivate);
                break;
            case App.MODE_ID_MQTT_PRIVATE:
                setCheckedButton(R.id.radioModeMqttPrivate);
                break;
            case App.MODE_ID_MQTT_PUBLIC:
                setCheckedButton(R.id.radioModeMqttPublic);
                break;
        }
    }

    @Override
    @Bindable
    public int getCheckedButton() {
        return checkedButtonId;
    }

    @Override
    @Bindable
    public void setCheckedButton(int buttonId) {
        this.checkedButtonId = buttonId;
        notifyChange();
    }

    @Override
    public void onNextClicked() {
        switch (this.checkedButtonId) {
            case R.id.radioModeHttpPrivate:
                preferences.setMode(App.MODE_ID_HTTP_PRIVATE, true);
                break;
            case R.id.radioModeMqttPrivate:
                preferences.setMode(App.MODE_ID_MQTT_PRIVATE, true);
                break;
            case R.id.radioModeMqttPublic:
                preferences.setMode(App.MODE_ID_MQTT_PUBLIC, true);
                break;
        }
        preferences.setSetupCompleted();
    }
}
