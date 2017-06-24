package org.owntracks.android.ui.preferences.connection;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

import com.afollestad.materialdialogs.MaterialDialog;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesConnectionBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostHttpBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostMqttBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionIdentificationBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionModeBinding;
import org.owntracks.android.support.Events;
import org.owntracks.android.ui.base.BaseActivity;

import timber.log.Timber;


public class ConnectionActivity extends BaseActivity<UiPreferencesConnectionBinding, ConnectionMvvm.ViewModel> implements ConnectionMvvm.View {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_preferences_connection, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
        setHasEventBus(true);

        //View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_delete_confirmation, null, false);
        //mBinding = DialogDeleteConfirmationBinding.bind(view);
        //mBinding.setViewModel(viewModel);
        //builder.setView(view);
        //builder.create();
    }

    @Override
    public void showModeDialog() {
        UiPreferencesConnectionModeBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_mode,  null, false);
        dialogBinding.setVm(viewModel.getModeDialogViewModel());

        new MaterialDialog.Builder(this)
                .customView(dialogBinding.getRoot(), true)
                .title(R.string.mode_heading)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .onPositive(dialogBinding.getVm())
                .show();
    }

    @Override
    public void showHostDialog() {
        if(viewModel.getModeId() == App.MODE_ID_HTTP_PRIVATE) {
            UiPreferencesConnectionHostHttpBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_host_http, null, false);
            dialogBinding.setVm(viewModel.getHostDialogViewModelHttp());
            new MaterialDialog.Builder(this)
                    .customView(dialogBinding.getRoot(), true)
                    .title(R.string.preferencesHost)
                    .positiveText(R.string.accept)
                    .negativeText(R.string.cancel)
                    .onPositive(dialogBinding.getVm())
                    .show();
        } else {
            UiPreferencesConnectionHostMqttBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_host_mqtt, null, false);
            dialogBinding.setVm(viewModel.getHostDialogViewModelMqtt());
            new MaterialDialog.Builder(this)
                    .customView(dialogBinding.getRoot(), true)
                    .title(R.string.preferencesHost)
                    .positiveText(R.string.accept)
                    .negativeText(R.string.cancel)
                    .onPositive(dialogBinding.getVm())
                    .show();
        }
    }

    @Override
    public void showIdentificationDialog() {
        UiPreferencesConnectionIdentificationBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_identification,  null, false);
        dialogBinding.setVm(viewModel.getIdentificationDialogViewModel());
        new MaterialDialog.Builder(this)
                .customView(dialogBinding.getRoot(), true)
                .title(R.string.preferencesIdentification)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .onPositive(dialogBinding.getVm())
                .show();
    }

    @Override
    public void showSecurityDialog() {
        Timber.e("NOT IMPLEMENTED");
    }

    @Override
    public void showParametersDialog() {
        Timber.e("NOT IMPLEMENTED");
    }

}
