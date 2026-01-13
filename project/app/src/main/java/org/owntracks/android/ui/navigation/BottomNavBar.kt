package org.owntracks.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import org.owntracks.android.R

/**
 * Bottom navigation bar items for the main screens
 */
enum class BottomNavItem(
    val destination: Destination,
    val icon: ImageVector,
    val labelResId: Int
) {
    Map(
        destination = Destination.Map,
        icon = Icons.Default.Map,
        labelResId = R.string.title_activity_map
    ),
    Contacts(
        destination = Destination.Contacts,
        icon = Icons.Default.People,
        labelResId = R.string.title_activity_contacts
    ),
    Waypoints(
        destination = Destination.Waypoints,
        icon = Icons.Default.LocationOn,
        labelResId = R.string.title_activity_waypoints
    ),
    Preferences(
        destination = Destination.Preferences,
        icon = Icons.Default.Settings,
        labelResId = R.string.title_activity_preferences
    )
}

/**
 * Bottom navigation bar component for switching between main screens.
 *
 * @param currentDestination The currently selected destination
 * @param onNavigate Callback when a navigation item is clicked
 * @param modifier Optional modifier
 */
@Composable
fun BottomNavBar(
    currentDestination: Destination,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        BottomNavItem.entries.forEach { item ->
            NavigationBarItem(
                selected = currentDestination == item.destination,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelResId)
                    )
                },
                label = { Text(stringResource(item.labelResId)) }
            )
        }
    }
}
