# OwnTracks Android - Data Flow Design Document

## Overview

OwnTracks receives a range of data items from a number of different sources, so this attempts to visualize how the different components interact and the data flows between them.

It's also there to help guide a simplification of the design.

## Architecture Overview

```mermaid
graph TB
    subgraph "UI Layer"
        MapActivity
        ContactsActivity
        WaypointsActivity
        StatusActivity
        WaypointActivity
        WelcomeActivity
        EditorActivity
        LoadActivity
        MapViewModel
        ContactsViewModel
        WaypointsViewModel
        StatusViewModel
        WaypointViewModel
        WelcomeViewModel
        EditorViewModel
        LoadViewModel
    end

    subgraph "Service Layer"
        BackgroundService
        MessageProcessor
        LocationProcessor
        Parser
        Preferences
        EncryptionProvider
    end

    subgraph "Repository Layer"
        ContactsRepo
        WaypointsRepo
        LocationRepo
        EndpointStateRepo
    end

    subgraph "Network Layer"
        MQTTMessageProcessorEndpoint
        HttpMessageProcessorEndpoint
    end

    subgraph "Location Layer"
        LocationProviderClient
        GeofencingClient
    end

    subgraph "Data Layer"
        prefs[(SharedPreferences)]
        room[(Room Waypoints)]
        sqlite[(SQLite MQTT)]
        files[(FileSystem Queue)]
    end

    MapActivity --> MapViewModel
    ContactsActivity --> ContactsViewModel
    WaypointsActivity --> WaypointsViewModel
    StatusActivity --> StatusViewModel
    WaypointActivity --> WaypointViewModel
    WelcomeActivity --> WelcomeViewModel
    EditorActivity --> EditorViewModel
    LoadActivity --> LoadViewModel

    MapViewModel --> LocationRepo
    MapViewModel --> ContactsRepo
    MapViewModel --> EndpointStateRepo
    MapViewModel --> WaypointsRepo
    MapViewModel --> Preferences
    MapViewModel --> LocationProcessor
    MapViewModel --> MessageProcessor

    ContactsViewModel --> ContactsRepo

    WaypointsViewModel --> WaypointsRepo
    WaypointsViewModel --> LocationProcessor

    StatusViewModel --> EndpointStateRepo
    StatusViewModel --> LocationRepo

    WaypointViewModel --> WaypointsRepo
    WaypointViewModel --> LocationRepo

    WelcomeViewModel --> Preferences

    EditorViewModel --> Preferences
    EditorViewModel --> Parser
    EditorViewModel --> WaypointsRepo

    LoadViewModel --> WaypointsRepo
    LoadViewModel --> Preferences
    LoadViewModel --> Parser

    BackgroundService --> LocationProviderClient
    BackgroundService --> GeofencingClient
    BackgroundService --> MessageProcessor
    BackgroundService --> LocationProcessor
    BackgroundService --> Preferences
    BackgroundService --> ContactsRepo
    BackgroundService --> LocationRepo
    BackgroundService --> WaypointsRepo

    LocationProcessor --> MessageProcessor
    LocationProcessor --> WaypointsRepo
    LocationProcessor --> LocationRepo
    LocationProcessor --> Preferences

    MessageProcessor --> MQTTMessageProcessorEndpoint
    MessageProcessor --> HttpMessageProcessorEndpoint
    MessageProcessor --> ContactsRepo
    MessageProcessor --> WaypointsRepo
    MessageProcessor --> EndpointStateRepo
    MessageProcessor --> Preferences
    MessageProcessor --> Parser

    Parser --> EncryptionProvider

    MQTTMessageProcessorEndpoint --> sqlite
    MessageProcessor --> files
    WaypointsRepo --> room
    Preferences --> prefs
```

## Outbound Data Flow - Publishing Location

### Complete Flow: Location Acquisition to Network Transmission

```mermaid
sequenceDiagram
    participant OS as Android OS
    participant LPC as LocationProviderClient
    participant BS as BackgroundService
    participant LP as LocationProcessor
    participant LR as LocationRepo
    participant WR as WaypointsRepo
    participant MP as MessageProcessor
    participant Q as MessageQueue
    participant P as Parser
    participant E as EncryptionProvider
    participant EP as Endpoint<br/>(MQTT/HTTP)
    participant NET as Network

    OS->>LPC: Location Update
    activate LPC
    LPC->>BS: onLocationChanged(Location)
    activate BS

    BS->>LP: onLocationChanged(Location)
    activate LP

    LP->>LP: Validate accuracy threshold
    LP->>LP: Check if redundant network location

    alt Location Valid
        LP->>WR: getAll() waypoints
        activate WR
        WR-->>LP: List<WaypointModel>
        deactivate WR

        LP->>LP: Check fused region detection

        alt Region transition detected
            LP->>LP: Create MessageTransition
            LP->>MP: queueMessageForSending(MessageTransition)
            activate MP
            MP->>Q: offer(message)
            deactivate MP
        end

        LP->>LP: Create MessageLocation from Location
        LP->>LP: Enrich with WiFi, battery, regions

        LP->>LR: setCurrentPublishedLocation(Location)
        activate LR
        LR->>LR: Update StateFlow
        deactivate LR

        LP->>MP: queueMessageForSending(MessageLocation)
        activate MP
        MP->>Q: offer(message)
        deactivate MP
    else Location Invalid
        LP->>LP: Discard location
    end

    deactivate LP
    deactivate BS
    deactivate LPC

    MP->>Q: take() [BLOCKING]
    activate MP
    Q-->>MP: MessageBase

    MP->>MP: Check retry state

    MP->>P: toJson(message)
    activate P

    alt Encryption Enabled
        P->>E: encrypt(json)
        activate E
        E-->>P: encrypted bytes
        deactivate E
        P->>P: Wrap in MessageEncrypted
    end

    P-->>MP: JSON string/bytes
    deactivate P

    MP->>EP: sendMessage(message)
    activate EP

    alt MQTT Mode
        EP->>EP: Build MQTT topic
        EP->>EP: Set QoS and retain flag
        EP->>NET: publish(topic, payload, qos, retain)
        activate NET
        NET-->>EP: Success/Failure
        deactivate NET
    else HTTP Mode
        EP->>EP: Build HTTP request
        EP->>EP: Add Basic Auth if configured
        EP->>NET: POST /endpoint
        activate NET
        NET-->>EP: HTTP Response
        deactivate NET
    end

    alt Success
        EP-->>MP: Result.success()
        MP->>Q: Message dequeued
        MP->>MP: Reset retry count
    else Retryable Failure
        EP-->>MP: Result.failure(RetryableException)
        MP->>Q: offerFirst() [REQUEUE]
        MP->>MP: Exponential backoff delay
    else Permanent Failure
        EP-->>MP: Result.failure(PermanentException)
        MP->>Q: Message dropped
        MP->>MP: Log error
    end

    deactivate EP
    deactivate MP
```

## Inbound Data Flow - Receiving Messages

### Sequence 3: Network to Contact/Waypoint Update

```mermaid
sequenceDiagram
    participant MQTT as MQTT Broker
    participant EP as MQTTEndpoint
    participant P as Parser
    participant E as EncryptionProvider
    participant MP as MessageProcessor
    participant CR as ContactsRepo
    participant WR as WaypointsRepo
    participant VM as UI ViewModels

    MQTT->>EP: messageArrived(topic, MqttMessage)
    activate EP

    EP->>P: fromJson(bytes)
    activate P

    alt Message is encrypted
        P->>P: Detect MessageEncrypted
        P->>E: decrypt(data)
        activate E
        E-->>P: decrypted json
        deactivate E
        P->>P: Deserialize to MessageBase
    end

    P-->>EP: MessageBase subclass
    deactivate P

    EP->>MP: onMessageReceived(message)
    activate MP

    alt MessageLocation
        MP->>CR: update(contactId, MessageLocation)
        activate CR
        CR->>CR: Update or create Contact
        CR->>CR: Emit ContactLocationUpdated event
        CR-->>MP: Updated
        deactivate CR

    else MessageTransition
        MP->>CR: update(contactId, MessageTransition)
        activate CR
        CR->>CR: Update Contact geofence state
        CR->>CR: Emit event
        CR-->>MP: Updated
        deactivate CR

    else MessageCard
        MP->>CR: update(contactId, MessageCard)
        activate CR
        CR->>CR: Update Contact name/avatar
        CR->>CR: Emit ContactCardUpdated event
        CR-->>MP: Updated
        deactivate CR

    else MessageWaypoint
        MP->>WR: insert/update(WaypointModel)
        activate WR
        WR->>WR: Persist to Database
        WR->>WR: Emit WaypointOperation event
        WR-->>MP: Updated
        deactivate WR

    else MessageCmd
        MP->>MP: Execute command

        alt REPORT_LOCATION
            MP->>MP: Trigger immediate location publish
        else WAYPOINTS
            MP->>WR: getAll()
            MP->>MP: Queue MessageWaypoints response
        else SET_CONFIGURATION
            MP->>MP: Import configuration to Preferences
        end

    else MessageClear
        MP->>CR: remove(contactId)
        activate CR
        CR->>CR: Emit ContactRemoved event
        CR-->>MP: Removed
        deactivate CR
    end

    deactivate MP
    deactivate EP

    Note right of CR: ContactsRepo emits events<br/>via SharedFlow which are<br/>observed by ViewModels

    CR->>VM: SharedFlow emission
    activate VM
    VM->>VM: Update UI state
    deactivate VM
```

## Geofencing Data Flow

### Sequence 4: Waypoint Creation to Geofence Registration

```mermaid
sequenceDiagram
    actor User
    participant WA as WaypointActivity
    participant WVM as WaypointViewModel
    participant WR as WaypointsRepo
    participant BS as BackgroundService
    participant GC as GeofencingClient
    participant OS as Android OS

    User->>WA: Create waypoint
    activate WA
    WA->>WVM: Save waypoint
    activate WVM

    WVM->>WR: insert(WaypointModel)
    activate WR

    WR->>WR: Persist to database
    WR->>WR: Emit WaypointOperation.Insert

    WR-->>WVM: Success
    deactivate WR

    WVM-->>WA: Waypoint created
    deactivate WVM
    deactivate WA

    WR->>BS: SharedFlow event
    activate BS

    BS->>WR: getAll() waypoints
    activate WR
    WR-->>BS: List<WaypointModel>
    deactivate WR

    BS->>BS: Convert to Geofence objects

    BS->>GC: addGeofences(List<Geofence>)
    activate GC

    alt GMS Flavor
        GC->>OS: Register hardware geofences
        activate OS
        OS-->>GC: Registration complete
        deactivate OS
    else OSS Flavor
        GC->>GC: Store for software detection
        Note right of GC: Software geofencing<br/>via fused region detection
    end

    GC-->>BS: Success/Failure
    deactivate GC
    deactivate BS
```

### Sequence 5: Geofence Transition Event

```mermaid
sequenceDiagram
    participant OS as Android OS
    participant GBR as GeofencingBroadcast<br/>Receiver
    participant BS as BackgroundService
    participant LP as LocationProcessor
    participant MP as MessageProcessor
    participant Q as MessageQueue

    alt Hardware Geofencing (GMS)
        OS->>GBR: Geofence transition
        activate GBR

        GBR->>GBR: Create Intent with<br/>geofencing event data
        GBR->>BS: startService(INTENT_ACTION_SEND_EVENT_CIRCULAR)
        deactivate GBR

        activate BS
        BS->>BS: Parse GeofencingEvent from Intent

    else Software Geofencing (Fused Detection)
        OS->>BS: Location update (normal flow)
        activate BS
        BS->>LP: onLocationChanged(Location)
        activate LP

        LP->>LP: For each waypoint:<br/>Calculate distance

        alt Distance crossed threshold
            LP->>LP: Determine enter/leave event
            LP->>LP: Create MessageTransition
        end
    end

    alt Transition detected
        BS->>LP: publishTransitionMessage(event, waypoint)

        LP->>LP: Create MessageTransition:
        Note right of LP: - event: "enter" or "leave"<br/>- desc: waypoint description<br/>- wtst: waypoint timestamp<br/>- lat/lon: trigger location<br/>- acc: location accuracy

        LP->>MP: queueMessageForSending(MessageTransition)
        activate MP
        MP->>Q: offer(MessageTransition)
        deactivate MP
    end

    deactivate LP
    deactivate BS
```

## Remote Command Execution

### Sequence 6: Remote Command Processing

```mermaid
sequenceDiagram
    participant MQTT as MQTT Broker
    participant EP as MQTTEndpoint
    participant MP as MessageProcessor
    participant LP as LocationProcessor
    participant LPC as LocationProviderClient
    participant WR as WaypointsRepo
    participant PREFS as Preferences

    MQTT->>EP: MessageCmd received
    activate EP

    EP->>MP: onMessageReceived(MessageCmd)
    activate MP

    MP->>MP: Extract CommandAction

    alt REPORT_LOCATION
        MP->>LP: publishLocationMessage(RESPONSE)
        activate LP
        LP->>LPC: singleHighAccuracyLocation()
        activate LPC
        LPC-->>LP: Location
        deactivate LPC
        LP->>LP: Create MessageLocation<br/>with trigger="r"
        LP->>MP: queueMessageForSending()
        deactivate LP

    else WAYPOINTS
        MP->>WR: getAll()
        activate WR
        WR-->>MP: List<WaypointModel>
        deactivate WR
        MP->>MP: Create MessageWaypoints
        MP->>MP: queueMessageForSending()

    else SET_WAYPOINTS
        MP->>WR: clearAll()
        activate WR
        WR->>WR: Clear database
        deactivate WR

        loop For each waypoint in command
            MP->>WR: insert(WaypointModel)
            activate WR
            WR->>WR: Persist to database
            deactivate WR
        end

        Note right of MP: Triggers geofence<br/>re-registration via<br/>WaypointsRepo events

    else SET_CONFIGURATION
        MP->>PREFS: importConfiguration(MessageConfiguration)
        activate PREFS
        PREFS->>PREFS: Validate configuration
        PREFS->>PREFS: Update SharedPreferences
        PREFS->>PREFS: Trigger preference change listeners
        PREFS-->>MP: Success/Failure
        deactivate PREFS

    else RECONNECT
        MP->>EP: disconnect()
        activate EP
        EP-->>MP: Disconnected
        deactivate EP
        MP->>EP: connect()
        activate EP
        EP-->>MP: Connected
        deactivate EP

    else RESTART
        MP->>MP: Restart location services
        Note right of MP: Stops and restarts location updates
    end

    deactivate MP
    deactivate EP
```

## Data Persistence

### Diagram: Storage Architecture

```mermaid
graph TB
    subgraph "Persistent Storage"
        SP[(SharedPreferences<br/>- All user preferences<br/>- Connection settings<br/>- Monitoring mode<br/>- UI preferences<br/>- MQTT/HTTP configuration)]
        room[(Room Database<br/>- Waypoint definitions<br/>- Waypoint metadata<br/>- Key-value pairs<br/>- Fast read access)]
        SQL[(SQLite Database<br/>- MQTT Paho persistence<br/>- QoS 1/2 messages<br/>- Subscription state)]
        FS[(File System<br/>- Outgoing message queue<br/>- Periodic snapshots<br/>- JSON serialized messages)]
    end

    subgraph "Transient Storage"
        CR[ContactsRepo<br/>In-Memory Map<br/>- Map String, Contact<br/>- Contact locations<br/>- Contact metadata]
        LR[LocationRepo<br/>StateFlow<br/>- Current device location<br/>- Published location]
        ESR[EndpointStateRepo<br/>StateFlow<br/>- Connection state<br/>- Queue length<br/>- Last message time<br/>- Error details]
    end

    Preferences --> SP
    WaypointsRepo --> room
    MQTTEndpoint --> SQL
    MessageProcessor --> FS

    note1[Cleared on app restart.<br/>Repopulated from MQTT<br/>on reconnection.]
    note2[Re-established from<br/>LocationProviderClient<br/>on service start.]

    CR -.-> note1
    LR -.-> note2
```

## Message Types

### Diagram: Message Type Hierarchy

```mermaid
classDiagram
    class MessageBase {
        <<abstract>>
        +String topic
        +Int qos
        +Boolean retained
        +Int numberOfRetries
        +isValidMessage() Boolean
        +toJson(parser) String
        +annotateFromPreferences(preferences)
    }

    class MessageLocation {
        +Double latitude
        +Double longitude
        +Int accuracy
        +Int altitude
        +Int velocity
        +Int bearing
        +Long timestamp
        +Int battery
        +BatteryStatus batteryStatus
        +ReportType trigger
        +List~String~ inregions
        +String trackerId
        +String ssid
        +String bssid
    }

    class MessageTransition {
        +String event
        +String desc
        +Long wtst
        +Double latitude
        +Double longitude
        +Int accuracy
        +Long timestamp
    }

    class MessageWaypoint {
        +String desc
        +Double latitude
        +Double longitude
        +Int radius
        +Long timestamp
    }

    class MessageWaypoints {
        +List~MessageWaypoint~ waypoints
    }

    class MessageCard {
        +String name
        +String face
    }

    class MessageCmd {
        +CommandAction action
        +MessageWaypoints waypoints
        +MessageConfiguration configuration
    }

    class MessageConfiguration {
        +ConnectionMode mode
        +String host
        +[100+ preference fields]
    }

    class MessageEncrypted {
        +String data
    }

    class MessageClear {
    }

    MessageBase <|-- MessageLocation
    MessageBase <|-- MessageTransition
    MessageBase <|-- MessageWaypoint
    MessageBase <|-- MessageWaypoints
    MessageBase <|-- MessageCard
    MessageBase <|-- MessageCmd
    MessageBase <|-- MessageConfiguration
    MessageBase <|-- MessageEncrypted
    MessageBase <|-- MessageClear

    note for MessageLocation "Outbound: Published location updates\nInbound: Received from contacts\nTrigger types: USER, PING, CIRCULAR, RESPONSE"
    note for MessageTransition "Outbound: Own geofence transitions\nInbound: Contact geofence transitions\nEvents: enter, leave, dwell"
    note for MessageCmd "Inbound only: Remote commands\nActions: reportLocation, waypoints,\nsetWaypoints, setConfiguration,\nreconnect, restart"
    note for MessageEncrypted "Wrapper for encrypted messages\nContains encrypted JSON in data field\nUses libsodium secret box"
```

## Component Dependencies

### Diagram: Dependency Graph

```mermaid
graph TB
    App --> BackgroundService
    App --> MessageProcessor
    App --> Repositories

    subgraph "Service Layer"
        BackgroundService --> LocationProviderClient
        BackgroundService --> GeofencingClient
        BackgroundService --> LocationProcessor
        BackgroundService --> MessageProcessor

        MessageProcessor --> MessageProcessorEndpoint
        MessageProcessor --> Parser
        MessageProcessor --> BlockingDeque

        LocationProcessor --> MessageProcessor
        LocationProcessor --> WifiInfoProvider
        LocationProcessor --> DeviceMetricsProvider
    end

    subgraph "Network Layer"
        MessageProcessorEndpoint
        MQTTEndpoint
        HttpEndpoint

        MQTTEndpoint -.implements.-> MessageProcessorEndpoint
        HttpEndpoint -.implements.-> MessageProcessorEndpoint

        MQTTEndpoint --> PahoMQTTClient[Paho MQTT Client]
        HttpEndpoint --> OkHttp
    end

    subgraph "Location Layer"
        LocationProviderClient
        GMSLocationProviderClient
        AospLocationProviderClient
        GeofencingClient
        GMSGeofencingClient
        NoopGeofencingClient

        GMSLocationProviderClient -.implements.-> LocationProviderClient
        AospLocationProviderClient -.implements.-> LocationProviderClient

        GMSGeofencingClient -.implements.-> GeofencingClient
        NoopGeofencingClient -.implements.-> GeofencingClient
    end

    subgraph "Data Layer"
        Repositories --> ContactsRepo
        Repositories --> WaypointsRepo
        Repositories --> LocationRepo
        Repositories --> EndpointStateRepo
    end

    Parser --> EncryptionProvider
    EncryptionProvider --> libsodium

    note1[GMS: Google Play Services<br/>OSS: Android native APIs]
    note2[GMS: Hardware geofencing<br/>OSS: Software fused detection]

    LocationProviderClient -.-> note1
    GeofencingClient -.-> note2
```

## Connection State Machine

```mermaid
stateDiagram-v2
    [*] --> IDLE

    IDLE: Service not started
    IDLE: No endpoint connection

    CONNECTING: Attempting connection
    CONNECTING: May take several seconds

    CONNECTED: Successfully connected
    CONNECTED: Can send/receive messages

    DISCONNECTED: Connection lost
    DISCONNECTED: Will attempt reconnection

    ERROR: Configuration error
    ERROR: Or permanent failure

    ERROR_CONFIGURATION: Invalid settings
    ERROR_CONFIGURATION: User must fix config

    IDLE --> CONNECTING: Service started
    CONNECTING --> CONNECTED: Connection successful
    CONNECTING --> ERROR: Connection failed
    CONNECTING --> ERROR_CONFIGURATION: Invalid config

    CONNECTED --> DISCONNECTED: Network lost<br/>Broker unavailable<br/>Auth failure
    DISCONNECTED --> CONNECTING: Retry attempt
    DISCONNECTED --> ERROR: Max retries exceeded

    ERROR --> CONNECTING: Retry after backoff
    ERROR --> ERROR_CONFIGURATION: Config invalid

    ERROR_CONFIGURATION --> IDLE: User fixes config<br/>Service restarts

    CONNECTED --> CONNECTING: User changes config<br/>Reconnect requested

    note right of CONNECTING
        Retry logic:
        - Exponential backoff
        - Network change triggers retry
        - WorkManager schedules retry
    end note

    note right of CONNECTED
        MQTT: Persistent connection
        HTTP: Stateless, always "connected"
    end note
```
