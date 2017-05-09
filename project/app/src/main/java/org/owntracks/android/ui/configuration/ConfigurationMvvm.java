package org.owntracks.android.ui.configuration;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
public interface ConfigurationMvvm {

    interface View extends MvvmView {
        void displayErrorPreferencesLoadFailed();
        void displayErrorExportFailed();

        boolean exportConfigurationToFile(String exportStr);

        void displaySuccessConfigurationExportToFile();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onExportConfigurationToFileClicked();
        void onExportWaypointsToEndpointClicked();

        void onPreferencesValueForKeySetSuccessful();

    }
}
