package org.owntracks.android.ui.welcome;

import android.databinding.Bindable;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface WelcomeFragmentMvvm  {


    interface View extends MvvmView {
        ViewModel getViewModel();
        void setActivityViewModel();}

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onNextClicked();
        @Bindable boolean isNextEnabled();
        @Bindable void setNextEnabled(boolean enabled);


    }
}
