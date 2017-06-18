package org.owntracks.android.ui.welcome.permission;

import android.view.View;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface PermissionFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {
        void requestPermission();

    }
    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
        boolean getPermissionGranted();
        void setPermissionGranted(boolean granted);
        void onFixClicked();
    }
}
