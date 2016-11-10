package org.owntracks.android.ui.status;
import android.databinding.Bindable;

import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.util.Date;

public interface StatusMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        ServiceMessage.EndpointState getEndpointState();
        String getEndpointMessage();
        int getEndpointQueue();
        boolean getPermissionLocation();
        Date getLocationUpdated();
        Date getAppStarted();
        Date getServiceStarted();

    }
}
