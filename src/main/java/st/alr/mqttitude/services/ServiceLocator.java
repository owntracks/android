package st.alr.mqttitude.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.db.WaypointDao.Properties;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.messages.LocationMessage;
import st.alr.mqttitude.messages.WaypointMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ServiceMqttCallbacks;
import st.alr.mqttitude.support.Preferences;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
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
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, ServiceMqttCallbacks, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    GoogleApiClient googleApiClient;
    private SharedPreferences sharedPreferences;
	private OnSharedPreferenceChangeListener preferencesChangedListener;
	private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
	private ServiceProxy context;

	private LocationRequest mLocationRequest;
	private boolean ready = false;
	private boolean foreground = false;

	private GeocodableLocation lastKnownLocation;
	private long lastPublish;
	private List<Waypoint> waypoints;
	private WaypointDao waypointDao;

	@Override
	public void onCreate(ServiceProxy p) {

		this.context = p;
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

        googleApiClient = new GoogleApiClient.Builder(this.context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
		if (!this.googleApiClient.isConnected() && !this.googleApiClient.isConnecting() && ServiceApplication.checkPlayServices()) {
            this.googleApiClient.connect();
        }
        this.ready = false;
        ServiceApplication.checkPlayServices(); // show error notification if
        // play services were disabled

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(this.toString(), "PlayServices API failed to connect. Result: " + connectionResult);
    }

    @Override
    public void onConnected(Bundle arg0) {
        Log.e(this.toString(), "PlayServices API connected");
        this.ready = true;
        initLocationRequest();
        initGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(this.toString(), "PlayServices API connection suspended");
    }

	public GeocodableLocation getLastKnownLocation() {
		if ((this.googleApiClient != null) && this.googleApiClient.isConnected() && (LocationServices.FusedLocationApi.getLastLocation(googleApiClient) != null))
			this.lastKnownLocation = new GeocodableLocation(LocationServices.FusedLocationApi.getLastLocation(googleApiClient));

		return this.lastKnownLocation;
	}

    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastKnownLocation = new GeocodableLocation(location);

            EventBus.getDefault().postSticky(new Events.CurrentLocationUpdated(lastKnownLocation));

            if (shouldPublishLocation())
                publishLocationMessage();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(this.toString(), "mLocationListener onStatusChanged: " +provider +" -> " + status);
        }

        public void onProviderEnabled(String provider) {
            Log.v(this.toString(), "mLocationListener onProviderEnabled: " +provider);
        }

        public void onProviderDisabled(String provider) {
            Log.v(this.toString(), "mLocationListener onProviderDisabled: " +provider);
        }
    };

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
                String[] geofenceIds = new String[event.getTriggeringGeofences().size()];
                for (int index = 0; index < event.getTriggeringGeofences().size(); index++) {

                    Waypoint w = this.waypointDao.queryBuilder().where(Properties.GeofenceId.eq(event.getTriggeringGeofences().get(index).getRequestId())).limit(1).unique();

                    if (w != null) {
                        Log.v(this.toString(), "Waypoint triggered " + w.getDescription() + " transition: " + transition);
                        EventBus.getDefault().postSticky(new Events.WaypointTransition(w, transition));
                        publishGeofenceTransitionEvent(w, transition);
                    }
                }
            }
        }
	}



	private boolean shouldPublishLocation() {
		if (this.lastPublish == 0)
			return true;

		if ((System.currentTimeMillis() - this.lastPublish) > TimeUnit.MINUTES.toMillis(Preferences.getPubInterval()))
			return true;

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
		this.mLocationRequest = LocationRequest.create();

        if(Preferences.getLocatorAccuracyBackground() == 0) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 1) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyBackground() == 2) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
		this.mLocationRequest.setInterval(Preferences
				.getLocatorInterval());
		this.mLocationRequest.setFastestInterval(0);
		this.mLocationRequest.setSmallestDisplacement(Preferences
				.getLocatorDisplacement());
	}

	private void setupForegroundLocationRequest() {
		this.mLocationRequest = LocationRequest.create();
        if(Preferences.getLocatorAccuracyForeground() == 0) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 1) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        } else if (Preferences.getLocatorAccuracyForeground() == 2) {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        } else {
            this.mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }

		this.mLocationRequest.setInterval(TimeUnit.SECONDS.toMillis(10));
		this.mLocationRequest.setFastestInterval(0);
		this.mLocationRequest.setSmallestDisplacement(50);
	}

	protected void handlePreferences() {
		requestLocationUpdates();
	}

	private void disableLocationUpdates() {
		if ((this.googleApiClient != null) && this.googleApiClient.isConnected()) {
            PendingResult<Status> r = LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, mLocationListener);
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
                }
            });
		}
	}

	private void requestLocationUpdates() {
        if (!this.ready) {
            Log.e(this.toString(), "requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }


        disableLocationUpdates();

        if (this.foreground)
            setupForegroundLocationRequest();
        else
            setupBackgroundLocationRequest();

        // State may have changed. Check again
		if (!this.ready) {
            Log.e(this.toString(), "requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
            return;
        }

		if (this.foreground || Preferences.getPub()) {
            PendingIntent i = ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_LOCATION_CHANGED, null);
            Log.v(this.toString(), "setting up location updates with pending intent " + i);

            PendingResult<Status> r = LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,mLocationRequest,mLocationListener);
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

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ((intent != null) && (intent.getAction() != null)) {
			if (intent.getAction().equals(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN)) {
				publishLocationMessage();
			} else if (intent.getAction().equals(Defaults.INTENT_ACTION_LOCATION_CHANGED)) {
				Location location = intent.getParcelableExtra(  LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);

				if (location != null)
					mLocationListener.onLocationChanged(location);

			} else if (intent.getAction().equals(Defaults.INTENT_ACTION_FENCE_TRANSITION)) {
				Log.v(this.toString(), "Geofence transition occured");
				onFenceTransition(intent);
			} else {
				Log.v(this.toString(), "Received unknown intent");
			}
		}

		return 0;
	}


	public void enableForegroundMode() {
		this.foreground = true;
        Log.v(this.toString(), "enableForegroundMode");
		requestLocationUpdates();
	}

	public void enableBackgroundMode() {
		this.foreground = false;
        Log.v(this.toString(), "enableBackgroundMode");

		requestLocationUpdates();
	}

	@Override
	public void onDestroy() {
		disableLocationUpdates();
	}

	private void publishGeofenceTransitionEvent(Waypoint w, int transition) {
		GeocodableLocation l = new GeocodableLocation("Waypoint");
		l.setLatitude(w.getLatitude());
		l.setLongitude(w.getLongitude());
		l.setAccuracy(w.getRadius());
		l.getLocation().setTime(System.currentTimeMillis());

		LocationMessage r = getLocationMessage(l);

		r.setTransition(transition);
		r.setWaypoint(w);
		r.setSupressesTicker(true);

		publishLocationMessage(r, "c");

	}

    public LocationMessage getLocationMessage(GeocodableLocation l) {
        LocationMessage lm;

        if(l!= null)
            lm = new LocationMessage(l);
        else
            lm = new LocationMessage(getLastKnownLocation());

        lm.setTrackerId(Preferences.getTrackerId());

        return lm;
    }

	private void publishWaypointMessage(WaypointMessage r) {
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(this.toString(),
					"publishWaypointMessage but ServiceMqtt not ready");
			return;
		}

		String topic = Preferences.getPubTopicBase(true);
		if (topic == null) {
			changeState(Defaults.State.ServiceLocator.NOTOPIC);
			return;
		}

        ServiceProxy.getServiceBroker().publish(
				topic + Preferences.getPubTopicPartWaypoints(), r.toString(),
				false, Preferences.getPubQos(), 20, this, null);
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

		// Safety checks
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(this.toString(), "publishLocationMessage but ServiceMqtt not ready");
			return;
		}

		if ((r == null) && (getLastKnownLocation() == null)) {
			changeState(Defaults.State.ServiceLocator.NOLOCATION);
			return;
		}

		String topic = Preferences.getPubTopicBase(true);
		if (topic == null) {
			changeState(Defaults.State.ServiceLocator.NOTOPIC);
			return;
		}

		LocationMessage report;
		if (r == null)
			report = getLocationMessage(null);
		else
			report = r;

		if (Preferences.getPubIncludeBattery())
			report.setBattery(App.getBatteryLevel());

        if(trigger != null)
            report.setTrigger(trigger);

		ServiceProxy.getServiceBroker().publish(topic, report.toString(),
				Preferences.getPubRetain(), Preferences.getPubQos(), 20, this,
				report);

	}

	@Override
	public void publishSuccessfull(Object extra) {
		if (extra == null)
			return;

		changeState(Defaults.State.ServiceLocator.INITIAL);
		EventBus.getDefault().postSticky(new Events.PublishSuccessfull(extra));
	}

	public static Defaults.State.ServiceLocator getState() {
		return state;
	}

	public static String getStateAsString(Context c) {
		return stateAsString(getState(), c);
	}

	public static String stateAsString(Defaults.State.ServiceLocator state,
			Context c) {
		return Defaults.State.toString(state, c);
	}

	private void changeState(Defaults.State.ServiceLocator newState) {
		Log.d(this.toString(), "ServiceLocator state changed to: " + newState);
		EventBus.getDefault().postSticky(
				new Events.StateChanged.ServiceLocator(newState));
		state = newState;
	}

	@Override
	public void publishFailed(Object extra) {
		changeState(Defaults.State.ServiceLocator.PUBLISHING_TIMEOUT);
	}

	@Override
	public void publishing(Object extra) {
		changeState(Defaults.State.ServiceLocator.PUBLISHING);
	}

	@Override
	public void publishWaiting(Object extra) {
		changeState(Defaults.State.ServiceLocator.PUBLISHING_WAITING);
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
		if (!remove && w.getShared()){
            WaypointMessage wpM = new WaypointMessage(w);
            wpM.setTrackerId(Preferences.getTrackerId());
            publishWaypointMessage(wpM);
        }

		if (!isWaypointWithValidGeofence(w))
			return;

		if (update || remove)
			removeGeofence(w);

		if (!remove && w.getRadius() != null && w.getRadius() > 0) {
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
					.setTransitionTypes(w.getTransitionType())
					.setCircularRegion(w.getLatitude(), w.getLongitude(), w.getRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

			fences.add(geofence);
		}

		if (fences.isEmpty()) {
			Log.v(this.toString(), "no geofences to add");
			return;
		}

		Log.v(this.toString(), "Adding " + fences.size() + " geofences");
        PendingResult<Status> r = LocationServices.GeofencingApi.addGeofences(googleApiClient, fences, ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_FENCE_TRANSITION, null));
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
			Log.v(this.toString(), "adding " + w.getGeofenceId()
					+ " for removal");
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
