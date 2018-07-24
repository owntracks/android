package org.owntracks.android.db.room;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.support.ObservableMutableLiveData;

import java.util.List;

import timber.log.Timber;

@Database(entities = {WaypointModel.class}, version = 2, exportSchema = false)
public abstract class WaypointsDatabase extends RoomDatabase  {

    private static WaypointsDatabase INSTANCE;
    private static String DBNAME = "org.owntracks.android.db";
    public static WaypointsDatabase getDatabase(@AppContext Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, WaypointsDatabase.class, "DBNAME").addMigrations(MIGRATION_1_2).build();
        }
        return INSTANCE;
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


    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            //NOOP
        }
    };

}