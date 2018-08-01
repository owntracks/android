package org.owntracks.android.ui.region;

import android.databinding.Bindable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class RegionViewModel extends BaseViewModel<RegionMvvm.View> implements RegionMvvm.ViewModel<RegionMvvm.View> {
    private WaypointsRepo waypointsRepo;

    private WaypointModel waypoint;

    @Inject
    public RegionViewModel(WaypointsRepo waypointsRepo) {
        super();
        this.waypointsRepo = waypointsRepo;
    }

    public void attachView(@NonNull RegionMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    public void loadWaypoint(long id) {
        WaypointModel w = waypointsRepo.get(id);
        if(w == null) {
            w = new WaypointModel();
        }
        setWaypoint(w);
    }

    public void setLatLng(double lat, double lon) {
        this.waypoint.setGeofenceLatitude(lat);
        this.waypoint.setGeofenceLongitude(lon);
        Timber.v("waypoint coordinates updated");
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

