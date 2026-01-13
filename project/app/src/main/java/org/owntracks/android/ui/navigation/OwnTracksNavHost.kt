package org.owntracks.android.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Main navigation host for the OwnTracks app.
 *
 * This is currently a skeleton implementation. As screens are migrated from
 * Activities/Fragments to Compose, they will be added here.
 *
 * Usage:
 * ```
 * setContent {
 *     OwnTracksTheme {
 *         OwnTracksNavHost()
 *     }
 * }
 * ```
 */
@Composable
fun OwnTracksNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destination.Map.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Destination.Map.route) {
            // TODO: Replace with MapScreen when migrated
            PlaceholderScreen("Map")
        }

        composable(Destination.Contacts.route) {
            // TODO: Replace with ContactsScreen when migrated
            PlaceholderScreen("Contacts")
        }

        composable(Destination.Waypoints.route) {
            // TODO: Replace with WaypointsScreen when migrated
            PlaceholderScreen("Waypoints")
        }

        composable(Destination.Status.route) {
            // TODO: Replace with StatusScreen when migrated
            PlaceholderScreen("Status")
        }

        composable(Destination.Preferences.route) {
            // TODO: Replace with PreferencesScreen when migrated
            PlaceholderScreen("Preferences")
        }

        composable(Destination.About.route) {
            // TODO: Replace with AboutScreen when migrated
            PlaceholderScreen("About")
        }

        composable(Destination.Welcome.route) {
            // TODO: Replace with WelcomeScreen when migrated
            PlaceholderScreen("Welcome")
        }

        composable(Destination.LogViewer.route) {
            // TODO: Replace with LogViewerScreen when migrated
            PlaceholderScreen("Log Viewer")
        }

        composable(Destination.Editor.route) {
            // TODO: Replace with EditorScreen when migrated
            PlaceholderScreen("Editor")
        }

        composable(
            route = Destination.Waypoint.route,
            arguments = listOf(
                navArgument(Destination.Waypoint.ARG_WAYPOINT_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val waypointId = backStackEntry.arguments?.getLong(Destination.Waypoint.ARG_WAYPOINT_ID) ?: 0L
            // TODO: Replace with WaypointScreen when migrated
            PlaceholderScreen("Waypoint: $waypointId")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$name Screen - Not yet migrated to Compose")
    }
}
