package org.owntracks.android.ui.preferences.connection;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesConnectionBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostHttpBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostMqttBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionIdentificationBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionModeBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionParametersBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionSecurityBinding;
import org.owntracks.android.preferences.Preferences;
import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.ui.base.BaseActivity;
import org.owntracks.android.ui.preferences.connection.dialog.BaseDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionParametersViewModel;
import org.owntracks.android.ui.status.StatusActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConnectionActivity extends BaseActivity<UiPreferencesConnectionBinding, ConnectionMvvm.ViewModel<ConnectionMvvm.View>> implements ConnectionMvvm.View {
    private BaseDialogViewModel activeDialogViewModel;

    @Inject
    RunThingsOnOtherThreads runThingsOnOtherThreads;

    @Inject
    MessageProcessor messageProcessor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disablesAnimation();
        bindAndAttachContentView(R.layout.ui_preferences_connection, savedInstanceState);
        setSupportToolbar(binding.appbar.toolbar);
        setHasEventBus(false);
    }

    @Override
    public void showModeDialog() {
        UiPreferencesConnectionModeBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_mode, null, false);

        dialogBinding.setVm(viewModel.getModeDialogViewModel());
        activeDialogViewModel = dialogBinding.getVm();

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.mode_heading)
                .setPositiveButton(R.string.accept, dialogBinding.getVm())
                .setNegativeButton(R.string.cancel, dialogBinding.getVm())
                .show();
    }

    @Override
    public void showHostDialog() {
        if (viewModel.getConnectionMode() == ConnectionMode.HTTP) {
            UiPreferencesConnectionHostHttpBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_host_http, null, false);
            dialogBinding.setVm(viewModel.getHostDialogViewModelHttp());
            activeDialogViewModel = dialogBinding.getVm();

            new AlertDialog.Builder(this)
                    .setView(dialogBinding.getRoot())
                    .setTitle(R.string.preferencesHost)
                    .setPositiveButton(R.string.accept, dialogBinding.getVm())
                    .setNegativeButton(R.string.cancel, dialogBinding.getVm())
                    .show();

        } else {
            UiPreferencesConnectionHostMqttBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_host_mqtt, null, false);
            dialogBinding.setVm(viewModel.getHostDialogViewModelMqtt());
            activeDialogViewModel = dialogBinding.getVm();

            new AlertDialog.Builder(this)
                    .setView(dialogBinding.getRoot())
                    .setTitle(R.string.preferencesHost)
                    .setPositiveButton(R.string.accept, dialogBinding.getVm())
                    .setNegativeButton(R.string.cancel, dialogBinding.getVm())
                    .show();
        }
    }

    @Override
    public void showIdentificationDialog() {
        UiPreferencesConnectionIdentificationBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_identification, null, false);
        dialogBinding.setVm(viewModel.getIdentificationDialogViewModel());
        activeDialogViewModel = dialogBinding.getVm();

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.preferencesIdentification)
                .setPositiveButton(R.string.accept, dialogBinding.getVm())
                .setNegativeButton(R.string.cancel, dialogBinding.getVm())
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void showSecurityDialog() {
        UiPreferencesConnectionSecurityBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_security, null, false);
        dialogBinding.setVm(viewModel.getConnectionSecurityViewModel());
        activeDialogViewModel = dialogBinding.getVm();

        new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.preferencesSecurity)
                .setPositiveButton(R.string.accept, dialogBinding.getVm())
                .setNegativeButton(R.string.cancel, dialogBinding.getVm())
                .show();
    }

    ConnectionParametersViewModel connectionParametersViewModel;

    @Override
    public void showParametersDialog() {
        UiPreferencesConnectionParametersBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.ui_preferences_connection_parameters, null, false);
        connectionParametersViewModel = viewModel.getConnectionParametersViewModel();
        dialogBinding.setVm(connectionParametersViewModel);
        activeDialogViewModel = dialogBinding.getVm();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setTitle(R.string.preferencesParameters)
                .setPositiveButton(R.string.accept, dialogBinding.getVm())
                .setNegativeButton(R.string.cancel, dialogBinding.getVm()).create();
        TextInputLayout keepAliveEditText = dialogBinding.getRoot().findViewById(R.id.keepalive);

        keepAliveEditText.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                Boolean valid = false;
                try {
                    int intValue = Integer.parseInt(text.toString());
                    if (preferences.keepAliveInRange(intValue) || (preferences.getExperimentalFeatures().contains(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE) && intValue >= 1)) {
                        valid = true;
                    }
                } catch (NumberFormatException e) {
                }
                if (valid) {
                    keepAliveEditText.setErrorEnabled(false);
                } else {
                    keepAliveEditText.setErrorEnabled(true);
                    keepAliveEditText.setError(getString(R.string.preferencesKeepaliveValidationError, preferences.getExperimentalFeatures().contains(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE) ? 1 : preferences.getMinimumKeepaliveSeconds()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (!keepAliveEditText.isErrorEnabled()) {
                connectionParametersViewModel.save();
                dialog.dismiss();
            }
        });
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

        if (viewModel.getConnectionMode() == ConnectionMode.HTTP) {
            getMenuInflater().inflate(R.menu.preferences_connection_http, menu);
        } else {
            getMenuInflater().inflate(R.menu.preferences_connection_mqtt, menu);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.connect) {
            if (messageProcessor.isEndpointReady()) {
                messageProcessor.reconnect();
            } else {
                Snackbar.make(findViewById(R.id.scrollView),
                        R.string.ERROR_CONFIGURATION,
                        Snackbar.LENGTH_SHORT
                ).show();
            }
            return true;
        } else if (itemId == R.id.status) {
            startActivity(new Intent(this, StatusActivity.class));

            return false;
        }
        return false;
    }
}
