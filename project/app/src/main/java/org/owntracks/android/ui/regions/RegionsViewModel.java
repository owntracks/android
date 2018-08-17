package org.owntracks.android.ui.regions;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import io.objectbox.query.Query;
import timber.log.Timber;

@PerActivity
public class RegionsViewModel extends BaseViewModel<RegionsMvvm.View> implements RegionsMvvm.ViewModel<RegionsMvvm.View> {
    private WaypointsRepo waypointsRepo;

    @Inject
        public RegionsViewModel(WaypointsRepo waypointsRepo) {
            super();
            Timber.v("new vm instantiated");
            this.waypointsRepo = waypointsRepo;
        }

        public Query<WaypointModel> getWaypointsList() {
            return this.waypointsRepo.getAllQuery();
        }

    @Override
    public void delete(WaypointModel model) {
        waypointsRepo.delete(model);
    }
}

