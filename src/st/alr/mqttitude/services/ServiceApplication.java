
package st.alr.mqttitude.services;

import java.util.Date;
import java.util.Map;

import st.alr.mqttitude.ActivityLauncher;
import st.alr.mqttitude.ActivityMain;
import st.alr.mqttitude.App;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.R;
import st.alr.mqttitude.support.BackgroundPublishReceiver;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.greenrobot.event.EventBus;

//package st.alr.mqttitude.services;
//
//import java.io.InputStream;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//import st.alr.mqttitude.ActivityMain;
//import st.alr.mqttitude.App;
//import st.alr.mqttitude.R;
//import st.alr.mqttitude.support.BackgroundPublishReceiver;
//import st.alr.mqttitude.support.Contact;
//import st.alr.mqttitude.support.Defaults;
//import st.alr.mqttitude.support.Events;
//import st.alr.mqttitude.support.GeocodableLocation;
//import st.alr.mqttitude.support.ReverseGeocodingTask;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.ComponentName;
//import android.content.ContentResolver;
//import android.content.ContentUris;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.content.pm.ApplicationInfo;
//import android.content.pm.PackageManager;
//import android.content.pm.PackageManager.NameNotFoundException;
//import android.database.ContentObserver;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.preference.PreferenceManager;
//import android.provider.ContactsContract;
//import android.provider.ContactsContract.CommonDataKinds.Event;
//import android.provider.Settings.Secure;
//import android.support.v4.app.NotificationCompat;
//import android.util.Log;
//
//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesUtil;
//
//import de.greenrobot.event.EventBus;
//
//public class ServiceApplication extends ServiceBindable {
//    private static SharedPreferences sharedPreferences;
//    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
//    private NotificationManager notificationManager;
//    private static NotificationCompat.Builder  notificationBuilder;
//    private static Class<?> locatorClass;
//    private GeocodableLocation lastPublishedLocation;
//    private Date lastPublishedLocationTime;
//    private static ServiceApplication instance;
//    private boolean even = false;
//    private Handler handler;
//    private int mContactCount;
//
//    private static ServiceLocator serviceLocator;
//    private static ServiceMqtt serviceMqtt;
//
//    
//    @Override
//    public void onCreate(){
//        super.onCreate();
//    }
//    
//    @Override
//    protected void onStartOnce() {
//        
//        Log.v(this.toString(), "ServiceApplication starting");
//        
//        instance = this;
//
//
//        handler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                onHandlerMessage(msg);
//            }
//        };
//
//        EventBus.getDefault().registerSticky(this);
//
//
//
//
//        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationBuilder = new NotificationCompat.Builder (this);
//
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//
//        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
//                if (key.equals(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED))
//                    handleNotification();
//            }
//        };
//        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
//        handleNotification();
//
//        ServiceConnection mqttConnection = new ServiceConnection() {
//            
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                serviceMqtt = null;                
//            }
//            
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                serviceMqtt = (ServiceMqtt) ((ServiceBindable.ServiceBinder)service).getService();                
//            }
//        };        
//        bindService(new Intent(this, ServiceMqtt.class), mqttConnection, Context.BIND_AUTO_CREATE);
//
//        
//        ServiceConnection locatorConnection = new ServiceConnection() {
//            
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//                serviceLocator = null;                
//            }
//            
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                serviceLocator = (ServiceLocator) ((ServiceBindable.ServiceBinder)service).getService();                
//            }
//        };
//        
//        bindService(new Intent(this, ServiceLocatorFused.class), locatorConnection, Context.BIND_AUTO_CREATE);
//
//        
//        mContactCount = getContactCount();
//        getApplicationContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, mObserver);
//
//        if(App.isDebugBuild())
//            updateTicker("MQTTitude service started");
//        
//    }
//    
//
//    private int getContactCount() {
//        Cursor cursor = null;
//        try {
//            cursor = getContentResolver().query(
//                    ContactsContract.Contacts.CONTENT_URI, null, null, null,
//                    null);
//            if (cursor != null) {
//                return cursor.getCount();
//            } else {
//                return 0;
//            }
//        } catch (Exception ignore) {
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//        return 0;
//    }
//
//
//    private ContentObserver mObserver = new ContentObserver(new Handler()) {
//
//
//        @Override
//        public void onChange(boolean selfChange) {
//            super.onChange(selfChange);
//
//            final int currentCount = getContactCount();
//            if (currentCount < mContactCount) {
//                Log.v(this.toString(), "Contact deleted");
//            } else if (currentCount == mContactCount) {
//                Log.v(this.toString(), "Contact updated");
//                for (Contact c : App.getContacts().values()) {
//                    if( updateContactData(c));
//                        EventBus.getDefault().post(new Events.ContactUpdated(c));
//                }
//            } else {
//                Log.v(this.toString(), "Contact added");
//                for (Contact c : App.getContacts().values()) {
//                    if( updateContactData(c));
//                        EventBus.getDefault().post(new Events.ContactUpdated(c));
//                }
//
//            }
//                        mContactCount = currentCount;
//        }
//
//    };
//
//
//       
//
//    /**
//     * @category NOTIFICATION HANDLING
//     */
//    private void handleNotification() {
//        Log.v(this.toString(), "handleNotification()");
//        stopForeground(true);
//        if (notificationEnabled())
//            createNotification();
//    }
//
//    private boolean notificationEnabled() {
//        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED,
//                Defaults.VALUE_NOTIFICATION_ENABLED);
//    }
//
//    private void createNotification() {
//        Log.v(this.toString(), "createNotification");
//
//        Intent resultIntent = new Intent(this, ActivityMain.class);
//        resultIntent.setAction("android.intent.action.MAIN");
//        resultIntent.addCategory("android.intent.category.LAUNCHER");
//
//        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//
//        notificationBuilder.setContentIntent(resultPendingIntent);
//
//        Intent intent = new Intent(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN);
//        intent.setClass(this, BackgroundPublishReceiver.class);
//
//        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
//
//        notificationBuilder.addAction(
//                R.drawable.ic_upload,
//                "Publish location",
//                pIntent);
//
//        updateNotification();
//    }
//
//    public void updateTicker(String text) {
//        notificationBuilder.setTicker(text + ((even = even ? false : true) ? " " : ""));
//        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
//        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
//
//        // if the notification is not enabled, the ticker will create an empty
//        // one that we get rid of
//        if (!notificationEnabled())
//            notificationManager.cancel(Defaults.NOTIFCATION_ID);
//    }
//
//    public void updateNotification() {
//        if (!notificationEnabled())
//            return;
//
//        String title = null;
//        String subtitle = null;
//        long time = 0;
//
//        if (lastPublishedLocation != null
//                && sharedPreferences.getBoolean("notificationLocation", true)) {
//            time = lastPublishedLocationTime.getTime();
//
//            if (lastPublishedLocation.getGeocoder() != null
//                    && sharedPreferences.getBoolean("notificationGeocoder", false)) {
//                title = lastPublishedLocation.toString();
//            } else {
//                title = lastPublishedLocation.toLatLonString();
//            }
//        } else {
//            title = getString(R.string.app_name);
//        }
//
//        subtitle = ServiceLocator.getStateAsString(this) + " | " + ServiceMqtt.getStateAsString(this);
//
//        notificationBuilder.setContentTitle(title);
//        notificationBuilder
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentText(subtitle)
//                .setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
//        if (time != 0)
//            notificationBuilder.setWhen(lastPublishedLocationTime.getTime());
//
//        startForeground(Defaults.NOTIFCATION_ID, notificationBuilder.build());
//       //notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());
//    }
//
//    public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
//        updateNotification();
//    }
//
//    public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
//        updateNotification();
//    }
//
//    private void onHandlerMessage(Message msg) {
//        switch (msg.what) {
//            case ReverseGeocodingTask.GEOCODER_RESULT:
//                geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
//                break;
//        }
//    }
//
//    private void geocoderAvailableForLocation(GeocodableLocation l) {
//        if (l == lastPublishedLocation)
//            updateNotification();
//    }
//
//    public void onEvent(Events.PublishSuccessfull e) {
//        Log.v(this.toString(), "Publish successful");
//        if (e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
//            GeocodableLocation l = (GeocodableLocation) e.getExtra();
//
//            this.lastPublishedLocation = l;
//            this.lastPublishedLocationTime = e.getDate();
//
//            if (sharedPreferences.getBoolean("notificationGeocoder", false)
//                    && l.getGeocoder() == null)
//                (new ReverseGeocodingTask(this, handler)).execute(new GeocodableLocation[] {
//                    l
//                });
//
//            updateNotification();
//
//            if (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
//                    Defaults.VALUE_TICKER_ON_PUBLISH))
//                updateTicker(getString(R.string.statePublished));
//
//        }
//    }
//    
//    public static ServiceApplication getInstance() {
//        return instance;
//    }
//        
//    public ServiceLocator getServiceLocator() {
//        return serviceLocator;        
//    }
//    
//    public ServiceMqtt getServiceMqtt() {
//        return serviceMqtt;
//    }
//
//    
//    
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        getContentResolver().unregisterContentObserver(mObserver);
//    }
//
//    public boolean updateContactData(Contact c){
//        Log.v(this.toString(), "Finding contact data for " + c.getTopic());
//        boolean ret = false;
//        String imWhere = ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + " = ? COLLATE NOCASE AND " + ContactsContract.CommonDataKinds.Im.DATA + " = ? COLLATE NOCASE";
//        String[] imWhereParams = new String[] {"Mqttitude", c.getTopic() };
//        Cursor imCur = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
//        
//        Log.v(this.toString(), "imcur:  " + imCur);
//        imCur.move(-1);
//        while (imCur.moveToNext()) {
//            Long cId = imCur.getLong(imCur.getColumnIndex(ContactsContract.Data.CONTACT_ID));                    
//            Log.v(this.toString(), "found matching contact with id "+ cId + " to be associated with topic " + imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)));
//            c.setUserImage(Contact.resolveImage(getContentResolver(), cId));
//            Log.v(this.toString(), "Display Name: " + imCur.getString(imCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
//            c.setName(imCur.getString(imCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));               
//            ret = true;
//        }
//        imCur.close();
//        Log.v(this.toString(), "search finished");
//        return ret;
//    }
//        
//    public static Map<String, Contact> getContacts() {
//        return App.getContacts();
//    }
//
//    public void onEventMainThread(Events.ContactLocationUpdated e) {
//        Log.v(this.toString(), "Contact location updated: " + e.getTopic() + " ->"
//                + e.getGeocodableLocation().toString() + " @ "
//                + new Date(e.getGeocodableLocation().getLocation().getTime() * 1000));
//
//        // Updates a contact or allocates a new one
//        Contact c = updateOrInitContact(e.getTopic(), e.getGeocodableLocation());
//        
//        // Fires a new event with the now updated or created contact to which fragments can react
//        EventBus.getDefault().post(new Events.ContactUpdated(c));       
//        
//    }
//
//    private Contact updateOrInitContact(String topic, GeocodableLocation location) {
//        Contact c = App.getContacts().get(topic);
//
//        if (c == null) {
//            Log.v(this.toString(), "Allocating new contact for " + topic);
//            c = new Contact(topic);
//            updateContactData(c);
//        }
//
//        c.setLocation(location);
//        
//        // Automatically fires onDatasetChanged of contacts adapter to update depending listViews
//        App.getContacts().put(topic, c);            
//        return c;
//    }
//    
//    public void enableForegroundMode() {
//            getServiceLocator().enableForegroundMode();
//    }
//
//    public void enableBackgroundMode() {
//            getServiceLocator().enableBackgroundMode();
//    }
//    
//    
//}

public class ServiceApplication implements ProxyableService {
    private static SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener;
    private NotificationManager notificationManager;
    private static NotificationCompat.Builder notificationBuilder;
    private GeocodableLocation lastPublishedLocation;
    private Date lastPublishedLocationTime;
    private boolean even = false;
    private Handler handler;
    private int mContactCount;
    private ServiceProxy context;

    @Override
    public void onCreate(ServiceProxy context) {
        this.context = context;
        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                if (key.equals(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED))
                    handleNotification();
            }
        };
        
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onHandlerMessage(msg);
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangedListener);
        handleNotification();

        mContactCount = getContactCount();
        context.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI,
                true, mObserver);

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return 0;
    }

    public boolean updateContactData(Contact c) {
        Log.v(this.toString(), "Finding contact data for " + c.getTopic());
        boolean ret = false;
        String imWhere = ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL
                + " = ? COLLATE NOCASE AND " + ContactsContract.CommonDataKinds.Im.DATA
                + " = ? COLLATE NOCASE";
        String[] imWhereParams = new String[] {
                "Mqttitude", c.getTopic()
        };
        Cursor imCur = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                imWhere, imWhereParams, null);

        Log.v(this.toString(), "imcur:  " + imCur);
        imCur.move(-1);
        while (imCur.moveToNext()) {
            Long cId = imCur.getLong(imCur.getColumnIndex(ContactsContract.Data.CONTACT_ID));
            Log.v(this.toString(),
                    "found matching contact with id "
                            + cId
                            + " to be associated with topic "
                            + imCur.getString(imCur
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)));
            c.setUserImage(Contact.resolveImage(context.getContentResolver(), cId));
            Log.v(this.toString(),
                    "Display Name: "
                            + imCur.getString(imCur
                                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
            c.setName(imCur.getString(imCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
            ret = true;
        }
        imCur.close();
        Log.v(this.toString(), "search finished");
        return ret;
    }

    private Contact updateOrInitContact(String topic, GeocodableLocation location) {
        Contact c = App.getContacts().get(topic);

        if (c == null) {
            Log.v(this.toString(), "Allocating new contact for " + topic);
            c = new st.alr.mqttitude.support.Contact(topic);
            updateContactData(c);
        }

        c.setLocation(location);

        // Automatically fires onDatasetChanged of contacts adapter to update
        // depending listViews
        App.getContacts().put(topic, c);
        return c;
    }

    public static Map<String, Contact> getContacts() {
        return App.getContacts();
    }

    public void onEvent(Events.ContactLocationUpdated e) {
        // Updates a contact or allocates a new one
        Contact c = updateOrInitContact(e.getTopic(), e.getGeocodableLocation());

        // Fires a new event with the now updated or created contact to which
        // fragments can react
        EventBus.getDefault().post(new Events.ContactUpdated(c));

    }

    private int getContactCount() {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
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
            if (currentCount < mContactCount) {
                Log.v(this.toString(), "Contact deleted");
            } else if (currentCount == mContactCount) {
                Log.v(this.toString(), "Contact updated");
                for (Contact c : App.getContacts().values()) {
                    if (updateContactData(c))
                        ;
                    EventBus.getDefault().post(new Events.ContactUpdated(c));
                }
            } else {
                Log.v(this.toString(), "Contact added");
                for (Contact c : App.getContacts().values()) {
                    if (updateContactData(c))
                        ;
                    EventBus.getDefault().post(new Events.ContactUpdated(c));
                }

            }
            mContactCount = currentCount;
        }

    };

    /**
     * @category NOTIFICATION HANDLING
     */
    private void handleNotification() {
        Log.v(this.toString(), "handleNotification()");
        ((ServiceProxy) context).stopForeground(true);
        if (notificationEnabled())
            createNotification();
    }

    private boolean notificationEnabled() {
        return sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_NOTIFICATION_ENABLED,
                Defaults.VALUE_NOTIFICATION_ENABLED);
    }

    private void createNotification() {
        Log.v(this.toString(), "createNotification");

        Intent resultIntent = new Intent(context, ActivityLauncher.class);
        resultIntent.setAction("android.intent.action.MAIN");
        resultIntent.addCategory("android.intent.category.LAUNCHER");

        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(resultPendingIntent);

//        Intent intent = new Intent(Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN);
//        intent.setClass(context, BackgroundPublishReceiver.class);

//        Intent i = new Intent(context, ServiceProxy.class);
//        i.setAction(ServiceProxy.SERVICE_LOCATOR);
//        i.putExtra("action", Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN);
        
//        locationIntent = PendingIntent.getService(context, 1, i, 0);                
//        mLocationClient.requestLocationUpdates(mLocationRequest, locationIntent);
//
//        
//        
        
//        Intent i = new Intent().setClass(context, ServiceProxy.class);
//
//        //Intent i = new Intent(context, ServiceProxy.class);
//        i.setAction(ServiceProxy.SERVICE_LOCATOR);
//        i.putExtra("action", Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN);
//        i.putExtra("foo", "baz");
//
       // PendingIntent pIntent = PendingIntent.getService(context, 99, i, 0);                
        PendingIntent pIntent = ServiceProxy.getPendingIntentForService(context, ServiceProxy.SERVICE_LOCATOR, Defaults.INTENT_ACTION_PUBLISH_LASTKNOWN, null);
//        
//        PendingIntent.getService(context, requestCode, intent, flags)
//        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, i, 0);

        notificationBuilder.addAction(
                R.drawable.ic_upload,
                context.getString(R.string.publish) ,
                pIntent);

        updateNotification();
    }

    public void updateTicker(String text) {
        notificationBuilder.setTicker(text + ((even = even ? false : true) ? " " : ""));
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        notificationManager.notify(Defaults.NOTIFCATION_ID, notificationBuilder.build());

        // if the notification is not enabled, the ticker will create an empty
        // one that we get rid of
        if (!notificationEnabled())
            notificationManager.cancel(Defaults.NOTIFCATION_ID);
    }

    public void updateNotification() {
        if (!notificationEnabled())
            return;

        String title = null;
        String subtitle = null;
        long time = 0;

        if (lastPublishedLocation != null
                && sharedPreferences.getBoolean("notificationLocation", true)) {
            time = lastPublishedLocationTime.getTime();

            if (lastPublishedLocation.getGeocoder() != null
                    && sharedPreferences.getBoolean("notificationGeocoder", false)) {
                title = lastPublishedLocation.toString();
            } else {
                title = lastPublishedLocation.toLatLonString();
            }
        } else {
            title = context.getString(R.string.app_name);
        }

        subtitle = ServiceLocator.getStateAsString(context) + " | "
                + ServiceBroker.getStateAsString(context);

        notificationBuilder.setContentTitle(title);
        notificationBuilder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentText(subtitle)
                .setPriority(android.support.v4.app.NotificationCompat.PRIORITY_MIN);
        if (time != 0)
            notificationBuilder.setWhen(lastPublishedLocationTime.getTime());

        context.startForeground(Defaults.NOTIFCATION_ID, notificationBuilder.build());
        // notificationManager.notify(Defaults.NOTIFCATION_ID,
        // notificationBuilder.build());
    }

    public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
        updateNotification();
    }

    public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
        updateNotification();
    }

    private void onHandlerMessage(Message msg) {
        switch (msg.what) {
            case ReverseGeocodingTask.GEOCODER_RESULT:
                geocoderAvailableForLocation(((GeocodableLocation) msg.obj));
                break;
        }
    }

    private void geocoderAvailableForLocation(GeocodableLocation l) {
        if (l == lastPublishedLocation)
            updateNotification();
    }

    public void onEvent(Events.PublishSuccessfull e) {
        Log.v(this.toString(), "Publish successful");
        if (e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
            GeocodableLocation l = (GeocodableLocation) e.getExtra();

            this.lastPublishedLocation = l;
            this.lastPublishedLocationTime = e.getDate();

            if (sharedPreferences.getBoolean("notificationGeocoder", false)
                    && l.getGeocoder() == null)
                (new ReverseGeocodingTask(context, handler)).execute(new GeocodableLocation[] {
                        l
                });

            updateNotification();

            if (sharedPreferences.getBoolean(Defaults.SETTINGS_KEY_TICKER_ON_PUBLISH,
                    Defaults.VALUE_TICKER_ON_PUBLISH))
                updateTicker(context.getString(R.string.statePublished));

        }
    }
}
