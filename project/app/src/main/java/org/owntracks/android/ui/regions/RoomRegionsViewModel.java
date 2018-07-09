package org.owntracks.android.ui.regions;

import android.arch.lifecycle.LiveData;

import org.owntracks.android.db.room.WaypointsDatabase;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseArchitectureViewModel;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class RoomRegionsViewModel extends BaseArchitectureViewModel {
    private final LiveData<List<WaypointModel>> waypointsList;

    private WaypointsDatabase waypointsDatabase;

    @Inject
        public RoomRegionsViewModel(WaypointsDatabase waypointsDatabase) {
            super();
            Timber.v("new vm instantiated");
            this.waypointsDatabase = waypointsDatabase;

            waypointsList = waypointsDatabase.waypointModel().getAll();
        }

        public void delete(WaypointModel w) {
            waypointsDatabase.delete(w);
        }

        public LiveData<List<WaypointModel>> getWaypointsList() {
            return waypointsList;
        }

    public void printId() {
        Timber.v("self: %s", this);
    }
}

