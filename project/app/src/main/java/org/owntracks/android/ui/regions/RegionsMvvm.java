package org.owntracks.android.ui.regions;

import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface RegionsMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
    }
}
