package org.owntracks.android.data.repos;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.MyObjectBox;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.WaypointModel_;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.support.Preferences;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.ObjectBoxLiveData;
import io.objectbox.exception.UniqueViolationException;
import io.objectbox.query.Query;
import timber.log.Timber;

public class ObjectboxWaypointsRepo extends WaypointsRepo  {
    private final Preferences preferences;
    private Box<org.owntracks.android.data.WaypointModel> box;

    public ObjectboxWaypointsRepo(@AppContext Context context, EventBus eventBus, Preferences preferences) {
        super(eventBus);
        BoxStore boxStore = MyObjectBox.builder().androidContext(context).build();
        this.box = boxStore.boxFor(org.owntracks.android.data.WaypointModel.class);
        this.preferences = preferences;
        if(!preferences.isObjectboxMigrated()) {
            migrateLegacyData(context);

        }
    }

    private static class LegacyOpenHelper extends SQLiteOpenHelper {

        LegacyOpenHelper(Context context) {
            super(context, "org.owntracks.android.db", null, 15);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private void migrateLegacyData(Context context) {
        try {

            LegacyOpenHelper helper = new LegacyOpenHelper(context);

            SQLiteDatabase db = helper.getReadableDatabase();

            String table = "WAYPOINT";
            String[] columns = {"DATE", "DESCRIPTION", "GEOFENCE_RADIUS", "GEOFENCE_LATITUDE", "GEOFENCE_LONGITUDE"};

            Cursor cursor = db.query(table, columns, null, null, null, null, null, null);
            try {
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            WaypointModel w = new WaypointModel(0, cursor.getLong(cursor.getColumnIndex("DATE")),
                                    cursor.getString(cursor.getColumnIndex("DESCRIPTION")),
                                    cursor.getDouble(cursor.getColumnIndex("GEOFENCE_LATITUDE")),
                                    cursor.getDouble(cursor.getColumnIndex("GEOFENCE_LONGITUDE")),
                                    cursor.getInt(cursor.getColumnIndex("GEOFENCE_RADIUS")), 0, 0);

                            Timber.v("Migration for model %s", w.toString());
                            insert_impl(w);
                        } catch (UniqueViolationException exception) {
                            Timber.v("UniqueViolationException during insert");
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Timber.e(e, "Error during migration");
            } finally {
                cursor.close();
                db.close();
            }
        } catch (Exception e) {
            Timber.e(e, "Error during migration");
        } finally {
            preferences.setObjectBoxMigrated();
        }
    }

    @Override
    public org.owntracks.android.data.WaypointModel get(long tst) {
        return this.box.query().equal(WaypointModel_.tst,tst).build().findUnique();
    }

    @Override
    public List<org.owntracks.android.data.WaypointModel> getAll() {
        return this.box.getAll();
    }

    @Override
    public List<WaypointModel> getAllWithGeofences() {
        return this.box.query().greater(WaypointModel_.geofenceRadius,0L).and().between(WaypointModel_.geofenceLatitude,-90, 90).and().between(WaypointModel_.geofenceLongitude,-180, 180).build().find();
    }

    @Override
    public ObjectBoxLiveData<WaypointModel> getAllLive() {
        return new ObjectBoxLiveData<>(getAllQuery());
    }

    @Override
    public Query<WaypointModel> getAllQuery() {
        return this.box.query().order(WaypointModel_.description).build();
    }

    @Override
    public void insert_impl(org.owntracks.android.data.WaypointModel w) {
        box.put(w);
    }

    @Override
    public void update_impl(org.owntracks.android.data.WaypointModel w) {
        box.put(w);
    }

    @Override
    public void delete_impl(org.owntracks.android.data.WaypointModel w) {
        box.remove(w);
    }
}
