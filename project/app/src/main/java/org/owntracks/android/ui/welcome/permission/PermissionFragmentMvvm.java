package org.owntracks.android.ui.welcome.permission;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface PermissionFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {
        void requestFix();
        Activity getActivity();
        void checkPermission();

    }
    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
        void onFixClicked();
    }
}
