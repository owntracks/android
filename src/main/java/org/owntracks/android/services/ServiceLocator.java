package org.owntracks.android.services;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.db.WaypointDao.Properties;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.messages.WaypointMessage;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageLifecycleCallbacks;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StatisticsProvider;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;


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

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, MessageLifecycleCallbacks, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "ServiceLocator";

    public static final String RECEIVER_ACTION_GEOFENCE_TRANSITION = "org.owntracks.android.RECEIVER_ACTION_GEOFENCE_TRANSITION";
    public static final String RECEIVER_ACTION_LOCATION_CHANGE = "org.owntracks.android.RECEIVER_ACTION_LOCATION_CHANGE";
    public static final String RECEIVER_ACTION_PUBLISH_LASTKNOWN = "org.owntracks.android.RECEIVER_ACTION_PUBLISH_LASTKNOWN";
    public static final String RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL = "org.owntracks.android.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL";

    GoogleApiClient googleApiClient;
    private ServiceProxy context;

    private LocationRequest mLocationRequest;
    private boolean ready = false;
    private boolean foreground = App.isInForeground();
    private Location lastKnownLocation;
    private long lastPublish;
    private WaypointDao waypointDao;
    private static boolean hasLocationPermission = false;



    @Override
    public void onCreate(ServiceProxy p) {

        this.context = p;
        this.lastPublish = 0;
        this.waypointDao = Dao.getWaypointDao();
        hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if(!hasLocationPermission)
            ServiceProxy.getServiceNotification().notifyMissingPermissions();

        Preferences.registerOnPreferenceChangedListener(new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {

            }

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (
                        key.equals(Preferences.getKey(R.string.keyPub)) ||
                                key.equals(Preferences.getKey(R.string.keyLocatorInterval)) ||
                                key.equals(Preferences.getKey(R.string.keyLocatorDisplacement)) ||
                                key.equals(Preferences.getKey(R.string.keyLocatorAccuracyForeground)) ||
                                key.equals(Preferences.getKey(R.string.keyLocatorAccuracyBackground))) {
                    handlePreferences();
                }

            }
        });


        Log.v(TAG, "Initializing GoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (ServiceApplication.checkPlayServices()) {
            if (!this.googleApiClient.isConnected() && !this.googleApiClient.isConnecting()) {
                Log.v(TAG, "Connecting GoogleApiClient");
                this.googleApiClient.connect();
            } else {
                Log.v(TAG, "GoogleApiClient is already connected or connecting");
            }
        }

        this.ready = false;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "GoogleApiClient connection failed with result: " + connectionResult);
        this.ready = false;
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e(TAG, "GoogleApiClient is now connected");
        StatisticsProvider.setTime(context, StatisticsProvider.SERVICE_LOCATOR_PLAY_CONNECTED);

        this.ready = true;
        initLocationRequest();
        removeGeofences();
        requestGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "GoogleApiClient connection suspended");
        this.ready = false;
    }

    public Location getLastKnownLocation() {

        if ((this.googleApiClient != null) && this.googleApiClient.isConnected()) {
            try {
                Log.v(TAG, "getting last known location");
                this.lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                hasLocationPermission = true;

            } catch (SecurityException e) {
                Log.e(TAG, e.getMessage());
                hasLocationPermission = false;
                ServiceProxy.getServiceNotification().notifyMissingPermissions();

                this.lastKnownLocation = null;
            }

        }

		return this.lastKnownLocation;
	}


    public void enteredWifiNetwork(String ssid) {
        Log.v(TAG, "matching waypoints against SSID " + ssid);

        List<Waypoint> ws = this.waypointDao.queryBuilder().where(Properties.ModeId.eq(Preferences.getModeId()), Properties.WifiSSID.like("TestSSID")).build().list();

        for (Waypoint w : ws) {
            Log.v(TAG, "matched waypoint with ssid " + w.getDescription());
           publishSsidTransitionMessage(w);
           w.setLastTriggered(System.currentTimeMillis()/1000);
           this.waypointDao.update(w);
        }



    }

	public void onFenceTransition(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        Log.v(TAG, "onFenceTransistion");
        if(event != null){
            if(event.hasError()) {
                Log.e(TAG, "Geofence event has error: " + event.getErrorCode());
                return;
            }

            int transition = event.getGeofenceTransition();

            if(transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

                    Waypoint w = this.waypointDao.queryBuilder().where(Properties.GeofenceId.eq(event.getTriggeringGeofences().get(index).getRequestId())).limit(1).unique();

                    if (w != null) {
                        Log.v(TAG, "Waypoint triggered " + w.getDescription() + " transition: " + transition);
                        w.setLastTriggered(System.currentTimeMillis());
                        this.waypointDao.update(w);
                        EventBus.getDefault().postSticky(new Events.WaypointTransition(w, transition));
                        publishTransitionMessage(w, event.getTriggeringLocation(), transition);
                    }
                }
            }
        }
	}



	private boolean shouldPublishLocation() {
        if(!Preferences.getPub())
            return false;

        if (!this.foreground)
            return true;

        return (System.currentTimeMillis() - this.lastPublish) > TimeUnit.MINUTES.toMillis(1);
    }




	private void initLocationRequest() {
		requestLocationUpdates();
	}



	private void setupBackgroundLocationRequest() {
        //Log.v(TAG, "setupBackgroundLocationRequest");

        this.mLocationRequest = LocationRequest.create();

        if(Preferences.getLocatorAccuracyBackground() == 0) {
            Log.v(TAG, "setupBackgroundLocationRequest PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 1) {
            Log.v(TAG, "setupBackgroundLocationRequest PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 2) {
            Log.v(TAG, "setupBackgroundLocationRequest PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            Log.v(TAG, "setupBackgroundLocationRequest PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        Log.v(TAG, "setupBackgroundLocationRequest interval: " + Preferences.getLocatorIntervalMillis());
        Log.v(TAG, "setupBackgroundLocationRequest displacement: " + Preferences.getLocatorDisplacement());

        this.mLocationRequest.setInterval(Preferences.getLocatorIntervalMillis());
		this.mLocationRequest.setFastestInterval(10000);
		this.mLocationRequest.setSmallestDisplacement(Preferences.getLocatorDisplacement());
	}

	private void setupForegroundLocationRequest() {
		this.mLocationRequest = LocationRequest.create();


        if(Preferences.getLocatorAccuracyForeground() == 0) {
            Log.v(TAG, "setupForegroundLocationRequest PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 1) {
            Log.v(TAG, "setupForegroundLocationRequest PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 2) {
            Log.v(TAG, "setupForegroundLocationRequest PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            Log.v(TAG, "setupForegroundLocationRequest PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        this.mLocationRequest.setInterval(TimeUnit.SECONDS.toMillis(10));
		this.mLocationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        this.mLocationRequest.setSmallestDisplacement(50);

	}

	protected void handlePreferences() {
		requestLocationUpdates();
	}

	private void disableLocationUpdates() {

		if ((this.googleApiClient != null) && this.googleApiClient.isConnected()) {

            PendingResult<Status> r = LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Log.v(TAG, "removeLocationUpdates successful");
                    } else if (status.hasResolution()) {
                        Log.v(TAG, "removeLocationUpdates failed. HasResolution");
                    } else {
                        Log.v(TAG, "removeLocationUpdates failed. " + status.getStatusMessage());
                    }
                }
            });
		}
	}

	private void requestLocationUpdates() {
        if (!this.ready || googleApiClient == null || !googleApiClient.isConnected()) {
            Log.e(TAG, "requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }

        Log.v(TAG, "checking location permission");



        disableLocationUpdates();

        Log.v(TAG, "requestLocationUpdates fg:" + this.foreground + " app fg:" + App.isInForeground());
        try {

            if (this.foreground)
                setupForegroundLocationRequest();
            else
                setupBackgroundLocationRequest();

            PendingResult<Status> r = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        Log.v(TAG, "requestLocationUpdates successful");
                    } else if (status.hasResolution()) {
                        Log.v(TAG, "requestLocationUpdates failed. HasResolution");
                    } else {
                        Log.v(TAG, "requestLocationUpdates failed. " + status.getStatusMessage());
                    }
                }
            });
            hasLocationPermission = true;
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            hasLocationPermission = false;
            ServiceProxy.getServiceNotification().notifyMissingPermissions();

        }


    }


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null)
            return 0;

        Log.v(TAG, "onStartCommand " + intent.getAction());
        if (ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL.equals(intent.getAction())) {
            publishManualLocationMessage();
        } else if (intent.getAction().equals(ServiceLocator.RECEIVER_ACTION_GEOFENCE_TRANSITION)) {
            onFenceTransition(intent);
        } else {
            Log.e(TAG, "Received unknown intent action: " + intent.getAction());
        }

		return 0;
	}

    @Override
    public void onEvent(Events.Dummy event) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.v(TAG, "onLocationChanged");

        if(!isForeground()) {
            StatisticsProvider.setTime(context, StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_LAST_CHANGE);
            StatisticsProvider.incrementCounter(context, StatisticsProvider.SERVICE_LOCATOR_BACKGROUND_LOCATION_CHANGES);
        }
        lastKnownLocation = location;

        EventBus.getDefault().postSticky(new Events.CurrentLocationUpdated(lastKnownLocation));

        if (shouldPublishLocation())
            publishLocationMessage();
    }

	public void enableForegroundMode() {
		this.foreground = true;
		requestLocationUpdates();
	}

	public void enableBackgroundMode() {
		this.foreground = false;
        requestLocationUpdates();
	}

	@Override
	public void onDestroy() {
        disableLocationUpdates();
	}

	private void publishTransitionMessage(Waypoint w, Location triggeringLocation, int transition) {
        MessageTransition message = new MessageTransition();
        message.setTransition(transition);
        message.setTid(Preferences.getTrackerId(true));
        message.setLat(triggeringLocation.getLatitude());
        message.setLon(triggeringLocation.getLongitude());
        message.setAcc(triggeringLocation.getAccuracy());
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setWtst(TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime()));
        message.setDesc(w.getShared() ? w.getDescription() : null);

        message.setTopic(Preferences.getPubTopicEvents());
        message.setQos(Preferences.getPubQosEvents());
        message.setRetained(Preferences.getPubRetainEvents());

        ServiceProxy.getServiceBroker().publish(message);
	}
    private void publishSsidTransitionMessage(Waypoint w) {
     //   ServiceProxy.getServiceBroker().publish(new TransitionMessage(w), Preferences.getPubTopicEvents(), Preferences.getPubQosEvents(), Preferences.getPubRetainEvents(), null, null);
    }



	private void publishWaypointMessage(WaypointMessage message) {
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(TAG, "publishWaypointMessage called without a broker instance");
            return;
		}

        //TODO:
        //ServiceProxy.getServiceBroker().publish(message, Preferences.getPubTopicWaypoints(), Preferences.getPubQosWaypoints(), Preferences.getPubRetainWaypoints(), null, null);
	}

    public void publishManualLocationMessage() {
        publishLocationMessage("u"); // manual publish requested by the user
    }

    public void publishResponseLocationMessage() {
        publishLocationMessage("r"); // response to a "reportLocation" request
    }

    public void publishLocationMessage() {
        publishLocationMessage(null); // automatic publish after a location change
	}

	private void publishLocationMessage(String trigger) {

		if (ServiceProxy.getServiceBroker() == null) {
            Log.e(TAG, "publishLocationMessage called without a broker instance");
            return;
		}

        Location l = getLastKnownLocation();
		if (l == null) {
            Log.e(TAG, "publishLocationMessage called without a known location");
			return;
		}

		MessageLocation message = new MessageLocation();
        message.setLat(l.getLatitude());
        message.setLon(l.getLongitude());
        message.setAcc(Math.round(l.getAccuracy()));
        message.setT(trigger);
        message.setTst(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        message.setTid(Preferences.getTrackerId(true));
        if(Preferences.getPubLocationIncludeBattery())
            message.setBat(App.getBatteryLevel());

        message.setTopic(Preferences.getPubTopicLocations());
        message.setQos(Preferences.getPubQosLocations());
        message.setRetained(Preferences.getPubRetainLocations());

		ServiceProxy.getServiceBroker().publish(message);

	}

	@Override
	public void onMessagePublishSuccessful(Object extra, boolean wasQueued) {
        Log.v(TAG, "onMessagePublishSuccessful. WasQueued: " + !wasQueued);
		if (extra == null)
			return;

		EventBus.getDefault().postSticky(new Events.PublishSuccessful(extra, wasQueued));
	}

    @Override
    public void onMessagePublishFailed(Object extra) {  }

    @Override
    public void onMesssagePublishing(Object extra) { }

    @Override
    public void onMessagePublishQueued(Object extra) { }

    public void onEvent(Events.WaypointAdded e) {
		handleWaypoint(e.getWaypoint(), false, false);
	}

	public void onEvent(Events.WaypointUpdated e) {
		handleWaypoint(e.getWaypoint(), true, false);
	}

	public void onEvent(Events.WaypointRemoved e) {
		handleWaypoint(e.getWaypoint(), false, true);
	}

    public void onEvent(Events.ModeChanged e) {
        removeGeofencesByWaypoint(loadWaypointsForModeId(e.getOldModeId()));
        requestGeofences();
    }

    public void onEvent(Events.PermissionGranted e) {
        Log.v(TAG, "Events.PermissionGranted: " + e.getPermission() );
        if(!hasLocationPermission && e.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            hasLocationPermission = true;
            Log.v(TAG, "requesting geofences and location updates");
            requestGeofences();
            requestLocationUpdates();
        }
    }




    private void handleWaypoint(Waypoint w, boolean update, boolean remove) {
        if(update && remove)
            throw new IllegalArgumentException("update and remove cannot be true at the same time");

        // We've an update and the waypoint is shared. Send out the new waypoint
        if (!remove && w.getShared()){
            WaypointMessage wpM = new WaypointMessage(w);
            wpM.setTrackerId(Preferences.getTrackerId(true));
            publishWaypointMessage(wpM);
        }

		if (update || remove)
			removeGeofence(w);

		if (!remove && isWaypointWithValidGeofence(w)) {
			requestGeofences();
		}
	}

	private void requestGeofences() {
		if (!this.ready)
			return;

		List<Geofence> fences = new ArrayList<>();

		for (Waypoint w : loadWaypointsForCurrentModeWithValidGeofence()) {

			// if id is null, waypoint is not added yet
			if (w.getGeofenceId() == null) {
				w.setGeofenceId(UUID.randomUUID().toString());
				this.waypointDao.update(w);
			} else {
				continue;
			}

            Geofence geofence = new Geofence.Builder()
					.setRequestId(w.getGeofenceId())
					.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
					.setCircularRegion(w.getGeofenceLatitude(), w.getGeofenceLongitude(), w.getGeofenceRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

            Log.v(TAG, "adding geofence for waypoint " + w.getDescription() + " mode: " + w.getModeId() );
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
                        Log.v(TAG, "Geofence registration successfull");
                    } else if (status.hasResolution()) {
                        Log.v(TAG, "Geofence registration failed. HasResolution");
                    } else {
                        Log.v(TAG, "Geofence registration failed. " + status.getStatusMessage());
                    }
                }
            });
            hasLocationPermission = true;

        }catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
            hasLocationPermission = false;
            ServiceProxy.getServiceNotification().notifyMissingPermissions();
        }
	}


    private GeofencingRequest getGeofencingRequest(List<Geofence> fences) {
        GeofencingRequest.Builder  builder = new GeofencingRequest().Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER); //trigger transition when geofence is setup and device is already in it
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
		ArrayList<String> l = new ArrayList<String>();

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
                    Log.v(TAG, "Geofence removal successfull");
                } else if (status.hasResolution()) {
                    Log.v(TAG, "Geofence removal failed. HasResolution");
                } else {
                    Log.v(TAG, "Geofence removal failed. " + status.getStatusMessage());
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
        return this.waypointDao.queryBuilder().where(WaypointDao.Properties.ModeId.eq(modeId), Properties.GeofenceLatitude.isNotNull(), Properties.GeofenceLongitude.isNotNull(), Properties.GeofenceRadius.isNotNull(), Properties.GeofenceRadius.gt(0)).build().list();
    }

	private boolean isWaypointWithValidGeofence(Waypoint w) {
		return (w.getGeofenceRadius() != null) && (w.getGeofenceRadius() > 0) && (w.getGeofenceLatitude() != null) && (w.getGeofenceLongitude() != null);
	}

    public boolean isReady() {
        return ready;
    }

    public boolean isForeground() {
        return foreground;
    }

    public boolean hasLocationClient() {
        return googleApiClient != null;
    }

    public boolean hasLocationRequest() {
        return mLocationRequest != null;
    }


    public static boolean hasLocationPermission() {
        return hasLocationPermission;
    }
}
