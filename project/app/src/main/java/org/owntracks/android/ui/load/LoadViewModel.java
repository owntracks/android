package org.owntracks.android.ui.load;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.databinding.Bindable;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.messages.MessageBase;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.io.IOException;

import javax.inject.Inject;

import timber.log.Timber;


@PerActivity
public class LoadViewModel extends BaseViewModel<LoadMvvm.View> implements LoadMvvm.ViewModel<LoadMvvm.View> {
    @Bindable
    private String configurationPretty;
    private MessageConfiguration configuration;

    @Inject
    public LoadViewModel(@AppContext Context context) {

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
        this.configuration = MessageConfiguration.class.cast(App.getParser().fromJson(json.getBytes()));
        this.configurationPretty = App.getParser().toJsonPlainPretty(this.configuration);
        notifyPropertyChanged(BR.configurationPretty);
    }


    public void saveConfiguration() {
        App.getPreferences().importFromMessage(configuration);
    }
}
