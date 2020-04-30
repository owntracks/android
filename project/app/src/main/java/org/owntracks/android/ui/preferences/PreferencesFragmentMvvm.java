package org.owntracks.android.ui.preferences;

import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface PreferencesFragmentMvvm {

    interface View extends MvvmView {
        void loadRoot();

        void setModeSummary(int mode);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        Preferences getPreferences();
    }
}
