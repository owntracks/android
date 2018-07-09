package org.owntracks.android.ui.preferences.connection;

import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesConnectionIdentificationBinding;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.BaseDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostHttpDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostMqttDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionIdentificationViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionModeDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionParametersViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionSecurityViewModel;


public interface ConnectionMvvm {

    interface View extends MvvmView {
        void showModeDialog();
        void showHostDialog();
        void showIdentificationDialog();
        void showSecurityDialog();
        void showParametersDialog();
        void recreateOptionsMenu();

        }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onModeClick();
        void onHostClick();
        void onIdentificationClick();
        void onSecurityClick();
        void onParametersClick();

        int getModeId();
        void setModeId(int newModeId);

        ConnectionHostMqttDialogViewModel getHostDialogViewModelMqtt();
        ConnectionHostHttpDialogViewModel getHostDialogViewModelHttp();
        ConnectionModeDialogViewModel getModeDialogViewModel();
        ConnectionIdentificationViewModel getIdentificationDialogViewModel();
        ConnectionSecurityViewModel getConnectionSecurityViewModel();
        ConnectionParametersViewModel getConnectionParametersViewModel();
    }
}
