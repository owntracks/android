package org.owntracks.android.ui.preferences.connection;

import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
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

        ConnectionMode getConnectionMode();
        void setConnectionMode(ConnectionMode newModeId);

        ConnectionHostMqttDialogViewModel getHostDialogViewModelMqtt();
        ConnectionHostHttpDialogViewModel getHostDialogViewModelHttp();
        ConnectionModeDialogViewModel getModeDialogViewModel();
        ConnectionIdentificationViewModel getIdentificationDialogViewModel();
        ConnectionSecurityViewModel getConnectionSecurityViewModel();
        ConnectionParametersViewModel getConnectionParametersViewModel();
    }
}
