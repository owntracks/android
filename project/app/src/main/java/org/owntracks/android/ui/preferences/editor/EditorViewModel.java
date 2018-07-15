package org.owntracks.android.ui.preferences.editor;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;


@PerActivity
public class EditorViewModel extends BaseViewModel<EditorMvvm.View> implements EditorMvvm.ViewModel<EditorMvvm.View> {
    @Bindable
    String effectiveConfiguration;

    @Inject
    public EditorViewModel() {
    }

    public void attachView(@NonNull EditorMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        updateEffectiveConfiguration();
    }

    private void updateEffectiveConfiguration() {
        try {
            MessageConfiguration m = App.getPreferences().exportToMessage();
            m.setWaypoints(null);
            m.set(Preferences.Keys.PASSWORD, "********");
            setEffectiveConfiguration(App.getParser().toJsonPlainPretty(m));
        } catch (IOException e) {
            getView().displayLoadFailed();
        }
    }

    @Bindable
    public String getEffectiveConfiguration() {
        return effectiveConfiguration;
    }

    @Bindable
    public void setEffectiveConfiguration(String effectiveConfiguration) {
        this.effectiveConfiguration = effectiveConfiguration;
    }

    @Override
    public void onExportConfigurationToFileClicked() {
        String exportStr;
        try {
            exportStr = App.getParser().toJsonPlain(App.getPreferences().exportToMessage());
        } catch (IOException e) {
            getView().displayExportToFileFailed();
            return;
        }

        // Actual export handled by view because it requires a contexts
        if(getView().exportConfigurationToFile(exportStr)) {
            getView().displayExportToFileSuccessful();
        }
    }


    @Override
    public void onPreferencesValueForKeySetSuccessful() {
        updateEffectiveConfiguration();
        notifyPropertyChanged(BR.effectiveConfiguration);
    }
}
