package org.owntracks.android.ui.preferences.connection;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostHttpDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostMqttDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionIdentificationViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionModeDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionParametersViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionSecurityViewModel;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;
import timber.log.Timber;

@ActivityScoped
public class ConnectionViewModel extends BaseViewModel<ConnectionMvvm.View> implements ConnectionMvvm.ViewModel<ConnectionMvvm.View> {

    private final Preferences preferences;
    private final Context context;
    private int modeId;


    @Inject
    ConnectionViewModel(Preferences preferences, @ApplicationContext Context context) {
        this.preferences = preferences;
        this.context = context;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull ConnectionMvvm.View view) {
        super.attachView(savedInstanceState, view);
        setModeId(preferences.getMode());
    }

    @Override
    public void setModeId(int newModeId) {
        this.modeId = newModeId;
    }

    @Bindable
    @Override
    public int getModeId() {
        return modeId;
    }

    @Subscribe
    public void onEvent(Events.ModeChanged e) {
        Timber.v("mode changed %s", e.getNewModeId());
        setModeId(e.getNewModeId());
        getView().recreateOptionsMenu();
        notifyChange();
    }

    @Override
    public void onModeClick() {
        getView().showModeDialog();
    }

    @Override
    public void onHostClick() {
        getView().showHostDialog();
    }

    @Override
    public void onIdentificationClick() {
        getView().showIdentificationDialog();
    }

    @Override
    public void onSecurityClick() {
        getView().showSecurityDialog();
    }

    @Override
    public void onParametersClick() {
        getView().showParametersDialog();
    }

    @Override
    public ConnectionModeDialogViewModel getModeDialogViewModel() {
        return new ConnectionModeDialogViewModel(preferences);
    }

    @Override
    public ConnectionHostMqttDialogViewModel getHostDialogViewModelMqtt() {
        return new ConnectionHostMqttDialogViewModel(preferences);
    }

    @Override
    public ConnectionHostHttpDialogViewModel getHostDialogViewModelHttp() {
        return new ConnectionHostHttpDialogViewModel(preferences);
    }

    @Override
    public ConnectionIdentificationViewModel getIdentificationDialogViewModel() {
        return new ConnectionIdentificationViewModel(preferences);
    }

    @Override
    public ConnectionSecurityViewModel getConnectionSecurityViewModel() {
        return new ConnectionSecurityViewModel(preferences, navigator, context);
    }

    @Override
    public ConnectionParametersViewModel getConnectionParametersViewModel() {
        return new ConnectionParametersViewModel(preferences);
    }

}
