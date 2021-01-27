package org.owntracks.android.ui.welcome;

import org.owntracks.android.ui.base.view.MvvmView;

public interface WelcomeFragmentMvvm  {

    interface View extends MvvmView {
        boolean isNextEnabled();
        void onShowFragment();
    }
}
