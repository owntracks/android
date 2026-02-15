# Changelog

## Version 2.5.6

### New features

- Migrated message queue to use Room database instead of tape2 for better async performance and reliability
- Added imperial units display option in Map preferences, allowing distances, speeds, altitudes, and accuracy to be shown in feet/miles/mph instead of meters/km/kph

### Bug fixes

- Fix slow starts by avoiding blocking the main thread on startup with the disk-backed queue.
- Significantly reduced battery drain and improved responsiveness in MQTT mode during poor network conditions by changing reconnection retry strategy from linear to exponential backoff
- Fixed `imperialUnitsDisplay` preference not being persisted or read correctly from the preferences store

## Version 2.5.5

### New features

- Visible reconnection feedback on Connection preferences page (#2156)

### Bug fixes

- "Request Location" button on contact now sends the command message to the right topic in MQTT mode (#2101)
- Fixed MQTT not automatically reconnecting after network loss by preventing reconnect tasks from being cancelled prematurely (#2132)
- Fixed crash when location provider returns null location on Android 16 (#2110)
- Significantly reduced memory usage in logging infrastructure by reducing in-memory log buffer from 10,000 to 500 entries and eliminating redundant SimpleDateFormat instances (~98% memory reduction)
- Improved side navigation drawer layout handling for better compatibility with system bars (#2102)

## Version 2.5.4

### New features

- Galician Translation (thanks to Miguel Anxo Bouzada)
- Hebrew Translation (thanks to Ahiel and Natan)
- Dutch Translation (thanks to all contributors)
- Setting a preference that doesn't actually change the value won't have any effects (such as MQTT reconnecting) (#1875)
- Status messages contain the app version and flavour
- There's now an option to send a remote command "request location" to selected contacts from the map view
- New experimental preference `discardNetworkLocationThresholdSeconds` that allows OT to discard locations from inaccurate providers (e.g. network) if there's been a recent location from an accurate provider (e.g. gps/fused). (#2053)

### Bug fixes

- Try to not block the main thread when generating an Status Message, which causes an ANR
- Messages that fail to send because the endpoint isn't ready now retry every 10 seconds, not every second
- Import config screen displays JSON config LTR under RTL locales
- setting / importing configuration options that are enums are now case-insensitive
- Fix regression where setting the locatorPriority preference using a number wasn't working (#1874)
- Slightly less noisey and more useful logging at the info level
- Persistent notification updates its "when" displayed time to be that of the last update (#1954)

## Version 2.5.3

### New features

- OSM map is a little easier to zoom without accidentally rotating (#1825)

### Bug fixes

- Use AGP-provided version of R8 rather than version from Google so that F-Droid can build it (#1852)

## Version 2.5.2

### New features

- Added `cog` field to location messages showing current bearing (#1777)
- Added `status` remote command to retrieve system configuration status (#1618)
- On crash, details written to file and then printed at the top of the log next time OT starts

### Bug fixes

- Fix crash where changing the theme via setting the preferences remotely causes the theme change to not happen on the main thread
- Fix crash where trying to close the MQTT connection whilst it's connecting thows an unhandled exception
- Only latest stop reason should be printed to logs on startup
- Fix bug where geofencing client wasn't initialized properly, leading to very unreliable region transition detection (#1764)
- Fix bug where some settings (`pubQos`, `mqttProtocolLevel` etc.) couldn't be set via the config editor (#1801)
- Fix crash when trying to decode an invalid face image on an info card
- Fix MQTT disconnect when receiving an encrypted message that can't be decrypted (#1831)
- Fix HTTP client certs not working properly with Nginx (#1793)
- Fix ability to handle trigger="v" and "C" locations generated from iOS (#1768)

## Version 2.5.1

### New features

- The background location permission is explicitly asked for in the welcome activity. It's also prompted if missing (but foreground location permissions are present) in the map activity, to catch people upgrading from <2.5.0.

### Bug fixes

- Re-added `tst` from Lwt MQTT message type that was accidentally dropped in 2.5.0 (#1766)
- Fixed bug where locations with either "timer" ("t") or "beacon" ("b") type weren't processed by the app (#1768)
- Fixed bug where negative latitudes and longitudes couldn't be entered into the waypoints activity (#1765)

## Version 2.5.0

### Breaking changes

- OwnTracks will no longer manage your TLS CAs or client certs. For custom CA certs, you will need to add your CA to your device's CA store, and OwnTracks will use that store as a trusted reference for verifying TLS endpoints. Similarly, for client certificates, you'll need to add that certificate to the device's certificate store There should be a notification when OwnTracks starts after upgrade. (#736, #1061)
- TLSv1 and TLSv1.1 are deprecated. Supported TLS versions are 1.2 and 1.3.

### New features

- Now translated into Indonesian, Italian, and British English(!)
- Minimum device version is now SDK v24 (Android 7.0 Nougat)
- When displayed, LatLngs now are limited to 4 decimal places, because more precision than that is a bit silly (#1278, #1279)
- The config editor text is now selectable
- No need to restart the app once loading in a config any more!
- Add an option for a location message to be republished on reconnection to MQTT (#1273, #1178)
- MyLocation button now differentiates between no location, location available and following
- Added korean translation
- Shortcuts to preferences and logs on the launcher icon
- Waypoints can be imported on the config import screen (#1284)
- MQTT connection now reacts to changes in the device's default network, explicitly doing a reconnect (#642)
- Waypoint delete UX made a little easier. Explicit button in the waypoint activity, rather than a long-press
- Notification permissions are requested sensibly on Android 13+
- Google Play Store build numbers will now just increment from 40800000, rather than reflect the version string
- Background location permission! (Hurray!)
- Prompt for location & notification permissions in the welcome screen
- An exported log file also contains the threadname for each logentry
- Removed the undocumented `REREQUEST_LOCATION_UPDATES` intent
- Added `clearWaypoints` remote command (#1022)
- Contact direction arrow now moves with device to point in the actual direction of the contact
- Share button added to contact sheet (#1465)
- Changing the connection details will now clear the contacts and the location message backlog (#1598)
- Messages now include a random `_id` (String) field which can be used by any consumer to correlate and distinguish send/return messages
- `pubExtendedData` preference renamed to `extendedData` (#1654)
- `reportLocation` command is now supported in HTTP mode

### Bug fixes

- Labels on the contact sheet should all be the same size as each other (#1277)
- Clicking on a contact marker in the contacts activity doesn't always center the map on the marker (#1349, #1280)
- Don't show an error message on non-error conditions exporting config (#1280)
- Both cloud and local backup should work now
- Config export actually exports to a local file now, rather than just a somewhat useless "share"
- `conn` value correctly filled out as `o` (offline) when there's no network connection (#1442)
- `batt` is ommitted from locations if extended data is disabled (#741)
- Don't import waypoints from config if they're not valid (#1597)
- Fix the list of MQTT topics that we listen to, so that we only listen to the cmd topic for our device
- Publish new waypoints on the correct `/waypoint` topic
- Fix issue where notificiation permission banner was shown on devices where the notifications were actually just disabled
- Incoming messages that can't be parsed are now correctly handled as MessageUnknown
- Map blue dot should show the location accuracy circle properly
- Fix crash on importing config URIs that weren't valid
- Fix bug when sharing logs via GMail (#1600)
- Default the locater fastest interval to 1-second to address changes in Android 14 that was fixing fastest interval to `interval` seconds.

## Version 2.4.12

### Bug fixes

- Fixed Google Maps layers not showing (#1460)
- Added new APK signing key to the docs (#1461)
- Added specific notification permission request (#1462)

## Version 2.4.11

- Bumped targetSdk to 33 to comply with Google Play Store policies
- APK signing key updated to `1F:C4:DE:52:D0:DA:A3:3A:9C:0E:3D:67:21:7A:77:C8:95:B4:62:66:EF:02:0F:AD:0D:48:21:6A:6A:D6:CB:70`

## Version 2.4.10

### New features

- Battery optimization status page row now links straight to the right dialog on OSS flavour (#1239, thanks @nycex)
- Added Korean translations (#1244, thanks @whatareyoudoingfor)
- Added Danish tranlsations (thanks @atjn)
- Allow map rotation (can be disabled in preferences) (#1236)
- Add a scale bar to the OSM map view (#1263)
- Allow scaling the OSM map tiles to help with map readability on some devices with the `osmTileScaleFactor` setting (#1223, #1262)
- [OSS] Allow the user to request background location permission ("All the time") in the device settings (#1255)
- [OSS] Link straight to the correct battery whitelisting dialog from the status page (#1239)
- Themed icon on Android 13

### Bug fixes

- Blue dot on OSM layer should try to be in the middle of the accuracy circle (#1078)
- The map should broadly try and stay in the same place when switching layers
- The map should also roughly try and remember its position / orientation / zoom when switching away from the app and back again
- OSM map layers are a little more readable with larger text sizes (#1223)

## Version 2.4.9

### Bug fixes

- Fix issue with setting monitoring via a remote command "set configuration" message (#1221)

## Version 2.4.8

### New features

- OpenStreetMap now available as a map layer (!). Layer style toggle switch between the different Google Maps layer styles (Default, Hybrid, Satellite, Terrain), OpenStreetMap and Wikimedia. (#1181)
- OSM map now uses a blue dot and arrow for current location (#1078)
- Use new Google Maps renderer on gms flavour: <https://developers.google.com/maps/documentation/android-sdk/renderer>
- Add preference to draw regions on the map (#1068)
- Add a preference to prevent location updates any faster than the requested interval (#1168)
- UI for mode changing is now clearer, using a bottom sheet dialog popup with explanation notes on each mode (#1197)
- Updated the Welcome screens to add a new section describing the server requirements
- Update to Catalan, Japanese and Polish translations (thanks all!)
- Log viewer should now automatically scroll, and generally be less bad

### Bug fixes

- Fix crash when the ForegroundService is requested to start after reboot or upgrade on Android 12. In theory, this crash should never happen, as these intents are specifically exempted from restrictions in starting foreground services. However, sometimes an exception causing a crash was seen on certain devices, so this tries to handle that a bit more gracefully.

## Version 2.4.7

Target Android SDK is now 31.

### New features

- Location updates will now contain the app's mode as a field if extended data reporting is enabled (#1160). Documentation change at <https://github.com/owntracks/booklet/commit/d4876781f801a8b006587e26ca3fba27328596b0>
- The minimum locator displacement configuration value is now exposed in the preferences (\*#1177)

### Bug fixes

- Fixed issue where the app would crash when resuming due to a bug in how the MapFragment was created
- Fixed a boat-load of memory leaks (thanks to [LeakCanary](https://square.github.io/leakcanary/))
- Tweaked the font sizing in the contacts sheet to be a little more coherent (#1153)
- Translation updates (thanks to all who contributed) (#1167)

## Version 2.4.6

### Bug fixes

- Fix format string bug in some locale resource files that caused a hard crash on startup in those locales. Oops. Sorry everyone.
- MapFragmentFactory was throwing a NPE under certain circumstances. Workaround until we get MapFragmentViewModel in 2.5.0

## Verison 2.4.5

### Bug fixes

- Fix crash resuming the MapActivity caused by FragmentFactory handling fragment creation correctly onResume
- Auto-sizing of text on the bottomsheet contact details pull-up should work properly now (#1123)
- Locales with incomplete translations should fall back to the default locale rather than show a blank (#1145)

## Verison 2.4.4

### New features

- Updated Russian translations (thanks @Allineer - #1123)

### Bug fixes

- Fix a crash when running into an issue deserializing a persistent-storage-queue message (#1129)
- The OSS build now should be GMS / non-OSS dependency free. Hopefully.
- When receiving an encrypted message over MQTT that can't be decrypted, don't treat as an MQTT error and disconnect, but log more gracefully.
- Fix issue when MQTT fails to reconnect if it encounters a client-cert TLS issue.

## Version 2.4.3

### New features

- Added Japanese, Czech and partial Portuguese, Chinese translations. Many thanks to all the contributors!
- Device location enable / disable is now detected, along with location permissions on the map view. Location permissions no longer asked for in the welcome screen, and it's now possible to use without granting location permissions (or enabling device location). (#1102)
- Battery doze whitelisting in Status screen can now take you to the relevant settings page
- Connection timeout is now configurable with `connectionTimeoutSeconds` preference
- [OSS] Significant location mode now listens to GPS as well as passive / network sources (#1096)

### Bug fixes

- Fix crash in the MQTT library related to how the connection was closed (#1093).
- Cleansession preference shouldn't be exported in HTTP mode
- [OSS] OSM map now puts markers in the right place, rather than the wrong place
- Tries to detect problems when initializing WorkManager (#1064)
- [OSS] OSM map tiles are now cached in a location that's not included in device backups (#1108)
- MQTT `cleanSession` preference should now not be exported in HTTP mode

## Version 2.4.2

### New features

- The ongoing notification should now not wake up the lock screen if the content changes.

### Bug fixes

- Fix crash on startup where shared preferences has some invalid values
- Fix crash on being prompted to enable device location because of a callback not being initialized properly
- Fix crash due to a race when reverse geocoding the contacts
- Fix crash due to Android SDK bug throwing unexpected error when fetching current connection details

## Version 2.4.1

### Bug fixes

- Fix a race condition between activating and deactivating a map location source causing a crash on startup (#1081)

## Version 2.4.0

### New features

- The welcome screen is now swipey!
- Dark theme! (thanks to all who suggested, closes #787 #788 and #789)
- On Android API>=30, when the service is started in the background (e.g. after a boot or upgrade), it'll now notify the user that locations won't be received unless the app has been explicitly interacted with (via the activity, or changing the monitoring mode etc). This is because of the new [Android background location limits](https://developer.android.com/about/versions/oreo/background-location-limits). (See #976, #969, #967)
- Polish translation (thanks Robert!)
- You should now be able to exit the app by sending an intent (e.g. from other apps like Tasker etc.). Sending a "startservice" intent with `org.owntracks.android.EXIT` should stop the service as well as quit the app. Closes #982.
- Split the app into two Android flavors - OSS and GMS. GMS is what will continue to be published to the play store, but the OSS will now be published on the GitHub releases as well. The OSS version looks to remove dependencies on Google's Play Services and closed-source maps libraries. Ultmiate aim is to have a version we can distribute via F-Droid, giving users the option for a version that's more open.
- Log screen will now only display actual application logs, rather than just a dump from `adb logcat` which wasn't that useful.
- Exported configurations are now pretty printed (thanks @nycex)
- The outgoing message queue is now persisted to device storage, so it should survive both app and device restars (closes #994)
- Contact bottom sheet now includes details about their battery, speed, altitude, and bearing
- Location messages now also include the BSSID and SSID if available. Closes #871
- The Geocoder error notification can now be enabled/disabled in the preferences
- Added topic key to HTTP messages. Closes #1047
- [Experimental] Added the ability to use [OpenStreetMap](https://www.openstreetmap.org) instead of Google Maps as a mapping layer. Can be enabled by adding `useOSMMap` to the `experimentalFeatures` config key.
- [Experimental] Added the ability to use a pure AOSP location provider instead of the Google Play Services `FusedLocationProviderClient`. Can be enabled by adding `useAospLocationProvider` to the `experimentalFeatures` config key.
- [Experimental] Have the contact peek bearing arrow rotate depending on the current devices orientation

### Bug fixes

- Lower notification priority of geocoder error notifications to make them a bit less present / shouty
- If location is disabled on-device, OT will now prompt the user to enable on startup.
- Fix crash on opening contact list where there's a card for a user but no location yet (#984)
- Map view will now start on a recognizable map location (Paris) rather than at 0,0
- Clicking "back" with a contact sheet open will now close the sheet rather than minimize the activity
- Retry backoff for messages now goes up to 2 minutes
- Messagecards are cached, so if a contact is cleared and then re-appears on the broker, the image and name are correct
- If the username is not specified, the topic username is correctly set to be "user"

## Version 2.3.0

### New features

- Config can now be loaded by the app using an `owntracks:///` URI, either pointing at a remote config location or encoded inline in base64.
- Multiple MQTT topics (space-delimited) are now supported under the `subTopic` preference
- Message sending is now retried on failure up to 10 times, or 10,000 times for location messages to be better resilient against transient failures, but to also not block the queue for messages that upstream can't handle. (#936)
- Geocoder will now handle errors from the Geocoding service more gracefully, showing a notification and respecting rate-limiting / backing off. (#942)
- Minor UI changes separating the About screen from the rest of the preferences
- HTTP Useragent changed from "Owntracks/<build number>" to "Owntracks-Android/<build number>" to better indicate to servers which OT client it is
- Catalan language support (thanks Rafroset!)

### Bug fixes

- Fix bug relating to geocoding handling where the displayed value was not correctly updated.
- Geocoder now only considers first 4dp of location lat/lng to prevent too many requests resulting from tiny location drift
- Background location permission no longer needed, so removed
- Fix for regression introduced in 2.2 where self-signed certificates supplied as the CA were doing more restrictive hostname checking (#896). Hostnames are no longer matched if the CA cert is the same as the MQTT leaf cert.
- Geocoder preference should now work properly on API<24
- Initial location fix should now also work better on API<24
- Fix an issue where trying to exit the app wouldn't work if there were outstanding messages queued to be sent (#954)

## Version 2.2.2

### Breaking changes

- TLS hostname verification now no longer considers the CN field in the server certificate. This is in line with the deprecation of using CN for host verification as of RFC2818 (2000) and RFC6125 (2011). Instead, hostnames will be validated against the list of subjectAltNames (SANs) in the server certificate. If you were previously relying on the CN field in your certificate and didn't have any SANs listed, or the SAN list doesn't contain the hostname you're connecting to, TLS connections may now fail. The suggested course of action is to regenerate the cert ensuring that the SAN list is correct.

### Bug fixes

- Fixed a crash caused by a race condition on some devices where certain things are used before init (#890)
- Fixed issue with HTTP send failures not being re-tried (#893)
- Fixed playstore links (#894)
- Fixed bug where exiting the app didn't cancel background tasks (#899)

## Version 2.2.1

### New features

- Russian translation!
- TLSv1.3 support
- Location messages now include the batteryStatus in line with iOS (#841)
- Added explicit remote configuration preference toggle
- Reverted the `tst` field of location/waypoint/transition messages to mean the time that the event occurred. Added a new `created_at` field to these types with the timestamp of the message creation.

### Bug fixes

- Prevent crash when a user inputs an invalid MQTT URL (#852)
- Fix an issue related to remoteCommands not being received or handled correctly (#842)
- Config import no longer crashes when waypoints are absent (#818)
- Map now correctly updates the blue dot when in foreground
- Configuration management screen now shows waypoints that will be exported
- Monitoring mode button should now be correct when Map activity is resumed

## Version 2.2.0

### New features

- Created a changelog!
- Logs now viewable and shareable from within the application (Status -> View Logs)
- Debug logs are now toggleable from the logs viewer UI
- Migrated preferences to androidx library
- MQTT errors are now displayed more clearly on the connection status.
- Locator intervals are now user-configurable, and differentiated between significant changes and move modes.
- Clearing a contact sends a zero-length payload rather than a JSON message for type `MessageClear`, in line with the iOS app and booklet
- Users can specify the MQTT client ID explicitly
- Removed confusion around MQTT auth (no username vs anonymous). Now, not supplying a username = anonymous auth
- Added French translations

### Bug fixes

- MQTT processor should be more stable at re-connecting when disconnected
- Improved reliability of internal message queue / backoff mechanism when unable to send a message
- Fixed some issues on the welcome screens on older (API<=24) devices
- Fixed race conditions in usage of the MQTT client
- Fixed input weirdness when trying to edit latlngs in the region UI
- Fixed some bugs in the configuration import / export handling
- Fixed a bug where MQTT tried to connect without a valid configuration
- Fixed a bug preventing users from putting the mode into low power mode
- Stale incoming locations are only discarded if the preference is set
- All messages have the current time populated into the `tst` field, as opposed to the time of the last location fix
- Better reliability in reading from mistyped preferences.
- Few general lint / stability fixes

## Version 2.1.3

### New features

- `dontReuseHttpClient` config key to create a new HTTP client on every request. Can fix stuck queue on certain devices (see #656)
- Removed locator interval and displacement UI settings because they were misunderstood causing too much confusion for many users
- made locator accuracy configurable in significant mode with locatorPriority configuration key
- Improve display of error conditions
- Remote reportLocation command triggers new location fix (thanks to @grheard)
- Added debug log toggle in preferences
- Removed region place picker because API was retiered by Google

### Bug fixes

- wrong long press label of report button
- crash on invalid http headers
- http connection not closed correctly on errors
- location request not setup again on locator parameter change
- include debugLog configuration key in export and editor
- some translations
- Downgraded MQTT library to fix several issues
- Back buttons and arrows in preferences
- Monitoring mode not saved/restored correctly
- Removed test location listener that could crash the background service

## Version 2.1.2

### New features

- Debug logger that can export application logs to a HTML file. See our booklet for more details.

### Bug fixes

- Crash in VersionFragment when no suitable browser is installed
- Wrong wtst unit in waypoint messages (#631)
- Crash when setting illegal username in HTTP mode
- Crash when setting invalid geofence parameter

## Version 2.1.1

### Bug fixes

- waypoint transitions were sent in quiet mode
- reportLocation command was ignored in MQTT mode (#627)
- wrong data types for alt, vel attributes in location message (#629)
- crashes in WelcomeActivity
- crash during EncryptionProvider initialization
- crash when receiving waypoint message without radius
- Setup screen got stuck on restrictions page
- Improved error reporting (#567)

## Version 2.1.0

### New features

- Foreground/Background mode replaced by monitoring modes (quiet => No automatic location reports/passive location gathering, manual => Only automatic region location reports/passive location gathering, significant => automatic location reports/active location gathering based on Wifi/Cell location, move => frequent location reports/active location gathering based on GPS). Significant changes mode corresponds to the old behavior.
- Locator displacement and interval only influence significant location monitoring mode
- Monitoring modes displayed in notification when endpoint state is idle or connected
- Prefilling new region with current location
- Added estimated distance to contacts in details view
- Moved background jobs to Android Work Manager. Reconnect/Ping jobs now have a minimum interval of 15 minutes.
- Spanish translation. Thanks to @Kaysera.
- Improved dependency handling and fixed several bugs

### Bug fixes

- Waypoint import not working probperly
- Config import not displayed correctly
- Crash on first launch
- Locations not correctly requested
- Removed copy mode
- Removed GUI to configure unauthenticated connections. auth preferences key can still be set via configuration editor if unauthenticated connections are really required

## Version 2.0.0

### New features

- Removed support for bluetooth beacons because that feature accounted for over 90% of all crashes
- Removed support for shared waypoints. Waypoints are now always treated as shared
- Removed support for our hosted public MQTT platform
- Added ping functionality that will trigger a location report periodically
- Added support for Opencage Geocoder as alternative to Google
- Added custom useragent to HTTP mode to better identify the app in logs
- Added altitude to extended data publishes
- New algorithm for region detection that is assisted by location publishes
- Added support for inregions element of location publishes

### Bug fixes

- Fixed encryption issues with HTTP mode
- Rewrote backend service from the ground up to work more reliable on modern Android versions that prevent many actions when the app is in the background.
