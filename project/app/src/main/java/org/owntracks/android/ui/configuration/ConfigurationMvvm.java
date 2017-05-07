package org.owntracks.android.ui.configuration;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
public interface ConfigurationMvvm {

    interface View extends MvvmView {
        void displayErrorPreferencesLoadFailed();
        void displayErrorExportFailed();

        boolean exportConfigurationToFile(String exportStr);

        void displaySuccessConfigurationExportToFile();

        void showImportConfigurationValueView();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onExportConfigurationToFileClicked();

        void onExportWaypointsToEndpointClicked();

        void onImportConfigurationFromFileClicked();

        void onImportConfigurationValueClicked();

        void onImportConfigurationSingleValueClicked();

        void onPreferencesValueForKeySetSuccessful();

        void onPreferencesValueForKeySetFailed();
    }
}
