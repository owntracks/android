package org.owntracks.android.ui.welcome;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeFragmentMvvm  {


    interface View extends MvvmView {
        void onNextClicked();
        boolean isNextEnabled();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
    }
}
