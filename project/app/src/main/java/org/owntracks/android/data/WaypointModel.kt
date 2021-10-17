package org.owntracks.android.data

import android.location.Location
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique
import org.owntracks.android.BR
import org.owntracks.android.model.messages.MessageWaypoint
import java.util.concurrent.TimeUnit

@Entity
class WaypointModel : BaseObservable {
    @Id
    var id: Long = 0
    var description = ""

    @Bindable
    var geofenceLatitude = 0.0
        set(value) {
            field = value.coerceAtLeast(-90.0).coerceAtMost(90.0)
            notifyPropertyChanged(BR.geofenceLatitude)
        }

    @Bindable
    var geofenceLongitude = 0.0
        set(value) {
            field = value.coerceAtLeast(-180.0).coerceAtMost(180.0)
            notifyPropertyChanged(BR.geofenceLongitude)
        }
    var geofenceRadius = 0

    // unit is seconds
    var lastTriggered: Long = 0
        private set
    var lastTransition = 0

    // unit is seconds
    @Unique
    @Index
    var tst: Long = 0
        private set

    constructor() {
        tst = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }

    constructor(
            id: Long,
            tst: Long,
            description: String,
            geofenceLatitude: Double,
            geofenceLongitude: Double,
            geofenceRadius: Int,
            lastTransition: Int,
            lastTriggered: Long
    ) {
        this.id = id
        this.tst = tst
        this.description = description
        this.geofenceLatitude = geofenceLatitude
        this.geofenceLongitude = geofenceLongitude
        this.geofenceLongitude = geofenceLongitude
        this.geofenceRadius = geofenceRadius
        this.lastTransition = lastTransition
        this.lastTriggered = lastTriggered
    }

    // User has entered something that can't be converted to a double
    // TODO: figure out validation feeback
    @get:Bindable
    var geofenceLatitudeAsStr: String
        get() = geofenceLatitude.toString()
        set(value) {
            try {
                geofenceLatitude = value.toDouble()
            } catch (e: NumberFormatException) {
                // User has entered something that can't be converted to a double
                // TODO: figure out validation feeback
            }
        }

    // User has entered something that can't be converted to a double
    // TODO: figure out validation feeback
    @get:Bindable
    var geofenceLongitudeAsStr: String
        get() = geofenceLongitude.toString()
        set(value) {
            try {
                geofenceLongitude = value.toDouble()
            } catch (e: NumberFormatException) {
                // User has entered something that can't be converted to a double
                // TODO: figure out validation feeback
            }
        }

    @get:Bindable
    var geofenceRadiusAsStr: String
        get() = geofenceRadius.toString()
        set(value) {
            try {
                geofenceRadius = value.toInt()
            } catch (e: java.lang.NumberFormatException) {

            }
        }


    val location: Location
        get() {
            val l = Location("waypoint")
            l.latitude = geofenceLatitude
            l.longitude = geofenceLongitude
            l.accuracy = geofenceRadius.toFloat()
            return l
        }

    fun setLastTriggeredNow() {
        lastTriggered = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }

    val isUnknown: Boolean
        get() = lastTransition == 0

    override fun toString(): String {
        return "WaypointModel($id,$tst,$description)"
    }

    fun toMessageWaypoint(): MessageWaypoint = MessageWaypoint().apply {
        description = this@WaypointModel.description
        latitude = geofenceLatitude
        longitude = geofenceLongitude
        radius = geofenceRadius
        timestamp = tst
    }
}