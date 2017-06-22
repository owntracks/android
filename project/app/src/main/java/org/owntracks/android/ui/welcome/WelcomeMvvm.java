package org.owntracks.android.ui.welcome;

import android.databinding.Bindable;
import android.os.Bundle;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeMvvm {

    interface View extends MvvmView {
        WelcomeFragmentMvvm.View getCurrentFragment();
        void showNextFragment();
        void setPagerIndicator(int position);

        // Called from Fragments to set
        void setFragmentViewModel(WelcomeFragmentMvvm.ViewModel viewModel);
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        @Bindable boolean isDoneEnabled();
        void onAdapterPageSelected(int position);
        void onNextClicked();

        WelcomeFragmentMvvm.ViewModel<WelcomeFragmentMvvm.View> getFragmentViewModel();
        void setFragmentViewModel(WelcomeFragmentMvvm.ViewModel fragmentViewModel);
    }
}
