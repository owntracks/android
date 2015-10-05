package org.owntracks.android.services;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.Geofence;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.activities.ActivityLauncher;
import org.owntracks.android.activities.ActivityMessages;
import org.owntracks.android.activities.ActivityPreferencesConnection;
import org.owntracks.android.messages.LocationMessage;
import org.owntracks.android.messages.MsgMessage;
import org.owntracks.android.messages.TransitionMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.SnackbarFactory;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class ServiceNotification implements ProxyableService, StaticHandlerInterface {
    public static final String INTENT_ACTION_CANCEL_EVENT_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION";
    public static final String INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION = "org.owntracks.android.intent.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION";
    private static final String TAG ="ServiceNotification" ;

    private ServiceProxy context;
    private Preferences.OnPreferenceChangedListener preferencesChangedListener;
    private StaticHandler handler;
    private GeocodableLocation lastPublishedLocation;
    private long lastPublishedLocationTst = 0;
    private NotificationManager notificationManager;

    // Ongoing notification
    private static final int NOTIFICATION_ID_ONGOING = 1;
    private NotificationCompat.Builder notificationBuilderOngoing;
    private PendingIntent notificationIntentOngoing;
    private Notification notificationOngoing;

    // Event notification
    private static final int NOTIFICATION_ID_EVENTS = 2;
    private NotificationCompat.Builder notificationBuilderEvents;
    private LinkedList<Spannable> notificationListEvents;
    private Notification notificationEvents;

    // Message notifications
    private NotificationCompat.Builder notificationBuilderMessages;
    private static final int NOTIFICATION_ID_MESSAGES = 3;
    private LinkedList<Spannable> notificationListMessages;
    private Notification notificationMessages;
    private SimpleDateFormat dateFormater;


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
                if (key.equals(Preferences.getKey(R.string.keyNotification)) || key.equals(Preferences.getKey(R.string.keyNotificationLocation)) || key.equals(Preferences.getKey(R.string.keyNotificationMessages)) || key.equals(Preferences.getKey(R.string.keyNotificationEvents))) {
                    Log.v(TAG, "notification prefs changed");
                    clearNotifications();
                    setupNotifications();
                    updateNotifications();
                }
            }



        };

        this.handler = new StaticHandler(this);

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

    private void setupNotifications() {

        setupNotificationOngoing();
        setupNotificationEvents();
        setupNotificationMessages();
    }

    private void updateNotifications() {
        updateNotificationOngoing();
        updateNotificationMessage();
        updateNotificationEvents();
    }

    /*
    * NOTIFICATION BUILDER SETUP
    * */
    private void setupNotificationOngoing() {
        Log.v(TAG, "setupNotificationOngoing. Enabled: " +Preferences.getNotification() );
        if (!Preferences.getNotification())
            return;

        notificationBuilderOngoing = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityLauncher.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilderOngoing.setContentIntent(resultPendingIntent);
        this.notificationIntentOngoing = ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_LOCATOR, ServiceLocator.RECEIVER_ACTION_PUBLISH_LASTKNOWN_MANUAL, null);
        notificationBuilderOngoing.addAction(R.drawable.ic_report_notification, this.context.getString(R.string.publish), this.notificationIntentOngoing);
    }


    @SuppressLint("NewApi")
    private void setupNotificationMessages() {
        if (!Preferences.getNotification())
            return;

        notificationBuilderMessages = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityMessages.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilderMessages.setContentIntent(resultPendingIntent);
        notificationBuilderMessages.setDeleteIntent(ServiceProxy.getBroadcastIntentForService(this.context, ServiceProxy.SERVICE_NOTIFICATION, ServiceNotification.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION, null));

        notificationBuilderMessages.setSmallIcon(R.drawable.ic_notification);
        notificationBuilderMessages.setGroup(NOTIFICATION_ID_MESSAGES + "");
        notificationBuilderMessages.setAutoCancel(true);
        notificationBuilderMessages.setShowWhen(false);


        //if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderMessages.setColor(ContextCompat.getColor(context, R.color.primary));
            notificationBuilderMessages.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderMessages.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderMessages.setVisibility(Notification.VISIBILITY_PUBLIC);
        //}

    }


    @SuppressLint("NewApi")
    private void setupNotificationEvents() {
        if (!Preferences.getNotificationEvents())
            return;

        notificationBuilderEvents = new NotificationCompat.Builder(context);

        Intent resultIntent = new Intent(this.context, ActivityLauncher.class);
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


    /*
    * NEW NOTIFICATION HANDLING
    * */
    public void updateNotificationOngoing() {
        if (!Preferences.getNotification())
            return;

        String title;
        String subtitle = ServiceBroker.getStateAsString(this.context);
        long when = this.lastPublishedLocationTst;


        if (isLastPublishedLocationWithGeocoderAvailable() && Preferences.getNotificationLocation()) {
            title = this.lastPublishedLocation.getGeocoder();
        } else {
            title = this.context.getString(R.string.app_name);
        }

        notificationBuilderOngoing.setContentTitle(title).setSmallIcon(R.drawable.ic_notification).setContentText(subtitle);
        notificationBuilderOngoing.setWhen(when);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            notificationBuilderOngoing.setColor(context.getResources().getColor(R.color.primary));
            notificationBuilderOngoing.setPriority(Notification.PRIORITY_MIN);
            notificationBuilderOngoing.setCategory(Notification.CATEGORY_SERVICE);
            notificationBuilderOngoing.setVisibility(Notification.VISIBILITY_PUBLIC);
        }



        this.notificationOngoing = notificationBuilderOngoing.build();
        this.context.startForeground(NOTIFICATION_ID_ONGOING, this.notificationOngoing);
    }


    public void addNotificationEvents(TransitionMessage m) {
        Contact c = App.getContact(ServiceProxy.getServiceParser().getBaseTopicForEvent(m.getTopic()));
        String name;
        String dateStr = dateFormater.format(new Date());
        String transition =  context.getString(m.getTransition() == Geofence.GEOFENCE_TRANSITION_ENTER ? R.string.transitionentering : R.string.transitionleaving);
        String location = m.getDescription();

        if (location == null) {
            location = context.getString(R.string.aLocation);
        }

        if(c != null)
         name = c.getDisplayName();
        else {
            name = m.getTrackerId();

            if (name == null) {
                name = m.getTopic();
            }
        }

        Spannable message = new SpannableString(dateStr + ": " + name + " " + transition + " " + location);
        message.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, dateStr.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        notificationListEvents.push(message);
    }

    private void clearNotificationTransitions() {
        this.notificationListEvents.clear();
        notificationManager.cancel(NOTIFICATION_ID_EVENTS);

    }


    public void updateNotificationEvents() {
        if(!Preferences.getNotification() || !Preferences.getNotificationEvents() || this.notificationListEvents.size() == 0)
            return;


        InboxStyle style  = new InboxStyle();
        for (Spannable text : this.notificationListEvents)
            style.addLine(text);

        String title = context.getString(R.string.events);
        style.setBigContentTitle(title);

        notificationBuilderEvents.setStyle(style);
        notificationBuilderEvents.setContentText(this.notificationListEvents.getFirst());
        notificationBuilderEvents.setContentTitle(title);
        notificationBuilderEvents.setNumber(this.notificationListEvents.size());

        notificationManager.notify(NOTIFICATION_ID_EVENTS, notificationBuilderEvents.build());
    }

    public void addNotificationMessage(MsgMessage m) {

        String channel = "#" + m.getChannel();
        Spannable message = new SpannableString(channel + ": " + m.getDesc());
        message.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, channel.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        notificationListMessages.push(message);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ServiceNotification.INTENT_ACTION_CANCEL_MESSAGE_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationMessages();
        } else if (ServiceNotification.INTENT_ACTION_CANCEL_EVENT_NOTIFICATION.equals(intent.getAction())) {
            clearNotificationTransitions();
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

    private void geocoderAvailableForLocation(GeocodableLocation l) {
        if (l == this.lastPublishedLocation)
            updateNotificationOngoing();
    }

    private boolean isLastPublishedLocationWithGeocoderAvailable() {
        return this.lastPublishedLocation != null && this.lastPublishedLocation.getGeocoder() != null;
    }


    @Override
    public void onEvent(Events.Dummy event) { }

    public void onEvent(Events.MsgMessageReceived e) {
        if(e.getMessage().isExpired())
            return;

        addNotificationMessage(e.getMessage());
        updateNotificationMessage();
    }

    public void onEvent(Events.TransitionMessageReceived e) {
        if(e.getTransitionMessage().isRetained())
            return;

        e.getTransitionMessage().setTopic(e.getTopic());
        addNotificationEvents(e.getTransitionMessage());
        updateNotificationEvents();
    }

    public void onEventMainThread(Events.PublishSuccessful e) {
        Log.v(TAG, "publish successfull. this.lastPublishedLocationTst:" + this.lastPublishedLocationTst + ", " + "event: " +  e.getDate().getTime());
        if ((e.getExtra() != null) && (e.getExtra() instanceof LocationMessage) ) {

            if (Preferences.getNotificationLocation()) {
                LocationMessage l = (LocationMessage) e.getExtra();
                this.lastPublishedLocation = l.getLocation();
                this.lastPublishedLocationTst = l.getLocation().getDate().getTime();

                if (l.getLocation().getGeocoder() == null)
                    (new ReverseGeocodingTask(this.context, this.handler)).execute(new GeocodableLocation[]{l.getLocation()});

                updateNotificationOngoing();
             }

            if (App.isInForeground() && App.getCurrentActivity() != null && App.getCurrentActivity() instanceof SnackbarFactory.SnackbarFactoryDelegate) {
                SnackbarFactory.show(SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate) App.getCurrentActivity(), R.string.statePublished, Snackbar.LENGTH_SHORT));
            }
        }
    }

    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        final Activity a = App.getCurrentActivity();
        if (App.isInForeground() && a != null && a instanceof SnackbarFactory.SnackbarFactoryDelegate) {
            Snackbar s = null;

            if (e.getState() == ServiceBroker.State.CONNECTED) {
                s = SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate)a, R.string.snackbarConnected, Snackbar.LENGTH_LONG);
            } else if (e.getState() == ServiceBroker.State.CONNECTING) {
                s = SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate)a, R.string.snackbarConnecting, Snackbar.LENGTH_LONG);
            } else if (e.getState() == ServiceBroker.State.DISCONNECTED || e.getState() == ServiceBroker.State.DISCONNECTED_USERDISCONNECT) {
                s = SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate)a, R.string.snackbarDisconnected, Snackbar.LENGTH_LONG);
                s.setAction(R.string.snackbarDisconnectedReconnect, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ServiceProxy.runOrBind(a, new Runnable() {
                            @Override
                            public void run() {
                                ServiceProxy.getServiceBroker().reconnect();
                            }
                        });
                    }
                });

            } else if (e.getState() == ServiceBroker.State.DISCONNECTED_ERROR) {
                s = SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate)a, R.string.snackbarDisconnectedError, Snackbar.LENGTH_INDEFINITE);
                s.setAction(R.string.snackbarDisconnectedReconnect, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ServiceProxy.runOrBind(a, new Runnable() {
                            @Override
                            public void run() {
                                ServiceProxy.getServiceBroker().reconnect();
                            }
                        });
                    }
                });
            } else if (e.getState() == ServiceBroker.State.DISCONNECTED_CONFIGINCOMPLETE) {
                s = SnackbarFactory.make((SnackbarFactory.SnackbarFactoryDelegate)a, R.string.snackbarDisconnectedError, Snackbar.LENGTH_INDEFINITE);
                s.setAction(R.string.snackbarConfigIncompleteFix, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(a, ActivityPreferencesConnection.class);
                        a.startActivity(intent);
                    }
                });
            }

            if(s != null)
                SnackbarFactory.show(s);

        }

        updateNotificationOngoing();
    }

}

