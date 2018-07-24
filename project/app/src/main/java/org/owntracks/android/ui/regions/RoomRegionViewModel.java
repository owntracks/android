package org.owntracks.android.ui.regions;

import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.base.viewmodel.BaseArchitectureViewModel;

import javax.inject.Inject;

import timber.log.Timber;

@PerActivity
public class RoomRegionViewModel extends BaseArchitectureViewModel  {
    private WaypointsRepo waypointsRepo;
    private WaypointModel model;
    public final MutableLiveData<String> description = new MutableLiveData<>();
    public final MutableLiveData<Double> latitude = new MutableLiveData<>();
    public final MutableLiveData<Double> longitude = new MutableLiveData<>();
    public final MutableLiveData<Integer> radius = new MutableLiveData<>();
    public final MutableLiveData<Long> id = new MutableLiveData<>();
    public final MutableLiveData<Long> lastTriggered = new MutableLiveData<>();
    public final MutableLiveData<Integer> lastTransition = new MutableLiveData<>();
    public final MutableLiveData<Boolean> canSave = new MutableLiveData<>();

    @Inject
    public RoomRegionViewModel(WaypointsRepo waypointsRepo) {
        super();
        this.waypointsRepo = waypointsRepo;
    }

    public void setWaypoint(WaypointModel w) {
    }

    public void loadWaypoint(long id) {
        Timber.v("loading waypoint with id: %s",id);
        new LoadTask(waypointsRepo).execute(id);
    }

    public void onloadWaypoint(@Nullable WaypointModel model) {
        Timber.v("load result: %s", model);
        this.model = model;
        if (model != null){
            this.description.postValue(model.getDescription());
            this.latitude.postValue(model.getGeofenceLatitude());
            this.longitude.postValue(model.getGeofenceLongitude());
            this.radius.postValue(model.getGeofenceRadius());
            this.id.postValue(model.getId());
            this.lastTriggered.postValue(model.getLastTriggered());
            this.lastTransition.postValue(model.getLastTransition());
      }

    }

    public void setLatLng(double lat, double lon) {
        this.latitude.postValue(lat);
        this.longitude.postValue(lon);
    }

    public boolean canSaveWaypoint() {
        return this.description.getValue() != null && this.latitude.getValue() != null && this.longitude.getValue() != null;
    }

    public void saveWaypoint() {
        if(canSaveWaypoint()) {
            if(model == null) {
                model = new WaypointModel();
            }

            model.setDescription(description.getValue());
            model.setGeofenceLatitude(latitude.getValue());
            model.setGeofenceLongitude(longitude.getValue());
            model.setGeofenceRadius(radius.getValue());

            // Will do insert or update
            waypointsRepo.insert(model);
        }
    }

    private class LoadTask extends AsyncTask<Long, Void, WaypointModel> {
        private final WaypointsRepo waypointsRepo;

        public LoadTask(WaypointsRepo waypointsRepo) {
            this.waypointsRepo = waypointsRepo;
        }
        @Override
        protected WaypointModel doInBackground(final Long... params) {
            return waypointsRepo.getSync(params[0]);
        }

        @Override
        protected void onPostExecute(WaypointModel result) {
            if(result != null)
                onloadWaypoint(result);
        }
    }

    public void onTextChanged() {
        canSave.postValue(canSaveWaypoint());
    }

}

