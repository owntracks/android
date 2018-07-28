package org.owntracks.android.db.room;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.WorkerThread;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
interface WaypointModelDao {
    @Query("select * from WaypointModel")
    LiveData<List<WaypointModel>> getAll();

    @Query("select * from WaypointModel")
    List<WaypointModel> getAllSync();


    @Query("select * from WaypointModel where id = :id")
    LiveData<WaypointModel> getById(long id);

    @WorkerThread
    @Query("select * from WaypointModel where id = :id")
    WaypointModel getSync(long id);

    @Insert(onConflict = REPLACE)
    void insert(WaypointModel waypoint);

    @Update(onConflict = REPLACE)
    void update(WaypointModel waypoint);

    @Delete
    void delete(WaypointModel waypoint);
}