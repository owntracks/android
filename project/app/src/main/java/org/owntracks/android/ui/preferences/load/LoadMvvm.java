package org.owntracks.android.ui.preferences.load;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface LoadMvvm {

    interface View extends MvvmView {
        void showFinishDialog();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void extractPreferences(Uri uri);

        void saveConfiguration();

        MutableLiveData<Throwable> importFailure();

        MutableLiveData<String> formattedEffectiveConfiguration();
    }
}
