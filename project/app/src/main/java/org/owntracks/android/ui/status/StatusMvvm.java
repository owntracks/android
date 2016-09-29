package org.owntracks.android.ui.status;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
public interface StatusMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        String getEndpointState();
        int getEndpointQueue();
        boolean getPermissionLocation();
        long getLocationServiceUpdateDate();
    }
}
