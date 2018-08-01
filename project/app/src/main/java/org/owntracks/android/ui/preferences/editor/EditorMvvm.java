package org.owntracks.android.ui.preferences.editor;
import android.databinding.Bindable;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
public interface EditorMvvm {

    interface View extends MvvmView {
        void displayLoadFailed();

        void displayExportToFileFailed();
        void displayExportToFileSuccessful();

        boolean exportConfigurationToFile(String exportStr);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onPreferencesValueForKeySetSuccessful();
        @Bindable String getEffectiveConfiguration();
    }
}
