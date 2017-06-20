package org.owntracks.android.ui.welcome;

import android.databinding.Bindable;
import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
import org.owntracks.android.ui.welcome.intro.IntroFragmentMvvm;
import org.owntracks.android.ui.welcome.mode.ModeFragmentMvvm;
import org.owntracks.android.ui.welcome.permission.PermissionFragmentMvvm;

public interface WelcomeFragmentMvvm  {


    interface View extends MvvmView {
        ViewModel getViewModel();
        void setActivityViewModel();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void onNextClicked();
        @Bindable boolean isNextEnabled();
        @Bindable void setNextEnabled(boolean enabled);
    }
}
