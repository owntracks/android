package org.owntracks.android.data.repos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.objectbox.Box
import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.exception.UniqueViolationException
import io.objectbox.query.Query
import org.owntracks.android.data.MyObjectBox
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.WaypointModel_
import org.owntracks.android.support.Preferences
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectboxWaypointsRepo @Inject constructor(
        @ApplicationContext context: Context,
        private val preferences: Preferences,
) : WaypointsRepo() {
    private val box: Box<WaypointModel>

    init {
        val boxStore = MyObjectBox.builder().androidContext(context).build()
        box = boxStore.boxFor(WaypointModel::class.java)
        if (!preferences.isObjectboxMigrated) {
            migrateLegacyData(context)
        }
    }

    private class LegacyOpenHelper(context: Context?) :
            SQLiteOpenHelper(context, "org.owntracks.android.db", null, 15) {
        override fun onCreate(db: SQLiteDatabase) {}
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    private fun migrateLegacyData(context: Context) {
        try {
            val table = "WAYPOINT"
            val columns = arrayOf(
                    "DATE",
                    "DESCRIPTION",
                    "GEOFENCE_RADIUS",
                    "GEOFENCE_LATITUDE",
                    "GEOFENCE_LONGITUDE"
            )
            val columnList = Arrays.asList(*columns)
            try {
                LegacyOpenHelper(context).use { helper ->
                    helper.readableDatabase.use { db ->
                        db.query(table, columns, null, null, null, null, null, null).use { cursor ->
                            if (cursor.moveToFirst()) {
                                do {
                                    try {
                                        val w = WaypointModel(
                                                0,
                                                cursor.getLong(columnList.indexOf("DATE")),
                                                cursor.getString(columnList.indexOf("DESCRIPTION")),
                                                cursor.getDouble(columnList.indexOf("GEOFENCE_LATITUDE")),
                                                cursor.getDouble(columnList.indexOf("GEOFENCE_LONGITUDE")),
                                                cursor.getInt(columnList.indexOf("GEOFENCE_RADIUS")),
                                                0,
                                                0
                                        )
                                        Timber.v("Migration for model %s", w.toString())
                                        insertImpl(w)
                                    } catch (exception: UniqueViolationException) {
                                        Timber.v("UniqueViolationException during insert")
                                    }
                                } while (cursor.moveToNext())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during migration")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during migration")
        } finally {
            preferences.setObjectBoxMigrated()
        }
    }

    override fun get(tst: Long): WaypointModel? {
        return box.query().equal(WaypointModel_.tst, tst).build().findUnique()
    }

    override val all: List<WaypointModel>
        get() = box.all
    override val allWithGeofences: List<WaypointModel>
        get() = box.query().greater(WaypointModel_.geofenceRadius, 0L).and()
                .between(WaypointModel_.geofenceLatitude, -90, 90).and()
                .between(WaypointModel_.geofenceLongitude, -180, 180).build().find()
    override val allLive: ObjectBoxLiveData<WaypointModel>
        get() = ObjectBoxLiveData(allQuery)
    override val allQuery: Query<WaypointModel>
        get() = box.query().order(WaypointModel_.description).build()

    public override fun insertImpl(waypoint: WaypointModel) {
        box.put(waypoint)
    }

    public override fun updateImpl(waypoint: WaypointModel) {
        box.put(waypoint)
    }

    public override fun deleteImpl(waypoint: WaypointModel) {
        box.remove(waypoint)
    }

    override fun reset() {
        box.removeAll()
    }
}