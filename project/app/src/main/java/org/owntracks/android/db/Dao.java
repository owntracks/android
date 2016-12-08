package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static final String TAG = "Dao";
    private static org.owntracks.android.db.WaypointDao waypointDao;
    private static SQLiteDatabase db;

    public static void initialize(Context c) {
        org.owntracks.android.db.DaoMaster.DevOpenHelper helper1 = new org.owntracks.android.db.DaoMaster.DevOpenHelper(c, NAME, null);
        db = helper1.getWritableDatabase();
        org.owntracks.android.db.DaoMaster daoMaster1 = new org.owntracks.android.db.DaoMaster(db);
        org.owntracks.android.db.DaoSession daoSession1 = daoMaster1.newSession();
        waypointDao = daoSession1.getWaypointDao();

    }


    public static SQLiteDatabase getDb() { return db; }
    public static org.owntracks.android.db.WaypointDao getWaypointDao() {  return waypointDao; }
}
