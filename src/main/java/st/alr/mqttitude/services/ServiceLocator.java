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
import st.alr.mqttitude.model.LocationMessage;
import st.alr.mqttitude.model.WaypointMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ServiceMqttCallbacks;
import st.alr.mqttitude.support.Preferences;
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
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;

import de.greenrobot.event.EventBus;

public class ServiceLocator implements ProxyableService, ServiceMqttCallbacks,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener,
		LocationClient.OnRemoveGeofencesResultListener,
		LocationClient.OnAddGeofencesResultListener {

	private SharedPreferences sharedPreferences;
	private OnSharedPreferenceChangeListener preferencesChangedListener;
	private static Defaults.State.ServiceLocator state = Defaults.State.ServiceLocator.INITIAL;
	private ServiceProxy context;

	private LocationClient mLocationClient;
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

		this.sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this.context);

		this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreference, String key) {
				if (key.equals(Preferences.getKey(R.string.keyPubAutoEnabled))
						|| key.equals(Preferences
								.getKey(R.string.keyPubAutoInterval)))
					handlePreferences();
			}
		};
		this.sharedPreferences
				.registerOnSharedPreferenceChangeListener(this.preferencesChangedListener);

		this.mLocationClient = new LocationClient(this.context, this, this);

		if (!this.mLocationClient.isConnected()
				&& !this.mLocationClient.isConnecting()
				&& ServiceApplication.checkPlayServices())
			this.mLocationClient.connect();

	}

	public GeocodableLocation getLastKnownLocation() {
		if ((this.mLocationClient != null)
				&& this.mLocationClient.isConnected()
				&& (this.mLocationClient.getLastLocation() != null))
			this.lastKnownLocation = new GeocodableLocation(
					this.mLocationClient.getLastLocation());

		return this.lastKnownLocation;
	}

	public void onFenceTransition(Intent intent) {
		int transitionType = LocationClient.getGeofenceTransition(intent);

		// Test that a valid transition was reported
		if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
				|| (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)) {
			List<Geofence> triggerList = LocationClient
					.getTriggeringGeofences(intent);

			for (int i = 0; i < triggerList.size(); i++) {

				Waypoint w = this.waypointDao
						.queryBuilder()
						.where(Properties.GeofenceId.eq(triggerList.get(i)
								.getRequestId())).limit(1).unique();


				if (w != null) {
					Log.v(this.toString(), "Waypoint triggered " + w.getDescription() + " transition: " + transitionType);

					EventBus.getDefault().postSticky(
							new Events.WaypointTransition(w, transitionType));

					publishGeofenceTransitionEvent(w, transitionType);
				}
			}
		}
	}

	@Override
	public void onLocationChanged(Location arg0) {
		Log.v(this.toString(), "onLocationChanged");
		this.lastKnownLocation = new GeocodableLocation(arg0);

		EventBus.getDefault().postSticky(
				new Events.CurrentLocationUpdated(this.lastKnownLocation));

		if (shouldPublishLocation())
			publishLocationMessage();
	}

	private boolean shouldPublishLocation() {
		if (this.lastPublish == 0)
			return true;

		if ((System.currentTimeMillis() - this.lastPublish) > TimeUnit.MINUTES
				.toMillis(Preferences.getPubAutoInterval()))
			return true;

		return false;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(this.toString(), "Failed to connect");
	}

	@Override
	public void onConnected(Bundle arg0) {
		this.ready = true;

		Log.v(this.toString(), "Connected");

		initLocationRequest();
		initGeofences();
	}

	private void initGeofences() {
		removeGeofences();
		requestGeofences();
	}

	private void initLocationRequest() {
		setupLocationRequest();
		requestLocationUpdates();
	}

	@Override
	public void onDisconnected() {
		this.ready = false;
		ServiceApplication.checkPlayServices(); // show error notification if
												// play services were disabled
	}

	private void setupBackgroundLocationRequest() {
		Log.v(this.toString(), "setupBackgroundLocationRequest with profile: "
				+ Preferences.getLocatorBackgroundAccuracy());

		this.mLocationRequest = LocationRequest.create();
		this.mLocationRequest.setPriority(Preferences
				.getLocatorBackgroundAccuracy());
		this.mLocationRequest.setInterval(Preferences
				.getLocatorBackgroundInterval());
		this.mLocationRequest.setFastestInterval(0);
		this.mLocationRequest.setSmallestDisplacement(Preferences
				.getLocatorBackgroundDisplacement());
	}

	private void setupForegroundLocationRequest() {
		Log.v(this.toString(), "setupForegroundLocationRequest with profile: "
				+ Preferences.getLocatorForegroundAccuracy());

		this.mLocationRequest = LocationRequest.create();
		this.mLocationRequest.setPriority(Preferences
				.getLocatorForegroundAccuracy());
		this.mLocationRequest.setInterval(TimeUnit.SECONDS.toMillis(10));
		this.mLocationRequest.setFastestInterval(0);
		this.mLocationRequest.setSmallestDisplacement(50);
	}

	protected void handlePreferences() {
		setupLocationRequest();
		requestLocationUpdates();
	}

	private void disableLocationUpdates() {
		Log.v(this.toString(), "Disabling updates");
		if ((this.mLocationClient != null)
				&& this.mLocationClient.isConnected()) {
			this.mLocationClient.removeLocationUpdates(ServiceProxy
					.getPendingIntentForService(this.context,
							ServiceProxy.SERVICE_LOCATOR,
							Defaults.INTENT_ACTION_LOCATION_CHANGED, null, 0));
		}
	}

	private void requestLocationUpdates() {
		if (!this.ready) {
			Log.e(this.toString(),
					"requestLocationUpdates but not connected to play services. Updates will be requested again once connected");
			return;
		}

		if (this.foreground || Preferences.isPubAutoEnabled()) {
			this.mLocationClient.requestLocationUpdates(this.mLocationRequest,
					ServiceProxy.getPendingIntentForService(this.context,
							ServiceProxy.SERVICE_LOCATOR,
							Defaults.INTENT_ACTION_LOCATION_CHANGED, null));

		} else {
			Log.d(this.toString(),
					"Location updates are disabled (not in foreground or background updates disabled)");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if ((intent != null) && (intent.getAction() != null)) {
			if (intent.getAction().equals(
					Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN)) {
				publishLocationMessage();
			} else if (intent.getAction().equals(
					Defaults.INTENT_ACTION_LOCATION_CHANGED)) {
				Location location = intent
						.getParcelableExtra(LocationClient.KEY_LOCATION_CHANGED);

				if (location != null)
					onLocationChanged(location);

			} else if (intent.getAction().equals(
					Defaults.INTENT_ACTION_FENCE_TRANSITION)) {
				Log.v(this.toString(), "Geofence transition occured");
				onFenceTransition(intent);

			} else {
				Log.v(this.toString(), "Received unknown intent");
			}
		}

		return 0;
	}

	private void setupLocationRequest() {
		if (!this.ready)
			return;

		disableLocationUpdates();

		if (this.foreground)
			setupForegroundLocationRequest();
		else
			setupBackgroundLocationRequest();
	}

	public void enableForegroundMode() {
		Log.d(this.toString(), "enableForegroundMode");
		this.foreground = true;
		setupLocationRequest();
		requestLocationUpdates();
		// removeGeofences();
		// initGeofences();
	}

	public void enableBackgroundMode() {
		Log.d(this.toString(), "enableBackgroundMode");
		this.foreground = false;
		setupLocationRequest();
		requestLocationUpdates();
		// removeGeofences();
		// initGeofences();
	}

	@Override
	public void onDestroy() {
		Log.v(this.toString(), "onDestroy. Disabling location updates");
		disableLocationUpdates();
	}

	private void publishGeofenceTransitionEvent(Waypoint w, int transition) {
		GeocodableLocation l = new GeocodableLocation("Waypoint");
		l.setLatitude(w.getLatitude());
		l.setLongitude(w.getLongitude());
		l.setAccuracy(w.getRadius());
		l.getLocation().setTime(System.currentTimeMillis());

		LocationMessage r = new LocationMessage(l);

		r.setTransition(transition);
		r.setWaypoint(w);
		r.setSupressesTicker(true);

		publishLocationMessage(r);

	}

	private void publishWaypointMessage(WaypointMessage r) {
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(this.toString(),
					"publishWaypointMessage but ServiceMqtt not ready");
			return;
		}

		String topic = Preferences.getPubTopic(true);
		if (topic == null) {
			changeState(Defaults.State.ServiceLocator.NOTOPIC);
			return;
		}

		ServiceProxy.getServiceBroker().publish(
				topic + Preferences.getWaypointPubTopicPart(), r.toString(),
				false, Preferences.getPubQos(), 20, this, null);
	}

	public void publishLocationMessage() {
		publishLocationMessage(null);
	}

	private void publishLocationMessage(LocationMessage r) {
		this.lastPublish = System.currentTimeMillis();

		// Safety checks
		if (ServiceProxy.getServiceBroker() == null) {
			Log.e(this.toString(),
					"publishLocationMessage but ServiceMqtt not ready");
			return;
		}

		if ((r == null) && (getLastKnownLocation() == null)) {
			changeState(Defaults.State.ServiceLocator.NOLOCATION);
			return;
		}

		String topic = Preferences.getPubTopic(true);
		if (topic == null) {
			changeState(Defaults.State.ServiceLocator.NOTOPIC);
			return;
		}

		LocationMessage report;
		if (r == null)
			report = new LocationMessage(getLastKnownLocation());
		else
			report = r;

		if (Preferences.includeBattery())
			report.setBattery(App.getBatteryLevel());

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
		if (!remove && w.getShared())
			publishWaypointMessage(new WaypointMessage(w));

		if (!isWaypointWithValidGeofence(w))
			return;

		if (update || remove)
			removeGeofence(w);

		if (!remove) {
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
					.setCircularRegion(w.getLatitude(), w.getLongitude(),
							w.getRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE).build();

			fences.add(geofence);
		}

		if (fences.isEmpty()) {
			Log.v(this.toString(), "no geofences to add");
			return;
		}

		Log.v(this.toString(), "Adding" + fences.size() + " geofences");
		this.mLocationClient.addGeofences(fences, ServiceProxy
				.getPendingIntentForService(this.context,
						ServiceProxy.SERVICE_LOCATOR,
						Defaults.INTENT_ACTION_FENCE_TRANSITION, null), this);

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

		this.mLocationClient.removeGeofences(ids, this);
	}

	public void onEvent(Object event) {
	}

	private List<Waypoint> loadWaypoints() {
		return this.waypoints = this.waypointDao.loadAll();
	}

	private boolean isWaypointWithValidGeofence(Waypoint w) {
		return (w.getRadius() != null) && (w.getRadius() > 0)
				&& (w.getLatitude() != null) && (w.getLongitude() != null);
	}

	@Override
	public void onAddGeofencesResult(int arg0, String[] arg1) {
		if (LocationStatusCodes.SUCCESS == arg0) {
			for (int i = 0; i < arg1.length; i++) {
				Log.v(this.toString(), "geofence " + arg1[i] + " added");

			}

		} else {
			Log.v(this.toString(), "geofence adding failed");
		}

	}

	@Override
	public void onRemoveGeofencesByPendingIntentResult(int arg0,
			PendingIntent arg1) {
		if (LocationStatusCodes.SUCCESS == arg0) {
			Log.v(this.toString(), "geofence removed");
		} else {
			Log.v(this.toString(), "geofence removing failed");
		}

	}

	@Override
	public void onRemoveGeofencesByRequestIdsResult(int arg0, String[] arg1) {
		if (LocationStatusCodes.SUCCESS == arg0) {
			for (int i = 0; i < arg1.length; i++) {
				Log.v(this.toString(), "geofence " + arg1[i] + " removed");
			}
		} else {
			Log.v(this.toString(), "geofence removing failed");
		}

	}
}
