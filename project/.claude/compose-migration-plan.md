# OwnTracks Compose Migration Plan

## Overview
Migrating OwnTracks Android app from Activities/Fragments with XML layouts to Jetpack Compose with Navigation Compose. The drawer navigation has been replaced with bottom navigation.

## Current Status

### Completed Infrastructure
- [x] Compose dependencies added to `libs.versions.toml`
- [x] Compose enabled in `app/build.gradle.kts`
- [x] Theme files created (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`)
- [x] Navigation infrastructure (`ui/navigation/Destinations.kt`, `OwnTracksNavHost.kt`)
- [x] ~~Compose drawer component (`ui/navigation/AppDrawer.kt`)~~ (deleted - was deprecated)
- [x] Bottom navigation component (`ui/navigation/BottomNavBar.kt`)
- [x] Bottom nav menu (`res/menu/bottom_nav_menu.xml`)

### Bottom Navigation Migration (COMPLETED)
Replaced drawer navigation with 4-tab bottom navigation across all main screens:

| Tab | Icon | Screen |
|-----|------|--------|
| Map | map | Main map view |
| Contacts | people | Contact list |
| Waypoints | location_on | Regions/geofences |
| Preferences | settings | Settings + Status + About |

**Changes made:**
- Created `BottomNavBar.kt` Compose component
- Created `bottom_nav_menu.xml` menu resource
- Updated `MapActivity` - removed DrawerLayout, added BottomNavigationView
- Updated `ContactsActivity/ContactsScreen` - replaced drawer with BottomNavBar
- Updated `WaypointsActivity/WaypointsScreen` - replaced drawer with BottomNavBar
- Updated `PreferencesActivity` - removed drawer, added BottomNavigationView
- Updated `StatusActivity/StatusScreen` - removed drawer, now uses back navigation (accessed from Preferences)
- Added Status, About, Exit to preferences_root.xml as menu items
- Updated `PreferencesFragment` to handle Status/About/Exit clicks

### Migrated Screens

#### 1. Status Screen (Now accessed via Preferences)
- **Files**: `StatusActivity.kt`, `StatusScreen.kt`
- **Deleted**: `ui_status.xml`
- **Features**: Status items, battery optimization dialog, location permissions dialog, view logs button
- **Navigation**: Back button (accessed from Preferences > Status)

#### 2. Contacts Screen
- **Files**: `ContactsActivity.kt`, `ContactsScreen.kt`
- **Deleted**: `ui_contacts.xml`
- **Features**: LazyColumn with contacts, contact avatar loading via `ContactImageBindingAdapter`, relative timestamps, geocoded locations
- **Navigation**: Bottom navigation bar

#### 3. Waypoints Screen
- **Files**: `WaypointsActivity.kt`, `WaypointsScreen.kt`
- **Deleted**: `ui_waypoints.xml`
- **Features**: LazyColumn with waypoints, overflow menu (import/export), add button, geofence transition status
- **Navigation**: Bottom navigation bar

#### 4. About Screen (Accessed via Preferences)
- **Files**: `AboutActivity.kt`, `AboutScreen.kt`, `LicensesScreen.kt`
- **Deleted**: `AboutFragment.kt`, `LicenseFragment.kt`, `about.xml`, `preferences_licenses.xml`
- **Features**:
  - Version info with changelog link
  - Documentation, License, Source Code links
  - Libraries/Licenses screen (internal navigation)
  - Translations with plural string support
  - Feedback section (Issues, Mastodon)
- **Navigation**: Back button (accessed from Preferences > About)

### Screens Using View System (with Bottom Nav)

#### 5. Preferences Screen
- **Current**: `PreferencesActivity.kt`, multiple `*Fragment.kt` files
- **Layout**: `ui_preferences.xml` with BottomNavigationView
- **Complexity**: High - uses PreferenceFragmentCompat with XML preferences
- **Navigation**: Bottom navigation bar
- **Contains**: Status, About, and Exit menu items

#### 6. Map Screen (Partial Compose Migration)
- **Current**: `MapActivity.kt`, `MapFragment.kt` (GMS/OSS variants), `ui_map.xml`
- **Layout**: CoordinatorLayout with BottomNavigationView + ComposeView overlay for FABs and bottom sheet
- **Navigation**: Bottom navigation bar
- **Features**: Map fragment, Compose FABs, Compose ModalBottomSheet for contact details
- **Bottom Sheet Migration (Phase 2 COMPLETED)**:
  - Created `ContactBottomSheet.kt` - Compose ModalBottomSheet for contact details
  - Displays contact info: avatar, name, timestamp, geocoded location
  - Contact details grid: accuracy, altitude, battery, speed, distance, bearing
  - Action buttons: Request Location, Navigate, Clear, Share
  - Deleted `ui_contactsheet_parameter.xml` and `AutoResizingTextViewWithListener.kt`
- **FABs Migration (Phase 3 COMPLETED)**:
  - Created `MapFabs.kt` - Compose component with MapLayersFab and MyLocationFab
  - MyLocationFab shows dynamic icon based on `MyLocationStatus` (disabled, available, following)
  - Removed FABs from `ui_map.xml`
  - Removed `@BindingAdapter("locationIcon")` from MapActivity companion object

### Secondary Screens (Back Navigation)

##### 7.1 LogViewerActivity (Migrated)
- **Files**: `LogViewerActivity.kt`, `LogViewerScreen.kt`
- **Deleted**: `LogEntryAdapter.kt`, `LogPalette.kt`, `ui_preferences_logs.xml`, `log_viewer_entry.xml`
- **Features**:
  - LazyColumn with log entries (horizontally scrollable)
  - Color-coded log levels (debug, info, warning, error)
  - Auto-scroll to bottom when new logs arrive
  - Overflow menu (toggle debug logs, clear)
  - FAB for sharing/exporting logs
  - Back button navigation

##### 7.2 WaypointActivity (Migrated)
- **Files**: `WaypointActivity.kt`, `WaypointScreen.kt`
- **Deleted**: `ui_waypoint.xml`
- **Features**:
  - Form with OutlinedTextField for description, latitude, longitude, radius
  - Input validation with error states
  - Save/Delete action buttons in TopAppBar
  - Delete confirmation dialog
  - Back button navigation
- **Note**: Moved `@BindingAdapter` functions for `relativeTimeSpanString` to `support/BindingAdapters.kt` (still used by ui_row_contact.xml and ui_row_waypoint.xml)

##### 7.3 WelcomeActivity (Migrated)
- **Files**: `BaseWelcomeActivity.kt`, `WelcomeScreen.kt`, `PlayServicesPage.kt` (gms)
- **Deleted**:
  - Layouts: `ui_welcome.xml`, `ui_welcome_intro.xml`, `ui_welcome_connection_setup.xml`, `ui_welcome_location_permission.xml`, `ui_welcome_notification_permission.xml`, `ui_welcome_finish.xml`, `ui_welcome_play.xml`
  - Kotlin: `WelcomeAdapter.kt`, `WelcomeViewModel.kt`, `WelcomeFragment.kt`, `IntroFragment.kt`, `ConnectionSetupFragment.kt`, `LocationPermissionFragment.kt`, `NotificationPermissionFragment.kt`, `FinishFragment.kt`, `PlayFragment.kt`, `PlayFragmentViewModel.kt`
- **Features**:
  - HorizontalPager for swipeable onboarding pages
  - Page indicator dots
  - Permission requests with `rememberLauncherForActivityResult`
  - GMS/OSS variants with different page lists
  - Play Services check page (GMS only)
  - Dynamic page list based on API level (NotificationPermission for Android 13+)

##### 7.4 EditorActivity (Migrated)
- **Files**: `EditorActivity.kt`, `EditorScreen.kt`
- **Deleted**: `ui_preferences_editor.xml`, `ui_preferences_editor_dialog.xml`
- **Features**:
  - Display effective configuration as JSON
  - Overflow menu (export, import file, edit single value)
  - Snackbar for feedback messages
  - Preference editor dialog with autocomplete for keys (ExposedDropdownMenuBox)
  - Export configuration to file picker
  - Back button navigation

##### 7.5 LoadActivity (Migrated)
- **Files**: `LoadActivity.kt`, `LoadScreen.kt`
- **Deleted**: `ui_preferences_load.xml`
- **Features**:
  - Loading state with CircularProgressIndicator
  - Success state showing imported configuration JSON
  - Failed state showing error message
  - Close/Save action buttons based on import status
  - Handles ACTION_VIEW intents, content URIs, owntracks:// scheme
  - File picker for manual import

## Established Patterns

### Activity Structure with Bottom Nav (Compose)
```kotlin
@AndroidEntryPoint
class SomeActivity : AppCompatActivity() {
    private val viewModel: SomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            OwnTracksTheme {
                SomeScreen(
                    viewModel = viewModel,
                    onNavigate = { destination ->
                        navigateToDestination(destination)
                    },
                    // ... other callbacks
                )
            }
        }
    }

    private fun navigateToDestination(destination: Destination) {
        val activityClass = destination.toActivityClass() ?: return
        if (this.javaClass != activityClass) {
            startActivity(Intent(this, activityClass))
        }
    }
}
```

### Screen Composable Structure with Bottom Nav
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomeScreen(
    // State from ViewModel
    someData: List<Item>,
    // Callbacks
    onNavigate: (Destination) -> Unit,
    onItemClick: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentDestination = Destination.SomeScreen,
                onNavigate = onNavigate
            )
        },
        modifier = modifier
    ) { paddingValues ->
        // Content
    }
}
```

### Secondary Screen with Back Navigation
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(...)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        // Content
    }
}
```

### State Collection
- StateFlow: `val state by viewModel.stateFlow.collectAsStateWithLifecycle()`
- LiveData: `val state by viewModel.liveData.observeAsState(initial = defaultValue)`
- Flow events: Use `LaunchedEffect` to collect

### Hybrid View + Compose (MapActivity Pattern)
For screens that can't fully migrate to Compose (e.g., MapActivity with MapFragment):
```kotlin
// In XML layout, add a ComposeView for Compose overlay:
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/composeBottomSheet"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

// In Activity, set Compose content on the ComposeView:
binding.composeBottomSheet.setContent {
    OwnTracksTheme {
        val state by viewModel.someState.observeAsState()
        if (showBottomSheet) {
            ContactBottomSheet(
                contact = contact,
                onDismiss = { /* handle dismiss */ },
                // ... other callbacks
            )
        }
    }
}
```

## Future Work

### Potential Improvements
1. **Single Activity Architecture**: Consolidate all screens into a single MainActivity with NavHost
2. **Map Screen Compose Migration**: Phase 4 as outlined below (Phases 1-3 completed)
3. **Preferences Screen Compose Migration**: Build custom Compose preferences UI

### Map Screen Migration Phases (Optional)
1. ~~**Phase 1 (Completed)**: Added bottom navigation, removed drawer~~
2. ~~**Phase 2 (Completed)**: Migrated bottom sheet content to Compose ModalBottomSheet~~
3. ~~**Phase 3 (Completed)**: Migrated FABs to Compose FloatingActionButton~~
4. **Phase 4 (Full migration)**: Convert to single-Activity with NavHost

## Build Command
```bash
./gradlew assembleGmsDebug --no-daemon -Dorg.gradle.java.home="C:\Program Files\Java\jdk-22"
```

## Dependencies Added
```toml
# In libs.versions.toml
compose-bom = "2024.12.01"
compose-compiler = "1.5.15"
navigation-compose = "2.8.5"
lifecycle-runtime-compose = "2.8.7"

# Libraries
compose-bom, compose-ui, compose-ui-graphics, compose-ui-tooling,
compose-ui-tooling-preview, compose-material3, compose-material-icons-extended,
compose-foundation, compose-runtime-livedata, activity-compose,
navigation-compose, lifecycle-runtime-compose
```

## Notes
- Kotlin version is 1.9.25, requires Compose Compiler 1.5.x (not 2.x)
- DataBinding is still enabled for non-migrated screens (MapActivity, PreferencesActivity)
- ViewModels remain unchanged - they work with both View system and Compose
- Hilt injection unchanged - @AndroidEntryPoint still works
- AppDrawer.kt deleted - bottom navigation is now used instead
