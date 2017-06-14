package org.owntracks.android.services;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.Service;
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
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class BackgroundService extends Service {
    public static final int INTENT_REQUEST_CODE_LOCATION = 1263;
    private static final int INTENT_REQUEST_CODE_GEOFENCE = 1264;

    private static final int NOTIFICATION_ID_ONGOING = 1;
    private static final int NOTIFICATION_ID_EVENT_GROUP = 2;

    private String NOTIFICATION_GROUP_EVENTS = "events";

    public static final String INTENT_ACTION_CHANGE_BG = "BG";
    public static final String INTENT_ACTION_SEND_LOCATION_PING = "LP";
    public static final String INTENT_ACTION_SEND_LOCATION_USER = "LU";
    public static final String INTENT_ACTION_SEND_LOCATION_RESPONSE = "LR";
    public static final String INTENT_ACTION_SEND_WAYPOINTS = "W";
    public static final String INTENT_ACTION_SEND_EVENT_CIRCULAR = "EC";

    private FusedLocationProviderClient mFusedLocationClient;
    private GeofencingClient mGeofencingClient;

    private Location lastLocation;
    private MessageLocation lastLocationMessage;
    private MessageProcessor.EndpointState lastEndpointState = MessageProcessor.EndpointState.INITIAL;


    private NotificationCompat.Builder activeNotificationBuilder;
    private NotificationCompat.Builder notificationBuilderEvents;
    private NotificationManager mNotificationManager;
    private Preferences preferences;
    private List<Waypoint> waypoints = new LinkedList<>();

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
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        sendOngoingNotification();

        setupLocationRequest();
        setupGeofences();
        setupLocationPing();

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
                    return;
                case INTENT_ACTION_SEND_WAYPOINTS:
                    Timber.e("INTENT_ACTION_SEND_WAYPOINTS not implemented");
                    return;


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

        Intent resultIntent = new Intent(App.getContext(), ActivityWelcome.class);
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
        String transition = getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving);
        String location = message.getDesc();

        if (location == null) {
            location = getString(R.string.aLocation);
        }
        String name = message.getTid();

        if (c != null)
            name = c.getFusedName();
        else if (name == null) {
            name = message.getContactKey();
        }

        notificationBuilderEvents.setContentTitle(name);
        notificationBuilderEvents.setContentText(transition + " " + location);
        notificationBuilderEvents.setWhen(when);
        notificationBuilderEvents.setShowWhen(true);
        // Deliver notification
        Notification n = notificationBuilderEvents.build();

        Timber.v("sending new transition notification");
        mNotificationManager.notify((int) System.currentTimeMillis() / 1000, n);

        sendEventStackNotification();
    }


    private void sendEventStackNotification() {
        if (Build.VERSION.SDK_INT >= 23) {
            Timber.v("SDK_INT >= 23, building stack notification");
            ArrayList<StatusBarNotification> groupedNotifications = new ArrayList<>();

            for (StatusBarNotification sbn : mNotificationManager.getActiveNotifications()) {
                // add any previously sent notifications with a group that matches our RemoteNotification
                // and exclude any previously sent stack notifications
                if (sbn.getId() != NOTIFICATION_ID_EVENT_GROUP && sbn.getId() != NOTIFICATION_ID_ONGOING) {
                    groupedNotifications.add(sbn);
                }
            }

            Timber.v("have %s groupedNotifications", groupedNotifications.size());

            // since we assume the most recent notification was delivered just prior to calling this method,
            // we check that previous notifications in the group include at least 2 notifications
            if (groupedNotifications.size() > 1) {

                String title = String.format(getString(R.string.notificationEventsTitle), groupedNotifications.size());
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

                // use convenience methods on our RemoteNotification wrapper to create a title
                builder.setContentTitle(getString(R.string.events));
                builder.setContentText(title);

                // for every previously sent notification that met our above requirements,
                // add a new line containing its title to the inbox style notification extender
                NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();

                for (StatusBarNotification activeSbn : groupedNotifications) {
                    String stackNotificationTitle = (String) activeSbn.getNotification().extras.get(NotificationCompat.EXTRA_TITLE);
                    String stackNotificationText = (String) activeSbn.getNotification().extras.get(NotificationCompat.EXTRA_TEXT);
                    String stackNotificationWhen = App.formatDate(TimeUnit.MILLISECONDS.toSeconds((activeSbn.getNotification().when)));


                    if (stackNotificationTitle != null && stackNotificationText != null) {
                        Spannable newLine = new SpannableString(String.format("%s %s %s", stackNotificationWhen, stackNotificationTitle, stackNotificationText));
                        newLine.setSpan(new StyleSpan(Typeface.BOLD), 0, stackNotificationWhen.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inbox.addLine(newLine);
                    }
                }

                inbox.setSummaryText(title);

                builder.setStyle(inbox);

                builder.setGroup(NOTIFICATION_GROUP_EVENTS); // same as group of single notifications
                builder.setGroupSummary(true);
                builder.setColor(getColor(R.color.primary));

                // if the user taps the notification, it should disappear after firing its content intent
                // and we set the priority to high to avoid Doze from delaying our notifications
                builder.setAutoCancel(true);
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
                builder.setSmallIcon(R.drawable.ic_notification);

                // create a unique PendingIntent using an integer request code.
                final int requestCode = (int) System.currentTimeMillis() / 1000;
                builder.setContentIntent(PendingIntent.getActivity(App.getContext(), requestCode, new Intent(App.getContext(), ActivityWelcome.class), PendingIntent.FLAG_ONE_SHOT));

                Notification stackNotification = builder.build();
                stackNotification.defaults = Notification.DEFAULT_ALL;

                mNotificationManager.notify(NOTIFICATION_GROUP_EVENTS, NOTIFICATION_ID_EVENT_GROUP, stackNotification);
            }
        }
    }


    private void setupLocationPing() {
        App.getScheduler().scheduleLocationPing();
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

            Waypoint w = App.getDao().loadWaypointForGeofenceId(event.getTriggeringGeofences().get(index).getRequestId());

            if (w != null) {
                Timber.v("waypoint triggered:%s transition:%s", w.getDescription(), transition);
                w.setLastTriggered(System.currentTimeMillis());

                App.getDao().getWaypointDao().update(w);
                App.getEventBus().postSticky(new Events.WaypointTransition(w, transition));
                publishLocationMessage(MessageLocation.REPORT_TYPE_CIRCULAR);
                publishTransitionMessage(w, event.getTriggeringLocation(), transition);

                //if(transition == Geofence.GEOFENCE_TRANSITION_EXIT || BuildConfig.DEBUG) {
                //    Timber.v("starting location lookup");
                //    LocationManager mgr = LocationManager.class.cast(context.getSystemService(Context.LOCATION_SERVICE));
                //    Criteria criteria = new Criteria();
                //    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                //    Bundle b = new Bundle();
                //    b.putInt("event", transition);
                //    b.putString("geofenceId", event.getTriggeringGeofences().get(index).getRequestId());
//
                //    PendingIntent p = ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_GEOFENCE_TRANSITION_LOOKUP, b);
//
                //    mgr.requestSingleUpdate(mgr.getBestProvider(criteria, true), p );
                //}
            }
        }

    }


    private void onLocationChanged(@NonNull Intent intent) {
        LocationResult locationResult = LocationResult.extractResult(intent);
        Location location = locationResult.getLastLocation();
        if (location != null) {
            Timber.v("location update received: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

            lastLocation = location;

            App.getEventBus().postSticky(lastLocation);

            for(Waypoint w : waypoints) {
                float meters = location.distanceTo(w.getLocation());

                Timber.v("location distance to waypoint %s,%s is %s, currently inside: %s", w.getDescription(), w.getGeofenceRadius(),meters, w.getStatus() == 1);
                if(meters <=w.getGeofenceRadius() && (w.getStatus() == 0 || w.getStatus() == -1)) {
                    Timber.v("entered geofence");
                    w.setStatus(1);
                    publishTransitionMessage(w, location, Geofence.GEOFENCE_TRANSITION_ENTER);
                } else if(meters >w.getGeofenceRadius() && w.getStatus() == 1) {
                    Timber.v("left geofence");
                    w.setStatus(-1);
                    publishTransitionMessage(w, location, Geofence.GEOFENCE_TRANSITION_EXIT );
                }
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
        if ((trigger == MessageLocation.REPORT_TYPE_DEFAULT || MessageLocation.REPORT_TYPE_PING.equals(trigger)) && !preferences.getPub()) {
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

    private void publishTransitionMessage(@NonNull Waypoint w, @NonNull Location triggeringLocation, int transition) {
        if (ignoreLowAccuracy(triggeringLocation))
            return;

        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(MessageTransition.TRIGGER_CIRCULAR);
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
        request.setPriority(getLocationRequestPriority(true));
        return request;
    }


    private int getLocationRequestPriority(boolean background) {
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
        if(missingLocationPermission()) {
            Timber.e("missing location permission");
            return;
        }

        Timber.v("loader thread:%s, isMain:%s", Looper.myLooper(), Looper.myLooper() == Looper.getMainLooper());

        LinkedList<Geofence> fences = new LinkedList<>();
        waypoints = App.getDao().loadWaypointsForCurrentModeWithValidGeofence();
        for (Waypoint w : waypoints ) {

            Timber.v("desc:%s", w.getDescription());
            // if id is null, waypoint is not added yet
            if (w.getGeofenceId() == null) {
                w.setGeofenceId(UUID.randomUUID().toString());
                App.getDao().getWaypointDao().update(w);
                Timber.v("new fence found without UUID");
            }

            Geofence geofence = new Geofence.Builder()
                    .setRequestId(w.getGeofenceId())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setNotificationResponsiveness((int) TimeUnit.MINUTES.toMillis(2))
                    .setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE).build();

            Timber.v("adding geofence for waypoint %s, mode:%s", w.getDescription(), w.getModeId());
            fences.add(geofence);
        }

        if (fences.isEmpty()) {
            return;
        }

        GeofencingRequest.Builder b = new GeofencingRequest.Builder();
        GeofencingRequest request = b.addGeofences(fences).build();
        mGeofencingClient.addGeofences(request, getGeofencePendingIntent());

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
        setupGeofences();
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
        Timber.v("transition %s", message.isIncoming());
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

    public NotificationCompat.Builder getEventsNotificationBuilder() {
        if (!preferences.getNotificationEvents())
            return null;

        Timber.v("building notification builder");


        if(notificationBuilderEvents!=null)
            return notificationBuilderEvents;

        Timber.v("builder not present, lazy building");


        notificationBuilderEvents = new NotificationCompat.Builder(this);


        Intent openIntent = new Intent(this, ActivityWelcome.class);
        openIntent.setAction("android.intent.action.MAIN");
        openIntent.addCategory("android.intent.category.LAUNCHER");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(openPendingIntent);
        //notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(true);
        notificationBuilderEvents.setGroup(NOTIFICATION_GROUP_EVENTS);
        notificationBuilderEvents.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notificationBuilderEvents.setCategory(NotificationCompat.CATEGORY_SERVICE);
        notificationBuilderEvents.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationBuilderEvents.setColor(getColor(R.color.primary));
        }

        return notificationBuilderEvents;
    }
}
