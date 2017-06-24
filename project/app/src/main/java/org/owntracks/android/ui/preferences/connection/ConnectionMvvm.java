package org.owntracks.android.ui.preferences.connection;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;


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

        boolean isModeMqtt();
        void setModeMqtt(boolean b);
    }
}
