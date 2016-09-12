package org.owntracks.android.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityWelcome;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.messages.MessageTransition;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.GeocodingProvider;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.Toasts;
import org.owntracks.android.support.interfaces.ProxyableService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class ServiceNotification implements ProxyableService {
    public static final String INTENT_ACTION_CANCEL_EVENT_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION";
    public static final String INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION"; //unused for now
    private static final String TAG ="ServiceNotification" ;

    private ServiceProxy context;
    private Preferences.OnPreferenceChangedListener preferencesChangedListener;
    private NotificationManager notificationManager;

    // Ongoing notification
    private static final int NOTIFICATION_ID_ONGOING = 1;
    private NotificationCompat.Builder notificationBuilderOngoing;
    private Notification notificationOngoing;
    private MessageLocation notificationOngoingLastLocationCache;
    private ServiceMessage.EndpointState notificationOngoingLastStateCache = ServiceMessage.EndpointState.INITIAL;

    // Event notification
    private static final int NOTIFICATION_ID_EVENTS = 2;
    private NotificationCompat.Builder notificationBuilderEvents;
    private LinkedList<Spannable> notificationListEvents;
    private Notification notificationEvents;

    // Message notifications. Unused for now
    private NotificationCompat.Builder notificationBuilderMessages;
    private static final int NOTIFICATION_ID_MESSAGES = 3;
    private LinkedList<Spannable> notificationListMessages;
    private Notification notificationMessages;
    private SimpleDateFormat dateFormater;


    // Permission notification
    private NotificationCompat.Builder notificationBuilderPermission;
    private static final int NOTIFICATION_ID_PERMISSION = 4;
    private Notification notificationPermission;



    @Override
    public void onCreate(ServiceProxy c) {
        this.context = c;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationListMessages = new LinkedList<>();
        this.notificationListEvents = new LinkedList<>();


        this.dateFormater = new SimpleDateFormat("HH:mm", context.getResources().getConfiguration().locale);

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        this.preferencesChangedListener = new Preferences.OnPreferenceChangedListener() {
            @Override
            public void onAttachAfterModeChanged() {
                clearNotifications();
                setupNotifications();
                updateNotifications();
            }

            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Preferences.Keys.NOTIFICATION) || key.equals(Preferences.Keys.NOTIFICATION_LOCATION) || key.equals(Preferences.Keys.NOTIFICATION_EVENTS)) {
                    Log.v(TAG, "notification prefs changed");
                    clearNotifications();
                    setupNotifications();
                    updateNotifications();
                }
            }



        };


        Preferences.registerOnPreferenceChangedListener(this.preferencesChangedListener);
        setupNotifications();
        updateNotifications();
    }



    private void clearNotifications() {
        if(!Preferences.getNotification())
            this.context.stopForeground(true);

        notificationManager.cancel(NOTIFICATION_ID_EVENTS);
        notificationManager.cancel(NOTIFICATION_ID_MESSAGES);

    }

    private void clearNotificationPermission() {
        notificationManager.cancel(NOTIFICATION_ID_PERMISSION);

    }

    private void setupNotifications() {

        setupNotificationOngoing();
        setupNotificationEvents();
    }



    private void updateNotifications() {
        updateNotificationOngoing();
        updateNotificationEvents();
    }

    /*
    * NOTIFICATION BUILDER SETUP
    * */
    private void setupNotificationOngoing() {
        Log.v(TAG, "setupNotificationOngoing. Enabled: " + Preferences.getNotification());
        if (!Preferences.getNotification())
            return;

        notificationBuilderOngoing = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderOngoing.setContentIntent(resultPendingIntent);
        notificationBuilderOngoing.setSortKey("a");

        notificationBuilderOngoing.addAction(R.drawable.ic_report_notification, this.context.getString(R.string.publish), ServiceProxy.getPendingIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null, PendingIntent.FLAG_CANCEL_CURRENT));


    }

    @SuppressLint("NewApi")
    private void setupNotificationEvents() {
        if (!Preferences.getNotificationEvents())
            return;

        notificationBuilderEvents = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityWelcome.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderEvents.setContentIntent(resultPendingIntent);
        notificationBuilderEvents.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION, null));
        notificationBuilderEvents.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderEvents.setAutoCancel(true);
        notificationBuilderEvents.setShowWhen(false);
        notificationBuilderEvents.setGroup(NOTIFICATION_ID_EVENTS + "");

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderEvents.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderEvents.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderEvents.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderEvents.setVisibility(Notification.VISIBILITY_PUBLIC);

        }
    }
    public void  updateNotificationOngoing() {
        Timber.v("enabled:%s, state:%s", Preferences.getNotification(), notificationOngoingLastStateCache.getLabel(context));
        if (!Preferences.getNotification())
            return;

        String subtitle = notificationOngoingLastStateCache.getLabel(context);

        if (isLastPublishedLocationWithGeocoderAvailable() && Preferences.getNotificationLocation()) {
            notificationBuilderOngoing.setContentTitle(this.notificationOngoingLastLocationCache.getGeocoder());
            notificationBuilderOngoing.setWhen(TimeUnit.SECONDS.toMillis(this.notificationOngoingLastLocationCache.getTst()));

        } else {
            notificationBuilderOngoing.setContentTitle(this.context.getString(R.string.app_name));
        }


        if (android.os.Build.VERSION.SDK_INT >= 23) {
            notificationBuilderOngoing.setColor(context.getResources().getColor(R.color.primary, context.getTheme()));
            notificationBuilderOngoing.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderOngoing.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        if(Preferences.getNotificationHigherPriority())
            notificationBuilderOngoing.setPriority(Notification.PRIORITY_DEFAULT);
        else
            notificationBuilderOngoing.setPriority(Notification.PRIORITY_MIN);

        notificationBuilderOngoing.setOngoing(true);
        notificationBuilderOngoing.setSmallIcon(R.drawable.ic_notification).setContentText(subtitle);
        this.notificationOngoing = notificationBuilderOngoing.build();
        this.context.startForeground(NOTIFICATION_ID_ONGOING, this.notificationOngoing);
    }


    public void addNotificationEvents(MessageTransition message) {

        FusedContact c = App.getFusedContact(message.getContactKey());

        String name;
        String dateStr = dateFormater.format(new Date());
        String transition =  context.getString(message.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionEntering : R.string.transitionLeaving);
        String location = message.getDesc();

        if (location == null) {
            location = context.getString(R.string.aLocation);
        }

        if(c != null)
            name = c.getFusedName();
        else {
            name = message.getTid();

            if (name == null) {
                name = message.getContactKey();
            }
        }

        Spannable notification = new SpannableString(dateStr + ": " + name + " " + transition + " " + location);
        notification.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, dateStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        notificationListEvents.push(notification);
    }

    private void clearNotificationTransitions() {
        this.notificationListEvents.clear();
        notificationManager.cancel(NOTIFICATION_ID_EVENTS);

    }


    public void updateNotificationEvents() {
        if(!Preferences.getNotification() || !Preferences.getNotificationEvents() || this.notificationListEvents.size() == 0)
            return;


        InboxStyle style  = new InboxStyle();


        ListIterator<Spannable> iter = this.notificationListEvents.listIterator();
        while(iter.hasNext()){
            style.addLine(iter.next());
        }

        String title = context.getString(R.string.events);
        style.setBigContentTitle(title);

        notificationBuilderEvents.setStyle(style);
        notificationBuilderEvents.setContentText(this.notificationListEvents.getFirst());
        notificationBuilderEvents.setContentTitle(title);
        notificationBuilderEvents.setNumber(this.notificationListEvents.size());

        notificationManager.notify(NOTIFICATION_ID_EVENTS, notificationBuilderEvents.build());
    }


    public void clearNotificationMessages() {
        this.notificationListMessages.clear();
        notificationManager.cancel(NOTIFICATION_ID_MESSAGES);
    }

    public void updateNotificationMessage() {
        if(this.notificationListMessages.size() == 0)
            return;

        InboxStyle style = new NotificationCompat.InboxStyle();
        for (Spannable text : this.notificationListMessages)
            style.addLine(text);

        String title = context.getString(R.string.notificationMessageTitle);
        style.setBigContentTitle(title);

        context.getString(R.string.notificationMessageTitle);
        notificationBuilderMessages.setStyle(style);
        notificationBuilderMessages.setContentText(notificationListMessages.getFirst());
        notificationBuilderMessages.setContentTitle(title);
        notificationBuilderMessages.setNumber(notificationListMessages.size());

        notificationManager.notify(NOTIFICATION_ID_MESSAGES, notificationBuilderMessages.build());
    }


    @Override
    public void onDestroy() {
        clearNotificationMessages();
        clearNotificationTransitions();
    }

    @Override
    public void onStartCommand(Intent intent, int flags, int startId) {
        if (ServiceNotification.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationMessages();
        } else if (ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationTransitions();
        }
    }



    private boolean isLastPublishedLocationWithGeocoderAvailable() {
        return this.notificationOngoingLastLocationCache != null && this.notificationOngoingLastLocationCache.getGeocoder() != null;
    }


    @Subscribe
    public void onEvent(Events.Dummy event) { }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.ModeChanged e) {
        updateNotificationOngoing(e.getNewModeId());
    }

    private void updateNotificationOngoing(int newModeId) {
        updateNotificationOngoing();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(Events.EndpointStateChanged e) {
        if (App.isInForeground()) {
            // Prevent double toasts when no connection can be established
            if(notificationOngoingLastStateCache == ServiceMessage.EndpointState.ERROR && e.getState() != ServiceMessage.EndpointState.DISCONNECTED)
                Toasts.showEndpointStateChange(e.getState());


        }
        notificationOngoingLastStateCache = e.getState();
        updateNotificationOngoing();
    }

    @Subscribe
    public void onEvent(Events.PermissionGranted e) {
        Log.v(TAG, "Events.PermissionGranted: " + e.getPermission() );
        if(e.getPermission().equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
            clearNotificationPermission();
        }
    }

    @Subscribe
    public void onEvent(MessageLocation m) {
        if(m.isOutgoing() && (notificationOngoingLastLocationCache == null || notificationOngoingLastLocationCache.getTst() <=  m.getTst())) {
            this.notificationOngoingLastLocationCache = m;
            GeocodingProvider.resolve(m, this);
        }
    }

    public void onMessageLocationGeocoderResult(MessageLocation m) {
        Timber.v("for location: %s", m.getGeocoder());
        if (m == notificationOngoingLastLocationCache) {
            updateNotificationOngoing();
        }
    }

    public void processMessage(MessageTransition message) {
        if(message.getRetained())
            return;

        addNotificationEvents(message);
        updateNotificationEvents();

    }
    public void notifyMissingPermissions() {

        notificationBuilderPermission = new NotificationCompat.Builder(context);

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        notificationBuilderPermission.setContentIntent(PendingIntent.getActivity(this.context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

        notificationBuilderPermission.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderPermission.setGroup(NOTIFICATION_ID_PERMISSION + "");
        notificationBuilderPermission.setAutoCancel(false);
        notificationBuilderPermission.setShowWhen(false);


        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderPermission.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderPermission.setPriority(Notification.PRIORITY_DEFAULT);
            notificationBuilderPermission.setCategory(Notification.CATEGORY_ERROR);
            notificationBuilderPermission.setVisibility(Notification.VISIBILITY_PUBLIC);
        }


        notificationBuilderPermission.setContentText("Missing a required permission");
        notificationBuilderPermission.setContentTitle(this.context.getString(R.string.app_name));

        notificationManager.notify(NOTIFICATION_ID_PERMISSION, notificationBuilderPermission.build());
    }
}

