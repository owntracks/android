package org.owntracks.android.ui.preferences;
import android.content.Context;
import android.preference.Preference;

import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.support.Parser;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.io.IOException;

public interface PreferencesFragmentMvvm {

    interface View extends MvvmView {
        void loadRoot();
        void setVersion();

        void setModeSummary(int mode);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        Preferences getPreferences();
    }
}
