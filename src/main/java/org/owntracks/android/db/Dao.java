package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public  class Dao {
    private static final String NAME = "org.owntracks.android.db";
    private static SQLiteDatabase db;
    private static ContactLinkDao contactLinkDao;
    private static WaypointDao waypointDao;
    private static MessageDao messageDao;

    public static void initialize(Context c) {

        DaoMaster.OpenHelper helper = new DaoMaster.OpenHelper(c, NAME, null) {
            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                DaoMaster.dropAllTables(db, true);
                onCreate(db);
            }
        };

        db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();

        contactLinkDao = daoSession.getContactLinkDao();
        waypointDao = daoSession.getWaypointDao();
        messageDao = daoSession.getMessageDao();
    }

    public static MessageDao getMessageDao() {
        return messageDao;
    }

    public static SQLiteDatabase getDb() { return db; }
    public static WaypointDao getWaypointDao() {  return waypointDao; }
    public static ContactLinkDao getContactLinkDao() {
        return contactLinkDao;
    }

}
