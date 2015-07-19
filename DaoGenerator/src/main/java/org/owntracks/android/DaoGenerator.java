package org.owntracks.android;

import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;

// Generates Data Access Objects in src/main/java/org.owntracks.android/db
// Increase schema version if changes are made
// To generate files, open Gradle (View > Tool Windows > Gradle) tasks and chose android > :DaoGenerator > Tasks > other, right click "run"  and select Run '[run]'.

public class DaoGenerator {
    private static final int SCHEMA_VERSION = 8;

    public static void main(String args[]) throws Exception {

        Schema schema = new Schema(SCHEMA_VERSION, "org.owntracks.android.db");
        schema.enableKeepSectionsByDefault();


        Entity contactLink = schema.addEntity("ContactLink");
        contactLink.addIdProperty().columnName("_id");
        Property topic = contactLink.addStringProperty("topic").notNull().getProperty();
        contactLink.addLongProperty("contactId");
        Property modeId = contactLink.addIntProperty("modeId").notNull().getProperty();

        // GreenDao does not yet support compound primary keys. We create a unique index on the two columns instead
        Index compoundPk = new Index();
        compoundPk.addProperty(topic);
        compoundPk.addProperty(modeId);
        compoundPk.makeUnique();
        contactLink.addIndex(compoundPk);


        Entity waypoint = schema.addEntity("Waypoint");
        waypoint.addIdProperty(); // For stable ids of cursor adapter
        waypoint.addStringProperty("description");
        waypoint.addStringProperty("geocoder");
        waypoint.addDoubleProperty("latitude");
        waypoint.addDoubleProperty("longitude");
        waypoint.addIntProperty("radius");
        waypoint.addStringProperty("ssid");
        waypoint.addBooleanProperty("shared");
        waypoint.addDateProperty("date");
        waypoint.addLongProperty("lastTriggered");
        waypoint.addStringProperty("geofenceId");
        waypoint.addIntProperty("modeId").notNull();


        Entity message = schema.addEntity("Message");
        message.addIdProperty(); // For stable ids of cursor adapter
        message.addStringProperty("externalId").unique();
        message.addLongProperty("tst");
        message.addLongProperty("expiresTst");
        message.addStringProperty("channel");
        message.addStringProperty("sender");
        message.addStringProperty("title");
        message.addStringProperty("description");
        message.addStringProperty("icon");
        message.addIntProperty("priority");
        message.addStringProperty("iconUrl");
        message.addStringProperty("url");

        new de.greenrobot.daogenerator.DaoGenerator().generateAll(schema, args[0]);
    }
}
