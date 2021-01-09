package org.owntracks.android.ui.preferences.load;
import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.support.Parser;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.io.IOException;

public interface LoadMvvm {

    interface View extends MvvmView {
        void showFinishDialog();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void saveConfiguration();
        void setConfiguration(String configuration) throws IOException, Parser.EncryptionException;

        MutableLiveData<Boolean> hasConfiguration() ;
        MutableLiveData<String> formattedEffectiveConfiguration();
    }
}
