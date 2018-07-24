package org.owntracks.android.data.repos;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.App;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.db.room.WaypointsDatabase;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.support.Preferences;

import java.util.List;

import dagger.Provides;
import timber.log.Timber;

public class RoomWaypointsRepo extends WaypointsRepo {
    static WaypointsDatabase database;

    public RoomWaypointsRepo(@AppContext Context context, EventBus eventBus) {
        super(eventBus);
        database = WaypointsDatabase.getDatabase(context);
    }


    @Override
    public WaypointModel getSync(long id) {
        return database.getSync(id);
    }

    @Override
    public List<WaypointModel> getAllSync() {
        return database.getAllSync();
    }

    @Override
    public LiveData<WaypointModel> get(long id) {
        return database.get(id);
    }

    @Override
    public LiveData<List<WaypointModel>> getAll() {
        return database.getAll();
    }

    @Override
    void insert_impl(WaypointModel w) {
        new insertTask().execute(w);
    }

    @Override
    void update_impl(WaypointModel w) {
        new updateTask().execute(w);
    }

    @Override
    void delete_impl(WaypointModel w) {
        new deleteTask().execute(w);
    }


    private static class insertTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            Timber.v("running insert on %s", params[0].getDescription());
            database.insertSync(params[0]);
            return null;
        }
    }

    private static class updateTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            database.updateSync(params[0]);
            return null;
        }
    }

    private static class deleteTask extends AsyncTask<WaypointModel, Void, Void> {
        @Override
        protected Void doInBackground(final WaypointModel... params) {
            database.deleteSync(params[0]);
            return null;
        }
    }
}