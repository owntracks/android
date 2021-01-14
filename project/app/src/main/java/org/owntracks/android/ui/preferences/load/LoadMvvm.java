package org.owntracks.android.ui.preferences.load;


import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.net.URI;

public interface LoadMvvm {

    interface View extends MvvmView {
        void showFinishDialog();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void extractPreferences(URI uri);

        void extractPreferences(byte[] content);

        void saveConfiguration();

        String getDisplayedConfiguration();

        ImportStatus getConfigurationImportStatus();
    }
}
