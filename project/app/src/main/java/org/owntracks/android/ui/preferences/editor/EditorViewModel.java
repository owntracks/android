package org.owntracks.android.ui.preferences.editor;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;


@PerActivity
public class EditorViewModel extends BaseViewModel<EditorMvvm.View> implements EditorMvvm.ViewModel<EditorMvvm.View> {
    private final Parser parser;
    private final Preferences preferences;
    @Bindable
    private String effectiveConfiguration;

    @Inject
    WaypointsRepo waypointsRepo;

    @Inject
    public EditorViewModel(Preferences preferences, Parser parser) {
        this.preferences = preferences;
        this.parser = parser; 
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull EditorMvvm.View view) {
        super.attachView(savedInstanceState, view);
        updateEffectiveConfiguration();
    }

    private void updateEffectiveConfiguration() {
        try {
            MessageConfiguration message = preferences.exportToMessage();
            message.setWaypoints(waypointsRepo.exportToMessage());
            message.set(preferences.getPreferenceKey(R.string.preferenceKeyPassword), "********");
            setEffectiveConfiguration(parser.toJsonPlainPretty(message));
        } catch (IOException e) {
            Timber.e(e);
            getView().displayLoadFailed();
        }
    }

    @Bindable
    public String getEffectiveConfiguration() {
        return effectiveConfiguration;
    }

    @Bindable
    private void setEffectiveConfiguration(String effectiveConfiguration) {
        this.effectiveConfiguration = effectiveConfiguration;
        notifyPropertyChanged(BR.effectiveConfiguration);
    }

    @Override
    public void onPreferencesValueForKeySetSuccessful() {
        updateEffectiveConfiguration();
        notifyPropertyChanged(BR.effectiveConfiguration);
    }
}
