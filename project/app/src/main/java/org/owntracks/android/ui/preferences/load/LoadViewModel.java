package org.owntracks.android.ui.preferences.load;

import android.content.Context;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;


@PerActivity
public class LoadViewModel extends BaseViewModel<LoadMvvm.View> implements LoadMvvm.ViewModel<LoadMvvm.View> {
    private final Preferences preferences;
    private final Parser parser;
    private final WaypointsRepo waypointsRepo;

    @Bindable
    private String configurationPretty;
    private MessageConfiguration configuration;

    @Inject
    public LoadViewModel(@AppContext Context context, Preferences preferences, Parser parser, WaypointsRepo waypointsRepo) {
        this.preferences = preferences;
        this.parser = parser;
        this.waypointsRepo = waypointsRepo;
    }

    public void attachView(@NonNull LoadMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Bindable
    public String getConfigurationPretty() {
        return configurationPretty;
    }

    public void setConfiguration(String json) throws IOException, Parser.EncryptionException {
        Timber.v("%s", json);

        this.configuration = MessageConfiguration.class.cast(parser.fromJson(json.getBytes()));
        this.configurationPretty = parser.toJsonPlainPretty(this.configuration);

        Timber.v("hasWaypoints: %s / #%s", configuration.hasWaypoints(), configuration.getWaypoints().size());

        notifyPropertyChanged(BR.configurationPretty);
        getView().showSaveButton();
    }


    public void saveConfiguration() {
        preferences.importFromMessage(configuration);

        if(configuration.hasWaypoints()) {
            waypointsRepo.importFromMessage(configuration.getWaypoints());
        }

        getView().showFinishDialog();
    }
}
