package org.owntracks.android.services;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;

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
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class BackgroundService extends Service {
    public static final int INTENT_REQUEST_CODE_LOCATION = 1263;
    private static final int INTENT_REQUEST_CODE_GEOFENCE = 1264;

    private static final int NOTIFICATION_ID_ONGOING = 1923;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);


        sendOngoingNotification();

        setupLocationRequest();
        setupGeofences();
        setupLocationPing();

        App.getEventBus().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(intent != null) {
            handleIntent(intent);
        }

        return START_STICKY;
    }

    private void handleIntent(@NonNull Intent intent) {
        if (LocationResult.hasResult(intent)) {
            onLocationChanged(intent);
        } else if(intent.getAction() != null){
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
        if (!Preferences.getNotification())
            return null;

        if(activeNotificationBuilder!=null)
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

        if (this.lastLocationMessage != null && this.lastLocationMessage.getGeocoder() != null && Preferences.getNotificationLocation()) {
            builder.setContentTitle(this.lastLocationMessage.getGeocoder());
            builder.setWhen(TimeUnit.SECONDS.toMillis(this.lastLocationMessage.getTst()));
        } else {
            builder.setContentTitle(getString(R.string.app_name));
        }

        builder.setPriority(Preferences.getNotificationHigherPriority() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_MIN);
        builder.setContentText(lastEndpointState.getLabel(App.getContext()));

        startForeground(NOTIFICATION_ID_ONGOING, builder.build());
    }

    private void setupLocationPing() {
        App.getScheduler().scheduleLocationPing();
    }




    private void onGeofencingEvent(@Nullable  GeofencingEvent event) {

        if(event == null) {
            Timber.e("GeofencingEvent null or hasError");
            return;
        }

        if(event.hasError()) {
            Timber.e("GeofencingEvent hasError: %s", event.getErrorCode());
            return;
        }


        int transition = event.getGeofenceTransition();
        for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

                Waypoint w =  App.getDao().loadWaypointForGeofenceId(event.getTriggeringGeofences().get(index).getRequestId());

                if (w != null) {
                    Timber.v("waypoint triggered:%s transition:%s", w.getDescription(),transition);
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

            if (shouldPublishLocation())
                publishLocationMessage(MessageLocation.REPORT_TYPE_DEFAULT);

        }
    }

    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = Preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

    private void publishLocationMessage(@Nullable String trigger) {
        Timber.v("trigger:%s", trigger);

        if(lastLocation == null) {
            Timber.e("no location available");
            return;
        }

        if(ignoreLowAccuracy(lastLocation))
            return;

        MessageLocation message = new MessageLocation();
        message.setLat(lastLocation.getLatitude());
        message.setLon(lastLocation.getLongitude());
        message.setAcc(Math.round(lastLocation.getAccuracy()));
        message.setT(trigger);
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setTid(Preferences.getTrackerId(true));
        message.setCp(Preferences.getCp());
        if(Preferences.getPubLocationExtendedData()) {
            message.setBatt(App.getBatteryLevel());

            NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if(activeNetwork != null) {

                if(!activeNetwork.isConnected()) {
                    message.setConn(MessageLocation.CONN_TYPE_OFFLINE);
                } else if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ) {
                    message.setConn(MessageLocation.CONN_TYPE_WIFI);
                } else if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    message.setConn(MessageLocation.CONN_TYPE_MOBILE);
                }
            }
        }

        App.getMessageProcessor().sendMessage(message);
    }

    private void publishTransitionMessage(@NonNull Waypoint w, @NonNull Location triggeringLocation, int transition) {
        if(ignoreLowAccuracy(triggeringLocation))
            return;

        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTrigger(MessageTransition.TRIGGER_CIRCULAR);
        message.setTid(Preferences.getTrackerId(true));
        message.setLat(triggeringLocation.getLatitude());
        message.setLon(triggeringLocation.getLongitude());
        message.setAcc(triggeringLocation.getAccuracy());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
        message.setDesc(w.getShared() ? w.getDescription() : null);

        App.getMessageProcessor().sendMessage(message);

    }

    private boolean shouldPublishLocation() {
        if(!Preferences.getPub())
            return false;

        if (!App.isInForeground())
            return true;

        // Publishes are throttled to 30 seconds when in the foreground to not spam the server
        return (System.currentTimeMillis() - this.lastLocation.getTime()) > TimeUnit.SECONDS.toMillis(30);
    }

    @SuppressWarnings("MissingPermission")
    private void setupLocationRequest() {
        Timber.v("updating location request");
        if(mFusedLocationClient == null) {
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
        request.setInterval(TimeUnit.SECONDS.toMillis(Preferences.getLocatorInterval()));
        request.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        request.setSmallestDisplacement(Preferences.getLocatorDisplacement());
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
        switch (background ? Preferences.getLocatorAccuracyBackground() : Preferences.getLocatorAccuracyForeground()) {
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

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void setupGeofences() {
        Timber.v("loader thread:%s, isMain:%s", Looper.myLooper(), Looper.myLooper() == Looper.getMainLooper());

        List<Geofence> fences = new LinkedList<>();
        for (Waypoint w : App.getDao().loadWaypointsForCurrentModeWithValidGeofence()) {

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
                    .setNotificationResponsiveness((int)TimeUnit.MINUTES.toMillis(2))
                    .setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE).build();

            Timber.v("adding geofence for waypoint %s, mode:%s", w.getDescription(), w.getModeId() );
            fences.add(geofence);
        }

        if (fences.isEmpty()) {
            return;
        }

        GeofencingRequest.Builder b = new GeofencingRequest.Builder();
        GeofencingRequest request = b.addGeofences(fences).build();
        mGeofencingClient.addGeofences(request, getGeofencePendingIntent());

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
        //if(message.isIncoming())
            //sendEventNotification(message);
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
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(MessageProcessor.EndpointState state) {
        this.lastEndpointState = state;
        sendOngoingNotification();
    }
}
