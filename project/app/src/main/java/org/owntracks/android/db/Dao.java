package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.owntracks.android.support.Preferences;

import java.util.Collections;
import java.util.List;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static final String TAG = "Dao";
    private org.owntracks.android.db.WaypointDao waypointDao;
    private SQLiteDatabase db;
    private final Preferences preferences;

    public Dao(Context c, Preferences p) {
        this.preferences = p;
        initialize(c);
    }
    private void initialize(Context c) {
        org.owntracks.android.db.DaoMaster.DevOpenHelper helper1 = new org.owntracks.android.db.DaoMaster.DevOpenHelper(c, NAME, null);
        helper1.setLoadSQLCipherNativeLibs(false);
        db = helper1.getWritableDatabase();
        org.owntracks.android.db.DaoMaster daoMaster1 = new org.owntracks.android.db.DaoMaster(db);
        org.owntracks.android.db.DaoSession daoSession1 = daoMaster1.newSession();
        waypointDao = daoSession1.getWaypointDao();
    }

    public SQLiteDatabase getDb() { return db; }
    public org.owntracks.android.db.WaypointDao getWaypointDao() {  return waypointDao; }

    @NonNull
    public List<Waypoint> loadWaypointsForCurrentMode() {
        return loadWaypointsForModeId(preferences.getModeId());
    }

    @NonNull
    public List<Waypoint> loadWaypointsForModeId(int modeId) {
        return safeList(this.waypointDao.queryBuilder().where(org.owntracks.android.db.WaypointDao.Properties.ModeId.eq(modeId)).build().list());
    }

    public Waypoint loadWaypointForId(String l) {
        return this.waypointDao.load(Long.parseLong(l));
    }

    @NonNull
    private static List<Waypoint> safeList( List<Waypoint> other ) {
        return other == null ? Collections.<Waypoint>emptyList() : other;
    }


}
