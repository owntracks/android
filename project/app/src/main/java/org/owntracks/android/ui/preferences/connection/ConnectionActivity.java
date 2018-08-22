package org.owntracks.android.ui.preferences.connection;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesConnectionBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostHttpBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostMqttBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionIdentificationBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionModeBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionParametersBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionSecurityBinding;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.preferences.connection.dialog.BaseDialogViewModel;
import org.owntracks.android.ui.status.StatusActivity;

import javax.inject.Inject;


public class ConnectionActivity extends BaseActivity<UiPreferencesConnectionBinding, ConnectionMvvm.ViewModel> implements ConnectionMvvm.View {
    private BaseDialogViewModel activeDialogViewModel ;

    @Inject
    Runner runner;

    @Inject
    protected MessageProcessor messageProcessor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_preferences_connection, savedInstanceState);
        setSupportToolbar(binding.toolbar, true, false);
        setDrawer(binding.toolbar);
        setHasEventBus(true);
    }

    @Override
    public void showModeDialog() {
        UiPreferencesConnectionModeBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_mode,  null, false);
        dialogBinding.setVm(viewModel.getModeDialogViewModel());
        activeDialogViewModel = dialogBinding.getVm();

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
        if(viewModel.getModeId() == MessageProcessorEndpointHttp.MODE_ID) {
            UiPreferencesConnectionHostHttpBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_host_http, null, false);
            dialogBinding.setVm(viewModel.getHostDialogViewModelHttp());
            activeDialogViewModel = dialogBinding.getVm();

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
            activeDialogViewModel = dialogBinding.getVm();

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
        activeDialogViewModel = dialogBinding.getVm();

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
        UiPreferencesConnectionSecurityBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_security,  null, false);
        dialogBinding.setVm(viewModel.getConnectionSecurityViewModel());
        activeDialogViewModel = dialogBinding.getVm();

        new MaterialDialog.Builder(this)
                .customView(dialogBinding.getRoot(), true)
                .title(R.string.preferencesSecurity)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .onPositive(dialogBinding.getVm())
                .show();

    }

    @Override
    public void showParametersDialog() {
        UiPreferencesConnectionParametersBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_parameters,  null, false);
        dialogBinding.setVm(viewModel.getConnectionParametersViewModel());
        activeDialogViewModel = dialogBinding.getVm();

        new MaterialDialog.Builder(this)
                .customView(dialogBinding.getRoot(), true)
                .title(R.string.preferencesParameters)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .onPositive(dialogBinding.getVm())
                .show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        activeDialogViewModel.onActivityResult(requestCode, resultCode, data);

    }

    public void recreateOptionsMenu() {
        invalidateOptionsMenu();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu != null) {
            menu.clear();
        }

        if(viewModel.getModeId() == MessageProcessorEndpointHttp.MODE_ID) {
            getMenuInflater().inflate(R.menu.preferences_connection_http, menu);
        } else {
            getMenuInflater().inflate(R.menu.preferences_connection_mqtt, menu);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect:
                if(messageProcessor.isEndpointConfigurationComplete()) {
                    Runnable r = new Runnable() {

                        @Override
                        public void run() {
                            messageProcessor.reconnect();
                        }
                    };
                    runner.postOnBackgroundHandlerDelayed(r, 1);

                } else {
                    Toast.makeText(this, R.string.ERROR_CONFIGURATION, Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.status:
                startActivity(new Intent(this, StatusActivity.class));
            default:
                return false;
        }
    }
}
