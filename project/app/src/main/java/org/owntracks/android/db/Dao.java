package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.owntracks.android.App;
import org.owntracks.android.support.Preferences;

import java.util.Collections;
import java.util.List;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static final String TAG = "Dao";
    private org.owntracks.android.db.WaypointDao waypointDao;
    private SQLiteDatabase db;

    private void initialize() {
        org.owntracks.android.db.DaoMaster.DevOpenHelper helper1 = new org.owntracks.android.db.DaoMaster.DevOpenHelper(App.getContext(), NAME, null);
        helper1.setLoadSQLCipherNativeLibs(false);
        db = helper1.getWritableDatabase();
        org.owntracks.android.db.DaoMaster daoMaster1 = new org.owntracks.android.db.DaoMaster(db);
        org.owntracks.android.db.DaoSession daoSession1 = daoMaster1.newSession();
        waypointDao = daoSession1.getWaypointDao();
    }

    public SQLiteDatabase getDb() { return db; }
    public org.owntracks.android.db.WaypointDao getWaypointDao() {  return waypointDao; }
}
