package org.owntracks.android.ui.preferences;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;


public class PreferencesFragmentViewModel extends BaseViewModel<PreferencesFragmentMvvm.View> implements PreferencesFragmentMvvm.ViewModel<PreferencesFragmentMvvm.View> {
    private final Preferences preferences;

    @Inject
    public PreferencesFragmentViewModel(Preferences preferences) {
        this.preferences = preferences;
    }

    public void attachView(@NonNull PreferencesFragmentMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        view.loadRoot();
        view.setModeSummary(preferences.getMode());
    }

    @Override
    public Preferences getPreferences() {
        return preferences;
    }

}
