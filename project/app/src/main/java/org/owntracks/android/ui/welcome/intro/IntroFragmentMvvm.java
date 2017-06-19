package org.owntracks.android.ui.welcome.intro;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface IntroFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {

    }
    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
    }
}
