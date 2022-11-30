package org.owntracks.android.ui.preferences;

import org.owntracks.android.preferences.Preferences;
import org.owntracks.android.preferences.types.ConnectionMode;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface PreferencesFragmentMvvm {

    interface View extends MvvmView {
        void loadRoot();

        void setModeSummary(ConnectionMode mode);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        Preferences getPreferences();
    }
}
