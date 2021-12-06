package org.owntracks.android.ui.preferences.connection;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.METValidator;

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
import org.owntracks.android.support.Preferences;
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
        setHasEventBus(true);
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
        if (viewModel.getModeId() == MessageProcessorEndpointHttp.MODE_ID) {
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
        onBackPressed();
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
        MaterialEditText keepAliveEditText = dialogBinding.getRoot().findViewById(R.id.keepalive);
        keepAliveEditText.addValidator(new METValidator(getString(R.string.preferencesKeepaliveValidationError, preferences.isExperimentalFeatureEnabled(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE) ? 1 : preferences.getMinimumKeepalive())) {
            @Override
            public boolean isValid(@NonNull CharSequence text, boolean isEmpty) {
                try {
                    int intValue = Integer.parseInt(text.toString());
                    return isEmpty || preferences.keepAliveInRange(intValue) || (preferences.isExperimentalFeatureEnabled(Preferences.EXPERIMENTAL_FEATURE_ALLOW_SMALL_KEEPALIVE) && intValue >= 1);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        keepAliveEditText.setAutoValidate(true);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (keepAliveEditText.validate()) {
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

        if (viewModel.getModeId() == MessageProcessorEndpointHttp.MODE_ID) {
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
            if (messageProcessor.isEndpointConfigurationComplete()) {
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
