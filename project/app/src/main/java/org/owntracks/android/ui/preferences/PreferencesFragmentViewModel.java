package org.owntracks.android.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import timber.log.Timber;


@PerFragment
public class PreferencesFragmentViewModel extends BaseViewModel<PreferencesFragmentMvvm.View> implements PreferencesFragmentMvvm.ViewModel<PreferencesFragmentMvvm.View> {
    private final Preferences preferences;

    @Inject
    public PreferencesFragmentViewModel(Preferences preferences) {
        this.preferences = preferences;
    }

    public void attachView(@NonNull PreferencesFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        view.loadRoot();
        view.setVersion();
        view.setModeSummary(preferences.getModeId());
    }

    @Override
    public Preferences getPreferences() {
        return preferences;
    }

}
