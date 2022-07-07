package org.owntracks.android.ui.status;

import androidx.lifecycle.LiveData;

import org.owntracks.android.data.EndpointState;
import org.owntracks.android.services.MessageProcessor;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import java.util.Date;

public interface StatusMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        EndpointState getEndpointState();
        String getEndpointMessage();
        int getEndpointQueue();

        long getLocationUpdated();
        Date getServiceStarted();
        LiveData<Boolean> getDozeWhitelisted();
        void refreshDozeModeWhitelisted();
    }
}
