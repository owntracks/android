package org.owntracks.android.data.repos;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.injection.scopes.PerApplication;

import javax.inject.Inject;

@PerApplication
public class LocationRepo {
    private final EventBus eventBus;
    private Location currentLocation;

    @Inject
    public LocationRepo(EventBus eventBus) {
        this.eventBus = eventBus;
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

    public void setCurrentLocation(@NonNull Location l) {
        this.currentLocation = l;
        eventBus.postSticky(l);
    }
}
