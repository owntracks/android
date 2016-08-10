package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static final String TAG = "Dao";
    private static WaypointDao waypointDao;
    private static SQLiteDatabase db;

    public static void initialize(Context c) {
        DaoMaster.DevOpenHelper helper1 = new DaoMaster.DevOpenHelper(c, NAME, null);
        db = helper1.getWritableDatabase();
        DaoMaster daoMaster1 = new DaoMaster(db);
        DaoSession daoSession1 = daoMaster1.newSession();
        waypointDao = daoSession1.getWaypointDao();

    }


    public static SQLiteDatabase getDb() { return db; }
    public static WaypointDao getWaypointDao() {  return waypointDao; }
}
