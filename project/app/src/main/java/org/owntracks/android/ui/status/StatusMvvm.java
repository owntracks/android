package org.owntracks.android.ui.status;
import android.databinding.Bindable;

import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;
public interface StatusMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        ServiceMessage.EndpointState getEndpointState();
        String getEndpointMessage();
        int getEndpointQueue();
        boolean getPermissionLocation();
        long getLocationUpdated();
        long getAppStarted();
        long getServiceStarted();

    }
}
