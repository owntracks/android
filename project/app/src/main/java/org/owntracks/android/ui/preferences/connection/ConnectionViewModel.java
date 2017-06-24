package org.owntracks.android.ui.preferences.connection;

import android.Manifest;
import android.content.Context;
import android.databinding.Bindable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;


@PerActivity
public class ConnectionViewModel extends BaseViewModel<ConnectionMvvm.View> implements ConnectionMvvm.ViewModel<ConnectionMvvm.View> {

    final Context context;
    final Preferences preferences;
    private boolean mqtt;

    @Inject
    public ConnectionViewModel(@AppContext Context context, Preferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    public void attachView(@NonNull ConnectionMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
        setModeMqtt(!preferences.isModeHttpPrivate());
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
    public boolean isModeMqtt() {
        return mqtt;
    }

    @Override
    public void setModeMqtt(boolean b) {
        this.mqtt = b; 
    }
}
