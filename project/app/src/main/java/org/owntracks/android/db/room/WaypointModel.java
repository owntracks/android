package org.owntracks.android.db.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;
import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

@Entity
public class WaypointModel {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    @NonNull
    @SuppressWarnings("NullableProblems")
    private String description;
    private double geofenceLatitude;
    private double geofenceLongitude;
    private int geofenceRadius;
    private long lastTriggered;
    private int lastTransition;

    @Ignore
    public WaypointModel() {
        setId(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    public WaypointModel(long id, @NonNull String description, double geofenceLatitude, double geofenceLongitude, int geofenceRadius) {
        this.id = id;
        this.description = description;
        this.geofenceLatitude = geofenceLatitude;
        this.geofenceLongitude = geofenceLongitude;
        this.geofenceRadius = geofenceRadius;
    }

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public double getGeofenceLatitude() {
        return geofenceLatitude;
    }

    public void setGeofenceLatitude(double geofenceLatitude) {
        if(geofenceLatitude > 90)
            this.geofenceLatitude = 90;
        if(geofenceLatitude < -90)
            this.geofenceLatitude = -90;
        else
            this.geofenceLatitude = geofenceLatitude;
    }

    public double getGeofenceLongitude() {
        return geofenceLongitude;
    }

    public void setGeofenceLongitude(double geofenceLongitude) {
        if(geofenceLongitude > 180 )
            this.geofenceLongitude = 180 ;
        if(geofenceLongitude < -180 )
            this.geofenceLongitude = -180 ;
        else
            this.geofenceLongitude = geofenceLongitude;
    }

    public int getGeofenceRadius() {
        return geofenceRadius;
    }

    public void setGeofenceRadius(int geofenceRadius) {
        this.geofenceRadius = geofenceRadius;
    }

    @NonNull
    public Location getLocation() {
        Location l= new Location("waypoint");
        l.setLatitude(getGeofenceLatitude());
        l.setLongitude(getGeofenceLongitude());
        l.setAccuracy(getGeofenceRadius());
        return l;
    }

    public long getLastTriggered() {
        return lastTriggered; // unit is seconds
    }

    public void setLastTriggered(long lastTriggered) {
        this.lastTriggered = lastTriggered;  // unit is seconds
    }

    @Ignore
    public void setLastTriggeredNow() {
        setLastTriggered(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    public int getLastTransition() {
        return this.lastTransition;
    }

    public void setLastTransition(int status) {
        this.lastTransition = status;
    }

    @Ignore
    public boolean isUnknown() {
        return this.lastTransition == 0;
    }

    @Ignore
    public boolean hasGeofence() {
        return geofenceRadius > 0;
    }

    @Ignore
    public long getTst() {
        return id; // unit is seconds
    }
}
