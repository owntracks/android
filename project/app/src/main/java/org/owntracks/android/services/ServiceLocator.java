package org.owntracks.android.services;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.MessageWaypoint;
import org.owntracks.android.messages.MessageWaypoints;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageWaypointCollection;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.interfaces.ProxyableService;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import timber.log.Timber;

public class ServiceLocator implements ProxyableService, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "ServiceLocator";

    public static final String RECEIVER_ACTION_GEOFENCE_TRANSITION = "org.owntracks.android.RECEIVER_ACTION_GEOFENCE_TRANSITION";
    public static final String RECEIVER_ACTION_GEOFENCE_TRANSITION_LOOKUP = "org.owntracks.android.RECEIVER_ACTION_GEOFENCE_TRANSITION_LOOKUP";
    public static final String RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL = "org.owntracks.android.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL";


    private GoogleApiClient googleApiClient;
    private ServiceProxy context;

    private LocationRequest mLocationRequest;
    private boolean ready = false;
    private Location lastKnownLocation;
    private long lastPublish;
    private WaypointDao waypointDao;
    private static boolean hasLocationPermission = false;



    @Override
    public void onCreate(ServiceProxy p) {
        this.context = p;
        checkLocationPermission();

        this.lastPublish = System.currentTimeMillis(); // defer first location report when the service is started;
        this.waypointDao = Dao.getWaypointDao();
        this.googleApiClient = new GoogleApiClient.Builder(this.context).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

        if (ServiceProxy.checkPlayServices() && !hasConnectedGoogleApiClient()) {
            this.googleApiClient.connect();
        }

        Preferences.registerOnPreferenceChangedListener(new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {

            }

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (
                        key.equals(Preferences.Keys.LOCATOR_INTERVAL) ||
                        key.equals(Preferences.Keys.LOCATOR_DISPLACEMENT) ||
                        key.equals(Preferences.Keys.LOCATOR_ACCURACY_FOREGROUND) ||
                        key.equals(Preferences.Keys.LOCATOR_ACCURACY_BACKGROUND)) {
                    requestLocationUpdates();
                }

            }
        });
    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.e("GoogleApiClient connection failed. Connection result: %s", connectionResult);
        this.ready = false;
    }

    @Override
    public void onConnected(Bundle arg0) {
        Timber.v("GoogleApiClient is now connected");

        this.ready = true;
        App.postOnBackgroundHandler(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates();
                removeGeofences();
                requestGeofences();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Timber.d("GoogleApiClient connection suspended");
        this.ready = false;
    }

    public Location getLastKnownLocation() {

        if (hasConnectedGoogleApiClient()) {
            try {
                this.lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                hasLocationPermission = true;

            } catch (SecurityException e) {
                handleSecurityException(e);
            }

        }

		return this.lastKnownLocation;
	}

    @SuppressWarnings("MissingPermission")
    private void onFenceTransition(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        Timber.v("");
        if(event != null){
            if(event.hasError()) {
                Timber.e("geofence event has error: %s", event.getErrorCode());
                return;
            }

            int transition = event.getGeofenceTransition();
            for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

                Waypoint w =  this.waypointDao.queryBuilder().where(WaypointDao.Properties.GeofenceId.eq(event.getTriggeringGeofences().get(index).getRequestId())).limit(1).unique();


                if (w != null) {
                    Timber.v("waypoint triggered:%s transition:%s", w.getDescription(),transition);
                    w.setLastTriggered(System.currentTimeMillis());
                    this.waypointDao.update(w);
                    App.getEventBus().postSticky(new Events.WaypointTransition(w, transition));
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
	}


    private void onEnsuredFenceTransition(Intent intent) {
        //Location location = (Location)intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
        Location location = intent.getParcelableExtra("com.google.android.location.LOCATION");

        Waypoint w =  this.waypointDao.queryBuilder().where(WaypointDao.Properties.GeofenceId.eq(intent.getStringExtra("geofenceId"))).limit(1).unique();

        Timber.v("waypoint location: %s %s %s %s %s %s", w.getDescription(), w.getLocation().getLatitude(), w.getLocation().getLongitude(), w.getLocation().getAccuracy(), w.getLocation().getProvider(), intent.getIntExtra("event", 0) );
        Timber.v("assisted location: %s %s %s %s", location.getLatitude(), location.getLongitude(), location.getAccuracy(), location.getProvider());
    }

	private boolean shouldPublishLocation() {
        if(!Preferences.getPub())
            return false;

        if (!ServiceProxy.isInForeground())
            return true;

        // Publishes are throttled to 30 seconds when in the foreground to not spam the server
        return (System.currentTimeMillis() - this.lastPublish) > TimeUnit.SECONDS.toMillis(30);
    }



	private void setupBackgroundLocationRequest() {
        //Timber.v("setupBackgroundLocationRequest");

        this.mLocationRequest = LocationRequest.create();

        if(Preferences.getLocatorAccuracyBackground() == 0) {
            Timber.v("PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 1) {
            Timber.v("PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 2) {
            Timber.v("PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            Timber.v("PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        Timber.v("setupBackgroundLocationRequest interval: %s",Preferences.getLocatorIntervalMillis());
        Timber.v("setupBackgroundLocationRequest displacement: %s",Preferences.getLocatorDisplacement());

        this.mLocationRequest.setInterval(Preferences.getLocatorIntervalMillis());
		this.mLocationRequest.setFastestInterval(10000);
		this.mLocationRequest.setSmallestDisplacement(Preferences.getLocatorDisplacement());
	}

	private void setupForegroundLocationRequest() {
		this.mLocationRequest = LocationRequest.create();


        if(Preferences.getLocatorAccuracyForeground() == 0) {
            Timber.v("setupForegroundLocationRequest PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 1) {
            Timber.v("setupForegroundLocationRequest PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 2) {
            Timber.v("setupForegroundLocationRequest PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            Timber.v("setupForegroundLocationRequest PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        this.mLocationRequest.setInterval(TimeUnit.SECONDS.toMillis(10));
		this.mLocationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        this.mLocationRequest.setSmallestDisplacement(50);

	}

	private void disableLocationUpdates() {

		if (hasConnectedGoogleApiClient()) {

            PendingResult<Status> r = LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Timber.v("removeLocationUpdates successfull");
                    } else if (status.hasResolution()) {
                        Timber.v("removeLocationUpdates failed. HasResolution");
                    } else {
                        Timber.v("removeLocationUpdates failed. status:%s", status.getStatusMessage());
                    }
                }
            });
		}
	}

    // Ensures location updates are not requested on main thread
	private void requestLocationUpdates() {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            App.postOnBackgroundHandlerDelayed (new Runnable() {
                @Override
                public void run() {
                    requestLocationUpdatesAsync();
                }
            }, 500);
        } else {
            requestLocationUpdatesAsync();
        }

    }

    private void requestLocationUpdatesAsync() {

        if (!isReady() || !hasConnectedGoogleApiClient()) {
            Timber.e("requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }



        disableLocationUpdates();

        try {

            if (ServiceProxy.isInForeground())
                setupForegroundLocationRequest();
            else
                setupBackgroundLocationRequest();

            PendingResult<Status> r = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Timber.v("requestLocationUpdates successful");
                    } else if (status.hasResolution()) {
                        Timber.v("requestLocationUpdates failed. HasResolution");
                    } else {
                        Timber.v("requestLocationUpdates failed. " + status.getStatusMessage());
                    }
                }
            });
            hasLocationPermission = true;
        } catch (SecurityException e) {
            handleSecurityException(e);
        }


    }


	@Override
	public void onStartCommand(Intent intent) {
        if(intent == null)
            return;

        Timber.v("action:%s", intent.getAction());
        if (ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL.equals(intent.getAction())) {
            reportLocationManually();
        } else if (ServiceLocator.RECEIVER_ACTION_GEOFENCE_TRANSITION.equals(intent.getAction())) {
            onFenceTransition(intent);
        } else if (ServiceLocator.RECEIVER_ACTION_GEOFENCE_TRANSITION_LOOKUP.equals(intent.getAction())) {
            //onEnsuredFenceTransition(intent);
        } else {
            Timber.e("received unknown intent action");
        }


	}

    @Subscribe
    public void onEvent(Events.Dummy event) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.v("onLocationChanged");

        lastKnownLocation = location;

        App.getEventBus().postSticky(new Events.CurrentLocationUpdated(lastKnownLocation));

        if (shouldPublishLocation())
            reportLocation();
    }

	void onEnterForeground() {
		requestLocationUpdates();
	}

	void onEnterBackground() {
        requestLocationUpdates();
	}

	@Override
	public void onDestroy() {
        disableLocationUpdates();
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

        ServiceProxy.getServiceMessage().sendMessage(message);
	}

    private boolean ignoreLowAccuracy(@NonNull Location l) {
        int threshold = Preferences.getIgnoreInaccurateLocations();
        return threshold > 0 && l.getAccuracy() > threshold;
    }

	private void publishWaypointMessage(Waypoint w) {
        MessageWaypoint message = MessageWaypoint.fromDaoObject(w);


        ServiceProxy.getServiceMessage().sendMessage(message);
	}

    private void reportLocationManually() {
        reportLocation(MessageLocation.REPORT_TYPE_USER); // manual publish requested by the user
    }

    void reportLocationResponse() {
        reportLocation(MessageLocation.REPORT_TYPE_RESPONSE); // response to a "reportLocation" request
    }

    private void reportLocation() {
        reportLocation(null); // automatic publish after a location change
	}

	private void reportLocation(String trigger) {
        Timber.v("trigger:%s", trigger);

        Location l = getLastKnownLocation();
		if (l == null) {
            Timber.e("reportLocation called without a known location");
			return;
		}

        if(ignoreLowAccuracy(l))
            return;

		MessageLocation message = new MessageLocation();
        message.setLat(l.getLatitude());
        message.setLon(l.getLongitude());
        message.setAcc(Math.round(l.getAccuracy()));
        message.setT(trigger);
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setTid(Preferences.getTrackerId(true));
        if(Preferences.getPubLocationExtendedData()) {
            message.setBatt(App.getBatteryLevel());
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                message.setDoze(PowerManager.class.cast(context.getSystemService(Context.POWER_SERVICE)).isDeviceIdleMode());
            }

            NetworkInfo activeNetwork = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
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

		ServiceProxy.getServiceMessage().sendMessage(message);
	}

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointAdded e) {
		handleWaypoint(e.getWaypoint(), false, false);
	}

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointUpdated e) {
		handleWaypoint(e.getWaypoint(), true, false);
	}

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Events.WaypointRemoved e) {
		handleWaypoint(e.getWaypoint(), false, true);
	}

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(Events.ModeChanged e) {
        Timber.v("mode changed requesting new geofences");
        removeGeofencesByWaypoint(loadWaypointsForModeId(e.getOldModeId()));
        requestGeofences();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(Events.PermissionGranted e) {
        Timber.v("Events.PermissionGranted: " + e.getPermission() );
        if(!hasLocationPermission && e.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            hasLocationPermission = true;
            Timber.v("requesting geofences and location updates");
            requestGeofences();
            requestLocationUpdates();
        }
    }




    private void handleWaypoint(Waypoint w, boolean update, boolean remove) {
        Timber.v("handleWaypoint u:" +update + " r:"+remove);
        if(update && remove)
            throw new IllegalArgumentException("update and remove cannot be true at the same time");

        // We've an update or created a waypoint and the waypoint is shared. Send out the new waypoint
        if (!remove && w.getShared()){
            publishWaypointMessage(w);
        }

		if (update || remove)
			removeGeofence(w);

		if (!remove && isWaypointWithValidGeofence(w)) {
			requestGeofences();
		}
	}

	private void requestGeofences() {
        Timber.v("loader thread:%s, isMain:%s", Looper.myLooper(), Looper.myLooper() == Looper.getMainLooper());
		if (!isReady())
			return;

        List<Geofence> fences = new ArrayList<>();
		for (Waypoint w : loadWaypointsForCurrentModeWithValidGeofence()) {

            Timber.v("desc:%s", w.getDescription());
			// if id is null, waypoint is not added yet
			if (w.getGeofenceId() == null) {
				w.setGeofenceId(UUID.randomUUID().toString());
				this.waypointDao.update(w);
                Timber.v("new fence found without UUID");

			}

            Geofence geofence = new Geofence.Builder()
					.setRequestId(w.getGeofenceId())
					.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setLoiteringDelay((int)TimeUnit.SECONDS.toMillis(15))
                    .setNotificationResponsiveness((int)TimeUnit.SECONDS.toMillis(30))
					.setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

            Timber.v("adding geofence for waypoint %s, mode:%s", w.getDescription(), w.getModeId() );
			fences.add(geofence);
		}

		if (fences.isEmpty()) {
			return;
		}


        try {

            PendingResult<Status> r = LocationServices.GeofencingApi.addGeofences(googleApiClient, getGeofencingRequest(fences), ServiceProxy.getBroadcastIntentForService(context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_GEOFENCE_TRANSITION, null));
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Timber.v("Geofence registration successfull");
                    } else if (status.hasResolution()) {
                        Timber.v("Geofence registration failed. HasResolution");
                    } else {
                        Timber.v("Geofence registration failed. " + status.getStatusMessage());
                    }
                }
            });
            hasLocationPermission = true;

        }catch (SecurityException e) {
            handleSecurityException(e);
        }
	}

    private void handleSecurityException(@Nullable  SecurityException e) {
        if(e != null)
            Timber.e(e.getMessage());
        hasLocationPermission = false;
        ServiceProxy.getServiceNotification().notifyMissingPermissions();
    }


    private GeofencingRequest getGeofencingRequest(List<Geofence> fences) {
        GeofencingRequest.Builder  builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL); //trigger transition when geofence is setup and device is already in it
        builder.addGeofences(fences);
        return builder.build();
    }

	private void removeGeofence(Waypoint w) {
		List<Waypoint> l = new LinkedList<>();
		l.add(w);
		removeGeofencesByWaypoint(l);
	}

	private void removeGeofences() {
		removeGeofencesByWaypoint(null);
	}

	private void removeGeofencesByWaypoint(List<Waypoint> list) {
		ArrayList<String> l = new ArrayList<>();

		// Either removes waypoints from the provided list or all waypoints
		for (Waypoint w : list == null ? loadWaypointsForCurrentMode() : list) {
			if (w.getGeofenceId() == null)
				continue;
			l.add(w.getGeofenceId());
			w.setGeofenceId(null);
			this.waypointDao.update(w);
		}

		removeGeofencesById(l);
	}

	private void removeGeofencesById(List<String> ids) {
		if (ids.isEmpty())
			return;

        PendingResult<Status> r = LocationServices.GeofencingApi.removeGeofences(googleApiClient, ids);
        r.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if (status.isSuccess()) {
                    Timber.v("Geofence removal successfull");
                } else if (status.hasResolution()) {
                    Timber.v("Geofence removal failed. HasResolution");
                } else {
                    Timber.v("Geofence removal failed. " + status.getStatusMessage());
                }
            }
        });
	}

	public void onEvent(Object event) {
	}

    private List<Waypoint> loadWaypointsForCurrentMode() {
        return loadWaypointsForModeId(Preferences.getModeId());
    }

    private List<Waypoint> loadWaypointsForModeId(int modeId) {
        return this.waypointDao.queryBuilder().where(WaypointDao.Properties.ModeId.eq(modeId)).build().list();
	}

    private List<Waypoint> loadWaypointsForCurrentModeWithValidGeofence() {
        return loadWaypointsForModeIdWithValidGeofence(Preferences.getModeId());
    }

    private List<Waypoint> loadWaypointsForModeIdWithValidGeofence(int modeId) {
        return this.waypointDao.queryBuilder().where(WaypointDao.Properties.ModeId.eq(modeId), WaypointDao.Properties.GeofenceLatitude.isNotNull(), WaypointDao.Properties.GeofenceLongitude.isNotNull(), WaypointDao.Properties.GeofenceRadius.isNotNull(), WaypointDao.Properties.GeofenceRadius.gt(0)).build().list();
    }

	private boolean isWaypointWithValidGeofence(Waypoint w) {
		return (w.getGeofenceRadius() != null) && (w.getGeofenceRadius() > 0);
	}

    private boolean isReady() {
        return ready;
    }


    private boolean hasConnectedGoogleApiClient() {
        return this.googleApiClient != null && this.googleApiClient.isConnected() && !this.googleApiClient.isConnecting();
    }

    private void checkLocationPermission() {
        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(!hasLocationPermission)
            handleSecurityException(null);
    }
    public boolean publishWaypointsMessage() {
        MessageWaypoints m = new MessageWaypoints();
        MessageWaypointCollection waypoints = Preferences.waypointsToJSON();
        if(waypoints == null)
            return false;

        m.setWaypoints(waypoints);

        ServiceProxy.getServiceMessage().sendMessage(m);
        return true;
    }

}
