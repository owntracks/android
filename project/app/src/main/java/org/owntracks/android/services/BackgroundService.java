package org.owntracks.android.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.LongSparseArray;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.injection.components.DaggerServiceComponent;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.DateFormatter;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Runner;
import org.owntracks.android.ui.map.MapActivity;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.DaggerService;
import timber.log.Timber;

public class BackgroundService extends DaggerService implements OnCompleteListener<Location> {
    private static final int INTENT_REQUEST_CODE_LOCATION = 1263;
    private static final int INTENT_REQUEST_CODE_GEOFENCE = 1264;
    private static final int INTENT_REQUEST_CODE_CLEAR_EVENTS = 1263;

    private static final int NOTIFICATION_ID_ONGOING = 1;
    private static final String NOTIFICATION_CHANNEL_ONGOING = "O";

    private static final int NOTIFICATION_ID_EVENT_GROUP = 2;
    private static final String NOTIFICATION_CHANNEL_EVENTS = "E";

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
    public static final String INTENT_ACTION_REREQUEST_LOCATION_UPDATES = "RRLU";

    private FusedLocationProviderClient mFusedLocationClient;
    private GeofencingClient mGeofencingClient;

    private LocationCallback locationCallback;
    private MessageLocation lastLocationMessage;
    private MessageProcessor.EndpointState lastEndpointState = MessageProcessor.EndpointState.INITIAL;


    private NotificationCompat.Builder activeNotificationBuilder;
    private NotificationCompat.Builder notificationBuilderEvents;
    private NotificationManager notificationManager;

    private NotificationManagerCompat notificationManagerCompat;
    private LongSparseArray<WaypointModel> waypoints = new LongSparseArray<>();

    private final LinkedList<Spannable> activeNotifications = new LinkedList<>();
    private int lastQueueLength = 0;
    private Notification stackNotification;

    @Inject
    Preferences preferences;

    @Inject
    protected EventBus eventBus;

    @Inject
    protected Scheduler scheduler;

    @Inject
    protected MessageProcessor messageProcessor;

    @Inject
    protected GeocodingProvider geocodingProvider;

    @Inject
    protected ContactsRepo contactsRepo;

    @Inject
    LocationRepo locationRepo;

    @Inject
    protected Runner runner;

    @Inject
    protected WaypointsRepo waypointsRepo;

    @Override
    public void onCreate() {

        Timber.v("Preferences instance: %s", preferences);

        //preferences = App.getPreferences();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        notificationManagerCompat = NotificationManagerCompat.from(this); //getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onLocationChanged(locationResult.getLastLocation());
            }
        };

        setupNotificationChannels();
        sendOngoingNotification();

        setupLocationRequest();
        setupLocationPing();

        setupGeofences();

        eventBus.register(this);
        eventBus.postSticky(new Events.ServiceStarted());
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
        if (intent.getAction() != null) {
            Timber.v("intent received with action:%s", intent.getAction());

            switch (intent.getAction()) {
                case INTENT_ACTION_CHANGE_BG:
                    setupLocationRequest();
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
                    publishLocationMessage(MessageLocation.REPORT_TYPE_RESPONSE);
                    return;
                case INTENT_ACTION_SEND_WAYPOINTS:
                    Timber.e("INTENT_ACTION_SEND_WAYPOINTS not implemented");
                    return;
                case INTENT_ACTION_CLEAR_NOTIFICATIONS:
                    clearEventStackNotification();
                case INTENT_ACTION_REREQUEST_LOCATION_UPDATES:
                    setupLocationRequest();

                default:
                    Timber.v("unhandled intent action received: %s", intent.getAction());
            }
        }
    }

    public void setupNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        NotificationChannel ongoingChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ONGOING, getString(R.string.notificationChannelOngoing), NotificationManager.IMPORTANCE_DEFAULT);
        ongoingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ongoingChannel.setDescription(getString(R.string.notificationChannelOngoingDescription));
        ongoingChannel.enableLights(false);
        ongoingChannel.enableVibration(false);
        ongoingChannel.setShowBadge(false);
        ongoingChannel.setSound(null, null);
        notificationManager.createNotificationChannel(ongoingChannel);

        NotificationChannel eventsChannel = new NotificationChannel(NOTIFICATION_CHANNEL_EVENTS, getString(R.string.events), NotificationManager.IMPORTANCE_HIGH);
        eventsChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        eventsChannel.setDescription(getString(R.string.notificationChannelEventsDescription));
        eventsChannel.enableLights(false);
        eventsChannel.enableVibration(false);
        eventsChannel.setShowBadge(true);
        eventsChannel.setSound(null, null);
        notificationManager.createNotificationChannel(eventsChannel);
    }


    @Nullable
    private NotificationCompat.Builder getOngoingNotificationBuilder() {
        if (activeNotificationBuilder != null)
            return activeNotificationBuilder;


        activeNotificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ONGOING);


        Intent resultIntent = new Intent(this, MapActivity.class);
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


        if (this.lastLocationMessage != null && preferences.getNotificationLocation()) {
            builder.setContentTitle(this.lastLocationMessage.getGeocoder());
            builder.setWhen(TimeUnit.SECONDS.toMillis(this.lastLocationMessage.getTst()));
            builder.setNumber(lastQueueLength);
        } else {
            builder.setContentTitle(getString(R.string.app_name));
        }

        builder.setPriority(preferences.getNotificationHigherPriority() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN);
        builder.setSound(null, AudioManager.STREAM_NOTIFICATION);
        builder.setContentText(lastEndpointState.getLabel(this));
        startForeground(NOTIFICATION_ID_ONGOING, builder.build());
    }


    private void sendEventNotification(MessageTransition message) {
        NotificationCompat.Builder builder = getEventsNotificationBuilder();

        if (builder == null) {
            Timber.e("no builder returned");
            return;
        }

        FusedContact c = contactsRepo.getById(message.getContactKey());

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
        notificationManagerCompat.notify(notificationEventsID++, n);
        //notificationManagerCompat.notify(NOTIFICATION_TAG_EVENTS_STACK, System.currentTimeMillis() / 1000, n) ;
        sendEventStackNotification(title, text, when);
    }


    private void sendEventStackNotification(String title, String text, long when) {
        if (Build.VERSION.SDK_INT >= 23) {
            Timber.v("SDK_INT >= 23, building stack notification");

            String whenStr = DateFormatter.formatDate(TimeUnit.MILLISECONDS.toSeconds((when)));

            Spannable newLine = new SpannableString(String.format("%s %s %s", whenStr, title, text));
            newLine.setSpan(new StyleSpan(Typeface.BOLD), 0, whenStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            activeNotifications.push(newLine);
            Timber.v("groupedNotifications: %s", activeNotifications.size());

            // since we assume the most recent notification was delivered just prior to calling this method,
            // we check that previous notifications in the group include at least 2 notifications
            if (activeNotifications.size() > 1) {

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_EVENTS);
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
                // insert a new line containing its title to the inbox style notification extender
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
                inbox.setSummaryText(summary);


                // Append new notification to existing
                CharSequence cs[] = null;

                if (stackNotification != null) {
                    cs = (CharSequence[]) stackNotification.extras.get(NotificationCompat.EXTRA_TEXT_LINES);
                }

                if (cs == null) {
                    cs = new CharSequence[0];
                }

                for (int i = 0; i < cs.length && i < 19; i++) {
                    inbox.addLine(cs[i]);
                }
                inbox.addLine(newLine);

                builder.setNumber(cs.length + 1);
                builder.setStyle(inbox);
                builder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis() / 1000, new Intent(this, MapActivity.class), PendingIntent.FLAG_ONE_SHOT));
                builder.setDeleteIntent(PendingIntent.getService(this, INTENT_REQUEST_CODE_CLEAR_EVENTS, (new Intent(this, BackgroundService.class)).setAction(INTENT_ACTION_CLEAR_NOTIFICATIONS), PendingIntent.FLAG_ONE_SHOT));

                stackNotification = builder.build();
                notificationManagerCompat.notify(NOTIFICATION_GROUP_EVENTS, NOTIFICATION_ID_EVENT_GROUP, stackNotification);
            }
        }
    }

    private void clearEventStackNotification() {
        Timber.v("clearing notification stack");
        activeNotifications.clear();

    }


    private void setupLocationPing() {
        scheduler.scheduleLocationPing();
    }


    @MainThread
    private void onWaypointTransition(WaypointModel w, @NonNull final Location l, final int transition, @NonNull final String trigger) {
        Timber.v("geofence %s/%s transition:%s, trigger:%s", w.getTst(), w.getDescription(), transition == Geofence.GEOFENCE_TRANSITION_ENTER ? "enter" : "exit", trigger);

        if (ignoreLowAccuracy(l)) {
            Timber.d("ignoring transition: low accuracy ");
            return;
        }

        // Don't send transition if the region is already triggered
        // If the region status is unknown, send transition only if the device is inside
        if (((transition == w.getLastTransition()) || (w.isUnknown() && transition == Geofence.GEOFENCE_TRANSITION_EXIT))) {
            Timber.d("ignoring initial or duplicate transition");
            w.setLastTransition(transition);
            waypointsRepo.update(w, false);
            return;
        }

        w.setLastTransition(transition);
        w.setLastTriggeredNow();
        waypointsRepo.update(w, false);

        publishTransitionMessage(w, l, transition, trigger);
        if (trigger.equals(MessageTransition.TRIGGER_CIRCULAR)) {
            publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR);
        }

        eventBus.postSticky(new Events.WaypointTransition(w, transition));
    }


    private void onGeofencingEvent(@Nullable final GeofencingEvent event) {

        if (event == null) {
            Timber.e("geofencingEvent null or hasError");
            return;
        }

        if (event.hasError()) {
            Timber.e("geofencingEvent hasError: %s", event.getErrorCode());
            return;
        }

        final int transition = event.getGeofenceTransition();
        for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {
            onWaypointTransition(waypoints.get(Long.parseLong(event.getTriggeringGeofences().get(index).getRequestId())), event.getTriggeringLocation(), transition, MessageTransition.TRIGGER_CIRCULAR);
        }
    }

    public void onLocationChanged(@Nullable Location location) {

        if (location != null && location.getTime() > locationRepo.getCurrentLocationTime()) {
            Timber.v("location update received: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

            locationRepo.setCurrentLocation(location);
            publishLocationMessage(MessageLocation.REPORT_TYPE_DEFAULT);
        }
    }


    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

    private void publishLocationMessage(@Nullable String trigger) {
        Timber.v("trigger:%s", trigger);

        if (!locationRepo.hasLocation()) {
            Timber.e("no location available");
            return;
        }

        // Automatic updates are discarded if automatic reporting is disabled
        if ((trigger == null || MessageLocation.REPORT_TYPE_PING.equals(trigger)) && !preferences.getPub()) {
            return;
        }

        if (ignoreLowAccuracy(locationRepo.getCurrentLocation())) {
            return;
        }
        Location lastLocation = locationRepo.getCurrentLocation();
        // Check if publish would trigger a region if fusedRegionDetection is enabled
        if(waypoints.size() > 0 && preferences.getFuseRegionDetection() && !MessageLocation.REPORT_TYPE_CIRCULAR.equals(trigger)) {
            for(int i = 0; i < waypoints.size(); i++) {
                WaypointModel waypoint = waypoints.get(waypoints.keyAt(i));
                onWaypointTransition(waypoint, locationRepo.getCurrentLocation(), lastLocation.distanceTo(waypoint.getLocation()) <= waypoint.getGeofenceRadius() ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT, MessageTransition.TRIGGER_LOCATION);
            }
        }

        MessageLocation message = new MessageLocation();
        message.setLat(lastLocation.getLatitude());
        message.setLon(lastLocation.getLongitude());
        message.setAlt(lastLocation.getAltitude());
        message.setAcc(Math.round(lastLocation.getAccuracy()));
        if (lastLocation.hasSpeed()) {
            message.setVelocity(lastLocation.getSpeed() * 3.6); // Convert m/s to km/h
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && lastLocation.hasVerticalAccuracy()) {
            message.setVac(Math.round(lastLocation.getVerticalAccuracyMeters()));
        }
        message.setT(trigger);
        if(MessageLocation.REPORT_TYPE_PING.equals(trigger)) {
            message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        } else {
            message.setTst(TimeUnit.MILLISECONDS.toSeconds(lastLocation.getTime()));
        }

        message.setTid(preferences.getTrackerId(true));
        message.setCp(preferences.getCp());
        message.setInRegions(getInRegions());

        if (preferences.getPubLocationExtendedData()) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                message.setBatt(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
            }

            ConnectivityManager cm = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE));
            NetworkInfo activeNetwork;
            if (cm != null && (activeNetwork = cm.getActiveNetworkInfo()) != null) {
                if (!activeNetwork.isConnected()) {
                    message.setConn(MessageLocation.CONN_TYPE_OFFLINE);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    message.setConn(MessageLocation.CONN_TYPE_WIFI);
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    message.setConn(MessageLocation.CONN_TYPE_MOBILE);
                }
            }
        }

        messageProcessor.sendMessage(message);


    }

    private void publishTransitionMessage(@NonNull WaypointModel w, @NonNull Location triggeringLocation, int transition, String trigger) {
        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(trigger);
        message.setTid(preferences.getTrackerId(true));
        message.setLat(triggeringLocation.getLatitude());
        message.setLon(triggeringLocation.getLongitude());
        message.setAcc(triggeringLocation.getAccuracy());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getTst()));
        message.setDesc(w.getDescription());
        messageProcessor.sendMessage(message);

    }


    @SuppressWarnings("MissingPermission")
    private void setupLocationRequest() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        Timber.v("updating location request");
        if (mFusedLocationClient == null) {
            Timber.e("mFusedLocationClient not available");
            return;
        }

        mFusedLocationClient.removeLocationUpdates(getLocationPendingIntent());
        mFusedLocationClient.requestLocationUpdates(App.isInForeground() ? getForegroundLocationRequest() : getBackgroundLocationRequest(), locationCallback,  runner.getBackgroundHandler().getLooper());
    }

    private PendingIntent getLocationPendingIntent() {
        Intent locationIntent = new Intent(getApplicationContext(), BackgroundService.class);
        return PendingIntent.getBroadcast(getApplicationContext(), INTENT_REQUEST_CODE_LOCATION, locationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent geofeneIntent = new Intent(this, BackgroundService.class);
        geofeneIntent.setAction(INTENT_ACTION_SEND_EVENT_CIRCULAR);
        return PendingIntent.getBroadcast(this, INTENT_REQUEST_CODE_GEOFENCE, geofeneIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        List<WaypointModel> loadedWaypoints = waypointsRepo.getAllWithGeofences();

        waypoints = new LongSparseArray<>(geofences.size());

        for (WaypointModel w : loadedWaypoints){
            waypoints.put(w.getTst(), w);
            Timber.v("desc:%s", w.getDescription());

            geofences.add(new Geofence.Builder()
                    .setRequestId(Long.toString(w.getId()))
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setNotificationResponsiveness((int) TimeUnit.MINUTES.toMillis(2))
                    .setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE).build());
        }

        if (geofences.size() > 0) {
            GeofencingRequest.Builder b = new GeofencingRequest.Builder();
            b.setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER);
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
    public void onEvent(Events.WaypointAdded e) {
        publishWaypointMessage(e.getWaypointModel());
        if(e.getWaypointModel().hasGeofence()) {
            removeGeofences();
            setupGeofences();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointUpdated e) {
        publishWaypointMessage(e.getWaypointModel());
        removeGeofences();
        setupGeofences();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointRemoved e) {
        if(e.getWaypointModel().hasGeofence()) {
            removeGeofences();
            setupGeofences();
        }
    }

    private void publishWaypointMessage(@NonNull WaypointModel e) {
        messageProcessor.sendMessage(waypointsRepo.fromDaoObject(e));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.ModeChanged e) {
        removeGeofences();
        setupGeofences();
        sendOngoingNotification();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageTransition message) {
        Timber.v("transition isIncoming:%s topic:%s", message.isIncoming(), message.getTopic());
        if (message.isIncoming())
            sendEventNotification(message);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageLocation m) {
        Timber.v("MessageLocation received %s, %s, outgoing: %s ", m, lastLocationMessage, m.isOutgoing());
        if (m.isDelivered() && (lastLocationMessage == null || lastLocationMessage.getTst() <= m.getTst())) {
            this.lastLocationMessage = m;
            sendOngoingNotification();
            geocodingProvider.resolve(m, this);
        }
    }

    public void onGeocodingProviderResult(MessageLocation m) {
        if (m == lastLocationMessage) {
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
    public void onEvent(Events.QueueChanged e) {
        this.lastQueueLength = e.getNewLength();
        sendOngoingNotification();
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true)
    public void onEvent(Events.PermissionGranted event) {
        Timber.v("location permission granted");
        removeGeofences();
        setupGeofences();

        try {
            Timber.v("Getting last location");
            mFusedLocationClient.getLastLocation().addOnCompleteListener(this);
        } catch (SecurityException ignored) {
        }

    }


    public NotificationCompat.Builder getEventsNotificationBuilder() {
        if (!preferences.getNotificationEvents())
            return null;

        Timber.v("building notification builder");


        if (notificationBuilderEvents != null)
            return notificationBuilderEvents;

        Timber.v("builder not present, lazy building");


        notificationBuilderEvents = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_EVENTS);


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


    @Override
    public void onComplete(@NonNull Task<Location> task) {
        onLocationChanged(task.getResult());
    }

    private final IBinder mBinder = new LocalBinder();

    public List<String> getInRegions() {
        LinkedList<String> l = new LinkedList<>();
        for(int i = 0; i < waypoints.size(); i++) {
            WaypointModel w = waypoints.get(waypoints.keyAt(i));
            if(w.getLastTransition() == Geofence.GEOFENCE_TRANSITION_ENTER )
                l.add(w.getDescription());

        }
        return l;
    }

    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Timber.v("in onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Timber.v("in onRebind()");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.v("Last client unbound from service");
        return true; // Ensures onRebind() is called when a client re-binds.
    }
}
