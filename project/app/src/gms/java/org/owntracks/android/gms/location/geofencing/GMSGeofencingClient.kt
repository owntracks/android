package org.owntracks.android.gms.location.geofencing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import timber.log.Timber
import org.owntracks.android.location.geofencing.GeofencingClient
import org.owntracks.android.location.geofencing.GeofencingRequest
import org.owntracks.android.services.BackgroundService

class GMSGeofencingClient(
    private val geofencingClient: com.google.android.gms.location.GeofencingClient
) : GeofencingClient {
  override fun removeGeofences(context: Context) {
    this.geofencingClient.removeGeofences(getPendingIntent(context))
        .addOnSuccessListener { Timber.d("Geofences removed successfully") }
        .addOnFailureListener { Timber.e(it, "Failed to remove geofences") }
  }

  @RequiresPermission(anyOf = ["android.permission.ACCESS_FINE_LOCATION"])
  override fun addGeofences(request: GeofencingRequest, context: Context) {
    val gmsRequest = request.toGMSGeofencingRequest()
    Timber.d("Adding ${gmsRequest.geofences?.size ?: 0} geofences via GMS")
    this.geofencingClient.addGeofences(gmsRequest, getPendingIntent(context))
        .addOnSuccessListener { Timber.i("Geofences added successfully") }
        .addOnFailureListener { Timber.e(it, "Failed to add geofences") }
  }

  private fun getPendingIntent(context: Context): PendingIntent {
    val geofenceIntent = Intent(context, GeofencingBroadcastReceiver::class.java)
    geofenceIntent.action = BackgroundService.INTENT_ACTION_SEND_EVENT_CIRCULAR
    val intentFlags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }
    return PendingIntent.getBroadcast(context, 0, geofenceIntent, intentFlags)
  }

  companion object {
    fun create(context: Context): GeofencingClient {
      return GMSGeofencingClient(LocationServices.getGeofencingClient(context))
    }
  }
}
