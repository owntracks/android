package org.owntracks.android.services

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.datetime.Clock
import org.owntracks.android.BaseApp.Companion.NOTIFICATION_CHANNEL_ONGOING
import org.owntracks.android.BaseApp.Companion.NOTIFICATION_ID_ONGOING
import org.owntracks.android.R
import org.owntracks.android.data.EndpointState
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.ui.map.MapActivity
import timber.log.Timber

class OngoingNotification(private val context: Context, initialMode: MonitoringMode) {
  data class ServiceNotificationState(
      val title: String,
      val content: String,
      val subText: String,
      val notificationHigherPriority: Boolean
  )

  private val notificationManagerCompat = NotificationManagerCompat.from(context)
  private val resultIntent by lazy {
    Intent(context, MapActivity::class.java)
        .setAction("android.intent.action.MAIN")
        .addCategory("android.intent.category.LAUNCHER")
        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
  }
  private val resultPendingIntent by lazy {
    PendingIntent.getActivity(
        context, 0, resultIntent, BackgroundService.UPDATE_CURRENT_INTENT_FLAGS)
  }
  private val publishPendingIntent by lazy {
    PendingIntent.getService(
        context,
        0,
        Intent().setAction(BackgroundService.INTENT_ACTION_SEND_LOCATION_USER),
        BackgroundService.UPDATE_CURRENT_INTENT_FLAGS)
  }
  private val changeMonitoringPendingIntent by lazy {
    PendingIntent.getService(
        context,
        0,
        Intent().setAction(BackgroundService.INTENT_ACTION_CHANGE_MONITORING),
        BackgroundService.UPDATE_CURRENT_INTENT_FLAGS)
  }
  private var serviceNotificationState =
      ServiceNotificationState(
          context.getString(R.string.app_name), "", getMonitoringLabel(initialMode), false)

  private val notificationBuilder =
      NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ONGOING)
          .setOngoing(true)
          .setContentIntent(resultPendingIntent)
          .setStyle(NotificationCompat.BigTextStyle())
          .addAction(
              R.drawable.ic_add_location_alt,
              context.getString(R.string.publish),
              publishPendingIntent)
          .addAction(
              R.drawable.ic_owntracks_80,
              context.getString(R.string.notificationChangeMonitoring),
              changeMonitoringPendingIntent)
          .setSmallIcon(R.drawable.ic_owntracks_80)
          .setSound(null, AudioManager.STREAM_NOTIFICATION)
          .setColor(context.getColor(R.color.OTPrimaryBlue))
          .setCategory(NotificationCompat.CATEGORY_SERVICE)
          .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

  fun getNotification() =
      notificationBuilder
          .setContentTitle(serviceNotificationState.title)
          .setContentText(serviceNotificationState.content)
          .setWhen(Clock.System.now().toEpochMilliseconds())
          .setSubText(serviceNotificationState.subText)
          .setPriority(
              if (serviceNotificationState.notificationHigherPriority) {
                NotificationCompat.PRIORITY_DEFAULT
              } else {
                NotificationCompat.PRIORITY_MIN
              })
          .build()

  private fun updateNotification() {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED) {
      notificationManagerCompat.notify(NOTIFICATION_ID_ONGOING, getNotification())
    } else {
      Timber.w(
          "Tried to update ongoing notification with $this but notification permissions were missing")
    }
  }

  fun setEndpointState(endpointState: EndpointState, host: String) {
    val notificationContent =
        when (endpointState) {
          EndpointState.CONNECTED,
          EndpointState.IDLE ->
              context.getString(
                  R.string.notificationEndpointStateConnected,
                  context.resources.getString(R.string.CONNECTED),
                  host)
          EndpointState.ERROR ->
              if (endpointState.error != null)
                  "${endpointState.getLabel(context)}: ${endpointState.getErrorLabel(context)}"
              else endpointState.getLabel(context)
          else -> endpointState.getLabel(context)
        }
    serviceNotificationState = serviceNotificationState.copy(content = notificationContent)
    updateNotification()
  }

  private fun getMonitoringLabel(monitoringMode: MonitoringMode) =
      context.run {
        when (monitoringMode) {
          MonitoringMode.Quiet -> getString(R.string.monitoring_quiet)
          MonitoringMode.Manual -> getString(R.string.monitoring_manual)
          MonitoringMode.Significant -> getString(R.string.monitoring_significant)
          MonitoringMode.Move -> getString(R.string.monitoring_move)
        }
      }

  fun setMonitoringMode(monitoringMode: MonitoringMode) {
    serviceNotificationState =
        serviceNotificationState.copy(subText = getMonitoringLabel(monitoringMode))
    updateNotification()
  }

  fun setTitle(title: String) {
    serviceNotificationState = serviceNotificationState.copy(title = title)
    updateNotification()
  }
}
