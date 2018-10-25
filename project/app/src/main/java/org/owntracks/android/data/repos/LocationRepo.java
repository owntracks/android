package org.owntracks.android.data.repos;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.location.Geofence;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.services.LocationProcessor;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

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
