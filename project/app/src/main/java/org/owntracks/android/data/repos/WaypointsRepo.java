package org.owntracks.android.data.repos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.model.messages.MessageWaypoint;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageWaypointCollection;

import java.util.List;

import io.objectbox.android.ObjectBoxLiveData;
import io.objectbox.query.Query;
import timber.log.Timber;

public abstract class WaypointsRepo {
    private final EventBus eventBus;
    protected WaypointsRepo(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    public abstract WaypointModel get(long tst);
    protected abstract List<WaypointModel> getAll();
    public abstract List<WaypointModel> getAllWithGeofences();
    public abstract ObjectBoxLiveData<WaypointModel> getAllLive();
    public abstract Query<WaypointModel> getAllQuery();

    public void insert(WaypointModel w) {
        insert_impl(w);
        eventBus.post(new Events.WaypointAdded(w));

    }

    public void update(WaypointModel w, boolean notify) {
        update_impl(w);
        if(notify) {
            eventBus.post(new Events.WaypointUpdated(w));
        }
    }

    public void delete(WaypointModel w) {
        delete_impl(w);
        eventBus.post(new Events.WaypointRemoved(w));
    }

    public void importFromMessage(@Nullable MessageWaypointCollection waypoints) {
        if(waypoints == null)
            return;

        for (MessageWaypoint m: waypoints) {
            // Delete existing waypoint if one with the same tst already exists
            WaypointModel existing = get(m.getTimestamp());
            if(existing != null) {
                delete(existing);
            }
            // check if the latitude and longitude are valid, otherwise do not replace the waypoint
            if ((m.getLatitude() >= -90.0) && (m.getLatitude() <= 90.0) && (m.getLongitude() >= -180.0) && (m.getLongitude() <= 180.0)) {
                insert(toDaoObject(m));
            } else {
                // log a warning for the waypoint that isn't being imported
                Timber.w("Ignoring waypoint with invalid coordinates: %s", m.getDescription());
            }
        }
    }

    @NonNull
    public MessageWaypointCollection exportToMessage() {
        MessageWaypointCollection messages = new MessageWaypointCollection();
        for(WaypointModel waypoint : getAll()) {
            messages.add(fromDaoObject(waypoint));
        }
        return messages;
    }

    private WaypointModel toDaoObject(@NonNull MessageWaypoint messageWaypoint) {
        return new WaypointModel(0, messageWaypoint.getTimestamp(),messageWaypoint.getDescription(), messageWaypoint.getLatitude(), messageWaypoint.getLongitude(), messageWaypoint.getRadius() != null ? messageWaypoint.getRadius() : 0, 0, 0);
    }

    public MessageWaypoint fromDaoObject(@NonNull WaypointModel w) {
        MessageWaypoint message = new MessageWaypoint();
        message.setDescription(w.getDescription());
        message.setLatitude(w.getGeofenceLatitude());
        message.setLongitude(w.getGeofenceLongitude());
        message.setRadius(w.getGeofenceRadius());
        message.setTimestamp(w.getTst());
        return message;
    }

    protected abstract void insert_impl(WaypointModel w);
    protected abstract void update_impl(WaypointModel w);
    protected abstract void delete_impl(WaypointModel w);

}
