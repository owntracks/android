package org.owntracks.android.ui.regions;

import android.arch.lifecycle.LiveData;

import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseArchitectureViewModel;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class RoomRegionsViewModel extends BaseArchitectureViewModel {
    private final LiveData<List<WaypointModel>> waypointsList;

    private WaypointsRepo waypointsRepo;

    @Inject
        public RoomRegionsViewModel(WaypointsRepo waypointsRepo) {
            super();
            Timber.v("new vm instantiated");
            this.waypointsRepo = waypointsRepo;

            waypointsList = waypointsRepo.getAll();
        }

        public void delete(WaypointModel w) {
            waypointsRepo.delete(w);
        }

        public LiveData<List<WaypointModel>> getWaypointsList() {
            return waypointsList;
        }

    public void printId() {
        Timber.v("self: %s", this);
    }
}

