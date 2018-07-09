package org.owntracks.android.db.room;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

@Entity
public class WaypointModel {
    @PrimaryKey(autoGenerate = true)
    private long id;

    //@NonNull
    private String description;
    private double geofenceLatitude;
    private double geofenceLongitude;
    private int geofenceRadius;


    @Ignore
    public WaypointModel() {
        setId(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }


    public WaypointModel(long id, String description, double geofenceLatitude, double geofenceLongitude, int geofenceRadius) {
        this.id = id;
        this.description = description;
        this.geofenceLatitude = geofenceLatitude;
        this.geofenceLongitude = geofenceLongitude;
        this.geofenceRadius = geofenceRadius;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
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

    @Nullable
    public Integer getGeofenceRadius() {
        return geofenceRadius;
    }

    public void setGeofenceRadius(Integer geofenceRadius) {
        this.geofenceRadius = geofenceRadius;
    }

    @NonNull
    public Location getLocation() {
        Location l= new Location("waypoint");
        l.setLatitude(getGeofenceLatitude());
        l.setLongitude(getGeofenceLongitude());
        l.setAccuracy(0);
        return l;
    }
    @Ignore
        private long lastTriggered;

    public void setGeofenceRadius(int geofenceRadius) {
        this.geofenceRadius = geofenceRadius;
    }

    @Ignore
    private int modeId;

        //@Transient
        @Ignore
        private int lastTransition;

        @Ignore
        public long getLastTriggered() {
            return lastTriggered;
        }

        public void setLastTriggered(long lastTriggered) {
            this.lastTriggered = lastTriggered;
        }

    @Ignore
    public int getModeId() {
            return modeId;
        }


        public void setModeId(int modeId) {
            this.modeId = modeId;
        }


        public void setLastTransition(int status) {
            this.lastTransition = status;
        }

    @Ignore
    public int getLastTransition() {
            return this.lastTransition;
        }

        public boolean isUnknown() {
            return this.lastTransition == 0;
        }

        public boolean hasGeofence() {
            return (getGeofenceRadius() != null) && (getGeofenceRadius() > 0);
        }

    }
