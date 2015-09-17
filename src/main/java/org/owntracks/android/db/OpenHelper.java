package org.owntracks.android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.mapbox.mapboxsdk.overlay.Marker;

import org.owntracks.android.model.GeocodableLocation;

import java.lang.ref.WeakReference;

public class OpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "OpenHelper";
    public static final String TABLE_LOCATIONS = "locations";
    public static final String COLUMN_TOPIC = "topic";
    public static final String COLUMN_TID = "tid";

    public static final String COLUMN_LINKNAME = "linkName";
    public static final String COLUMN_LINKFACE = "linkFace";
    public static final String COLUMN_CARDNAME = "cardName";
    public static final String COLUMN_CARDFACE = "cardFace";

    // Location cache
    public static final String COLUMN_LOC_LAT = "locLat";
    public static final String COLUMN_LOC_LON = "locLon";
    public static final String COLUMN_LOC_ACC = "locAcc";
    public static final String COLUMN_LOC_UPDATED = "locUpdated";
    public static final String COLUMN_LOC_GEOCODER = "locGeocoder";



    private static final String DATABASE_NAME = "locations.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_LOCATIONS + "(" + COLUMN_TOPIC
            + " st primary key autoincrement, " + COLUMN_TOPIC + " text not null, );"   ;

    public OpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "  + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        onCreate(db);
    }


}
