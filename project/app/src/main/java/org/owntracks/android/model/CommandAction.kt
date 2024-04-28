package org.owntracks.android.model

import com.fasterxml.jackson.annotation.JsonValue

enum class CommandAction(@JsonValue val value: String) {
  /** The owntracks model for command actions */
  REPORT_LOCATION("reportLocation"),
  SET_WAYPOINTS("setWaypoints"),
  CLEAR_WAYPOINTS("clearWaypoints"),
  SET_CONFIGURATION("setConfiguration"),
  WAYPOINTS("waypoints")
}
