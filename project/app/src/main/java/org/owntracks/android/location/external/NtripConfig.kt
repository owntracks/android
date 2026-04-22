package org.owntracks.android.location.external

import android.content.Context
import android.content.SharedPreferences

/** NTRIP caster parameters persisted via [SharedPreferences]. */
data class NtripConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 2101,
    val mountpoint: String = "",
    val user: String = "",
    val password: String = "",
    val serialBaud: Int = 115200,
) {
  val isReady: Boolean
    get() = host.isNotBlank() && mountpoint.isNotBlank()

  companion object {
    const val PREFS_NAME = "external_gnss_prefs"
    const val KEY_EXTERNAL_ENABLED = "external_gnss_enabled"
    const val KEY_NTRIP_ENABLED = "ntrip_enabled"
    const val KEY_HOST = "ntrip_host"
    const val KEY_PORT = "ntrip_port"
    const val KEY_MOUNTPOINT = "ntrip_mountpoint"
    const val KEY_USER = "ntrip_user"
    const val KEY_PASSWORD = "ntrip_password"
    const val KEY_SERIAL_BAUD = "serial_baud"

    fun load(context: Context): NtripConfig {
      val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return NtripConfig(
          enabled = p.getBoolean(KEY_NTRIP_ENABLED, false),
          host = p.getString(KEY_HOST, "") ?: "",
          port = p.readIntCompat(KEY_PORT, 2101),
          mountpoint = p.getString(KEY_MOUNTPOINT, "") ?: "",
          user = p.getString(KEY_USER, "") ?: "",
          password = p.getString(KEY_PASSWORD, "") ?: "",
          serialBaud = p.readIntCompat(KEY_SERIAL_BAUD, 115200),
      )
    }

    /**
     * EditTextPreference always stores values as [String], so reading them with
     * [SharedPreferences.getInt] throws [ClassCastException]. This helper tolerates either type.
     */
    private fun SharedPreferences.readIntCompat(key: String, defaultValue: Int): Int {
      if (!contains(key)) return defaultValue
      return try {
        getInt(key, defaultValue)
      } catch (_: ClassCastException) {
        getString(key, null)?.trim()?.toIntOrNull() ?: defaultValue
      }
    }

    fun isExternalGnssEnabled(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXTERNAL_ENABLED, false)
  }
}
