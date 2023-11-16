package org.owntracks.android.preferences

import android.os.Build
import java.util.Locale
import kotlin.reflect.KProperty
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.preferences.types.ConnectionMode
import org.owntracks.android.preferences.types.MonitoringMode
import org.owntracks.android.preferences.types.MqttProtocolLevel
import org.owntracks.android.preferences.types.MqttQos
import org.owntracks.android.preferences.types.StringMaxTwoAlphaNumericChars

interface DefaultsProvider {
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    fun <T> getDefaultValue(preferences: Preferences, property: KProperty<*>): T {
        return when (property) {
            Preferences::autostartOnBoot -> true
            Preferences::cleanSession -> false
            Preferences::clientId -> (preferences.username + preferences.deviceId).replace(
                "\\W".toRegex(),
                ""
            )
                .lowercase(Locale.getDefault())
            Preferences::connectionTimeoutSeconds -> 30
            Preferences::debugLog -> false
            Preferences::deviceId ->
                Build.DEVICE
                    ?.replace(" ", "-")
                    ?.replace("[^a-zA-Z0-9]+".toRegex(), "")
                    ?.lowercase(Locale.getDefault())
                    ?: "unknown"
            Preferences::dontReuseHttpClient -> false
            Preferences::enableMapRotation -> true
            Preferences::encryptionKey -> ""
            Preferences::experimentalFeatures -> emptySet<String>()
            Preferences::fusedRegionDetection -> true
            Preferences::firstStart -> true
            Preferences::host -> ""
            Preferences::ignoreInaccurateLocations -> 0
            Preferences::ignoreStaleLocations -> 0f
            Preferences::info -> true
            Preferences::keepalive -> 3600
            Preferences::locatorDisplacement -> 500
            Preferences::locatorInterval -> 60
            Preferences::mode -> ConnectionMode.MQTT
            Preferences::monitoring -> MonitoringMode.SIGNIFICANT
            Preferences::moveModeLocatorInterval -> 10
            Preferences::mqttProtocolLevel -> MqttProtocolLevel.MQTT_3_1
            Preferences::notificationEvents -> true
            Preferences::notificationGeocoderErrors -> true
            Preferences::notificationHigherPriority -> false
            Preferences::notificationLocation -> true
            Preferences::opencageApiKey -> ""
            Preferences::osmTileScaleFactor -> 1.0f
            Preferences::password -> ""
            Preferences::pegLocatorFastestIntervalToInterval -> false
            Preferences::ping -> 15
            Preferences::port -> 8883
            Preferences::pubExtendedData -> true
            Preferences::pubQos -> MqttQos.ONE
            Preferences::pubRetain -> true
            Preferences::pubTopicBase -> "owntracks/%u/%d"
            Preferences::publishLocationOnConnect -> false
            Preferences::cmd -> true
            Preferences::remoteConfiguration -> false
            Preferences::setupCompleted -> false
            Preferences::showRegionsOnMap -> false
            Preferences::sub -> true
            Preferences::subQos -> MqttQos.TWO
            Preferences::subTopic -> DEFAULT_SUB_TOPIC
            Preferences::theme -> AppTheme.AUTO
            Preferences::tls -> true
            Preferences::tlsClientCrt -> ""
            Preferences::tlsClientCrtPassword -> ""
            Preferences::tid -> StringMaxTwoAlphaNumericChars(
                preferences.deviceId.takeLast(2)
                    .ifEmpty { "na" }
            )
            Preferences::url -> ""
            Preferences::userDeclinedEnableLocationPermissions -> false
            Preferences::userDeclinedEnableLocationServices -> false
            Preferences::userDeclinedEnableNotificationPermissions -> false
            Preferences::username -> ""
            Preferences::ws -> false
            else -> {
                throw Exception("No default defined for ${property.name}")
            }
        } as T
    }

    companion object {
        const val DEFAULT_SUB_TOPIC = "owntracks/+/+"
    }
}
