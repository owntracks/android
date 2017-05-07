package org.owntracks.android.ui.configuration;

import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    public ConfigurationViewModel(@AppContext Context context) {

    }

    public void attachView(@NonNull ConfigurationMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        updateEffectiveConfiguration();
    }

    private void updateEffectiveConfiguration() {
        try {
            MessageConfiguration m = Preferences.exportToMessage();
            m.setWaypoints(null);
            setEffectiveConfiguration(Parser.toJsonPlainPretty(m));
        } catch (IOException e) {
            getView().displayErrorPreferencesLoadFailed();
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
            exportStr = Parser.toJsonPlain(Preferences.exportToMessage());
        } catch (IOException e) {
            getView().displayErrorExportFailed();
            return;
        }

        if(getView().exportConfigurationToFile(exportStr)) {
            getView().displaySuccessConfigurationExportToFile();
        }
    }

    @Override
    public void onExportWaypointsToEndpointClicked() {

    }

    @Override
    public void onImportConfigurationFromFileClicked() {

    }

    @Override
    public void onImportConfigurationValueClicked() {
        getView().showImportConfigurationValueView();
    }

    @Override
    public void onImportConfigurationSingleValueClicked() {
        getView().showImportConfigurationValueView();
    }

    @Override
    public void onPreferencesValueForKeySetSuccessful() {
        updateEffectiveConfiguration();
    }

    @Override
    public void onPreferencesValueForKeySetFailed() {

    }
}
