package org.owntracks.android.ui.welcome.mode;

import android.widget.RadioGroup;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.welcome.WelcomeFragmentMvvm;


public interface ModeFragmentMvvm {
    interface View extends WelcomeFragmentMvvm.View {

    }
    interface ViewModel<V extends MvvmView> extends WelcomeFragmentMvvm.ViewModel<V> {
        int getCheckedButton();
        void setCheckedButton(int buttonId);
    }
}
