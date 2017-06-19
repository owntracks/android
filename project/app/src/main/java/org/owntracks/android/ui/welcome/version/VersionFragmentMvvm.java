package org.owntracks.android.ui.welcome.version;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface VersionFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {

    }
    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
    }
}
