package org.owntracks.android.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.owntracks.android.R;
import org.owntracks.android.data.WaypointModel;
import org.owntracks.android.data.repos.ContactsRepo;
import org.owntracks.android.data.repos.LocationRepo;
import org.owntracks.android.data.repos.WaypointsRepo;
import org.owntracks.android.geocoding.GeocoderProvider;
import org.owntracks.android.location.LocationAvailability;
import org.owntracks.android.location.LocationCallback;
import org.owntracks.android.location.LocationProviderClient;
import org.owntracks.android.location.LocationRequest;
import org.owntracks.android.location.LocationResult;
import org.owntracks.android.location.LocationServices;
import org.owntracks.android.location.geofencing.Geofence;
import org.owntracks.android.location.geofencing.GeofencingClient;
import org.owntracks.android.location.geofencing.GeofencingEvent;
import org.owntracks.android.location.geofencing.GeofencingRequest;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.model.messages.MessageTransition;
import org.owntracks.android.services.worker.Scheduler;
import org.owntracks.android.support.DateFormatter;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RunThingsOnOtherThreads;
import org.owntracks.android.support.ServiceBridge;
import org.owntracks.android.support.preferences.OnModeChangedPreferenceChangedListener;
import org.owntracks.android.ui.map.MapActivity;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;
import static androidx.core.app.NotificationCompat.PRIORITY_LOW;
import static org.owntracks.android.App.NOTIFICATION_CHANNEL_EVENTS;
import static org.owntracks.android.App.NOTIFICATION_CHANNEL_ONGOING;
import static org.owntracks.android.geocoding.GeocoderProvider.ERROR_NOTIFICATION_CHANNEL_ID;

@AndroidEntryPoint
public class BackgroundService extends Service implements OnModeChangedPreferenceChangedListener, ServiceBridge.ServiceBridgeInterface {
    private static final int INTENT_REQUEST_CODE_GEOFENCE = 1264;
    private static final int INTENT_REQUEST_CODE_CLEAR_EVENTS = 1263;

    private static final int NOTIFICATION_ID_ONGOING = 1;
    private static final int NOTIFICATION_ID_EVENT_GROUP = 2;
    public static final String BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG = "backgroundRestrictionNotification";

    private static int notificationEventsID = 3;

    private final String NOTIFICATION_GROUP_EVENTS = "events";

    // NEW ACTIONS ALSO HAVE TO BE ADDED TO THE SERVICE INTENT FILTER
    private static final String INTENT_ACTION_CLEAR_NOTIFICATIONS = "org.owntracks.android.CLEAR_NOTIFICATIONS";
    private static final String INTENT_ACTION_SEND_LOCATION_USER = "org.owntracks.android.SEND_LOCATION_USER";
    private static final String INTENT_ACTION_SEND_EVENT_CIRCULAR = "org.owntracks.android.SEND_EVENT_CIRCULAR";
    private static final String INTENT_ACTION_REREQUEST_LOCATION_UPDATES = "org.owntracks.android.REREQUEST_LOCATION_UPDATES";
    private static final String INTENT_ACTION_CHANGE_MONITORING = "org.owntracks.android.CHANGE_MONITORING";
    private static final String INTENT_ACTION_EXIT = "org.owntracks.android.EXIT";

    private static final String INTENT_ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static final String INTENT_ACTION_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";

    private LocationProviderClient locationProviderClient;
    private GeofencingClient geofencingClient;

    private LocationCallback locationCallback;
    private LocationCallback locationCallbackOnDemand;
    private MessageLocation lastLocationMessage;
    private MessageProcessor.EndpointState lastEndpointState = MessageProcessor.EndpointState.INITIAL;


    private NotificationCompat.Builder activeNotificationCompatBuilder;
    private NotificationCompat.Builder eventsNotificationCompatBuilder;
    private NotificationManager notificationManager;

    private NotificationManagerCompat notificationManagerCompat;

    private final LinkedList<Spannable> activeNotifications = new LinkedList<>();
    private int lastQueueLength = 0;

    private boolean hasBeenStartedExplicitly = false;

    @Inject
    Preferences preferences;

    @Inject
    EventBus eventBus;

    @Inject
    Scheduler scheduler;

    @Inject
    LocationProcessor locationProcessor;

    @Inject
    GeocoderProvider geocoderProvider;

    @Inject
    ContactsRepo contactsRepo;

    @Inject
    LocationRepo locationRepo;

    @Inject
    RunThingsOnOtherThreads runThingsOnOtherThreads;

    @Inject
    WaypointsRepo waypointsRepo;

    @Inject
    ServiceBridge serviceBridge;

    @Inject
    MessageProcessor messageProcessor;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.v("Background service onCreate. ThreadID: %s", Thread.currentThread());
        serviceBridge.bind(this);
        locationProviderClient = LocationServices.INSTANCE.getLocationProviderClient(this, preferences);
        geofencingClient = LocationServices.INSTANCE.getGeofencingClient(this);
        notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(@NotNull LocationAvailability locationAvailability) {
                Timber.d("BackgroundService location availability %s", locationAvailability);
            }

            @Override
            public void onLocationResult(@NotNull LocationResult locationResult) {
                Timber.d("BackgroundService Location result received: %s", locationResult);
                onLocationChanged(locationResult.getLastLocation(), MessageLocation.REPORT_TYPE_DEFAULT);
            }
        };

        locationCallbackOnDemand = new LocationCallback() {
            @Override
            public void onLocationAvailability(@NotNull LocationAvailability locationAvailability) {

            }

            @Override
            public void onLocationResult(@NotNull LocationResult locationResult) {
                Timber.d("BackgroundService Ondemand Locationresult received: %s", locationResult);
                onLocationChanged(locationResult.getLastLocation(), MessageLocation.REPORT_TYPE_RESPONSE);
            }
        };

        startForeground(NOTIFICATION_ID_ONGOING, getOngoingNotification());

        setupLocationRequest();

        scheduler.scheduleLocationPing();

        setupGeofences();

        eventBus.register(this);
        eventBus.postSticky(new Events.ServiceStarted());

        messageProcessor.initialize();

        preferences.registerOnPreferenceChangedListener(this);
    }


    @Override
    public void onDestroy() {
        stopForeground(true);
        preferences.unregisterOnPreferenceChangedListener(this);
        messageProcessor.stopSendingMessages();
        super.onDestroy();
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
                case INTENT_ACTION_SEND_LOCATION_USER:
                    locationProcessor.publishLocationMessage(MessageLocation.REPORT_TYPE_USER);
                    return;
                case INTENT_ACTION_SEND_EVENT_CIRCULAR:
                    onGeofencingEvent(GeofencingEvent.fromIntent(intent));
                    return;
                case INTENT_ACTION_CLEAR_NOTIFICATIONS:
                    clearEventStackNotification();
                    return;
                case INTENT_ACTION_REREQUEST_LOCATION_UPDATES:
                    setupLocationRequest();
                    return;
                case INTENT_ACTION_CHANGE_MONITORING:
                    if (intent.hasExtra(preferences.getPreferenceKey(R.string.preferenceKeyMonitoring))) {
                        preferences.setMonitoring(intent.getIntExtra(preferences.getPreferenceKey(R.string.preferenceKeyMonitoring), preferences.getMonitoring()));
                    } else {
                        // Step monitoring mode if no mode is specified
                        preferences.setMonitoringNext();
                    }
                    hasBeenStartedExplicitly = true;
                    notificationManager.cancel(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0);
                    return;
                case INTENT_ACTION_BOOT_COMPLETED:
                case INTENT_ACTION_PACKAGE_REPLACED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasBeenStartedExplicitly) {
                        notifyUserOfBackgroundLocationRestriction();
                    }
                    return;
                case INTENT_ACTION_EXIT:
                    exit();
                    return;
                default:
                    Timber.v("unhandled intent action received: %s", intent.getAction());
            }
        } else {
            hasBeenStartedExplicitly = true;
        }
    }

    private void exit() {
        stopSelf();
        scheduler.cancelAllTasks();
        killProcess(myPid());
    }

    private void notifyUserOfBackgroundLocationRestriction() {
        Intent activityLaunchIntent = new Intent(getApplicationContext(), MapActivity.class);
        activityLaunchIntent.setAction("android.intent.action.MAIN");
        activityLaunchIntent.addCategory("android.intent.category.LAUNCHER");
        activityLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String notificationText = getString(R.string.backgroundLocationRestrictionNotificationText);
        String notificationTitle = getString(R.string.backgroundLocationRestrictionNotificationTitle);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), ERROR_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_owntracks_80)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, activityLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(PRIORITY_LOW)
                .setNotificationSilent()
                .build();

        notificationManager.notify(BACKGROUND_LOCATION_RESTRICTION_NOTIFICATION_TAG, 0, notification);
    }

    @Nullable
    private NotificationCompat.Builder getOngoingNotificationBuilder() {
        if (activeNotificationCompatBuilder != null)
            return activeNotificationCompatBuilder;

        Intent resultIntent = new Intent(this, MapActivity.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent publishIntent = new Intent();
        publishIntent.setAction(INTENT_ACTION_SEND_LOCATION_USER);
        PendingIntent publishPendingIntent = PendingIntent.getService(this, 0, publishIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent changeMonitoringIntent = new Intent();
        publishIntent.setAction(INTENT_ACTION_CHANGE_MONITORING);
        PendingIntent changeMonitoringPendingIntent = PendingIntent.getService(this, 0, publishIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        activeNotificationCompatBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ONGOING)
                .setContentIntent(resultPendingIntent)
                .setSortKey("a")
                .addAction(R.drawable.ic_baseline_publish_24, getString(R.string.publish), publishPendingIntent)
                .addAction(R.drawable.ic_owntracks_80, getString(R.string.notificationChangeMonitoring), changeMonitoringPendingIntent)
                .setSmallIcon(R.drawable.ic_owntracks_80)
                .setPriority(preferences.getNotificationHigherPriority() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN)
                .setSound(null, AudioManager.STREAM_NOTIFICATION)
                .setOngoing(true);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            activeNotificationCompatBuilder.setColor(getColor(R.color.primary))
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        return activeNotificationCompatBuilder;
    }

    private void updateOngoingNotification() {
        notificationManager.notify(NOTIFICATION_ID_ONGOING, getOngoingNotification());
    }

    private Notification getOngoingNotification() {
        NotificationCompat.Builder builder = getOngoingNotificationBuilder();

        if (builder == null)
            return null;

        if (this.lastLocationMessage != null && preferences.getNotificationLocation()) {
            builder.setContentTitle(this.lastLocationMessage.getGeocode());
            builder.setWhen(TimeUnit.SECONDS.toMillis(this.lastLocationMessage.getTimestamp()));
            builder.setNumber(lastQueueLength);
        } else {
            builder.setContentTitle(getString(R.string.app_name));
        }

        // Show monitoring mode if endpoint state is not interesting
        if (lastEndpointState == MessageProcessor.EndpointState.CONNECTED || lastEndpointState == MessageProcessor.EndpointState.IDLE) {
            builder.setContentText(getMonitoringLabel(preferences.getMonitoring()));
        } else if (lastEndpointState == MessageProcessor.EndpointState.ERROR && lastEndpointState.getMessage() != null) {
            builder.setContentText(lastEndpointState.getLabel(this) + ": " + lastEndpointState.getMessage());
        } else {
            builder.setContentText(lastEndpointState.getLabel(this));
        }
        return builder.build();
    }


    private String getMonitoringLabel(int mode) {
        switch (mode) {
            case LocationProcessor.MONITORING_QUIET:
                return getString(R.string.monitoring_quiet);
            case LocationProcessor.MONITORING_MANUAL:
                return getString(R.string.monitoring_manual);
            case LocationProcessor.MONITORING_SIGNIFICANT:
                return getString(R.string.monitoring_significant);
            case LocationProcessor.MONITORING_MOVE:
                return getString(R.string.monitoring_move);
        }
        return getString(R.string.na);
    }

    private void sendEventNotification(MessageTransition message) {
        NotificationCompat.Builder builder = getEventsNotificationBuilder();

        if (builder == null) {
            Timber.e("no builder returned");
            return;
        }

        FusedContact c = contactsRepo.getById(message.getContactKey());

        long when = TimeUnit.SECONDS.toMillis(message.getTimestamp());
        String location = message.getDescription();

        if (location == null) {
            location = getString(R.string.aLocation);
        }
        String title = message.getTrackerId();
        if (c != null)
            title = c.getFusedName();
        else if (title == null) {
            title = message.getContactKey();
        }

        String text = String.format("%s %s", getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving), location);


        eventsNotificationCompatBuilder.setContentTitle(title);
        eventsNotificationCompatBuilder.setContentText(text);
        eventsNotificationCompatBuilder.setWhen(TimeUnit.SECONDS.toMillis(message.getTimestamp()));
        eventsNotificationCompatBuilder.setShowWhen(true);
        eventsNotificationCompatBuilder.setGroup(NOTIFICATION_GROUP_EVENTS);
        // Deliver notification
        Notification n = eventsNotificationCompatBuilder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            sendEventStackNotification(title, text, when);
        } else {
            notificationManagerCompat.notify(notificationEventsID++, n);
        }
    }

    @RequiresApi(23)
    private void sendEventStackNotification(String title, String text, long when) {
        Timber.v("SDK_INT >= 23, building stack notification");

        String whenStr = DateFormatter.formatDate(TimeUnit.MILLISECONDS.toSeconds((when)));

        Spannable newLine = new SpannableString(String.format("%s %s %s", whenStr, title, text));
        newLine.setSpan(new StyleSpan(Typeface.BOLD), 0, whenStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        activeNotifications.push(newLine);
        Timber.v("groupedNotifications: %s", activeNotifications.size());
        String summary = getResources().getQuantityString(R.plurals.notificationEventsTitle, activeNotifications.size(), activeNotifications.size());

        NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
        inbox.setSummaryText(summary);

        for (Spannable n : activeNotifications) {
            inbox.addLine(n);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_EVENTS)
                .setContentTitle(getString(R.string.events))
                .setContentText(summary)
                .setGroup(NOTIFICATION_GROUP_EVENTS) // same as group of single notifications
                .setGroupSummary(true)
                .setColor(getColor(R.color.primary))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_owntracks_80)
                .setLocalOnly(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setNumber(activeNotifications.size())
                .setStyle(inbox)
                .setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis() / 1000, new Intent(this, MapActivity.class), PendingIntent.FLAG_ONE_SHOT))
                .setDeleteIntent(PendingIntent.getService(this, INTENT_REQUEST_CODE_CLEAR_EVENTS, (new Intent(this, BackgroundService.class)).setAction(INTENT_ACTION_CLEAR_NOTIFICATIONS), PendingIntent.FLAG_ONE_SHOT));

        Notification stackNotification = builder.build();
        notificationManagerCompat.notify(NOTIFICATION_GROUP_EVENTS, NOTIFICATION_ID_EVENT_GROUP, stackNotification);

    }

    private void clearEventStackNotification() {
        Timber.v("clearing notification stack");
        activeNotifications.clear();
    }

    private void onGeofencingEvent(@Nullable final GeofencingEvent event) {
        if (event == null) {
            Timber.e("geofencingEvent null");
            return;
        }

        if (event.hasError()) {
            Timber.e("geofencingEvent hasError: %s", event.getErrorCode());
            return;
        }

        final int transition = event.getGeofenceTransition();
        for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {
            WaypointModel w = waypointsRepo.get(Long.parseLong(event.getTriggeringGeofences().get(index).getRequestId()));
            if (w == null) {
                Timber.e("waypoint id %s not found for geofence event", event.getTriggeringGeofences().get(index).getRequestId());
                continue;
            }
            locationProcessor.onWaypointTransition(w, event.getTriggeringLocation(), transition, MessageTransition.TRIGGER_CIRCULAR);
        }
    }

    void onLocationChanged(@Nullable Location location, @Nullable String reportType) {
        if (location == null) {
            Timber.e("no location provided");
            return;
        }
        Timber.v("location update received: tst:%s, acc:%s, lat:%s, lon:%s type:%s", location.getTime(), location.getAccuracy(), location.getLatitude(), location.getLongitude(), reportType);

        if (location.getTime() > locationRepo.getCurrentLocationTime()) {
            locationProcessor.onLocationChanged(location, reportType);
        } else {
            Timber.v("Not re-sending message with same timestamp as last");
        }
    }

    @SuppressWarnings("MissingPermission")
    public void requestOnDemandLocationUpdate() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        LocationRequest request = new LocationRequest();

        request.setNumUpdates(1);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setExpirationDuration(TimeUnit.MINUTES.toMillis(1));

        Timber.d("On demand location request");
        LocationProviderClient client = LocationServices.INSTANCE.getLocationProviderClient(this, preferences);
        client.requestLocationUpdates(request, locationCallbackOnDemand, runThingsOnOtherThreads.getBackgroundLooper());
    }

    @SuppressWarnings("MissingPermission")
    private void setupLocationRequest() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        if (locationProviderClient == null) {
            Timber.e("FusedLocationClient not available");
            return;
        }
        int monitoring = preferences.getMonitoring();

        LocationRequest request = new LocationRequest();

        switch (monitoring) {
            case LocationProcessor.MONITORING_QUIET:
            case LocationProcessor.MONITORING_MANUAL:
                request.setInterval(TimeUnit.SECONDS.toMillis(preferences.getLocatorInterval()));
                request.setSmallestDisplacement((float) preferences.getLocatorDisplacement());
                request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                break;
            case LocationProcessor.MONITORING_SIGNIFICANT:
                request.setInterval(TimeUnit.SECONDS.toMillis(preferences.getLocatorInterval()));
                request.setSmallestDisplacement((float) preferences.getLocatorDisplacement());
                request.setPriority(getLocationRequestPriority());
                break;
            case LocationProcessor.MONITORING_MOVE:
                request.setInterval(TimeUnit.SECONDS.toMillis(preferences.getMoveModeLocatorInterval()));
                request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
        }
        Timber.d("Location update request params: %s", request);
        locationProviderClient.flushLocations();
        locationProviderClient.requestLocationUpdates(request, locationCallback, runThingsOnOtherThreads.getBackgroundLooper());
    }

    private int getLocationRequestPriority() {
        switch (preferences.getLocatorPriority()) {
            case 0:
                return LocationRequest.PRIORITY_NO_POWER;
            case 1:
                return LocationRequest.PRIORITY_LOW_POWER;
            case 3:
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case 2:
            default:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent geofenceIntent = new Intent(this, BackgroundService.class);
        geofenceIntent.setAction(INTENT_ACTION_SEND_EVENT_CIRCULAR);
        return PendingIntent.getBroadcast(this, INTENT_REQUEST_CODE_GEOFENCE, geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @SuppressWarnings("MissingPermission")
    private void setupGeofences() {
        if (missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        Timber.d("loader thread:%s, isMain:%s", Looper.myLooper(), Looper.myLooper() == Looper.getMainLooper());

        LinkedList<Geofence> geofences = new LinkedList<>();
        List<WaypointModel> loadedWaypoints = waypointsRepo.getAllWithGeofences();


        for (WaypointModel w : loadedWaypoints) {
            Timber.d("id:%s, desc:%s, lat:%s, lon:%s, rad:%s", w.getId(), w.getDescription(), w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius());

            try {
                geofences.add(
                        new Geofence(
                                Long.toString(w.getId()),
                                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT,
                                (int) TimeUnit.MINUTES.toMillis(2),
                                w.getGeofenceLatitude(),
                                w.getGeofenceLongitude(),
                                (float) w.getGeofenceRadius(),
                                Geofence.NEVER_EXPIRE,
                                null
                        )
                );
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Invalid geofence parameter");
            }
        }

        if (geofences.size() > 0) {
            GeofencingRequest request = new GeofencingRequest(Geofence.GEOFENCE_TRANSITION_ENTER, geofences);
            geofencingClient.addGeofences(request, getGeofencePendingIntent());
        }
    }

    private boolean missingLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED;
    }

    private void removeGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent());
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointAdded e) {
        locationProcessor.publishWaypointMessage(e.getWaypointModel()); // TODO: move to waypointsRepo
        if (e.getWaypointModel().hasGeofence()) {
            removeGeofences();
            setupGeofences();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointUpdated e) {
        locationProcessor.publishWaypointMessage(e.getWaypointModel()); // TODO: move to waypointsRepo
        removeGeofences();
        setupGeofences();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointRemoved e) {
        if (e.getWaypointModel().hasGeofence()) {
            removeGeofences();
            setupGeofences();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.ModeChanged e) {
        removeGeofences();
        setupGeofences();
        setupLocationRequest();
        updateOngoingNotification();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.MonitoringChanged e) {
        setupLocationRequest();
        updateOngoingNotification();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageTransition message) {
        Timber.d("transition isIncoming:%s topic:%s", message.isIncoming(), message.getTopic());
        if (message.isIncoming())
            sendEventNotification(message);
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Location location) {
        MessageLocation messageLocation = MessageLocation.fromLocation(location);
        if (lastLocationMessage == null || lastLocationMessage.getTimestamp() < messageLocation.getTimestamp()) {
            this.lastLocationMessage = messageLocation;
            geocoderProvider.resolve(messageLocation, this);
        }
    }

    public void onGeocodingProviderResult(MessageLocation m) {
        if (m == lastLocationMessage) {
            updateOngoingNotification();
        }
    }

    @Subscribe(sticky = true)
    public void onEvent(MessageProcessor.EndpointState state) {
        Timber.d(state.getError(), "endpoint state changed %s. Message: %s", state.getLabel(this), state.getMessage());
        this.lastEndpointState = state;
        updateOngoingNotification();
    }

    @Subscribe(sticky = true)
    public void onEvent(Events.QueueChanged e) {
        this.lastQueueLength = e.getNewLength();
        updateOngoingNotification();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.PermissionGranted event) {
        Timber.d("location permission granted");
        removeGeofences();
        setupGeofences();
        Timber.d("Getting last location");
        Location lastLocation = locationProviderClient.getLastLocation();
        if (lastLocation != null) {
            onLocationChanged(lastLocation, MessageLocation.REPORT_TYPE_DEFAULT);
        }
    }

    @Subscribe
    public void onEvent(Events.RestartApp e) {
        Timber.i("Triggering restart");
        scheduler.cancelAllTasks();
        messageProcessor.stopSendingMessages();
        ProcessPhoenix.triggerRebirth(this);
    }

    private NotificationCompat.Builder getEventsNotificationBuilder() {
        if (!preferences.getNotificationEvents())
            return null;

        Timber.d("building notification builder");

        if (eventsNotificationCompatBuilder != null)
            return eventsNotificationCompatBuilder;

        Timber.d("builder not present, lazy building");
        Intent openIntent = new Intent(this, MapActivity.class);
        openIntent.setAction("android.intent.action.MAIN");
        openIntent.addCategory("android.intent.category.LAUNCHER");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        eventsNotificationCompatBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_EVENTS)
                .setContentIntent(openPendingIntent)
                .setSmallIcon(R.drawable.ic_baseline_add_24)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            eventsNotificationCompatBuilder.setColor(getColor(R.color.primary));
        }

        return eventsNotificationCompatBuilder;
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onAttachAfterModeChanged() {
        //NOOP. Handled through eventbus
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (preferences.getPreferenceKey(R.string.preferenceKeyLocatorInterval).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyLocatorDisplacement).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyLocatorPriority).equals(key) ||
                preferences.getPreferenceKey(R.string.preferenceKeyMoveModeLocatorInterval).equals(key)
        ) {
            Timber.d("locator preferences changed. Resetting location request.");
            setupLocationRequest();
        }
    }

    public class LocalBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Timber.v("in onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Timber.v("Last client unbound from service");
    }
}
