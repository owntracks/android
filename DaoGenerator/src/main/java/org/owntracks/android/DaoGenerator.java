package org.owntracks.android;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

// Generates Data Access Objects in src/main/java/org.owntracks.android/db
// Increase schema version if changes are made
// To generate files, open Gradle (View > Tool Windows > Gradle) tasks and chose android > :DaoGenerator > Tasks > application, right click "run"  and select Run.

public class DaoGenerator {
    private static final int SCHEMA_VERSION = 14;

    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(SCHEMA_VERSION, "org.owntracks.android.db");
        schema.enableKeepSectionsByDefault();

        Entity waypoint = schema.addEntity("Waypoint");
        waypoint.addIdProperty(); // For stable ids of cursor adapter
        waypoint.addStringProperty("description");
        waypoint.addDoubleProperty("geofenceLatitude");
        waypoint.addDoubleProperty("geofenceLongitude");
        waypoint.addIntProperty("geofenceRadius");
        waypoint.addStringProperty("geofenceId");
        waypoint.addStringProperty("wifiSSID");
        waypoint.addStringProperty("beaconUUID");
        waypoint.addIntProperty("beaconMajor");
        waypoint.addIntProperty("beaconMinor");
        waypoint.addBooleanProperty("shared");
        waypoint.addDateProperty("date");
        waypoint.addLongProperty("lastTriggered");
        waypoint.addIntProperty("modeId").notNull();
        waypoint.addIntProperty("type").notNull();


        new de.greenrobot.daogenerator.DaoGenerator().generateAll(schema, args[0]);
    }
}
