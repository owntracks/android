package org.owntracks.android.ui.navigation

import android.app.Activity
import android.content.Intent
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import org.owntracks.android.data.repos.ContactsRepoChange
import org.owntracks.android.di.ComposablesEntryPoint
import org.owntracks.android.model.Contact
import org.owntracks.android.preferences.Preferences
import org.owntracks.android.preferences.types.AppTheme
import org.owntracks.android.services.BackgroundService
import org.owntracks.android.ui.contacts.ContactsScreenContent
import org.owntracks.android.ui.contacts.ContactsViewModel
import org.owntracks.android.ui.preferences.PreferenceScreen
import org.owntracks.android.ui.preferences.PreferencesScreenContent
import org.owntracks.android.ui.preferences.about.AboutActivity
import org.owntracks.android.ui.preferences.editor.EditorActivity
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.waypoint.WaypointActivity
import org.owntracks.android.ui.waypoints.WaypointsScreenContent
import org.owntracks.android.ui.waypoints.WaypointsViewModel
import timber.log.Timber

/**
 * Main navigation host for the OwnTracks app.
 *
 * Hosts the bottom navigation destinations (Map, Contacts, Waypoints, Preferences)
 * within a single activity architecture.
 *
 * @param navController The navigation controller for managing navigation state
 * @param startDestination The initial destination route
 * @param onContactSelected Callback when a contact is selected from the Contacts screen
 */
@Composable
fun OwnTracksNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destination.Map.route,
    onContactSelected: (Contact) -> Unit = {},
    preferencesCurrentScreen: PreferenceScreen = PreferenceScreen.Root,
    onPreferencesNavigateToScreen: (PreferenceScreen) -> Unit = {},
    triggerWaypointsExport: Boolean = false,
    onWaypointsExportTriggered: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Get dependencies from Hilt entry point
    val entryPoint = remember(activity) {
        activity?.let {
            EntryPointAccessors.fromActivity(it, ComposablesEntryPoint::class.java)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Destination.Map.route) {
            // Map content is handled by MapActivity's MapFragment
            // This composable is empty - the map is shown via the Fragment in the layout
        }

        composable(Destination.Contacts.route) {
            val viewModel: ContactsViewModel = hiltViewModel()
            val contactImageBindingAdapter = entryPoint?.contactImageBindingAdapter()

            if (contactImageBindingAdapter != null) {
                val contactsList = remember {
                    mutableStateListOf<Contact>().apply {
                        addAll(viewModel.contacts.values.sortedByDescending { it.locationTimestamp })
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.contactUpdatedEvent.collect { change ->
                        Timber.v("Received contactUpdatedEvent $change")
                        when (change) {
                            is ContactsRepoChange.ContactAdded -> {
                                contactsList.add(change.contact)
                                contactsList.sortByDescending { it.locationTimestamp }
                                viewModel.refreshGeocode(change.contact)
                            }
                            is ContactsRepoChange.ContactRemoved -> {
                                contactsList.removeAll { it.id == change.contact.id }
                            }
                            is ContactsRepoChange.ContactLocationUpdated -> {
                                val index = contactsList.indexOfFirst { it.id == change.contact.id }
                                if (index >= 0) {
                                    contactsList[index] = change.contact
                                    contactsList.sortByDescending { it.locationTimestamp }
                                }
                                viewModel.refreshGeocode(change.contact)
                            }
                            is ContactsRepoChange.ContactCardUpdated -> {
                                val index = contactsList.indexOfFirst { it.id == change.contact.id }
                                if (index >= 0) {
                                    contactsList[index] = change.contact
                                }
                            }
                            is ContactsRepoChange.AllCleared -> {
                                contactsList.clear()
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.contacts.values.forEach(viewModel::refreshGeocode)
                }

                ContactsScreenContent(
                    contacts = contactsList,
                    contactImageBindingAdapter = contactImageBindingAdapter,
                    onContactClick = { contact ->
                        onContactSelected(contact)
                        navController.navigate(Destination.Map.route) {
                            popUpTo(Destination.Map.route) { inclusive = false }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composable(Destination.Waypoints.route) {
            val viewModel: WaypointsViewModel = hiltViewModel()
            val waypoints by viewModel.waypointsFlow.collectAsStateWithLifecycle()

            // Handle export trigger from MapActivity's top bar menu
            LaunchedEffect(triggerWaypointsExport) {
                if (triggerWaypointsExport) {
                    viewModel.exportWaypoints()
                    onWaypointsExportTriggered()
                }
            }

            WaypointsScreenContent(
                waypoints = waypoints,
                onWaypointClick = { waypoint ->
                    context.startActivity(
                        Intent(context, WaypointActivity::class.java)
                            .putExtra("waypointId", waypoint.id)
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(Destination.Preferences.route) {
            val preferences = entryPoint?.preferences()

            // Handle back button when on a preferences sub-screen
            BackHandler(enabled = preferencesCurrentScreen != PreferenceScreen.Root) {
                onPreferencesNavigateToScreen(PreferenceScreen.Root)
            }

            if (preferences != null) {
                PreferencesScreenContent(
                    preferences = preferences,
                    currentScreen = preferencesCurrentScreen,
                    onNavigateToScreen = onPreferencesNavigateToScreen,
                    onNavigateToStatus = {
                        context.startActivity(Intent(context, StatusActivity::class.java))
                    },
                    onNavigateToAbout = {
                        context.startActivity(Intent(context, AboutActivity::class.java))
                    },
                    onNavigateToEditor = {
                        context.startActivity(Intent(context, EditorActivity::class.java))
                    },
                    onExitApp = {
                        context.stopService(Intent(context, BackgroundService::class.java))
                        activity?.finishAffinity()
                        Process.killProcess(Process.myPid())
                    },
                    onThemeChange = { theme ->
                        val mode = when (theme) {
                            AppTheme.Auto -> Preferences.SYSTEM_NIGHT_AUTO_MODE
                            AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                            AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                        }
                        AppCompatDelegate.setDefaultNightMode(mode)
                    },
                    onDynamicColorsChange = {
                        activity?.recreate()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Navigate to a destination with proper back stack handling.
 */
fun NavHostController.navigateToDestination(destination: Destination) {
    when (destination) {
        Destination.Map, Destination.Contacts, Destination.Waypoints, Destination.Preferences -> {
            navigate(destination.route) {
                // Pop up to the start destination to avoid building up a large back stack
                popUpTo(Destination.Map.route) {
                    saveState = true
                }
                // Avoid multiple copies of the same destination
                launchSingleTop = true
                // Restore state when reselecting a previously selected item
                restoreState = true
            }
        }
        else -> {
            // Other destinations are still separate activities
        }
    }
}
