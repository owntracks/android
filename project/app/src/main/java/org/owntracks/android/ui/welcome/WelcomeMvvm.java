package org.owntracks.android.ui.welcome;

import androidx.databinding.Bindable;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeMvvm {

    interface View extends MvvmView {
        void showNextFragment();
        void setPagerIndicator(int position);

        // Called from Fragments to set button states
        void setNextEnabled(boolean enabled);
        void setDoneEnabled(boolean enabled);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        @Bindable boolean isDoneEnabled();
        @Bindable boolean isNextEnabled();

        void onAdapterPageSelected(int position);

        void onNextClicked();
        void onDoneClicked();

        void setNextEnabled(boolean enabled);
        void setDoneEnabled(boolean enabled);
    }
}
