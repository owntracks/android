package org.owntracks.android.ui.preferences;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.injection.scopes.PerFragment;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


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
