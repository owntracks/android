package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.owntracks.android.support.Preferences;

import java.util.List;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static final String TAG = "Dao";
    private org.owntracks.android.db.WaypointDao waypointDao;
    private SQLiteDatabase db;
    private Preferences preferences;

    public Dao(Context c, Preferences p) {
        this.preferences = p;
        initialize(c);
    }
    private void initialize(Context c) {
        org.owntracks.android.db.DaoMaster.DevOpenHelper helper1 = new org.owntracks.android.db.DaoMaster.DevOpenHelper(c, NAME, null);
        db = helper1.getWritableDatabase();
        org.owntracks.android.db.DaoMaster daoMaster1 = new org.owntracks.android.db.DaoMaster(db);
        org.owntracks.android.db.DaoSession daoSession1 = daoMaster1.newSession();
        waypointDao = daoSession1.getWaypointDao();
    }


    public SQLiteDatabase getDb() { return db; }
    public org.owntracks.android.db.WaypointDao getWaypointDao() {  return waypointDao; }

    public List<Waypoint> loadWaypointsForCurrentMode() {
        return loadWaypointsForModeId(preferences.getModeId());
    }

    public List<Waypoint> loadWaypointsForModeId(int modeId) {
        return this.waypointDao.queryBuilder().where(org.owntracks.android.db.WaypointDao.Properties.ModeId.eq(modeId)).build().list();
    }

    public List<Waypoint> loadWaypointsForCurrentModeWithValidGeofence() {
        return loadWaypointsForModeIdWithValidGeofence(preferences.getModeId());
    }

    public List<Waypoint> loadWaypointsForModeIdWithValidGeofence(int modeId) {
        return this.waypointDao.queryBuilder().where(org.owntracks.android.db.WaypointDao.Properties.ModeId.eq(modeId), org.owntracks.android.db.WaypointDao.Properties.GeofenceLatitude.isNotNull(), org.owntracks.android.db.WaypointDao.Properties.GeofenceLongitude.isNotNull(), org.owntracks.android.db.WaypointDao.Properties.GeofenceRadius.isNotNull(), org.owntracks.android.db.WaypointDao.Properties.GeofenceRadius.gt(0)).build().list();
    }

    public Waypoint loadWaypointForGeofenceId(String geofenceId) {
        return this.waypointDao.queryBuilder().where(org.owntracks.android.db.WaypointDao.Properties.GeofenceId.eq(geofenceId)).limit(1).unique();
    }
}
