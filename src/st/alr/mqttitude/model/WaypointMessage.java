
package st.alr.mqttitude.model;

import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.db.Waypoint;

public class WaypointMessage {
    Waypoint waypoint;

    public WaypointMessage(Waypoint w) {
        this.waypoint = w;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("{");
        builder.append("\"_type\": ").append("\"").append("waypoint").append("\"");
        builder.append(", \"desc\": ").append("\"").append(this.waypoint.getDescription()).append("\"");
        builder.append(", \"lat\": ").append("\"").append(this.waypoint.getLatitude()).append("\"");
        builder.append(", \"lon\": ").append("\"").append(this.waypoint.getLongitude()).append("\"");
        builder.append(", \"tst\": ").append("\"").append((int) (TimeUnit.MILLISECONDS.toSeconds(this.waypoint.getDate().getTime()))).append("\"");
        builder.append(", \"rad\": ").append("\"").append(this.waypoint.getRadius() != null ? this.waypoint.getRadius() : 0).append("\"");

        return builder.append("}").toString();

    }
}
