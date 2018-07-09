package org.owntracks.android.db.room;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.support.ObservableMutableLiveData;

import timber.log.Timber;

@Database(entities = {WaypointModel.class}, version = 6)
public abstract class WaypointsDatabase extends RoomDatabase  {

    private static WaypointsDatabase INSTANCE;

    public static WaypointsDatabase getDatabase(@AppContext Context context, EventBus eventBus) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, WaypointsDatabase.class, "room_waypoint_db").fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }

    public abstract WaypointModelDao waypointModel();

    public LiveData<WaypointModel> get(long id){
        return INSTANCE.waypointModel().getById(id);
    }
    public void insert(WaypointModel w) {
        new insertTask().execute(w);
    }
    public void update(WaypointModel w) {
        new updateTask().execute(w);
    }

    public void delete(WaypointModel w) {
        new deleteTask().execute(w);
    }

    private static class insertTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            Timber.v("running insert on %s", params[0].getDescription());
            INSTANCE.waypointModel().insert(params[0]);
            Timber.v("inserted");
            return null;
        }
    }

    public static abstract class getTask extends AsyncTask<Long, Void, WaypointModel> {
        @Override
        protected WaypointModel doInBackground(final Long... params) {
            return INSTANCE.waypointModel().getByIdSync(params[0]);
        }

        protected abstract void onPostExecute(WaypointModel result);

    }


    private static class updateTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            INSTANCE.waypointModel().update(params[0]);
            return null;
        }
    }

    private static class deleteTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            INSTANCE.waypointModel().delete(params[0]);
            return null;
        }
    }

}