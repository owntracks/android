package org.owntracks.android.ui.regions;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.ui.base.view.MvvmView;
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel;

import io.objectbox.query.Query;

public interface RegionsMvvm {

    interface View extends MvvmView {
    }

    interface ViewModel<V extends MvvmView> extends MvvmViewModel<V> {
        Query<WaypointModel> getWaypointsList();
        void delete(WaypointModel model);

        void exportWaypoints();
    }
}
