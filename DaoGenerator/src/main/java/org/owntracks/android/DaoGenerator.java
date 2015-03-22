package org.owntracks.android;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

// Generates Data Access Objects in src/main/java/org.owntracks.android/db
// Increase schema version if changes are made
// To generate files, open Gradle (View > Tool Windows > Gradle) tasks and chose android > :DaoGenerator > Tasks > other, right click "run"  and select Run '[run]'.

public class DaoGenerator {
    private static final int SCHEMA_VERSION = 2;

    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(SCHEMA_VERSION, "org.owntracks.android.db");
        schema.enableKeepSectionsByDefault();


        Entity contactLink = schema.addEntity("ContactLink");
        contactLink.addStringProperty("topic").primaryKey().unique();
        contactLink.addLongProperty("contactId");


        Entity waypoint = schema.addEntity("Waypoint");
        waypoint.addLongProperty("id").primaryKey();
        waypoint.addStringProperty("description");
        waypoint.addDoubleProperty("latitude");
        waypoint.addDoubleProperty("longitude");
        waypoint.addStringProperty("geocoder");
        waypoint.addBooleanProperty("shared");
        waypoint.addDateProperty("date");
        waypoint.addIntProperty("radius");
        waypoint.addIntProperty("transitionType");
        waypoint.addStringProperty("geofenceId");

        new de.greenrobot.daogenerator.DaoGenerator().generateAll(schema, args[0]);
    }
}
