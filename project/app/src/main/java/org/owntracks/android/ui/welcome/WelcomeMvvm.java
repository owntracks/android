package org.owntracks.android.ui.welcome;

import androidx.databinding.Bindable;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeMvvm {

    // The view methods get called from all sorts of places. Fragments etc.
    interface View extends MvvmView {
        void showNextFragment();
        void setPagerIndicator(int position);

        // Called from Fragments to set button states
        void refreshNextDoneButtons();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        @Bindable boolean isDoneEnabled();
        @Bindable boolean isNextEnabled();

        void onAdapterPageSelected(int position);

        void onNextClicked();
        void onDoneClicked();

        // Only really called from the Activity to make sure the view is up to date
        void setNextEnabled(boolean enabled);
        void setDoneEnabled(boolean enabled);
    }
}
