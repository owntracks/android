package org.owntracks.android.ui.preferences.load;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.model.messages.MessageBase;
import org.owntracks.android.model.messages.MessageConfiguration;
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

    private MessageConfiguration configuration;
    private MutableLiveData<Boolean> hasConfigurationLive = new MutableLiveData<>();
    private MutableLiveData<String> formattedEffectiveConfiguration = new MutableLiveData<>();


    @Inject
    public LoadViewModel(@AppContext Context context, Preferences preferences, Parser parser, WaypointsRepo waypointsRepo) {
        this.preferences = preferences;
        this.parser = parser;
        this.waypointsRepo = waypointsRepo;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull LoadMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    public void setConfiguration(String json) throws IOException, Parser.EncryptionException {
        MessageBase message = parser.fromJson(json.getBytes());
        if (message instanceof MessageConfiguration) {
            this.configuration = (MessageConfiguration) parser.fromJson(json.getBytes());
            hasConfigurationLive.postValue(true);
            String prettyConfiguration;
            try {
                prettyConfiguration = parser.toJsonPlainPretty(this.configuration);
            } catch (IOException e) {
                Timber.e(e);
                prettyConfiguration = "Unable to parse configuration";
            }
            formattedEffectiveConfiguration.postValue(prettyConfiguration);
        } else {
            throw new IOException("Message is not a valid configuration message");
        }
    }

    public void saveConfiguration() {
        preferences.importFromMessage(configuration);
        if (!configuration.getWaypoints().isEmpty()) {
            waypointsRepo.importFromMessage(configuration.getWaypoints());
        }

        getView().showFinishDialog();
    }

    @Override
    public MutableLiveData<Boolean> hasConfiguration() {
        return hasConfigurationLive;
    }

    @Override
    public MutableLiveData<String> formattedEffectiveConfiguration() {
        return formattedEffectiveConfiguration;
    }
}
