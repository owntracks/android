OwnTracks Android App Release Notes
===================================
## OwnTracks 0.5.23
>Release date: 2015-10-11 for beta testers
* [FIX] #271 - Automatic reconnect fails
* [FIX] #273 - Updated support link
* [FIX] #278 - Waypoints not exorted
* [FIX] Changed locator interval unit to seconds to be in line with iOS
* [FIX] Not currectly subscribed to cmd topic
* [NEW] - Simplified notification preferences
* [NEW] - Added dedicated notifications for messages and events

## OwnTracks 0.5.22
>Release date: 2015-09-17
* [NEW] - Updated dependencies
* [NEW] - Replaced Google Maps with MapBox
* [NEW] - Added support for tid contact images

## OwnTracks 0.5.21
>Release date: 2015-08-23 for alpha testers
* [FIX] #214 - Hosted .otrc file not associated with OwnTracks app
* [FIX] #235 - Notification vibrate switch ignored
* [FIX] #234 - Removed statistics page when not a debug build
* [FIX] #209 - Added restart button after importing preferences
* [FIX] #236 - Disconnection with similar devices due to equal MQTT client IDs. Reported and fixed by @friesenkiwi. 
* [FIX] #232 - Statistics page layout could overflow on Android 4.4.x. Reported by @stylpen.
* [FIX] #233 - Export page layout could overflow on Android 4.4.x. Reported by @stylpen.

## OwnTracks 0.5.20 
>Release date: 2015-08-07 for beta testers
* [FIX] #219 - UserID and DeviceID must be mandatory
* [FIX] #222 - Manual Entry of Waypoints not allowing negative sign
* [FIX] #225 - Waypoint description does not get imported
* [NEW] Notification now indicates that the app cannot connect due to missing config parameters
* [NEW] Support for password protected client certificates
* [NEW] File picker for broker CA and client certificates

## OwnTracks 0.5.19 
>Release date: 2015-07-19for alpha testers
* [FIX] ReconnectHandler was not started correctly when a connection error occured
* [FIX] DeviceId field default was not updated correctly 
* [NEW] Added notifications for received messages
* [NEW] Simplified and improved several preferences
* [NEW] Added preference to enable or disable messaging


## OwnTracks 0.5.18
>Release date: 2015-07-06 for alpha testers
* [FIX] ReconnectHandler was not started correctly 
* [FIX] Queued messages were not send after a reconnect
* [FIX] ServiceLocator onLocationChange statistics counter was not reset correctly
* [NEW] ReconnectHandler now uses an exponential backoff time between 1 and 64 minutes
* [NEW] Global background mode for location requests instead of new requests when switching activities
* [NEW] Added a marker that display the current location on the map 

## OwnTracks 0.5.17
>Release date: 2015-06-25 for alpha testers
* [FIX] Fixed disorted message image backgrounds on higher DPI devices
* [FIX] #212 - Open browser when message with URL attribute is clicked
* [FIX] Message view was not updated correctly if a message was deleted
* [FIX] AdapterCursorLoader did not use stable IDs
* [NEW] Messages now indicate if they have an URL attribute
* [NEW] ServiceLocator does no longer throttle publishes
* [NEW] Exposing internal statistics counter in new activity
* [NEW] Refactored ActivityWaypoints to use RecyclerView
* [NEW] Waypoints list now updates in realtime

## OwnTracks 0.5.16
>Release date: 2015-06-24 for alpha testers
* [NEW] Added support for GehHash based messages
* [NEW] Added support for broadcast messages 
* [NEW] Added support for direct messages
* [NEW] Added support client certificates in private mode

## OwnTracks 0.5.15
>Release date: 2015-06-18 for alpha testers
* [FIX] Messages in hosted mode were not queued because they defaulted to QoS 0. Changed that to QoS 1. 
* [FIX] Messages were not inserted into the queue (derp!)
* [FIX] Messages were not queued correctly when the MQTT client was not initialized previously
* [NEW] Implemented a Broadcast Receiver for Geofence events to check whether it gives more reliable results
* [NEW] Removed some anoying logging that flooded LogCat output

## OwnTracks 0.5.14
>Release date: 2015-06-17 for alpha testers
* [FIX] #192 - Autostart on boot should be default
* [FIX] #198 - Desc attribute not present in transition event
* [FIX] #196 - Tid not configurable in hosted mode
* [FIX] Unable to save username
* [NEW] More granular resolving of Geocoder

## OwnTracks 0.5.13
>Release date: 2015-06-16 for alpha testers
* [FIX] #191 - First launch after clean install should be in Public mode
* [FIX] #193 - Battery level not included in location pubs 
* [FIX] #195 - Preferences were not loaded correctly after mode change
* [FIX] #197 - Race condition in message delivery callbacks
* [FIX] #201 - Missing text styles in Waypoint edit dialog
* [FIX] Restricted setting of preference keys from imported config in public and hosted mode
* [FIX] SQLite database was not initialized correctly after updating from a previous schema version

## OwnTracks 0.5.12
>Release date: 2015-06-15 for alpha testers
* [FIX] Recursive entries to multiple views
* [FIX] #184 - Crash in org.owntracks.android.services.ServiceLocator.requestGeofences  
* [FIX] #180 - Crash if the received lat value is >90 or lon >180 
* [FIX] #178 - Crash in org.owntracks.android.activities.ActivityImport.extractPreferences
* [FIX] #143 - t parameter missing when reporting location from notification bar
* [FIX] Map zoom on orientation change
* [FIX] Geofences were not respecting the currently set mode
* [FIX] Connect and disconnect button was not working correctly
* [NEW] Added wtst to waypoint transition messages
* [NEW] removed enter or leave from waypoint definitions
* [NEW] Added mode switcher 
* [NEW] Added public mode support 
* [NEW] Added hosted mode support 
* [NEW] Updated external libraries to current versions
* [NEW] Reworked navigation sidebar
* [NEW] Fixed toolbars in preferences
