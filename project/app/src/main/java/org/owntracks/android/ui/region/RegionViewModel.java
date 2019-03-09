package org.owntracks.android.ui.region;

import android.os.Bundle;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;

@PerActivity
public class RegionViewModel extends BaseViewModel<RegionMvvm.View> implements RegionMvvm.ViewModel<RegionMvvm.View> {
    private final LocationRepo locationRepo;
    private WaypointsRepo waypointsRepo;

    private WaypointModel waypoint;

    @Inject
    public RegionViewModel(WaypointsRepo waypointsRepo, LocationRepo locationRepo) {
        super();
        this.waypointsRepo = waypointsRepo;
        this.locationRepo = locationRepo; 
    }

    public void attachView(@NonNull RegionMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    public void loadWaypoint(long id) {
        WaypointModel w = waypointsRepo.get(id);
        if(w == null) {
            w = new WaypointModel();
            if (locationRepo.hasLocation()) {
                w.setGeofenceLatitude(locationRepo.getCurrentLocation().getLatitude());
                w.setGeofenceLongitude(locationRepo.getCurrentLocation().getLongitude());
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

    public void setWaypoint(WaypointModel waypoint) {
        this.waypoint = waypoint;
    }
}

