package org.owntracks.android.data.repos;

import android.arch.lifecycle.LiveData;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.support.Events;

import java.util.List;


public abstract class WaypointsRepo {
    protected EventBus eventBus;
    public WaypointsRepo(EventBus eventBus) {
        this.eventBus = eventBus;
    }
    public abstract WaypointModel getSync(long id);
    public abstract List<WaypointModel> getAllSync();

    public abstract LiveData<WaypointModel> get(long id);
    public abstract LiveData<List<WaypointModel>> getAll();

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

    abstract void insert_impl(WaypointModel w);
    abstract void update_impl(WaypointModel w);
    abstract void delete_impl(WaypointModel w);

}