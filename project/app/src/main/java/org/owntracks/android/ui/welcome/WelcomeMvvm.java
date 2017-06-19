package org.owntracks.android.ui.welcome;

import android.os.Bundle;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeMvvm {

    interface View extends MvvmView {
        void showNextFragment();
        void setPagerIndicator(int position);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onAdapterPageSelected(int position);
        void onNextClicked();
   }
}
