package org.owntracks.android.ui.welcome.mode;

import android.Manifest;
import android.content.Context;
import android.databinding.Bindable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.RadioGroup;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.welcome.intro.IntroFragmentMvvm;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import timber.log.Timber;


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

    @Override
    @Bindable
    public boolean isNextEnabled() {
        return true;
    }

    @Override
    public void setNextEnabled(boolean enabled) {

    }


}
