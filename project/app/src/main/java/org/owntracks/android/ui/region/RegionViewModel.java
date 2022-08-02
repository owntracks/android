package org.owntracks.android.ui.region;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;

@ActivityScoped
public class RegionViewModel extends BaseViewModel<RegionMvvm.View> implements RegionMvvm.ViewModel<RegionMvvm.View> {
    private final LocationRepo locationRepo;
    private final WaypointsRepo waypointsRepo;

    private WaypointModel waypoint;

    @Inject
    public RegionViewModel(WaypointsRepo waypointsRepo, LocationRepo locationRepo) {
        super();
        this.waypointsRepo = waypointsRepo;
        this.locationRepo = locationRepo;
    }

    public void attachView(@Nullable Bundle savedInstanceState, @NonNull RegionMvvm.View view) {
        super.attachView(savedInstanceState, view);
    }

    public void loadWaypoint(long id) {
        WaypointModel w = waypointsRepo.get(id);
        if(w == null) {
            w = new WaypointModel();
            if (locationRepo.getCurrentBlueDotOnMapLocation() != null) {
                w.setGeofenceLatitude(locationRepo.getCurrentBlueDotOnMapLocation().getLatitude());
                w.setGeofenceLongitude(locationRepo.getCurrentBlueDotOnMapLocation().getLongitude());
            } else {
                w.setGeofenceLatitude(0);
                w.setGeofenceLongitude(0);
            }
        }
        setWaypoint(w);
    }

    public boolean canSaveWaypoint() {
        return this.waypoint.getDescription().length() > 0;
    }

    public void saveWaypoint() {
        if(canSaveWaypoint()) {
            waypointsRepo.insert(waypoint);
        }
    }

    @Bindable
    public WaypointModel getWaypoint() {
        return waypoint;
    }

    private void setWaypoint(WaypointModel waypoint) {
        this.waypoint = waypoint;
    }
}
