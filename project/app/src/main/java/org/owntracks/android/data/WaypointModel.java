package org.owntracks.android.data;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.location.Location;
import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Unique;
import org.owntracks.android.BR;

@Entity
public class WaypointModel extends BaseObservable {

    @Id
    private long id;
    private String description = "";
    private double geofenceLatitude = 0.0;
    private double geofenceLongitude = 0.0;
    private int geofenceRadius = 0;
    private long lastTriggered = 0;
    private int lastTransition = 0;
    @Unique
    @Index
    private long tst = 0;

    public WaypointModel() {
        setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    public WaypointModel(long id, long tst, @NonNull String description, double geofenceLatitude, double geofenceLongitude, int geofenceRadius, int lastTransition, long lastTriggered) {
        this.id = id;
        this.tst = tst;
        this.description = description;
        this.geofenceLatitude = geofenceLatitude;
        this.geofenceLongitude = geofenceLongitude;
        this.geofenceRadius = geofenceRadius;
        this.lastTransition = lastTransition;
        this.lastTriggered = lastTriggered;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTst() {
        return tst; // unit is seconds
    }
    public void setTst(long tst) {
        this.tst = tst;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    @Bindable
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
        notifyPropertyChanged(BR.geofenceLatitude);
    }

    @Bindable
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
        notifyPropertyChanged(BR.geofenceLongitude);
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

    public void setLastTriggeredNow() {
        setLastTriggered(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    public int getLastTransition() {
        return this.lastTransition;
    }

    public void setLastTransition(int status) {
        this.lastTransition = status;
    }

    public boolean isUnknown() {
        return this.lastTransition == 0;
    }

    public boolean hasGeofence() {
        return geofenceRadius > 0;
    }

    public String toString() {
        return "WaypointModel("+getId()+","+getTst()+","+getDescription()+")";
    }
}
