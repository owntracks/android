package org.owntracks.android.ui.preferences.connection;

import android.content.DialogInterface;
import android.databinding.BaseObservable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesConnectionBinding;
import org.owntracks.android.databinding.UiPreferencesConnectionHostMqttBinding;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.BaseActivity;

import timber.log.Timber;


public class ConnectionActivity extends BaseActivity<UiPreferencesConnectionBinding, ConnectionMvvm.ViewModel> implements ConnectionMvvm.View, DialogInterface.OnShowListener, MaterialDialog.SingleButtonCallback {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_preferences_connection, savedInstanceState);
        setSupportToolbar(binding.toolbar);
        setDrawer(binding.toolbar);
        setHasEventBus(false);

        //View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_delete_confirmation, null, false);
        //mBinding = DialogDeleteConfirmationBinding.bind(view);
        //mBinding.setViewModel(viewModel);
        //builder.setView(view);
        //builder.create();

    }


    public static class ConnectionBinding extends BaseObservable {
        private String host;

        public String getHost() {
            return Preferences.getHost(); 
        }
        
        public void setHost(String host) {
            Timber.v("host set to %s", host);
            this.host = host; 
        }

        public void save() {

        }
    }

    @Override
    public void showModeDialog() {
        Timber.e("NOT IMPLEMENTED");
    }
    MaterialDialog d;
    @Override
    public void showHostDialog() {
        MaterialDialog.Builder b = new MaterialDialog.Builder(this);
         d = b.customView(R.layout.ui_preferences_connection_host_mqtt, true)
                .title(R.string.preferencesHost)
                .positiveText(R.string.accept)
                .negativeText(R.string.cancel)
                .showListener(this)
                .onPositive(this)
                .show();


    }

    @Override
    public void showIdentificationDialog() {
        Timber.e("NOT IMPLEMENTED");
    }

    @Override
    public void showSecurityDialog() {
        Timber.e("NOT IMPLEMENTED");
    }

    @Override
    public void showParametersDialog() {
        Timber.e("NOT IMPLEMENTED");
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        UiPreferencesConnectionHostMqttBinding binding = UiPreferencesConnectionHostMqttBinding.bind(d.getView());
        binding.setVm(new ConnectionBinding());

    }

    @Override
    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

    }
}
