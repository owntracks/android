package org.owntracks.android.services;

import android.Manifest;
import android.app.Notification;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.ui.map.MapActivity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class BackgroundService extends Service implements BeaconConsumer, RangeNotifier, MonitorNotifier, OnCompleteListener<Location> {
    private static final int INTENT_REQUEST_CODE_LOCATION = 1263;
    private static final int INTENT_REQUEST_CODE_GEOFENCE = 1264;
    private static final int INTENT_REQUEST_CODE_CLEAR_EVENTS = 1263;

    private static final int NOTIFICATION_ID_ONGOING = 1;
    private static final int NOTIFICATION_ID_EVENT_GROUP = 2;
    private static int notificationEventsID = 3;

    private final String NOTIFICATION_GROUP_EVENTS = "events";

    // NEW ACTIONS ALSO HAVE TO BE ADDED TO THE SERVICE INTENT FILTER
    public static final String INTENT_ACTION_CHANGE_BG = "BG";
    public static final String INTENT_ACTION_CLEAR_NOTIFICATIONS = "C";
    public static final String INTENT_ACTION_SEND_LOCATION_PING = "LP";
    public static final String INTENT_ACTION_SEND_LOCATION_USER = "LU";
    public static final String INTENT_ACTION_SEND_LOCATION_RESPONSE = "LR";
    public static final String INTENT_ACTION_SEND_WAYPOINTS = "W";
    public static final String INTENT_ACTION_SEND_EVENT_CIRCULAR = "EC";


    private static final int BEACON_MODE_LEGACY_SCANNING = 1;
    private static final int BEACON_MODE_OFF = 2;

    private FusedLocationProviderClient mFusedLocationClient;
    private GeofencingClient mGeofencingClient;

    private Location lastLocation;
    private MessageLocation lastLocationMessage;
    private MessageProcessor.EndpointState lastEndpointState = MessageProcessor.EndpointState.INITIAL;


    private NotificationCompat.Builder activeNotificationBuilder;
    private NotificationCompat.Builder notificationBuilderEvents;
    private NotificationManagerCompat mNotificationManager;
    private Preferences preferences;
    private List<Waypoint> waypoints = new LinkedList<>();
    private final LinkedList<Spannable> activeNotifications = new LinkedList<>();
    private BeaconManager beaconManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        preferences = App.getPreferences();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mNotificationManager = NotificationManagerCompat.from(this); //getSystemService(Context.NOTIFICATION_SERVICE);

        sendOngoingNotification();

        setupLocationRequest();
        setupGeofences();
        setupLocationPing();
        setupBeaconManager();

        App.getEventBus().register(this);
        App.getEventBus().postSticky(new Events.ServiceStarted());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent != null) {
            handleIntent(intent);
        }

        return START_STICKY;
    }

    private void handleIntent(@NonNull Intent intent) {
        if (LocationResult.hasResult(intent)) {
            onLocationChanged(intent);
        } else if (intent.getAction() != null) {
            Timber.v("intent received with action:%s", intent.getAction());

            switch (intent.getAction()) {
                case INTENT_ACTION_CHANGE_BG:
                    setupLocationRequest();
                    if(beaconManager != null && beaconManager.isBound(this))
                        beaconManager.setBackgroundMode(!App.isInForeground());
                    return;
                case INTENT_ACTION_SEND_LOCATION_PING:
                    publishLocationMessage(MessageLocation.REPORT_TYPE_PING);
                    return;
                case INTENT_ACTION_SEND_LOCATION_USER:
                    publishLocationMessage(MessageLocation.REPORT_TYPE_USER);
                    return;
                case INTENT_ACTION_SEND_EVENT_CIRCULAR:
                    onGeofencingEvent(GeofencingEvent.fromIntent(intent));
                    return;
                case INTENT_ACTION_SEND_LOCATION_RESPONSE:
                    Timber.e("INTENT_ACTION_SEND_LOCATION_RESPONSE not implemented");
                    return;
                case INTENT_ACTION_SEND_WAYPOINTS:
                    Timber.e("INTENT_ACTION_SEND_WAYPOINTS not implemented");
                    return;
                case INTENT_ACTION_CLEAR_NOTIFICATIONS:
                    clearEventStackNotification();

                default:
                    Timber.v("unhandled intent action received: %s", intent.getAction());
            }
        }
    }


    @Nullable
    private NotificationCompat.Builder getOngoingNotificationBuilder() {
        if (!preferences.getNotification())
            return null;

        if (activeNotificationBuilder != null)
            return activeNotificationBuilder;


        activeNotificationBuilder = new NotificationCompat.Builder(this);

        Intent resultIntent = new Intent(App.getContext(), MapActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        activeNotificationBuilder.setContentIntent(resultPendingIntent);
        activeNotificationBuilder.setSortKey("a");


        Intent publishIntent = new Intent();
        publishIntent.setAction(INTENT_ACTION_SEND_LOCATION_USER);
        PendingIntent publishPendingIntent = PendingIntent.getService(this, 0, publishIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        activeNotificationBuilder.addAction(R.drawable.ic_report_notification, getString(R.string.publish), publishPendingIntent);
        activeNotificationBuilder.setSmallIcon(R.drawable.ic_notification);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            activeNotificationBuilder.setColor(getColor(R.color.primary));
            activeNotificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            activeNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        activeNotificationBuilder.setOngoing(true);

        return activeNotificationBuilder;
    }

    private void sendOngoingNotification() {
        NotificationCompat.Builder builder = getOngoingNotificationBuilder();

        if (builder == null)
            return;

        if (this.lastLocationMessage != null && this.lastLocationMessage.getGeocoder() != null && preferences.getNotificationLocation()) {
            builder.setContentTitle(this.lastLocationMessage.getGeocoder());
            builder.setWhen(TimeUnit.SECONDS.toMillis(this.lastLocationMessage.getTst()));
        } else {
            builder.setContentTitle(getString(R.string.app_name));
        }

        builder.setPriority(preferences.getNotificationHigherPriority() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN);
        builder.setContentText(lastEndpointState.getLabel(App.getContext()));
        startForeground(NOTIFICATION_ID_ONGOING, builder.build());
    }


    private void sendEventNotification(MessageTransition message) {
        NotificationCompat.Builder builder = getEventsNotificationBuilder();

        if (builder == null) {
            Timber.e("no builder returned");
            return;
        }

        FusedContact c = App.getFusedContact(message.getContactKey());

        long when = message.getTst() * 1000;
        String location = message.getDesc();

        if (location == null) {
            location = getString(R.string.aLocation);
        }
        String title = message.getTid();
        if (c != null)
            title = c.getFusedName();
        else if (title == null) {
            title = message.getContactKey();
        }

        String text = String.format("%s %s", getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving), location);


        notificationBuilderEvents.setContentTitle(title);
        notificationBuilderEvents.setContentText(text);
        notificationBuilderEvents.setWhen(when);
        notificationBuilderEvents.setShowWhen(true);
        notificationBuilderEvents.setGroup(NOTIFICATION_GROUP_EVENTS);
        // Deliver notification
        Notification n = notificationBuilderEvents.build();

        Timber.v("sending new transition notification");
        mNotificationManager.notify(notificationEventsID++, n);
        //mNotificationManager.notify(NOTIFICATION_TAG_EVENTS_STACK, System.currentTimeMillis() / 1000, n) ;
        sendEventStackNotification(title, text, when);
    }


    private void sendEventStackNotification(String title, String text, long when) {
        if (Build.VERSION.SDK_INT >= 23) {
            Timber.v("SDK_INT >= 23, building stack notification");

            String whenStr = App.formatDate(TimeUnit.MILLISECONDS.toSeconds((when)));

            Spannable newLine = new SpannableString(String.format("%s %s %s", whenStr, title, text));
            newLine.setSpan(new StyleSpan(Typeface.BOLD), 0, whenStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            activeNotifications.push(newLine);
            Timber.v("groupedNotifications: %s", activeNotifications.size());

            // since we assume the most recent notification was delivered just prior to calling this method,
            // we check that previous notifications in the group include at least 2 notifications
            if (activeNotifications.size() > 1) {

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                String summary = getString(R.string.notificationEventsTitle, activeNotifications.size());
                builder.setContentTitle(getString(R.string.events));
                builder.setContentText(summary);
                builder.setGroup(NOTIFICATION_GROUP_EVENTS); // same as group of single notifications
                builder.setGroupSummary(true);
                builder.setColor(getColor(R.color.primary));
                builder.setAutoCancel(true);
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                builder.setSmallIcon(R.drawable.ic_notification);
                builder.setDefaults(Notification.DEFAULT_ALL);
                // for every previously sent notification that met our above requirements,
                // add a new line containing its title to the inbox style notification extender
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
                inbox.setSummaryText(summary);
                for (Spannable activeSbn : activeNotifications) {
                    inbox.addLine(activeSbn);
                }

                builder.setStyle(inbox);
                builder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis() / 1000, new Intent(App.getContext(), MapActivity.class), PendingIntent.FLAG_ONE_SHOT));
                builder.setDeleteIntent(PendingIntent.getService(this, INTENT_REQUEST_CODE_CLEAR_EVENTS, (new Intent(this, BackgroundService.class)).setAction(INTENT_ACTION_CLEAR_NOTIFICATIONS), PendingIntent.FLAG_ONE_SHOT));

                Notification stackNotification = builder.build();
                mNotificationManager.notify(NOTIFICATION_GROUP_EVENTS, NOTIFICATION_ID_EVENT_GROUP, stackNotification);
            }
        }
    }

    private void clearEventStackNotification() {
        Timber.v("clearing notification stack");
        activeNotifications.clear();

    }


    private void setupLocationPing() {
        App.getScheduler().scheduleLocationPing();
    }


    private void onWaypointTransition(@NonNull Waypoint w, @NonNull Location l, int transition, @NonNull String trigger) {
        Timber.v("%s transition:%s, trigger:%s", w.getDescription(), transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit", trigger);

        if(ignoreLowAccuracy(l)) {
            Timber.d("ignoring transition: low accuracy ");
            return;
        }


        // Don't send transition if the region is already triggered
        // If the region status is unknown, send transition only if the device is inside

        if (preferences.getFuseRegionDetection() && ((transition == w.getLastTransition()) || (w.isUnknown() && transition == Geofence.GEOFENCE_TRANSITION_EXIT))) {
            Timber.d("ignoring transition: duplicate");
            w.setLastTransition(transition);
            return;
        }

        w.setLastTransition(transition);
        w.setLastTriggered(System.currentTimeMillis());
        App.getDao().getWaypointDao().update(w);

        if(!trigger.equals(MessageTransition.TRIGGER_LOCATION))
            publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR);
        publishTransitionMessage(w, l, transition, trigger);

        App.getEventBus().postSticky(new Events.WaypointTransition(w, transition));

    }

    private void onGeofencingEvent(@Nullable GeofencingEvent event) {

        if (event == null) {
            Timber.e("GeofencingEvent null or hasError");
            return;
        }

        if (event.hasError()) {
            Timber.e("GeofencingEvent hasError: %s", event.getErrorCode());
            return;
        }


        int transition = event.getGeofenceTransition();
        for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

            Waypoint w = App.getDao().loadWaypointForId(event.getTriggeringGeofences().get(index).getRequestId());

            if (w != null)
                onWaypointTransition(w, event.getTriggeringLocation(), transition, MessageTransition.TRIGGER_CIRCULAR);
        }
    }


    private void onLocationChanged(@NonNull Intent intent) {
        LocationResult locationResult = LocationResult.extractResult(intent);
        Location location = locationResult.getLastLocation();
        if(location != null) {
            onLocationChanged(location);
        }
    }

    private void onLocationChanged(@NonNull Location location) {
        if (((lastLocation == null) || (location.getTime() > lastLocation.getTime()))) {
            Timber.v("location update received: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

            lastLocation = location;

            App.getEventBus().postSticky(lastLocation);

            for(Waypoint w : waypoints) {
                onWaypointTransition(w, location, location.distanceTo(w.getLocation()) <=w.getGeofenceRadius() ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT, MessageTransition.TRIGGER_LOCATION);
            }

            publishLocationMessage(MessageLocation.REPORT_TYPE_DEFAULT);

        }
    }

    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

    private void publishLocationMessage(@Nullable String trigger) {
        Timber.v("trigger:%s", trigger);

        if (lastLocation == null) {
            Timber.e("no location available");
            return;
        }

        // Automatic updates are discarded if automatic reporting is disabled
        if ((trigger == null || MessageLocation.REPORT_TYPE_PING.equals(trigger)) && !preferences.getPub()) {
            return;
        }

        if (ignoreLowAccuracy(lastLocation)) {
            return;
        }

        MessageLocation message = new MessageLocation();
        message.setLat(lastLocation.getLatitude());
        message.setLon(lastLocation.getLongitude());
        message.setAlt(lastLocation.getAltitude());
        message.setAcc(Math.round(lastLocation.getAccuracy()));
        message.setT(trigger);
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setTid(preferences.getTrackerId(true));
        message.setCp(preferences.getCp());
        if (preferences.getPubLocationExtendedData()) {
            message.setBatt(App.getBatteryLevel());

            NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (activeNetwork != null) {

                if (!activeNetwork.isConnected()) {
                    message.setConn(MessageLocation.CONN_TYPE_OFFLINE);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    message.setConn(MessageLocation.CONN_TYPE_WIFI);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    message.setConn(MessageLocation.CONN_TYPE_MOBILE);
                }
            }
        }

        App.getMessageProcessor().sendMessage(message);
    }

    private void publishTransitionMessage(@NonNull Waypoint w, @NonNull Location triggeringLocation, int transition, String trigger) {
        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(trigger);
        message.setTid(preferences.getTrackerId(true));
        message.setLat(triggeringLocation.getLatitude());
        message.setLon(triggeringLocation.getLongitude());
        message.setAcc(triggeringLocation.getAccuracy());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
        message.setDesc(w.getShared() ? w.getDescription() : null);
        App.getMessageProcessor().sendMessage(message);

    }


    @SuppressWarnings("MissingPermission")
    private void setupLocationRequest() {
        if(missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        Timber.v("updating location request");
        if (mFusedLocationClient == null) {
            Timber.e("mFusedLocationClient not available");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(getLocationPendingIntent());
        LocationRequest request = App.isInForeground() ? getForegroundLocationRequest() : getBackgroundLocationRequest();
        mFusedLocationClient.requestLocationUpdates(request, getLocationPendingIntent());
    }

    private PendingIntent getLocationPendingIntent() {
        Intent locationIntent = new Intent(getApplicationContext(), BackgroundService.class);
        return PendingIntent.getService(getApplicationContext(), INTENT_REQUEST_CODE_LOCATION, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent geofeneIntent = new Intent(getApplicationContext(), BackgroundService.class);
        geofeneIntent.setAction(INTENT_ACTION_SEND_EVENT_CIRCULAR);
        return PendingIntent.getService(getApplicationContext(), INTENT_REQUEST_CODE_GEOFENCE, geofeneIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private LocationRequest getBackgroundLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(TimeUnit.SECONDS.toMillis(preferences.getLocatorInterval()));
        request.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        request.setSmallestDisplacement(preferences.getLocatorDisplacement());
        request.setPriority(getLocationRequestPriority(true));
        return request;
    }

    private LocationRequest getForegroundLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(TimeUnit.SECONDS.toMillis(TimeUnit.SECONDS.toMillis(10)));
        request.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        request.setSmallestDisplacement(50);
        request.setPriority(getLocationRequestPriority(false));
        return request;
    }


    private int getLocationRequestPriority(boolean background) {
        Timber.v("request background:%s, acc:%s ", background, preferences.getLocatorAccuracyBackground());

        switch (background ? preferences.getLocatorAccuracyBackground() : preferences.getLocatorAccuracyForeground()) {
            case 0:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case 1:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
            case 2:
                return LocationRequest.PRIORITY_LOW_POWER;
            case 3:
                return LocationRequest.PRIORITY_NO_POWER;
            default:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void setupGeofences() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        Timber.v("loader thread:%s, isMain:%s", Looper.myLooper(), Looper.myLooper() == Looper.getMainLooper());

        LinkedList<Geofence> geofences = new LinkedList<>();

        waypoints = App.getDao().loadWaypointsForCurrentMode();

        for (Waypoint w : waypoints) {

            if (w.hasGeofence()) {
                Timber.v("desc:%s", w.getDescription());

                w.setLastTransition(0); // Reset in-memory status

                try {
                    geofences.add(new Geofence.Builder()
                            .setRequestId(w.getId().toString())
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setNotificationResponsiveness((int) TimeUnit.MINUTES.toMillis(2))
                            .setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
                            .setExpirationDuration(Geofence.NEVER_EXPIRE).build());

                } catch (Exception e) {
                    Timber.e("invalid geofence for waypoint %s", w.getDescription());
                }
            }
        }

        if(geofences.size()> 0) {
            GeofencingRequest.Builder b = new GeofencingRequest.Builder();
            GeofencingRequest request = b.addGeofences(geofences).build();
            mGeofencingClient.addGeofences(request, getGeofencePendingIntent());
        }

    }



    private boolean missingLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
    }

    private void removeGeofences() {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Waypoint e) {
        removeGeofences();
        removeBeacons();
        setupGeofences();
        setupBeacons();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.ModeChanged e) {
        removeGeofences();
        setupGeofences();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageTransition message) {
        Timber.v("transition isIncoming:%s topic:%s", message.isIncoming(), message.getTopic());
        if(message.isIncoming())
            sendEventNotification(message);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageLocation m) {
        if(m.isOutgoing() && (lastLocationMessage == null || lastLocationMessage.getTst() <=  m.getTst())) {
            this.lastLocationMessage = m;
            App.getGeocodingProvider().resolve(m, this);
        }
    }

    public void onGeocodingProviderResult(MessageLocation m) {
        if(m == lastLocationMessage) {
            sendOngoingNotification();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true)
    public void onEvent(MessageProcessor.EndpointState state) {
        Timber.v("endpoint state changed %s", state.getLabel(this));
        this.lastEndpointState = state;
        sendOngoingNotification();
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true)
    public void onEvent(Events.PermissionGranted event) {
        try {
            Timber.v("location permission granted. Getting last location.");
            mFusedLocationClient.getLastLocation().addOnCompleteListener(this);
        } catch (SecurityException ignored) {}
        setupGeofences();
        setupBeacons();
        setupLocationRequest();
    }



    public NotificationCompat.Builder getEventsNotificationBuilder() {
        if (!preferences.getNotificationEvents())
            return null;

        Timber.v("building notification builder");


        if(notificationBuilderEvents!=null)
            return notificationBuilderEvents;

        Timber.v("builder not present, lazy building");


        notificationBuilderEvents = new NotificationCompat.Builder(this);


        Intent openIntent = new Intent(this, MapActivity.class);
        openIntent.setAction("android.intent.action.MAIN");
        openIntent.addCategory("android.intent.category.LAUNCHER");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(openPendingIntent);
        //notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(true);
        notificationBuilderEvents.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEvents.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEvents.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationBuilderEvents.setColor(getColor(R.color.primary));
        }

        return notificationBuilderEvents;
    }

    public void setupBeaconManager() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null || preferences.getBeaconMode() == BEACON_MODE_OFF) {
            Timber.e("bluetooth not available or BEACON_MODE_OFF");
            return;
        }

        try { // bluetoothAdapter.isMultipleAdvertisementSupported might throw a NullPointerException for some reason
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Timber.v("isMultipleAdvertisementSupported: %s", bluetoothAdapter.isMultipleAdvertisementSupported());
                Timber.v("isOffloadedFilteringSupported: %s", bluetoothAdapter.isOffloadedFilteringSupported());
                Timber.v("isOffloadedScanBatchingSupported: %s", bluetoothAdapter.isOffloadedScanBatchingSupported());
            } } catch(NullPointerException ignored) {}


        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25")); //altbeacon
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")); //iBeacon
        try {
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(preferences.getBeaconLayout())); // custom
        } catch (BeaconParser.BeaconLayoutException ignored) {}
        beaconManager.setForegroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(30));
        beaconManager.setBackgroundBetweenScanPeriod(TimeUnit.SECONDS.toMillis(120));
        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        if(preferences.getBeaconMode()  == BEACON_MODE_LEGACY_SCANNING) {
            BeaconManager.setAndroidLScanningDisabled(true);
        }

        try {
            beaconManager.updateScanPeriods();
        } catch (RemoteException ignored) {}

        beaconManager.addMonitorNotifier(this);
        beaconManager.addRangeNotifier(this);
        beaconManager.setRegionStatePersistenceEnabled(true);
        setupBeacons();
    }

    private void setupBeacons() {
        if(beaconManager == null)
            return;

        for (Waypoint w : waypoints) {
            if (w.hasBeacon()) {

                try {
                    Region r = new Region(w.getId().toString(), Identifier.parse(w.getId().toString()), w.getBeaconMajor() != null ? Identifier.fromInt(w.getBeaconMajor()) : null, w.getBeaconMinor() != null ? Identifier.fromInt(w.getBeaconMinor()) : null);
                    beaconManager.startMonitoringBeaconsInRegion(r);
                    Timber.v("region %s UUID:%s major:%s minor:%s", r.getUniqueId(), r.getId1(), r.getId2(), r.getId3());
                } catch (Exception ignored) { }

            }
        }
    }

    void removeBeacons() {
        if(beaconManager == null)
            return;

        for(Region r : beaconManager.getMonitoredRegions()) {
            try {
                beaconManager.stopMonitoringBeaconsInRegion(r);
            } catch (RemoteException ignored) { }
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {

    }

    @Override
    public void didEnterRegion(Region region) {
        Waypoint w = App.getDao().loadWaypointForId(region.getUniqueId());
        onWaypointTransition (w, w.getLocation(), Geofence.GEOFENCE_TRANSITION_ENTER, MessageTransition.TRIGGER_BEACON);

    }

    @Override
    public void didExitRegion(Region region) {
        Waypoint w = App.getDao().loadWaypointForId(region.getUniqueId());
        onWaypointTransition(w, w.getLocation(), Geofence.GEOFENCE_TRANSITION_EXIT, MessageTransition.TRIGGER_BEACON);
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Timber.v("state determined for %s", region.getUniqueId());
    }

    @Override
    public void onComplete(@NonNull Task<Location> task) {
        Location l = task.getResult();
        if(l != null)
            onLocationChanged(l);
    }
}
