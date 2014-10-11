package org.owntracks.android;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

// Generates Data Access Objects in src/main/java/st/alr/mqttitude/db
// Increase schema version if changes are made
// To generate files, open gGradle tasks and double click android > :DaoGenerator > run

public class DaoGenerator {
    private static final int SCHEMA_VERSION = 1;

    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(SCHEMA_VERSION, "st.alr.mqttitude.db");
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
        waypoint.addFloatProperty("radius");
        waypoint.addIntProperty("transitionType");
        waypoint.addStringProperty("geofenceId");

        new de.greenrobot.daogenerator.DaoGenerator().generateAll(schema, args[0]);
    }
}
