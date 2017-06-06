package org.owntracks.android.ui.configuration;

import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;


@PerActivity
public class ConfigurationViewModel extends BaseViewModel<org.owntracks.android.ui.configuration.ConfigurationMvvm.View> implements org.owntracks.android.ui.configuration.ConfigurationMvvm.ViewModel<org.owntracks.android.ui.configuration.ConfigurationMvvm.View> {
    @Bindable
    String effectiveConfiguration;

    @Inject
    public ConfigurationViewModel() {
    }

    public void attachView(@NonNull ConfigurationMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        updateEffectiveConfiguration();
    }

    private void updateEffectiveConfiguration() {
        try {
            MessageConfiguration m = Preferences.exportToMessage();
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
            exportStr = App.getParser().toJsonPlain(Preferences.exportToMessage());
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
