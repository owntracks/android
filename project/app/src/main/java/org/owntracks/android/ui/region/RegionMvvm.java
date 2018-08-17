package org.owntracks.android.ui.region;

import android.databinding.Bindable;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface RegionMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        void loadWaypoint(long waypointId);

        @Bindable WaypointModel getWaypoint();
        boolean canSaveWaypoint();
        void saveWaypoint();
        void setLatLng(double latitude, double longitude);
    }
}
