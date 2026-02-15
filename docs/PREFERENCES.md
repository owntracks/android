# OwnTracks Android - Preferences Reference

These are the preferences that the app manages in its preference store that can modify behaviour.

## Key

- **MQTT**: Available in MQTT mode
- **HTTP**: Available in HTTP mode
- **Exportable**: Can be included in configuration export/import

---

## Connection Settings

### `mode`
- **Type**: Integer (ConnectionMode)
- **Values**: `0` (MQTT), `3` (HTTP)
- **Default**: `0` (MQTT)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Specifies the connection mode for communicating with the backend server. MQTT provides bi-directional communication with support for receiving remote commands, while HTTP is a simpler one-way reporting mode.

---

## MQTT Connection Parameters

These settings apply only when using MQTT mode (`mode=0`).

### `host`
- **Type**: String
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Hostname or IP address of the MQTT broker. Must be a valid hostname containing only alphanumeric characters, hyphens, and periods.

### `port`
- **Type**: Integer
- **Default**: `1883` (or `8883` for TLS)
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: TCP port number for the MQTT broker connection.

### `clientId`
- **Type**: String
- **Default**: Auto-generated unique identifier
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: MQTT client identifier. Must be unique per broker connection and contain only alphanumeric characters (max 23 characters per MQTT spec).

### `ws`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Enable WebSocket transport for MQTT connection instead of raw TCP.

### `keepalive`
- **Type**: Integer (seconds)
- **Default**: `900` (15 minutes)
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: MQTT keepalive interval in seconds. The client sends PINGREQ messages at this interval to maintain the connection and detect network failures.

### `cleanSession`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: When enabled, the broker discards previous session information on connection. When disabled, the broker retains subscriptions and queued messages for the client.

### `mqttProtocolLevel`
- **Type**: Integer
- **Values**: `3` (MQTT 3.1), `4` (MQTT 3.1.1), `5` (MQTT 5.0)
- **Default**: `4` (MQTT 3.1.1)
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: MQTT protocol version to use for broker communication.

### `info`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Enables publication of device info messages (card messages) containing user's name and profile image.

---

## HTTP Connection Parameters

These settings apply only when using HTTP mode (`mode=3`).

### `url`
- **Type**: String (URI)
- **Default**: Empty
- **MQTT**: ✗ | **HTTP**: ✓
- **Description**: Complete HTTP(S) endpoint URL where location messages will be POST'ed. Must be a valid URI.

### `dontReuseHttpClient`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✗ | **HTTP**: ✓
- **Description**: When enabled, creates a new HTTP client for each request instead of reusing a connection pool. May help with certain proxy or firewall configurations but reduces performance.

---

## Authentication & Security

### `username`
- **Type**: String
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Username for authentication with the broker (MQTT) or HTTP endpoint (Basic Auth).

### `password`
- **Type**: String
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Password for authentication. Stored securely in Android's encrypted preferences.

### `tls`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Enable TLS/SSL encryption for MQTT connections. Changes default port to 8883.

### `tlsClientCrt`
- **Type**: String
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Alias of the client certificate installed in Android's keystore for mutual TLS authentication.

### `encryptionKey`
- **Type**: String (32 characters)
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Symmetric encryption key (32 characters) for end-to-end encryption of location messages using libsodium secret box. When set, messages are wrapped in MessageEncrypted before transmission.

---

## Device Identification

### `deviceId`
- **Type**: String (alphanumeric)
- **Default**: Auto-generated from device identifiers
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Unique device identifier used in MQTT topics and message metadata. Must contain only alphanumeric characters.

### `tid`
- **Type**: String (2 alphanumeric characters)
- **Default**: Last 2 characters of `deviceId`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Tracker ID - a short 2-character identifier displayed on the map for this device. Must be 2 alphanumeric characters.

---

## MQTT Topics & Quality of Service

### `pubTopicBase`
- **Type**: String
- **Default**: `owntracks/%u/%d`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Base MQTT topic for publishing messages. Supports placeholders: `%u` (username), `%d` (deviceId). Topics are automatically constructed as:
  - Location: `{pubTopicBase}`
  - Events: `{pubTopicBase}/event`
  - Waypoints: `{pubTopicBase}/waypoint`

### `pubQos`
- **Type**: Integer (MqttQos)
- **Values**: `0` (At most once), `1` (At least once), `2` (Exactly once)
- **Default**: `1`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Quality of Service level for published location and event messages.

### `pubRetain`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: When enabled, the broker retains the last published location message and delivers it to new subscribers.

### `sub`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Enable subscription to receive messages from other devices and the broker.

### `subTopic`
- **Type**: String
- **Default**: `owntracks/+/+`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: MQTT topic pattern to subscribe to for receiving messages from other users. Supports MQTT wildcards (`+` for single level, `#` for multi-level).

### `subQos`
- **Type**: Integer (MqttQos)
- **Values**: `0`, `1`, `2`
- **Default**: `1`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Quality of Service level for subscription.

---

## Location Tracking

### `monitoring`
- **Type**: Integer (MonitoringMode)
- **Values**: `-1` (Quiet), `0` (Manual), `1` (Significant), `2` (Move)
- **Default**: `1` (Significant)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Location monitoring mode:
  - **Quiet**: No automatic location updates
  - **Manual**: Location published only on user request
  - **Significant**: Location published on significant motion detection (battery efficient)
  - **Move**: Active tracking with regular interval updates (battery intensive)

### `locatorInterval`
- **Type**: Integer (seconds)
- **Default**: `300` (5 minutes)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Location update interval in Move mode. Minimum time between location publications.

### `moveModeLocatorInterval`
- **Type**: Integer (seconds)
- **Default**: `60` (1 minute)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Faster location update interval when in Move monitoring mode. Overrides `locatorInterval` when monitoring mode is set to Move.

### `locatorDisplacement`
- **Type**: Integer (meters)
- **Default**: `500`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Minimum displacement distance in meters that must occur before location is published (in addition to interval checks).

### `locatorPriority`
- **Type**: Integer (LocatorPriority)
- **Values**: `0` (No Power), `1` (Low Power), `2` (Balanced), `3` (High Accuracy)
- **Default**: `2` (Balanced)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Location provider accuracy/power priority:
  - **No Power**: Passive location (no active requests)
  - **Low Power**: ~10km accuracy, low battery impact
  - **Balanced**: ~100m accuracy, moderate battery
  - **High Accuracy**: Best accuracy using GPS, high battery usage

### `pegLocatorFastestIntervalToInterval`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: When enabled, sets the fastest location update interval equal to the standard interval. Prevents the system from delivering location updates faster than desired.

### `ignoreInaccurateLocations`
- **Type**: Integer (meters)
- **Default**: `0` (disabled)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Reject location updates with accuracy radius worse than this value (in meters). Set to 0 to accept all locations regardless of accuracy.

### `ignoreStaleLocations`
- **Type**: Float (days)
- **Default**: `0.0` (disabled)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Reject location updates older than this many days. Helps filter cached or outdated location data.

### `discardNetworkLocationThresholdSeconds`
- **Type**: Integer (seconds)
- **Default**: `30`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Number of seconds that must elapse after receiving a GPS location before accepting network-based locations again. Prevents less accurate network locations from overriding fresh GPS data.

---

## Geofencing & Regions

### `fusedRegionDetection`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Enable software-based geofence detection. On OSS builds without Google Play Services, this is the only geofencing method available. Detects enter/leave events by calculating distance to waypoints.

### `showRegionsOnMap`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Display geofence regions (waypoints with radius) on the map as circles.

---

## Message Content

### `extendedData`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Include extended data in location messages:
  - Battery level and charging status
  - WiFi SSID and BSSID
  - Velocity and bearing
  - Altitude
  - Current geofence regions

### `publishLocationOnConnect`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Automatically publish current location when connection to broker/endpoint is established. Useful for letting others know you're online.

---

## Remote Control

### `cmd`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Enable processing of remote commands (MessageCmd) received via MQTT. Allows remote triggering of location reports, waypoint management, and configuration changes.

### `remoteConfiguration`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✗
- **Description**: Allow remote configuration changes via MessageConfiguration. When disabled, blocks remote configuration imports for security.

### `ping`
- **Type**: Integer (seconds)
- **Default**: `0` (disabled)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Interval in seconds for automatic location pings. When set, publishes location at this interval regardless of movement. Set to 0 to disable.

---

## Notifications

### `notificationLocation`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Show persistent notification with current location information in the system tray.

### `notificationEvents`
- **Type**: Boolean
- **Default**: `true`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Show notifications for geofence transition events (enter/leave waypoints).

### `notificationGeocoderErrors`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Display notifications when reverse geocoding requests fail.

### `notificationHigherPriority`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Use higher priority for notifications, making them more prominent in the notification tray.

---

## User Interface

### `theme`
- **Type**: Integer (AppTheme)
- **Values**: `0` (Auto), `1` (Light), `2` (Dark)
- **Default**: `0` (Auto - follows system)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Application theme preference.

### `mapLayerStyle`
- **Type**: Integer (MapLayerStyle)
- **Default**: Varies by build flavor
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Map visual style. Available options depend on build flavor (Google Maps or OpenStreetMap).

### `enableMapRotation`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Allow map rotation based on device orientation or compass heading.

### `osmTileScaleFactor`
- **Type**: Float
- **Default**: `1.0`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Scale factor for OpenStreetMap tiles (OSS flavor only). Values >1.0 increase tile size for better readability on high-DPI displays.

### `imperialUnitsDisplay`
- **Type**: String (UnitsDisplay)
- **Values**: `METRIC`, `IMPERIAL`
- **Default**: `METRIC`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Controls whether the UI displays measurements in metric (m, km, kph) or imperial (ft, mi, mph) units. Affects distance, speed, altitude, and accuracy values shown in the contact detail bottom sheet.

---

## Reverse Geocoding

### `reverseGeocodeProvider`
- **Type**: Integer (ReverseGeocodeProvider)
- **Values**: `0` (None), `1` (Device), `2` (OpenCage)
- **Default**: `1` (Device)
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Service to use for converting coordinates to addresses:
  - **None**: No reverse geocoding
  - **Device**: Use Android's built-in Geocoder (Google services required)
  - **OpenCage**: Use OpenCage Data API (requires API key)

### `opencageApiKey`
- **Type**: String
- **Default**: Empty
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: API key for OpenCage Data reverse geocoding service. Required when `reverseGeocodeProvider=2`.

---

## System & Startup

### `autostartOnBoot`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Automatically start OwnTracks background service when device boots. Note: May not work reliably on all Android versions due to manufacturer battery optimizations.

### `connectionTimeoutSeconds`
- **Type**: Integer (seconds)
- **Default**: `30`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Network connection timeout for establishing connections to broker or HTTP endpoint.

---

## Development & Debugging

### `debugLog`
- **Type**: Boolean
- **Default**: `false`
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Enable verbose debug logging. Logs are written to device logcat and can be viewed in the Status screen.

### `experimentalFeatures`
- **Type**: Set<String>
- **Default**: Empty set
- **MQTT**: ✓ | **HTTP**: ✓
- **Description**: Set of experimental feature flags. Available features:
  - `showExperimentalPreferenceUI`: Show experimental settings in UI
  - `locationPingUsesHighAccuracyLocationRequest`: Use high accuracy for ping requests

---

## Internal Preferences

These preferences are stored but not exported/imported in configuration files:

### `firstStart`
- **Type**: Boolean
- **Description**: Tracks whether this is the first app launch. Used to trigger welcome screen.

### `setupCompleted`
- **Type**: Boolean
- **Description**: Indicates whether initial setup wizard has been completed.

### `userDeclinedEnableLocationPermissions`
- **Type**: Boolean
- **Description**: User explicitly declined location permissions prompt.

### `userDeclinedEnableBackgroundLocationPermissions`
- **Type**: Boolean
- **Description**: User explicitly declined background location permissions.

### `userDeclinedEnableLocationServices`
- **Type**: Boolean
- **Description**: User explicitly declined to enable device location services.

### `userDeclinedEnableNotificationPermissions`
- **Type**: Boolean
- **Description**: User explicitly declined notification permissions (Android 13+).

---

## Configuration Import/Export

Preferences can be exported and imported as JSON configuration files. The export behavior differs by connection mode:

- **MQTT Mode**: All preferences marked with `exportModeMqtt=true` are included
- **HTTP Mode**: All preferences marked with `exportModeHttp=true` are included

Preferences marked with both `exportModeMqtt=false` and `exportModeHttp=false` are never exported (e.g., permission decline flags).

### Remote Configuration

When `remoteConfiguration=true` and `cmd=true`, the device can receive configuration changes via MQTT MessageConfiguration messages. This allows centralized management of device settings but should be used carefully for security reasons.

---

## Preferences That Trigger Queue/Contact Reset

Changing certain connection-related preferences causes the app to wipe the outgoing message queue and clear all contacts:

- `mode` - Connection mode
- `url` - HTTP endpoint URL
- `host` - MQTT broker host
- `port` - MQTT broker port
- `username` - Authentication username
- `clientId` - MQTT client ID
- `tlsClientCrt` - TLS client certificate

This ensures clean state when switching between different servers or connection modes.
