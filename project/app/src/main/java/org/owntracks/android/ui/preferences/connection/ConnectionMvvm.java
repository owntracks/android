package org.owntracks.android.ui.preferences.connection;

import com.afollestad.materialdialogs.MaterialDialog;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostHttpDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionHostMqttDialogViewModel;
import org.owntracks.android.ui.preferences.connection.dialog.ConnectionModeDialogViewModel;


public interface ConnectionMvvm {

    interface View extends MvvmView {
        void showModeDialog();
        void showHostDialog();
        void showIdentificationDialog();
        void showSecurityDialog();
        void showParametersDialog();
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

    }
}
