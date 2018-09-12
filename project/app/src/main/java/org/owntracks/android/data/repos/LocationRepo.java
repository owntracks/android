package org.owntracks.android.data.repos;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

public class LocationRepo {
    private final EventBus eventBus;
    private Location currentLocation;

    public LocationRepo(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setCurrentLocation(@NonNull Location l) {
        this.currentLocation = l;
        eventBus.postSticky(l);
    }

    @Nullable
    public Location getCurrentLocation() {
        return this.currentLocation;
    }

    public boolean hasLocation() {
        return this.currentLocation != null;
    }

    public long getCurrentLocationTime() {
        return hasLocation() ? this.currentLocation.getTime() : 0;
    }
}
