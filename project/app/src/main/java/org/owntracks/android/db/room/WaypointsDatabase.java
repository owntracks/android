package org.owntracks.android.db.room;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.database.SQLException;
import android.support.annotation.NonNull;

import org.owntracks.android.injection.qualifier.AppContext;

import java.util.List;

import timber.log.Timber;

@Database(entities = {WaypointModel.class}, version = 17, exportSchema = false)
public abstract class WaypointsDatabase extends RoomDatabase  {
    private static final String DBNAME = "org.owntracks.android.db";

    private static WaypointsDatabase INSTANCE;
    public static WaypointsDatabase getDatabase(@AppContext Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, WaypointsDatabase.class, DBNAME).addMigrations(LEGACY, M_15_16, M_16_17).build();
        }
        return  INSTANCE;
    }

    public abstract WaypointModelDao waypointModel();
    public LiveData<List<WaypointModel>> getAll(){
        return INSTANCE.waypointModel().getAll();
    }

    public LiveData<WaypointModel> get(long id){
        return INSTANCE.waypointModel().getById(id);
    }
    public void insertSync(WaypointModel w) {
        INSTANCE.waypointModel().insert(w);
    }
    public void updateSync(WaypointModel w) {
        INSTANCE.waypointModel().update(w);
    }
    public void deleteSync(WaypointModel w) {
        INSTANCE.waypointModel().delete(w);
    }

    public WaypointModel getSync(long id) {
        return INSTANCE.waypointModel().getSync(id);
    }

    public List<WaypointModel> getAllSync() {
        return INSTANCE.waypointModel().getAllSync();
    }


    private static final Migration LEGACY = new Migration(1, 15) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.v("running migration from schema version 0->15: SKIP LEGACY SCHEMA VERSIONS");
        }
    };
    private static final Migration M_15_16 = new Migration(15, 16) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.v("running migration from schema version 15->16: MIGRATE LEGACY DATA");

            try {
                database.beginTransaction();
                database.execSQL("CREATE TABLE WaypointModel(id INTEGER PRIMARY KEY, description TEXT NOT NULL, geofenceLatitude REAL NOT NULL, geofenceLongitude REAL NOT NULL, geofenceRadius INTEGER NOT NULL)");
                database.execSQL("INSERT INTO WaypointModel(id, description, geofenceLatitude, geofenceLongitude, geofenceRadius) SELECT _id, DESCRIPTION, GEOFENCE_LATITUDE, GEOFENCE_LONGITUDE, GEOFENCE_RADIUS FROM WAYPOINT;");
                database.execSQL("DROP TABLE if exists WAYPOINT");
                database.setTransactionSuccessful();

            } catch (SQLException e) {
                Timber.e("not migrating existing waypoints");
            } finally {
                database.endTransaction();
            }

        }
    };

    private static final Migration M_16_17 = new Migration(16, 17) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            Timber.v("running migration from schema version 16->17: ADD LAST TRIGGERED/STATE");
            database.execSQL("ALTER TABLE  WaypointModel ADD lastTriggered INTEGER DEFAULT 0 NOT NULL");
            database.execSQL("ALTER TABLE  WaypointModel ADD lastTransition INTEGER DEFAULT 0 NOT NULL");
        }
    };
}