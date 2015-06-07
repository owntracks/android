package org.owntracks.android.services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

import org.owntracks.android.activities.ActivityLauncher;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.ContactLink;
import org.owntracks.android.db.ContactLinkDao;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.messages.DumpMessage;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.event.EventBus;

public class ServiceApplication implements ProxyableService,
		StaticHandlerInterface {
    public static final int NOTIFCATION_ID = 1338;
    public static final int NOTIFCATION_ID_TICKER = 1339;
    public static final int NOTIFCATION_ID_CONTACT_TRANSITION = 1340;

    final static String NOTIFCATION_ID_CONTACT_TRANSITION_GROUP = "org.owntracks.android.group.transition";

    private static SharedPreferences sharedPreferences;
	private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
	private NotificationManager notificationManager;
	private static NotificationCompat.Builder notificationBuilder;
    private static NotificationCompat.Builder notificationBuilderTicker;

    private static boolean playServicesAvailable;
	private GeocodableLocation lastPublishedLocation;
	private Date lastPublishedLocationTime;
	private boolean even = false;
	private Handler handler;
	//private int mContactCount;
	private ServiceProxy context;
    private HandlerThread notificationThread;
    private Handler notificationHandler;
    private SimpleDateFormat transitionDateFormater;

    private LinkedList<Spannable> contactTransitionNotifications;
    private HashSet<Uri> contactTransitionNotificationsContactUris;

    @Override
	public void onCreate(ServiceProxy context) {
		this.context = context;
		checkPlayServices();
        this.notificationThread = new HandlerThread("NOTIFICATIONTHREAD");
        this.notificationThread.start();
        this.notificationHandler = new Handler(this.notificationThread.getLooper());
		this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.contactTransitionNotifications = new LinkedList<Spannable>();
        this.contactTransitionNotificationsContactUris = new HashSet<>();
		notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilderTicker = new NotificationCompat.Builder(context);

		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.transitionDateFormater = new SimpleDateFormat("HH:mm", context.getResources().getConfiguration().locale);

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
        if ((intent != null) && (intent.getAction() != null) && intent.getAction().equals(ServiceProxy.INTENT_ACTION_CANCEL_TRANSITION_NOTIFICATION)) {
            clearTransitionNotifications();
        }
        return 0;
    }



    @Override
	public void handleHandlerMessage(Message msg) {
		switch (msg.what) {
		case ReverseGeocodingTask.GEOCODER_RESULT:
			geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
			break;
		}
	}

    public void onEventMainThread(Events.ClearLocationMessageReceived e) {
        App.removeContact(e.getContact());
    }


    private Contact lazyUpdateContactFromMessage(String topic, GeocodableLocation l, String trackerId) {
        Log.v(this.toString(), "lazyUpdateContactFromMessage for: " +topic);
        org.owntracks.android.model.Contact c = App.getContact(topic);

        if (c == null) {
            c = App.getInitializingContact(topic);


            if(c == null) {
                Log.v(this.toString(), "creating new contact without card: " + topic);
                c = new org.owntracks.android.model.Contact(topic);
            } else {
                Log.v(this.toString(), "creating unintialized contact with card: " + topic);
            }
            resolveContact(c);
            c.setLocation(l);
            c.setTrackerId(trackerId);
            App.addContact(c);
        } else {
            c.setLocation(l);
            c.setTrackerId(trackerId);
            EventBus.getDefault().post(new Events.ContactUpdated(c));
        }
        return c;
    }

    private String getBaseTopic(String forStr, String topic) {
        if(topic.endsWith(forStr))
            return topic.substring(0, (topic.length()  - forStr.length()));
        else
            return topic;
    }

    private String getBaseTopicForEvent(String topic) {
        return getBaseTopic(Preferences.getPubTopicEventsPart(), topic);
    }

    private String getBaseTopicForInfo(String topic) {
        return getBaseTopic(Preferences.getPubTopicInfoPart(), topic);
    }

    public void onEventMainThread(Events.TransitionMessageReceived e) {
        Contact c = lazyUpdateContactFromMessage(getBaseTopicForEvent(e.getTopic()), e.getGeocodableLocation(), e.getTransitionMessage().getTrackerId());

        if(e.getTransitionMessage().isRetained() && Preferences.getNotificationOnTransitionMessage()) {
            Log.v(this.toString(), "transition: " + e.getTransitionMessage().getTransition());
            addTransitionMessageNotification(e, c);
        }



    }


    public void onEventMainThread(Events.CardMessageReceived e) {
        String topic = getBaseTopicForInfo(e.getTopic());
        Contact c = App.getContact(topic);
        Log.v(this.toString(), "card message received for: " + topic);

        if(App.getInitializingContact(topic) != null) {
                Log.v(this.toString(), "ignoring second card for uninitialized contact " + topic);
                return;
        }
        if(c == null) {
            Log.v(this.toString(), "initializing card for: " + topic);

            c = new Contact(topic);
            c.setCardFace(e.getCardMessage().getFace());
            c.setCardName(e.getCardMessage().getName());

            App.addUninitializedContact(c);
         } else {

            Log.v(this.toString(), "updating card for existing contact: " + topic);
            c.setCardFace(e.getCardMessage().getFace());
            c.setCardName(e.getCardMessage().getName());
            EventBus.getDefault().post(new Events.ContactUpdated(c));
        }
    }

    public void onEventMainThread(Events.LocationMessageReceived e) {
        lazyUpdateContactFromMessage(e.getTopic(), e.getGeocodableLocation(), e.getLocationMessage().getTrackerId());
	}

    public void onEventMainThread(Events.ConfigurationMessageReceived e){

        Preferences.fromJsonObject(e.getConfigurationMessage().toJSONObject());

        // Reconnect to broker after new configuration has been saved.
        Runnable r = new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceBroker().reconnect();
            }
        };
        new Thread(r).start();

    }

	private Notification notification;
	private PendingIntent notificationIntent;

    private PendingIntent transitionCancelIntent;

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
				ServiceProxy.INTENT_ACTION_PUBLISH_LASTKNOWN_MANUAL, null);
		notificationBuilder.addAction(R.drawable.ic_report_notification, this.context.getString(R.string.publish), this.notificationIntent);
		updateNotification();
	}

	private static void showPlayServicesNotAvilableNotification() {
		NotificationCompat.Builder nb = new NotificationCompat.Builder(
				App.getContext());
		NotificationManager nm = (NotificationManager) App.getContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);

		//nb.setContentTitle(App.getContext().getString(R.string.app_name))
		//		.setSmallIcon(R.drawable.ic_notification)
		//		.setContentText("Google Play Services are not available")
				//.setPriority(NotificationCompat.PRIORITY_MIN);
		//nm.notify(Defaults.NOTIFCATION_ID, nb.build());

	}

	public void updateTicker(String text, boolean vibrate) {
        Log.v(this.toString(), "vibrate: " + vibrate);
        // API >= 21 doesn't have a ticker
        if(android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderTicker.setPriority(NotificationCompat.PRIORITY_HIGH);
            notificationBuilderTicker.setColor(context.getResources().getColor(R.color.primary));
            notificationBuilderTicker.setSmallIcon(R.drawable.ic_notification);
            notificationBuilderTicker.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderTicker.setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationBuilderTicker.setContentTitle(context.getString(R.string.app_name));
            notificationBuilderTicker.setContentText(text + ((this.even = !this.even) ? " " : ""));
            notificationBuilderTicker.setAutoCancel(true);



        } else {
            notificationBuilderTicker.setSmallIcon(R.drawable.ic_notification);
            notificationBuilderTicker.setTicker(text + ((this.even = !this.even) ? " " : ""));

        }
        if(vibrate) {
            notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            notificationBuilderTicker.setVibrate(new long[]{0, 500}); // 0 ms delay, 500 ms vibration
        }

		// Clear ticker
        this.notificationManager.notify(NOTIFCATION_ID_TICKER, notificationBuilderTicker.build());

		// if the notification is not enabled, the ticker will create an empty
		// one that we get rid of
		if (!Preferences.getNotification()) {
            this.notificationManager.cancel(NOTIFCATION_ID_TICKER);
        } else {

            notificationHandler.postDelayed(new Runnable() {

                public void run() {
                    notificationManager.cancel(NOTIFCATION_ID_TICKER);
                }}, 1500);

        }
	}

	public void updateNotification() {
		if (!Preferences.getNotification() || !playServicesAvailable)
			return;

		String title;
		String subtitle;
		long time = 0;

		if ((this.lastPublishedLocation != null) && Preferences.getNotificationLocation()) {
			time = this.lastPublishedLocationTime.getTime();

			if ((this.lastPublishedLocation.getGeocoder() != null) && Preferences.getNotificationGeocoder()) {
				title = this.lastPublishedLocation.toString();
			} else {
				title = this.lastPublishedLocation.toLatLonString();
			}
		} else {
			title = this.context.getString(R.string.app_name);
		}


        subtitle = ServiceBroker.getStateAsString(this.context);

        notificationBuilder.setContentTitle(title).setSmallIcon(R.drawable.ic_notification).setContentText(subtitle);

        if(android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilder.setColor(context.getResources().getColor(R.color.primary));
            notificationBuilder.setPriority(Notification.PRIORITY_MIN);
            notificationBuilder.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        if (time != 0)
			notificationBuilder.setWhen(this.lastPublishedLocationTime.getTime());

		this.notification = notificationBuilder.build();
		this.context.startForeground(NOTIFCATION_ID, this.notification);
	}


    public void addTransitionMessageNotification(Events.TransitionMessageReceived e, Contact c) {
        String location = e.getTransitionMessage().getDescription();
        if(location == null) {
            location = "a location";
        }

        String name = c.getDisplayName();

        if(name == null) {
            name = e.getTransitionMessage().getTrackerId();
        }

        if(name == null) {
            name = e.getTopic();
        }

        String transition;
        if(e.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            transition = context.getString(R.string.transitionentering);
        } else {
            transition = context.getString(R.string.transitionleaving);
        }
        String dateStr = transitionDateFormater.format(new Date());
        Spannable message = new SpannableString(dateStr + ": "+ name + " " + transition + " " + location);
        message.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, dateStr.length()+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        contactTransitionNotifications.push(message);

        if(c.getLinkLookupUri() != null)
            this.contactTransitionNotificationsContactUris.add(c.getLinkLookupUri());

        if(transitionCancelIntent != null)
            transitionCancelIntent.cancel();

        this.transitionCancelIntent = ServiceProxy.getPendingIntentForService(
                this.context, ServiceProxy.SERVICE_APP,
                ServiceProxy.INTENT_ACTION_CANCEL_TRANSITION_NOTIFICATION, null);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
        for (Spannable text : this.contactTransitionNotifications) {
            style.addLine(text);
        }


        String title = "New waypoint transitions";
        style.setBigContentTitle(title);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this.context)
                .setSmallIcon(R.drawable.ic_notification)
                .setStyle(style)
                .setContentText(this.contactTransitionNotifications.getFirst()) // InboxStyle doesn't show text when only one line is added. In this case ContentText is shown
                .setContentTitle(title) // InboxStyle doesn't show title only one line is added. In this case ContentTitle is shown
                .setGroup(NOTIFCATION_ID_CONTACT_TRANSITION_GROUP)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setNumber(this.contactTransitionNotifications.size())
                .setDeleteIntent(this.transitionCancelIntent);

        for(Uri uri : this.contactTransitionNotificationsContactUris) {
            Log.v(this.toString(), "adding persion uri: " + uri.toString());
            builder.addPerson(uri.toString());
        }

        if(android.os.Build.VERSION.SDK_INT >= 21) {
            builder.setColor(context.getResources().getColor(R.color.primary));
            builder.setPriority(Notification.PRIORITY_MIN);
            builder.setCategory(Notification.CATEGORY_SERVICE);
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        notificationManager.notify(NOTIFCATION_ID_CONTACT_TRANSITION, builder.build());


    }

    private void clearTransitionNotifications() {
        this.contactTransitionNotifications.clear(); // no need for synchronized, both add and clear are run on main thread
        this.contactTransitionNotificationsContactUris.clear();
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
        if(Preferences.getNotificationTickerOnWaypointTransition()) {
            updateTicker(context.getString(e.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving) + " " + e.getWaypoint().getDescription(), Preferences.getNotificationVibrateOnWaypointTransition());
        }
	}

	public void onEvent(Events.PublishSuccessfull e) {
		if ((e.getExtra() != null) && (e.getExtra() instanceof LocationMessage) && !e.wasQueued()) {
			LocationMessage l = (LocationMessage) e.getExtra();

			this.lastPublishedLocation = l.getLocation();
			this.lastPublishedLocationTime = l.getLocation().getDate();

			if (Preferences.getNotificationGeocoder() && (l.getLocation().getGeocoder() == null))
				(new ReverseGeocodingTask(this.context, this.handler)).execute(new GeocodableLocation[] { l.getLocation() });

			updateNotification();

			if (Preferences.getNotificationTickerOnPublish() && !l.getSupressTicker())
				updateTicker(this.context.getString(R.string.statePublished), Preferences.getNotificationVibrateOnPublish());

		}
	}

	public static boolean checkPlayServices() {
		playServicesAvailable = ConnectionResult.SUCCESS == GooglePlayServicesUtil.isGooglePlayServicesAvailable(App.getContext());

		if (!playServicesAvailable)
			showPlayServicesNotAvilableNotification();

		return playServicesAvailable;

	}

	public void updateAllContacts() {
        for (Contact c : App.getCachedContacts().values()) {
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
            c.setHasLink(false);
			return;
		}

        // Resolve image and name from contact id
		Cursor cursor = this.context.getContentResolver().query(RawContacts.CONTENT_URI, null,ContactsContract.Data.CONTACT_ID + " = ?", new String[] { contactId + "" }, null);
		if (!cursor.isAfterLast()) {

			while (cursor.moveToNext()) {
				Bitmap image = Contact.resolveImage(this.context.getContentResolver(), contactId);
				String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                setContactImageAndName(c, image, displayName);
                c.setHasLink(true);
                c.setLinkLookupURI(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contactId));
				found = true;
				break;
			}
		}

		if (!found) {
            setContactImageAndName(c, null, null);
            c.setHasLink(false);
        }
		cursor.close();

	}

	void setContactImageAndName(Contact c, Bitmap image, String name) {
		c.setLinkName(name);
		c.setLinkFace(image);
	}

	private long getContactId(Contact c) {

        ContactLink cl = queryContactLink(c);
        return cl != null ? cl.getContactId() : 0;
	}
    private ContactLink queryContactLink(Contact c) {
        QueryBuilder qb = App.getContactLinkDao().queryBuilder();

        Query query = qb.where(
                qb.and(
                        ContactLinkDao.Properties.Topic.eq(c.getTopic()),
                        ContactLinkDao.Properties.ModeId.eq(Preferences.getModeId())
                )
        ).build();

        return (ContactLink)query.unique();
    }


	public void linkContact(Contact c, long contactId) {
		ContactLink cl = new ContactLink(null, c.getTopic(), contactId, Preferences.getModeId());
		App.getContactLinkDao().insertOrReplace(cl);

		resolveContact(c);
		EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
	}

    public void unlinkContact(Contact c) {
        ContactLink cl = queryContactLink(c);
        if(cl != null)
            App.getContactLinkDao().delete(cl);
        c.setLinkName(null);
        c.setLinkFace(null);
        c.setHasLink(false);
        EventBus.getDefault().postSticky(new Events.ContactUpdated(c));
    }


    public void dump() {
        Log.v(this.toString(), "Initiating dump procedure");
        DumpMessage dump = new DumpMessage();
        dump.setLocation(ServiceProxy.getServiceLocator().getLocationMessage(null));
        dump.setConfiguration(new ConfigurationMessage(EnumSet.of(ConfigurationMessage.Includes.CONNECTION, ConfigurationMessage.Includes.IDENTIFICATION)));

        dump.setLocatorReady(ServiceProxy.getServiceLocator().isReady());
        dump.setLocatorState(ServiceLocator.getState());
        dump.setLocatorForeground(ServiceProxy.getServiceLocator().isForeground());
        dump.setLocatorLastKnownLocation(ServiceProxy.getServiceLocator().getLastKnownLocation());
        dump.setLocatorLastPublishDate(ServiceProxy.getServiceLocator().getLastPublishDate());
        dump.setLocatorWaypointCount(ServiceProxy.getServiceLocator().getWaypointCount());
        dump.setLocatorHasLocationClient(ServiceProxy.getServiceLocator().hasLocationClient());
        dump.setLocatorHasLocationRequest(ServiceProxy.getServiceLocator().hasLocationRequest());
        dump.setLocatorDebug(ServiceProxy.getServiceLocator().getDebugData());

        dump.setBrokerError(ServiceProxy.getServiceBroker().getError());
        dump.setBrokerState(ServiceBroker.getState());
        dump.setBrokerDeferredPublishablesCount(ServiceProxy.getServiceBroker().getDeferredPublishablesCound());
        dump.setApplicationPlayServicesAvailable(playServicesAvailable);
        Log.v(this.toString(), "Dump data: " + dump.toString());

        ServiceProxy.getServiceBroker().publish(dump, Preferences.getDeviceTopic(true), 0, false);

    }
}
