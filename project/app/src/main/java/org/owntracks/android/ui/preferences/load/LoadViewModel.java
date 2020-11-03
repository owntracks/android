package org.owntracks.android.ui.preferences.load;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;


@PerActivity
public class LoadViewModel extends BaseViewModel<LoadMvvm.View> implements LoadMvvm.ViewModel<LoadMvvm.View> {
    private final Preferences preferences;
    private final Parser parser;
    private final WaypointsRepo waypointsRepo;

    private MessageConfiguration configuration;

    @Inject
    public LoadViewModel(@AppContext Context context, Preferences preferences, Parser parser, WaypointsRepo waypointsRepo) {
        this.preferences = preferences;
        this.parser = parser;
        this.waypointsRepo = waypointsRepo;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull LoadMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    public String setConfiguration(String json) throws IOException, Parser.EncryptionException {
        this.configuration = (MessageConfiguration) parser.fromJson(json.getBytes());
        return parser.toJsonPlainPretty(this.configuration);
    }

    public void saveConfiguration() {
        preferences.importFromMessage(configuration);
        if (!configuration.getWaypoints().isEmpty()) {
            waypointsRepo.importFromMessage(configuration.getWaypoints());
        }

        getView().showFinishDialog();
    }

    @Override
    public boolean hasConfiguration() {
        return this.configuration != null;
    }
}
