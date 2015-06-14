package org.owntracks.android.services;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.db.WaypointDao.Properties;
import org.owntracks.android.messages.TransitionMessage;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.messages.WaypointMessage;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.MessageCallbacks;
import org.owntracks.android.support.Preferences;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, MessageCallbacks, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static enum State {
        INITIAL, PUBLISHING, PUBLISHING_QUEUED, NOLOCATION
    }
    private static ServiceLocator.State state = ServiceLocator.State.INITIAL;

    GoogleApiClient googleApiClient;
    private SharedPreferences sharedPreferences;
	private OnSharedPreferenceChangeListener preferencesChangedListener;
	private ServiceProxy context;

	private LocationRequest mLocationRequest;
	private boolean ready = false;
	private boolean foreground = false;

	private GeocodableLocation lastKnownLocation;
	private long lastPublish;
	private List<Waypoint> waypoints;
	private WaypointDao waypointDao;

    // Debug structures for issue #86
    private Date debugLocatorServiceStartDate;
    private Date debugLocationAPIConnectDate;
    private int debugRequestPriority;
    private float debugRequestDisplaycement;
    private long debugRequestInterval;
    private float debugRequestSmallestInterval;

    public JSONObject getDebugData() {
        JSONObject j = new JSONObject();
        try {
            j.put("debugLocatorServiceStartDate", debugLocatorServiceStartDate.toString());
            j.put("debugLocationAPIConnectDate", debugLocationAPIConnectDate.toString());
            j.put("debugRequestPriority", debugRequestPriority);
            j.put("debugRequestDisplaycement", debugRequestDisplaycement);
            j.put("debugRequestInterval", debugRequestInterval);
            j.put("debugRequestSmallestInterval", debugRequestSmallestInterval);

        } catch(Exception e) {}
        return j;
    }

	@Override
	public void onCreate(ServiceProxy p) {
        Log.e(this.toString(), "ServiceLocator onCreate");

        this.debugLocatorServiceStartDate = new java.util.Date();
		this.context = p;

        Log.v(this.toString(), "initialized for ServiceLocator");
        this.lastPublish = 0;
		this.waypointDao = App.getWaypointDao();
		loadWaypoints();

		this.sharedPreferences = PreferenceManager .getDefaultSharedPreferences(this.context);

		this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreference, String key) {
				if (key.equals(Preferences.getKey(R.string.keyPub)) || key.equals(Preferences .getKey(R.string.keyPubInterval)))
					handlePreferences();
			}
		};
		this.sharedPreferences .registerOnSharedPreferenceChangeListener(this.preferencesChangedListener);



        Log.v(this.toString(), "Checking if Play Services are available");
        ServiceApplication.checkPlayServices(); // show error notification if  play services were disabled

        Log.v(this.toString(), "Initializing GoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if(ServiceApplication.checkPlayServices()) {
            if (!this.googleApiClient.isConnected() && !this.googleApiClient.isConnecting()) {
                Log.v(this.toString(), "Connecting GoogleApiClient");
                this.googleApiClient.connect();
            } else {
                Log.v(this.toString(), "GoogleApiClient is already connected or connecting");
            }
        }

        this.ready = false;

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(this.toString(), "GoogleApiClient connection failed with result: " + connectionResult);
        this.ready = false;
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e(this.toString(), "GoogleApiClient is now connected");
        debugLocationAPIConnectDate = new Date();
        this.ready = true;
        initLocationRequest();
        initGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(this.toString(), "GoogleApiClient connection suspended");
        this.ready = false;
    }

	public GeocodableLocation getLastKnownLocation() {
		if ((this.googleApiClient != null) && this.googleApiClient.isConnected() && (LocationServices.FusedLocationApi.getLastLocation(googleApiClient) != null))
			this.lastKnownLocation = new GeocodableLocation(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));

		return this.lastKnownLocation;
	}

	public void onFenceTransition(Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        Log.v(this.toString(), "onFenceTransistion");
        if(event != null){
            if(event.hasError()) {
                Log.e(this.toString(), "Geofence event has error: " + event.getErrorCode());
                return;
            }

            int transition = event.getGeofenceTransition();

            if(transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT){
                for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

                    Waypoint w = this.waypointDao.queryBuilder().where(Properties.GeofenceId.eq(event.getTriggeringGeofences().get(index).getRequestId())).limit(1).unique();

                    if (w != null) {
                        Log.v(this.toString(), "Waypoint triggered " + w.getDescription() + " transition: " + transition);
                        EventBus.getDefault().postSticky(new Events.WaypointTransition(w, transition));
                        publishTransitionMessage(w, event.getTriggeringLocation(), transition);
                    }
                }
            }
        }
	}



	private boolean shouldPublishLocation() {
        if (this.lastPublish == 0) {
            return true;
        }


        //Log.v(this.toString(), "shouldPublishLocation: time interval -> false");
        //Log.v(this.toString(), "shouldPublishLocation: System time:"+ System.currentTimeMillis());
        //Log.v(this.toString(), "shouldPublishLocation: Last publish time:"+ this.lastPublish);
        //Log.v(this.toString(), "shouldPublishLocation: configured pub interval:"+ TimeUnit.MINUTES.toMillis(Preferences.getPubInterval()));
        //Log.v(this.toString(), "shouldPublishLocation: time since last publish:"+ (System.currentTimeMillis() - this.lastPublish));

        if ((System.currentTimeMillis() - this.lastPublish) > TimeUnit.MINUTES.toMillis(Preferences.getPubInterval())) {
            //Log.v(this.toString(), "interval gt configured pub interval?: true");
            return true;
        } else {
            //Log.v(this.toString(), "interval gt configured pub interval?: false");
        }
		return false;
	}



    private void initGeofences() {
		removeGeofences();
		requestGeofences();
	}

	private void initLocationRequest() {
		requestLocationUpdates();
	}



	private void setupBackgroundLocationRequest() {
        //Log.v(this.toString(), "setupBackgroundLocationRequest");

        this.mLocationRequest = LocationRequest.create();

        if(Preferences.getLocatorAccuracyBackground() == 0) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 1) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 2) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        //Log.v(this.toString(), "setupBackgroundLocationRequest interval: " + Preferences.getLocatorIntervalMillis());
        //Log.v(this.toString(), "setupBackgroundLocationRequest displacement: " + Preferences.getLocatorDisplacement());

        this.mLocationRequest.setInterval(Preferences.getLocatorIntervalMillis());
		this.mLocationRequest.setFastestInterval(10000);
		this.mLocationRequest.setSmallestDisplacement(Preferences.getLocatorDisplacement());
	}

	private void setupForegroundLocationRequest() {
		this.mLocationRequest = LocationRequest.create();
        if(Preferences.getLocatorAccuracyForeground() == 0) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_HIGH_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 1) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_BALANCED_POWER_ACCURACY");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 2) {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_LOW_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            //Log.v(this.toString(), "setupBackgroundLocationRequest PRIORITY_NO_POWER");
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
        //Log.v(this.toString(), "setupBackgroundLocationRequest interval: " + TimeUnit.SECONDS.toMillis(10));
        //Log.v(this.toString(), "setupBackgroundLocationRequest displacement: 50");
        this.mLocationRequest.setInterval(TimeUnit.SECONDS.toMillis(10));
		this.mLocationRequest.setFastestInterval(TimeUnit.SECONDS.toMillis(10));
        this.mLocationRequest.setSmallestDisplacement(50);
	}

	protected void handlePreferences() {
		requestLocationUpdates();
	}

	private void disableLocationUpdates() {

		if ((this.googleApiClient != null) && this.googleApiClient.isConnected()) {
            //Log.v(this.toString(), "disableLocationUpdates");

            final PendingIntent i = getPendingIntentForLocationRequest();

            PendingResult<Status> r = LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, i);
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.v(this.toString(), "removeLocationUpdates successfull");
                    } else if (status.hasResolution()) {
                        Log.v(this.toString(), "removeLocationUpdates failed. HasResolution");
                    } else {
                        Log.v(this.toString(), "removeLocationUpdates failed. " + status.getStatusMessage());
                    }
                    i.cancel();
                }
            });
		}
	}

	private void requestLocationUpdates() {
        if (!this.ready || googleApiClient == null || !googleApiClient.isConnected()) {
            Log.e(this.toString(), "requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }


        disableLocationUpdates();

        if (this.foreground)
            setupForegroundLocationRequest();
        else
            setupBackgroundLocationRequest();

		if (this.foreground || Preferences.getPub()) {
            debugRequestDisplaycement=mLocationRequest.getSmallestDisplacement();
            debugRequestInterval=mLocationRequest.getInterval();
            debugRequestSmallestInterval = mLocationRequest.getSmallestDisplacement();
            debugRequestPriority=mLocationRequest.getPriority();

            PendingResult<Status> r = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, getPendingIntentForLocationRequest());
            r.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Log.v(this.toString(), "requestLocationUpdates successfull");
                    } else if (status.hasResolution()) {
                        Log.v(this.toString(), "requestLocationUpdates failed. HasResolution");
                    } else {
                        Log.v(this.toString(), "requestLocationUpdates failed. " + status.getStatusMessage());
                    }
                }
            });
		} else
			Log.d(this.toString(), "Location updates not requested (in foreground: "+ this.foreground +", background updates: " +  Preferences.getPub());

	}

    private PendingIntent getPendingIntentForLocationRequest() {
        return ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceProxy.INTENT_ACTION_LOCATION_CHANGED, null, 0);
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent != null) && (intent.getAction() != null)) {

			if (intent.getAction().equals(ServiceProxy.INTENT_ACTION_PUBLISH_LASTKNOWN)) {
                publishLocationMessage();
            } else if (intent.getAction().equals(ServiceProxy.INTENT_ACTION_PUBLISH_LASTKNOWN_MANUAL)) {
                publishManualLocationMessage();
			} else if (intent.getAction().equals(ServiceProxy.INTENT_ACTION_LOCATION_CHANGED)) {
                Location location = intent.getParcelableExtra(  LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);

                // TODO: check if new location is newer and more accurate than last one
				if (location != null) {
                    lastKnownLocation = new GeocodableLocation(location);

                    EventBus.getDefault().postSticky(new Events.CurrentLocationUpdated(lastKnownLocation));

                    if (shouldPublishLocation())
                        publishLocationMessage();
                }
			} else if (intent.getAction().equals(ServiceProxy.INTENT_ACTION_FENCE_TRANSITION)) {
				onFenceTransition(intent);
			} else {
				Log.v(this.toString(), "Received unknown intent");
			}
		}

		return 0;
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
        ServiceProxy.getServiceBroker().publish(new TransitionMessage(w, triggeringLocation, transition), Preferences.getPubTopicEvents(), Preferences.getPubQosEvents(), Preferences.getPubRetainEvents(), null, null);
	}

    // gets the current location if we have one
    // queries the current location otherwise
    public LocationMessage getLocationMessage(GeocodableLocation l) {
        LocationMessage lm;

        if(l!= null)
            lm = new LocationMessage(l);
        else
            lm = new LocationMessage(getLastKnownLocation());

        return lm;
    }

	private void publishWaypointMessage(WaypointMessage message) {
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(this.toString(), "publishWaypointMessage but ServiceMqtt not ready");
		}

        ServiceProxy.getServiceBroker().publish(message, Preferences.getPubTopicWaypoints(), Preferences.getPubQosWaypoints(), Preferences.getPubRetainWaypoints(), null, null);
	}

    public void publishManualLocationMessage() {
        publishLocationMessage(null, "u"); // manual publish requested by the user
    }

    public void publishResponseLocationMessage() {
        publishLocationMessage(null, "r"); // response to a "reportLocation" request
    }


    public void publishLocationMessage() {
		publishLocationMessage(null, null);
	}

	private void publishLocationMessage(LocationMessage r, String trigger) {
		this.lastPublish = System.currentTimeMillis();

		if (ServiceProxy.getServiceBroker() == null) {
			return;
		}

		if ((r == null) && (getLastKnownLocation() == null)) {
			changeState(ServiceLocator.State.NOLOCATION);
			return;
		}

		LocationMessage report;
		if (r == null)
			report = getLocationMessage(null);
		else
			report = r;

        if(trigger != null)
            report.setTrigger(trigger);


		ServiceProxy.getServiceBroker().publish(report, Preferences.getPubTopicLocations(), Preferences.getPubQosLocations(), Preferences.getPubRetainLocations(), this, report);

	}

	@Override
	public void publishSuccessfull(Object extra, boolean wasQueued) {
        Log.v(this.toString(), "publish successful in service locator. Initial try: " + !wasQueued);
		if (extra == null)
			return;

		changeState(ServiceLocator.State.INITIAL);
		EventBus.getDefault().postSticky(new Events.PublishSuccessfull(extra, wasQueued));
	}

    @Override
    public void publishFailed(Object extra) {

    }

    public static ServiceLocator.State getState() {
		return state;
	}



	private void changeState(ServiceLocator.State newState) {
		Log.d(this.toString(), "ServiceLocator state changed to: " + newState);
		EventBus.getDefault().postSticky( new Events.StateChanged.ServiceLocator(newState));
		state = newState;
	}

	@Override
	public void publishQueued(Object extra) {
		changeState(ServiceLocator.State.PUBLISHING_QUEUED);
	}

	@Override
	public void publishing(Object extra) {
		changeState(ServiceLocator.State.PUBLISHING);
	}


	public long getLastPublishDate() {
		return this.lastPublish;
	}

	public void onEvent(Events.WaypointAdded e) {
		handleWaypoint(e.getWaypoint(), false, false);
	}

	public void onEvent(Events.WaypointUpdated e) {
		handleWaypoint(e.getWaypoint(), true, false);
	}

	public void onEvent(Events.WaypointRemoved e) {
		handleWaypoint(e.getWaypoint(), false, true);
	}

	private void handleWaypoint(Waypoint w, boolean update, boolean remove) {
        Log.v(this.toString(), "handleWaypoint: update:" +update + " remove:"+remove );

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

		loadWaypoints();

		List<Geofence> fences = new ArrayList<Geofence>();

		for (Waypoint w : this.waypoints) {
			if (!isWaypointWithValidGeofence(w))
				continue;

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
					.setCircularRegion(w.getLatitude(), w.getLongitude(), w.getRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

			fences.add(geofence);
		}

		if (fences.isEmpty()) {
			Log.v(this.toString(), "no geofences to add");
			return;
		}

		Log.v(this.toString(), "Adding " + fences.size() + " geofences");
        PendingResult<Status> r = LocationServices.GeofencingApi.addGeofences(googleApiClient, fences, ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceProxy.INTENT_ACTION_FENCE_TRANSITION, null));
        r.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.v(this.toString(), "Geofence registration successfull");
                } else if (status.hasResolution()) {
                    Log.v(this.toString(), "Geofence registration failed. HasResolution");
                } else {
                    Log.v(this.toString(), "Geofence registration failed. " + status.getStatusMessage());
                }
            }
        });
	}

	private void removeGeofence(Waypoint w) {
		List<Waypoint> l = new LinkedList<Waypoint>();
		l.add(w);
		removeGeofencesByWaypoint(l);
	}

	private void removeGeofences() {
		removeGeofencesByWaypoint(null);
	}

	private void removeGeofencesByWaypoint(List<Waypoint> list) {
		ArrayList<String> l = new ArrayList<String>();

		// Either removes waypoints from the provided list or all waypoints
		for (Waypoint w : list == null ? loadWaypoints() : list) {
			if (w.getGeofenceId() == null)
				continue;
			Log.v(this.toString(), "adding " + w.getGeofenceId() + " for removal");
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
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.v(this.toString(), "Geofence removal successfull");
                } else if (status.hasResolution()) {
                    Log.v(this.toString(), "Geofence removal failed. HasResolution");
                } else {
                    Log.v(this.toString(), "Geofence removal failed. " + status.getStatusMessage());
                }
            }
        });
	}

	public void onEvent(Object event) {
	}

	private List<Waypoint> loadWaypoints() {
		return this.waypoints = this.waypointDao.loadAll();
	}

	private boolean isWaypointWithValidGeofence(Waypoint w) {
		return (w.getRadius() != null) && (w.getRadius() > 0) && (w.getLatitude() != null) && (w.getLongitude() != null);
	}

    public boolean isReady() {
        return ready;
    }

    public boolean isForeground() {
        return foreground;
    }

    public Integer getWaypointCount() {
        return waypoints != null ? waypoints.size() : -1;
    }

    public boolean hasLocationClient() {
        return googleApiClient != null;
    }

    public boolean hasLocationRequest() {
        return mLocationRequest != null;
    }
}
