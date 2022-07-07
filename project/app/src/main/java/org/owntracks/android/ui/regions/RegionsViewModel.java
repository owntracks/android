package org.owntracks.android.ui.regions;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.services.LocationProcessor;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import io.objectbox.query.Query;
import timber.log.Timber;

@ActivityScoped
public class RegionsViewModel extends BaseViewModel<RegionsMvvm.View> implements RegionsMvvm.ViewModel<RegionsMvvm.View> {
    private final LocationProcessor locationProcessor;
    private final WaypointsRepo waypointsRepo;

    @Inject
    public RegionsViewModel(WaypointsRepo waypointsRepo, LocationProcessor locationProcessor) {
        super();
        Timber.v("new vm instantiated");
        this.waypointsRepo = waypointsRepo;
        this.locationProcessor = locationProcessor;
    }

        public Query<WaypointModel> getWaypointsList() {
            return this.waypointsRepo.getAllQuery();
        }

    @Override
    public void delete(WaypointModel model) {
        waypointsRepo.delete(model);
    }

    @Override
    public void exportWaypoints() {
        locationProcessor.publishWaypointsMessage();
    }
}
