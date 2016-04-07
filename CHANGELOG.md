OwnTracks Android App Release Notes
===================================
## OwnTracks 0.6.15
>Release date: 2016-04-07
* [FIX] #355 - Crash when picking a friend without a location
* [FIX] #355 - Notification click doesn't open the app
* [FIX] Map markers were not cleared on mode or broker change

## OwnTracks 0.6.14
>Release date: 2016-04-04
* [FIX] some compatibility issues
* [FIX] bottom sheet not expanding on older devices
* [FIX] several possible crashes
* [NEW] performance improvements for marker drawing

## OwnTracks 0.6.13
>Release date: 2016-04-04
[FIX] Bottom sheet not appearing in lower APIs
[FIX] Flicker in bottoms sheet when setting marker
[FIX] Marker bitmap not decoded in background thread
[FIX] Contact image provider async task stalled by other async tasks
[FIX] Possible crash in ping Sender

## OwnTracks 0.6.12
>Release date: 2016-04-03
[FIX] - #350 crash on lower API due to bug in google support library

## OwnTracks 0.6.11
>Release date: 2016-04-03
* [FIX] #339 - custom certificates couldn't be imported from some locations
* [FIX] #338 - beacon functionality can cause battery drain
* [FIX] #344 - regions loose "shared" indicator on import enhancement
* [NEW] follow mode is now automatically used from friends view and after long pressing on details on map
* [NEW] regions can be deleted by long pressing them
* [NEW] setup wizard that allows to set the mode prior to the first connection

## OwnTracks 0.6.10
>Release date: 2016-03-28
* [FIX] #338 - Bluetooth beacon functionality can cause battery drain
* [FIX] #344 - Waypoints loose "shared" indicator on import enhancement
* [FIX] #343 - 0.6.9: waypoint export should say 'queued'
* [FIX] #341 - remove the *map center* button on the *Friends* page

## OwnTracks 0.6.09
>Release date: 2016-03-25
* [FIX] #339 - Latest Android version doesn't accept CA and client crts
* [FIX] #338 - crash on preferences click

## OwnTracks 0.6.08
>Release date: 2016-03-25 for alpha testers
* [FIX] Changed enabled waypoint label (again)
* [FIX] Notification action to report location
* [NEW] Websocket support in private mode
* [NEW] Geocoder in notification

## OwnTracks 0.6.07
>Release date: 2016-03-24 for alpha testers
* [FIX] Changed enabled waypoint label
* [FIX] Transition messages not send
* [FIX] Configuration import didn't respect target mode
* [NEW] Refactored message handling code

## OwnTracks 0.6.06
>Release date: 2016-03-19 for alpha testers
* [FIX] Fixed crash in GeocodingProvider
* [FIX] #336 - Fixed crash in regions list
* [FIX] Fixed setWaypoints remote cmd
* [NEW] Dropped remoteCommandReportLocation preferences key in favor of the general cmd setting used by iOS
* [NEW] Prepared message backend for HTTP mode
* [NEW] Changed monitoring settings of beacon service

## OwnTracks 0.6.05
>Release date: 2016-03-16 for alpha testers
* [FIX] #335 bat -> batt in location payloads
* [FIX] Screen flicker when switching modes
* [NEW] #287 - Export waypoints via MQTT

## OwnTracks 0.6.04
>Release date: 2016-03-14 for alpha testers
* [FIX] #333 - active beacon event shows inactive in Region list
* [FIX] #320 - import otrc from exported on 061 fails bug
* [FIX] Configuration export didn't include type attribute for exported waypoints
* [FIX] No tranisitions sent when exiting a beacon region

## OwnTracks 0.6.02
>Release date: 2016-03-11 for alpha testers
* [FIX] Empty map due to wrong package name
* [FIX] Crash on Status activity
* [FIX] Queue lenght on Status activity was always 0
* [FIX] #323 061: region for Beacon bug
* [FIX] #322 061: waypoints marked as Shared are not being published
* [NEW] Update support libraries to 23.2.1
* [NEW] Date format for today skips yyyy-mm-dd prefix
* [NEW] Removed connectivity snackbar from connection preferences (replaced by Toast)

## OwnTracks 0.6.01
>Release date: 2016-03-11 for alpha testers
* [NEW] - Completely rewritten message backend
* [NEW] - Reimplemented Google Maps
* [NEW] - Rewrote preferences code
* [NEW] - Rewrote contact image handling
* [NEW] - Added BLE beacon support
* [NEW] - Dropped OwnTracks Hosted support 
* [NEW] - Dropped Message support 
* [NEW] - Removed unneeded permissions 
* [NEW] - Support for Android M permissions 
* [NEW] - Improved handling of certificates  
* [NEW] - Added support for symmetric payload encryption  
* [NEW] - Improved reconnect behaviour (#271, #297, #302)  

* [FIX] #294 - Android app doesn't suscribe right topic for Messages
* [FIX] #289 - Go back from THANKS Menu



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
