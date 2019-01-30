package org.owntracks.android.ui.welcome.play;

import androidx.databinding.Bindable;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface PlayFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {
        void requestFix();
    }

    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
        void onFixClicked();

        @Bindable boolean isFixAvailable();
        @Bindable void setFixAvailable(boolean enabled);
    }
}
