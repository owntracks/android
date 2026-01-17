package org.owntracks.android.ui.navigation

import org.owntracks.android.ui.contacts.ContactsActivity
import org.owntracks.android.ui.map.MapActivity
import org.owntracks.android.ui.preferences.PreferencesActivity
import org.owntracks.android.ui.waypoints.WaypointsActivity

/**
 * Navigation destinations for the OwnTracks app.
 * Used with Jetpack Navigation Compose.
 */
sealed class Destination(val route: String) {
    data object Map : Destination("map")
    data object Contacts : Destination("contacts")
    data object Waypoints : Destination("waypoints")
    data object Status : Destination("status")
    data object Preferences : Destination("preferences")
    data object About : Destination("about")
    data object Welcome : Destination("welcome")
    data object LogViewer : Destination("log_viewer")
    data object Editor : Destination("editor")

    // Destinations with arguments
    data object Waypoint : Destination("waypoint/{waypointId}") {
        fun createRoute(waypointId: Long) = "waypoint/$waypointId"
        const val ARG_WAYPOINT_ID = "waypointId"
    }
}

/**
 * Top-level destinations shown in the main navigation (bottom nav or similar)
 */
val topLevelDestinations = listOf(
    Destination.Map,
    Destination.Contacts,
    Destination.Waypoints,
    Destination.Status
)

/**
 * Extension function to map a Destination to its corresponding Activity class.
 * Used for navigation in the multi-activity architecture.
 */
fun Destination.toActivityClass(): Class<*>? = when (this) {
    Destination.Map -> MapActivity::class.java
    Destination.Contacts -> ContactsActivity::class.java
    Destination.Waypoints -> WaypointsActivity::class.java
    Destination.Preferences -> PreferencesActivity::class.java
    else -> null
}
