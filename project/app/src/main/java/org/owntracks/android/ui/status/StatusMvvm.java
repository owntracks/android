package org.owntracks.android.ui.status;

import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.util.Date;

public interface StatusMvvm {

    interface View extends MvvmView {
        void showIgnoreDozeActivity();
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        MessageProcessor.EndpointState getEndpointState();
        String getEndpointMessage();
        int getEndpointQueue();
        boolean getPermissionLocation();
        long getLocationUpdated();
        Date getServiceStarted();
        boolean getDozeWhitelisted();
        void onIgnoreDozeClicked(); 
    }
}
