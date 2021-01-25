package org.owntracks.android.ui.welcome;

import org.owntracks.android.ui.base.view.MvvmView;

public interface WelcomeMvvm {

    // The view methods get called from all sorts of places. Fragments etc.
    interface View extends MvvmView {
        void showNextFragment();
        void setPagerIndicator(int position);

        // Called from Fragments to set button states
        void refreshNextDoneButtons();
    }
}
