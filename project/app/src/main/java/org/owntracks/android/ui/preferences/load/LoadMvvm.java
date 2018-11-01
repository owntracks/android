package org.owntracks.android.ui.preferences.load;
import org.owntracks.android.support.Parser;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.io.IOException;

public interface LoadMvvm {

    interface View extends MvvmView {
        void showFinishDialog();
        void showSaveButton();

    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void saveConfiguration();
        boolean hasConfiguration();
        String setConfiguration(String configuration) throws IOException, Parser.EncryptionException;
    }
}
