package org.owntracks.android.ui.region;

import android.databinding.Bindable;

import org.owntracks.android.db.Waypoint;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

public interface RegionMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        @Bindable
        Waypoint getWaypoint();
        void setWaypoint(long waypoint);

        boolean canSaveWaypoint();
        void saveWaypoint();
    }
}
