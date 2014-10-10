package st.alr.mqttitude.services;

import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map.Entry;

import st.alr.mqttitude.ActivityLauncher;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.ContactLink;
import st.alr.mqttitude.messages.ConfigurationMessage;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.messages.DumpMessage;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.messages.LocationMessage;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import de.greenrobot.event.EventBus;

@SuppressLint("NewApi")
public class ServiceApplication implements ProxyableService,
		StaticHandlerInterface {
	private static SharedPreferences sharedPreferences;
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
	private NotificationManager notificationManager;
	private static NotificationCompat.Builder notificationBuilder;
	private static boolean playServicesAvailable;
	private GeocodableLocation lastPublishedLocation;
	private Date lastPublishedLocationTime;
	private boolean even = false;
	private Handler handler;
	//private int mContactCount;
	private ServiceProxy context;

	@Override
	public void onCreate(ServiceProxy context) {
		this.context = context;
		checkPlayServices();

		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationBuilder = new NotificationCompat.Builder(context);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreference, String key) {
				if (key.equals(Preferences.getKey(R.string.keyNotification)) || key.equals(Preferences
                        .getKey(R.string.keyNotificationGeocoder)) || key.equals(Preferences
                        .getKey(R.string.keyNotificationLocation)))
					handleNotification();
			}

		};

		this.handler = new StaticHandler(this);

		sharedPreferences
				.registerOnSharedPreferenceChangeListener(this.preferencesChangedListener);
		handleNotification();

		//this.mContactCount = getContactCount();
		//context.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, this.mObserver);

	}

	@Override
	public void onDestroy() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		return 0;
	}

	@Override
	public void handleHandlerMessage(Message msg) {
		switch (msg.what) {
		case ReverseGeocodingTask.GEOCODER_RESULT:
			geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
			break;
		}
	};

	public void onEventMainThread(Events.LocationMessageReceived e) {
		Contact c = App.getContact(e.getTopic());

		if (c == null) {
			c = new Contact(e.getTopic());
			resolveContact(c);
            c.setLocation(e.getGeocodableLocation());
            c.setTid(e.getLocationMessage().getTid());
            App.addContact(c);
		} else {
			c.setLocation(e.getGeocodableLocation());
            c.setTid(e.getLocationMessage().getTid());
            EventBus.getDefault().post(new Events.ContactUpdated(c));
        }
	}

/*	private int getContactCount() {
		Cursor cursor = null;
		try {
			cursor = this.context.getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI, null, null, null,
					null);
			if (cursor != null) {
				return cursor.getCount();
			} else {
				return 0;
			}
		} catch (Exception ignore) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return 0;
	}

	private ContentObserver mObserver = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			final int currentCount = getContactCount();
			// We can only track the contact count so we cannot determine which
			// contact was added, removed or updated. Thus we have to update all
			if (currentCount != ServiceApplication.this.mContactCount) {
				updateAllContacts();
			}
			ServiceApplication.this.mContactCount = currentCount;
		}

	};*/
	private Notification notification;
	private PendingIntent notificationIntent;

	/**
	 * @category NOTIFICATION HANDLING
	 */
	private void handleNotification() {
		this.context.stopForeground(true);

		if (this.notificationManager != null)
			this.notificationManager.cancelAll();

		if (Preferences.getNotification() || !playServicesAvailable)
			createNotification();

	}

	private void createNotification() {
		notificationBuilder = new NotificationCompat.Builder(this.context);

		Intent resultIntent = new Intent(this.context, ActivityLauncher.class);
		resultIntent.setAction("android.intent.action.MAIN");
		resultIntent.addCategory("android.intent.category.LAUNCHER");

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent resultPendingIntent = PendingIntent.getActivity(
				this.context, 0, resultIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		notificationBuilder.setContentIntent(resultPendingIntent);

		this.notificationIntent = ServiceProxy.getPendingIntentForService(
				this.context, ServiceProxy.SERVICE_LOCATOR,
				Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN, null);
		notificationBuilder.addAction(R.drawable.ic_action_upload,
				this.context.getString(R.string.publish),
				this.notificationIntent);
		updateNotification();
	}

	private static void showPlayServicesNotAvilableNotification() {
		NotificationCompat.Builder nb = new NotificationCompat.Builder(
				App.getContext());
		NotificationManager nm = (NotificationManager) App.getContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);

		nb.setContentTitle(App.getContext().getString(R.string.app_name))
				.setSmallIcon(R.drawable.ic_notification)
				.setContentText("Google Play Services are not available")
				.setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
		nm.notify(Defaults.NOTIFCATION_ID, nb.build());

	}

	public void updateTicker(String text) {
		notificationBuilder.setTicker(text
				+ ((this.even = this.even ? false : true) ? " " : ""));
		notificationBuilder.setSmallIcon(R.drawable.ic_notification);
		this.notificationManager.notify(Defaults.NOTIFCATION_ID,
				notificationBuilder.build());

		// Clear ticker
		notificationBuilder.setTicker(null);
		this.notificationManager.notify(Defaults.NOTIFCATION_ID,
				notificationBuilder.build());

		// if the notification is not enabled, the ticker will create an empty
		// one that we get rid of
		if (!Preferences.getNotification())
			this.notificationManager.cancel(Defaults.NOTIFCATION_ID);
	}

	public void updateNotification() {
		if (!Preferences.getNotification() || !playServicesAvailable)
			return;

		String title = null;
		String subtitle = null;
		long time = 0;

		if ((this.lastPublishedLocation != null)
				&& Preferences.getNotificationLocation()) {
			time = this.lastPublishedLocationTime.getTime();

			if ((this.lastPublishedLocation.getGeocoder() != null)
					&& Preferences.getNotificationGeocoder()) {
				title = this.lastPublishedLocation.toString();
			} else {
				title = this.lastPublishedLocation.toLatLonString();
			}
		} else {
			title = this.context.getString(R.string.app_name);
		}


        if(ServiceLocator.getState() == Defaults.State.ServiceLocator.INITIAL)
		    subtitle = ServiceBroker.getStateAsString(this.context);
        else
            subtitle = ServiceLocator.getStateAsString(this.context) + " | " + ServiceBroker.getStateAsString(this.context);

        notificationBuilder.setContentTitle(title);
		notificationBuilder
				.setSmallIcon(R.drawable.ic_notification)
				.setContentText(subtitle)
				.setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
		if (time != 0)
			notificationBuilder.setWhen(this.lastPublishedLocationTime.getTime());

		this.notification = notificationBuilder.build();
		this.context.startForeground(Defaults.NOTIFCATION_ID, this.notification);
	}

	public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
		updateNotification();
	}

	public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
		updateNotification();
	}

	private void geocoderAvailableForLocation(GeocodableLocation l) {
		if (l == this.lastPublishedLocation)
			updateNotification();
	}

	public void onEvent(Events.WaypointTransition e) {
        if(e.getWaypoint().getNotificationOnEnter() || e.getWaypoint().getNotificationOnLeave()) {
            String formatString = e.getWaypoint().getNotificationMessage();

            if(formatString == null || formatString.equals(""))
                formatString = context.getString(R.string.waypointLocalDefaultFormatMessage);

            updateTicker(Defaults.formatNotificationMessage(context, formatString, e.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER, e.getWaypoint()));

        }
	}

	public void onEvent(Events.PublishSuccessfull e) {
		if ((e.getExtra() != null) && (e.getExtra() instanceof LocationMessage)) {
			LocationMessage l = (LocationMessage) e.getExtra();

			this.lastPublishedLocation = l.getLocation();
			this.lastPublishedLocationTime = l.getLocation().getDate();

			if (Preferences.getNotificationGeocoder() && (l.getLocation().getGeocoder() == null))
				(new ReverseGeocodingTask(this.context, this.handler)).execute(new GeocodableLocation[] { l.getLocation() });

			updateNotification();

			if (Preferences.getNotificationTickerOnPublish() && !l.doesSupressTicker())
				updateTicker(this.context.getString(R.string.statePublished));

		}
	}

	public static boolean checkPlayServices() {
		playServicesAvailable = ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(App.getContext());

		if (!playServicesAvailable)
			showPlayServicesNotAvilableNotification();

		return playServicesAvailable;

	}

	public void updateAllContacts() {
		Iterator<Contact> it = App.getCachedContacts().values().iterator();
		while (it.hasNext()) {
			Contact c = it.next();
			resolveContact(c);
			EventBus.getDefault().post(new Events.ContactUpdated(c));
		}

	}

	/*
	 * Resolves username and image either from a locally saved mapping or from
	 * synced cloud contacts. If no mapping is found, no name is set and the
	 * default image is assumed
	 */
	void resolveContact(Contact c) {

		long contactId = getContactId(c);
		boolean found = false;

		if (contactId <= 0) {
			setContactImageAndName(c, null, null);
			return;
		}

        // Resolve image and name from contact id
		Cursor cursor = this.context.getContentResolver().query(RawContacts.CONTENT_URI, null,ContactsContract.Data.CONTACT_ID + " = ?", new String[] { contactId + "" }, null);
		if (!cursor.isAfterLast()) {

			while (cursor.moveToNext()) {
				Bitmap image = Contact.resolveImage(this.context.getContentResolver(), contactId);
				String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				c.setName(displayName);
				c.setUserImage(image);
				found = true;
				break;
			}
		}

		if (!found)
			setContactImageAndName(c, null, null);

		cursor.close();

	}

	void setContactImageAndName(Contact c, Bitmap image, String name) {
		c.setName(name);
		c.setUserImage(image);
	}

	private long getContactId(Contact c) {
		ContactLink cl = App.getContactLinkDao().load(c.getTopic());
		return cl != null ? cl.getContactId() : 0;
	}


	public void linkContact(Contact c, long contactId) {
		ContactLink cl = new ContactLink(c.getTopic(), contactId);
		App.getContactLinkDao().insertOrReplace(cl);

		resolveContact(c);
		EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
	}

    public void unlinkContact(Contact c) {
        App.getContactLinkDao().deleteByKey(c.getTopic());
        c.setName(null);
        c.setUserImage(null);
        EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
    }


    public void dump() {
        Log.v(this.toString(), "Initiating dump procedure");
        DumpMessage dump = new DumpMessage();
        dump.setLocation(ServiceProxy.getServiceLocator().getLocationMessage(null));
        dump.setConfiguration(new ConfigurationMessage(EnumSet.of(ConfigurationMessage.Includes.PREFERENCES, ConfigurationMessage.Includes.CONNECTION, ConfigurationMessage.Includes.IDENTIFICATION)));

        dump.setLocatorReady(ServiceProxy.getServiceLocator().isReady());
        dump.setLocatorState(ServiceLocator.getState());
        dump.setLocatorForeground(ServiceProxy.getServiceLocator().isForeground());
        dump.setLocatorLastKnownLocation(ServiceProxy.getServiceLocator().getLastKnownLocation());
        dump.setLocatorLastPublishDate(ServiceProxy.getServiceLocator().getLastPublishDate());
        dump.setLocatorWaypointCount(ServiceProxy.getServiceLocator().getWaypointCount());
        dump.setLocatorHasLocationClient(ServiceProxy.getServiceLocator().hasLocationClient());
        dump.setLocatorHasLocationRequest(ServiceProxy.getServiceLocator().hasLocationRequest());
        dump.setBrokerKeepAliveSeconds(ServiceProxy.getServiceBroker().getKeepaliveSeconds());
        dump.setBrokerError(ServiceProxy.getServiceBroker().getError());
        dump.setBrokerState(ServiceBroker.getState());
        dump.setBrokerDeferredPublishablesCount(ServiceProxy.getServiceBroker().getDeferredPublishablesCound());
        dump.setApplicationPlayServicesAvailable(playServicesAvailable);
        Log.v(this.toString(), "Dump data: " + dump.toString());

        ServiceProxy.getServiceBroker().publish(Preferences.getPubTopicBase(true), dump.toString());

    }
}
