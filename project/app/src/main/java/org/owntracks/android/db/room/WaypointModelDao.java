package org.owntracks.android.db.room;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Update;
import android.os.AsyncTask;
import android.support.annotation.WorkerThread;

import com.google.android.gms.location.Geofence;

import org.owntracks.android.support.ObservableMutableLiveData;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface WaypointModelDao {

    @Query("select * from WaypointModel")
    LiveData<List<WaypointModel>> getAll();


    @Query("select * from WaypointModel where id = :id")
    LiveData<WaypointModel> getById(long id);

    @Query("select * from WaypointModel where id = :id")
    @WorkerThread
    WaypointModel getByIdSync(long id);

    @Insert(onConflict = REPLACE)
    void insert(WaypointModel waypoint);

    @Update(onConflict = REPLACE)
    void update(WaypointModel waypoint);

    @Delete
    void delete(WaypointModel waypoint);
}