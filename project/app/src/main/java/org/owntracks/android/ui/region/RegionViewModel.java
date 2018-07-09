package org.owntracks.android.ui.region;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseViewModel;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity

public class RegionViewModel extends BaseViewModel<RegionMvvm.View> implements RegionMvvm.ViewModel<RegionMvvm.View> {
    private Waypoint waypoint;

    @Inject
    Dao dao;

    @Inject
    public RegionViewModel() {

    }

    public void attachView(@NonNull RegionMvvm.View view, @Nullable Bundle savedInstanceState) {
        super.attachView(view, savedInstanceState);
    }

    @Override
    public Waypoint getWaypoint() {
        return waypoint;
    }

    @Override
    public void setWaypoint(long waypointId) {
        this.waypoint = dao.getWaypointDao().loadByRowId(waypointId);
        if(this.waypoint == null) {
            this.waypoint = new Waypoint();
        }
    }

    @Override
    public boolean canSaveWaypoint() {
        return this.waypoint.getDescription() != null && this.waypoint.getDescription().length() > 0
                && ((this.waypoint.getGeofenceLatitude() <= 90) && (this.waypoint.getGeofenceLatitude() >= -90))
                && ((this.waypoint.getGeofenceLongitude() <= 180) && (this.waypoint.getGeofenceLongitude() >= -180));
    }

    @Override
    public void saveWaypoint() {
        if(canSaveWaypoint()) {
            this.dao.getWaypointDao().save(this.waypoint);
        }
    }
}
