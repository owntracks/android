package org.owntracks.android.ui.waypoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.owntracks.android.data.waypoints.WaypointModel
import org.owntracks.android.data.waypoints.WaypointsRepo
import org.owntracks.android.services.LocationProcessor
import javax.inject.Inject

@HiltViewModel
class WaypointsViewModel
@Inject
constructor(waypointsRepo: WaypointsRepo, private val locationProcessor: LocationProcessor) :
    ViewModel() {

  val waypoints: StateFlow<List<WaypointModel>> = waypointsRepo.allLive
      .stateIn(
          scope = viewModelScope,
          started = SharingStarted.Lazily,
          initialValue = emptyList()
      )


  fun exportWaypoints() {
    viewModelScope.launch { locationProcessor.publishWaypointsMessage() }
  }
//  fun migrateWaypoints() {
//    Timber.v("UnIdling migrationIdlingResource")
////    migrationIdlingResource.setIdleState(false)
//    waypointsRepo.migrateFromLegacyStorage(viewModelScope,
//        Dispatchers.IO).invokeOnCompletion { throwable ->
////      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
////        PackageManager.PERMISSION_GRANTED) {
////        throwable?.run {
////          Timber.e(throwable, "Error migrating waypoints")
////          NotificationCompat.Builder(
////              applicationContext, GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID)
////              .setContentTitle(getString(R.string.waypointMigrationErrorNotificationTitle))
////              .setContentText(getString(R.string.waypointMigrationErrorNotificationText))
////              .setAutoCancel(true)
////              .setSmallIcon(R.drawable.ic_owntracks_80)
////              .setStyle(
////                  NotificationCompat.BigTextStyle()
////                      .bigText(getString(R.string.waypointMigrationErrorNotificationText)))
////              .setPriority(NotificationCompat.PRIORITY_LOW)
////              .setSilent(true)
////              .build()
////              .run { notificationManager.notify("WaypointsMigrationNotification", 0, this) }
////        }
////      } else if (throwable != null) {
////        Timber.w(
////            throwable,
////            "notification permissions not granted, can't display waypoints migration error notification")
////      }
////      Timber.v("Idling migrationIdlingResource")
////      migrationIdlingResource.setIdleState(true)
//    }
  }

