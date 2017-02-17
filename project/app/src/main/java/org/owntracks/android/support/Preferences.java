package org.owntracks.android.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.owntracks.android.App;
import org.owntracks.android.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.owntracks.android.db.Dao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageWaypoint;

import timber.log.Timber;


public class Preferences {
    public static final String FILENAME_PRIVATE = "org.owntracks.android.preferences.private";
    public static final String FILENAME_HTTP = "org.owntracks.android.preferences.http";
    public static final String FILENAME_PUBLIC = "org.owntracks.android.preferences.public";

    private static SharedPreferences activeSharedPreferences;
    private static SharedPreferences sharedPreferences;

    private static SharedPreferences privateSharedPreferences;
    private static SharedPreferences httpSharedPreferences;
    private static SharedPreferences publicSharedPreferences;

    private static int modeId = App.MODE_ID_MQTT_PRIVATE;
    private static String deviceUUID = "";

    public static boolean isModeMqttPrivate(){ return modeId == App.MODE_ID_MQTT_PRIVATE; }

    public static boolean isModeMqttPublic(){ return modeId == App.MODE_ID_MQTT_PUBLIC; }

    public static boolean isModeHttpPrivate(){ return modeId == App.MODE_ID_HTTP_PRIVATE; }

    public static String getDeviceUUID() {
        return deviceUUID;
    }

    public static void initialize(Context c){
        Timber.v("preferences initializing");
        activeSharedPreferencesChangeListener = new LinkedList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c); // only used for modeId and firstStart keys
        privateSharedPreferences = c.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE);
        httpSharedPreferences = c.getSharedPreferences(FILENAME_HTTP, Context.MODE_PRIVATE);
        publicSharedPreferences = c.getSharedPreferences(FILENAME_PUBLIC, Context.MODE_PRIVATE);

        deviceUUID = sharedPreferences.getString(Keys._DEVICE_UUID, "undefined-uuid");
        initMode(sharedPreferences.getInt(Keys.MODE_ID, getIntResource(R.integer.valModeId)));
    }


    public static void initMode(int active) {
        // Check for valid mode IDs and fallback to Private if an invalid mode is set
        if(active == App.MODE_ID_MQTT_PRIVATE || active == App.MODE_ID_MQTT_PUBLIC || active == App.MODE_ID_HTTP_PRIVATE) {
            setMode(active, true);
        } else {
            setMode(App.MODE_ID_MQTT_PRIVATE, true);
        }
    }

    public static void setMode(int active) {
        setMode(active, false);
    }
    private static void setMode(int active, boolean init){
        Timber.v("setMode: " + active);

        if(!init && modeId == active)
            return;

        Timber.v("setting mode to: " + active);

        detachAllActivePreferenceChangeListeners();
        int oldModeId = modeId;
        modeId = active;
        switch (modeId) {
            case App.MODE_ID_MQTT_PRIVATE:
                activeSharedPreferences = privateSharedPreferences;
                break;
            case App.MODE_ID_MQTT_PUBLIC:
                activeSharedPreferences = publicSharedPreferences;
                break;
            case App.MODE_ID_HTTP_PRIVATE:
                activeSharedPreferences = httpSharedPreferences;
                break;

        }
        sharedPreferences.edit().putInt(Keys.MODE_ID, modeId).apply();

        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).apply();
        httpSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).apply();
        publicSharedPreferences.edit().putInt(Keys.MODE_ID, modeId).apply();

        attachAllActivePreferenceChangeListeners();

        if(!init) {
            Timber.v("broadcasting mode change event");
            App.getEventBus().post(new Events.ModeChanged(oldModeId,modeId));
        }
    }

    private static LinkedList<OnPreferenceChangedListener> activeSharedPreferencesChangeListener;

    public static void registerOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
        activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        activeSharedPreferencesChangeListener.push(listener);
    }

    public static void unregisterOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
        activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        activeSharedPreferencesChangeListener.remove(listener);
    }

    private static void detachAllActivePreferenceChangeListeners() {
        for(SharedPreferences.OnSharedPreferenceChangeListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private static void attachAllActivePreferenceChangeListeners() {
        for(OnPreferenceChangedListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
            listener.onAttachAfterModeChanged();
        }
    }


    public interface OnPreferenceChangedListener extends SharedPreferences.OnSharedPreferenceChangeListener {
        void onAttachAfterModeChanged();
    }









    //public static String getContactKey(int resId) {
    //    return App.getContext().getString(resId);
    //}

    public static boolean getBoolean(String key,  int defId) {
        return getBoolean(key, defId, defId, false);
    }

    public static boolean getBoolean(String key, int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getBooleanRessource(defIdPublic) :  activeSharedPreferences.getBoolean(key, getBooleanRessource(defIdPublic));
        }

        return activeSharedPreferences.getBoolean(key, getBooleanRessource(defIdPrivate));
    }

    public static boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

    // For keys that do not need overrides in any modes
    public static int getInt(String key,  int defId) {
        return getInt(key, defId, defId, false);
    }
    public static int getInt(String key,  int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getIntResource(defIdPublic) :  activeSharedPreferences.getInt(key, getIntResource(defIdPublic));
        }

        return activeSharedPreferences.getInt(key, getIntResource(defIdPrivate));
    }
    public static int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }


    public static int getIntegerDefaultValue(int defaultValueResPrivate, int defaultValueResPublic) {
        return isModeMqttPrivate() ? getIntResource(defaultValueResPrivate) : getIntResource(defaultValueResPublic);
    }



    // Gets the key from specified preferences
    // If the returned value is an empty string or null, the default id is returned
    // This is a quick fix as an empty string does not return the default value
    private static String getStringWithFallback(SharedPreferences preferences, String key, int defId) {
        String s = preferences.getString(key, "");
        return ("".equals(s)) ? getStringRessource(defId) : s;
    }

    public static String getString(String key,  int defId) {
        return getString(key, defId, defId, defId, false, false);
    }
    public static String getString(String key,  int defIdPrivate, int defIdPublic, int defIdHttp, boolean forceDefIdPublic, boolean forceDefIdHttp) {
        if (isModeHttpPrivate()) {
            return forceDefIdHttp ? getStringRessource(defIdHttp) : getStringWithFallback(httpSharedPreferences, key, defIdHttp);
        }

        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getStringRessource(defIdPublic) : getStringWithFallback(publicSharedPreferences, key, defIdPublic);
        }

        return getStringWithFallback(privateSharedPreferences, key, defIdPrivate);
    }

    public static String getStringDefaultValue(@StringRes  int defIdPrivate, @StringRes int defIdPublic) {
        return isModeMqttPrivate() ? getStringRessource(defIdPrivate) : getStringRessource(defIdPublic);
    }

    public static String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }

    public static void setString(String key, String value) {
        setString(key, value, true);
    }
    public static void setString(String key, String value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putString(key, value).apply();
    }

    public static void setInt(String key, int value) {
        setInt(key, value, true);
    }
    public static void setInt(String key, int value, boolean allowSetWhenPublic) {
        if((isModeMqttPublic() && !allowSetWhenPublic)) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putInt(key, value).apply();
    }
    public static void setBoolean(String key, boolean value) {
        setBoolean(key, value, true);
    }
    public static void setBoolean(String key, boolean value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static void clearKey(String key) {
        activeSharedPreferences.edit().remove(key).apply();
    }

    @Export(key =Keys.MODE_ID, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public static int getModeId() { return modeId; }


    public SharedPreferences getActiveSharedPreferences() {
        return activeSharedPreferences;
    }

    public static String getAndroidId() {
        return App.getAndroidId();
    }

    public static boolean canConnect() {
        if(isModeMqttPrivate()) {
            return !getHost().trim().equals("") && !getUsername().trim().equals("")  && (!getAuth() || !getPassword().trim().equals(""));
        } else if(isModeMqttPublic()) {
            return true;
        }
        return false;
    }







    @SuppressLint("CommitPrefEdits")
    public static void importFromMessage(MessageConfiguration m) {

        HashMap<String, Method> methods = getImportMethods();

        if(m.containsKey(Keys.MODE_ID)) {
            setMode((Integer) m.get(Keys.MODE_ID));
            m.removeKey(Keys.MODE_ID);
        }

        // Don't show setup if a config has been imported
        setSetupCompleted();


        for(String key : m.getKeys()) {
            try {
                Object value = m.get(key);

                Timber.v("import for key %s:%s", key, value);
                if(value==null) {
                    Timber.v("clearing value for key %s", key);
                    clearKey(key);
                } else
                    Timber.v("method: %s", methods.get(key).getName());
                   methods.get(key).invoke(null, m.get(key));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        activeSharedPreferences.edit().commit();
        if(m.hasWaypoints()) {
            importWaypointsFromJson(m.getWaypoints());
        }


    }

    public static MessageWaypointCollection waypointsToJSON() {

        MessageWaypointCollection messages = new MessageWaypointCollection();
        for(Waypoint waypoint : Dao.getWaypointDao().loadAll()) {
            messages.add(MessageWaypoint.fromDaoObject(waypoint));
        }
        return messages;
    }


    public static void importWaypointsFromJson(@Nullable List<MessageWaypoint> j) {
        if(j == null)
            return;

        WaypointDao dao = Dao.getWaypointDao();
        List<Waypoint> deviceWaypoints =  dao.loadAll();

        for (MessageWaypoint m: j) {
            Waypoint w = m.toDaoObject();

            for(Waypoint e : deviceWaypoints) {
                // remove exisiting waypoint before importing new one
                if(TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()) == TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime())) {
                    Timber.v("removing existing waypoint with same tst before adding it");
                    dao.delete(e);
                    App.getEventBus().post(new Events.WaypointRemoved(e));
                }
            }

            dao.insert(w);
            App.getEventBus().post(new Events.WaypointAdded(w));
        }
    }

    @Export(key =Keys.REMOTE_CONFIGURATION, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public static boolean getRemoteConfiguration() {
        return getBoolean(Keys.REMOTE_CONFIGURATION, R.bool.valRemoteConfiguration, R.bool.valRemoteConfigurationPublic, true);
    }

    @Export(key =Keys.REMOTE_COMMAND, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public static boolean getRemoteCommand() {
        return getBoolean(Keys.REMOTE_COMMAND, R.bool.valRemoteCommand);
    }

    @Import(key =Keys.REMOTE_CONFIGURATION)
    public static void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(Keys.REMOTE_CONFIGURATION, aBoolean, false);
    }

    @Import(key =Keys.REMOTE_COMMAND)
    public static void setRemoteCommand(boolean aBoolean) {
        setBoolean(Keys.REMOTE_COMMAND, aBoolean, true);
    }

    @Import(key =Keys.CLEAN_SESSION)
    public static void setCleanSession(boolean aBoolean) {
        setBoolean(Keys.CLEAN_SESSION, aBoolean, false);
    }

    @Export(key =Keys.CLEAN_SESSION, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public static boolean getCleanSession() {
        return getBoolean(Keys.CLEAN_SESSION, R.bool.valCleanSession,R.bool.valCleanSessionPublic, true);
    }


    @Export(key =Keys.PUB_EXTENDED_DATA, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public static boolean getPubLocationExtendedData() {
        return getBoolean(Keys.PUB_EXTENDED_DATA, R.bool.valPubExtendedData, R.bool.valPubExtendedData, false);
    }

    @Export(key =Keys.LOCATOR_DISPLACEMENT, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public static int getLocatorDisplacement() {
        return getInt(Keys.LOCATOR_DISPLACEMENT, R.integer.valLocatorDisplacement);
    }

    public static long getLocatorIntervalMillis() {
        return TimeUnit.SECONDS.toMillis(getLocatorInterval());
    }

    @Export(key =Keys.LOCATOR_INTERVAL, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public static int getLocatorInterval() {
        return getInt(Keys.LOCATOR_INTERVAL, R.integer.valLocatorInterval);
    }

    @Export(key =Keys.USERNAME, exportModeMqttPrivate =true)
    public static String getUsername() {
        // in public, the username is just used to build the topic public/user/$deviceId
        return getString(Keys.USERNAME, R.string.valEmpty, R.string.valUsernamePublic, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.AUTH, exportModeMqttPrivate =true)
    public static boolean getAuth() {
        return getBoolean(Keys.AUTH, R.bool.valAuth, R.bool.valAuthPublic, true);

    }

    @Export(key =Keys.DEVICE_ID, exportModeMqttPrivate =true)
    public static String getDeviceId() {
        return getDeviceId(true);
    }

    public static String getDeviceId(boolean fallbackToDefault) {
        if(Preferences.isModeMqttPublic())
            return getDeviceUUID();

        String deviceId = getString(Keys.DEVICE_ID, R.string.valEmpty);
        if ("".equals(deviceId) && fallbackToDefault)
            return getDeviceIdDefault();
        return deviceId;
    }

    @Export(key =Keys.IGNORE_STALE_LOCATIONS, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public static int getIgnoreStaleLocations() {
        return getInt(Keys.IGNORE_STALE_LOCATIONS, R.integer.valIgnoreStaleLocations);
    }

    @Import(key =Keys.IGNORE_STALE_LOCATIONS )
    public static void setIgnoreSTaleLocations(int days) {
        setInt(Keys.IGNORE_STALE_LOCATIONS, days, false);
    }

    @Export(key =Keys.IGNORE_INACCURATE_LOCATIONS, exportModeMqttPrivate =true, exportModeHttpPrivate =true, exportModeMqttPublic = true)
    public static int getIgnoreInaccurateLocations() {
        return getInt(Keys.IGNORE_INACCURATE_LOCATIONS, R.integer.valIgnoreInaccurateLocations);
    }

    @Import(key =Keys.IGNORE_INACCURATE_LOCATIONS )
    public static void setIgnoreInaccurateLocations(int meters) {
        setInt(Keys.IGNORE_INACCURATE_LOCATIONS, meters, true);
    }




    // Not used on public, as many people might use the same device type
    public static String getDeviceIdDefault() {
        // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
        return android.os.Build.DEVICE.replace(" ", "-").replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
    }

    @Export(key =Keys.CLIENT_ID, exportModeMqttPrivate =true)
    public static String getClientId() {
        return getClientId(false);
    }
    public static String getClientId(boolean fallbackToDefault) {
        if(isModeMqttPublic())
            return MqttAsyncClient.generateClientId();

        String clientId = getString(Keys.CLIENT_ID, R.string.valEmpty);
        if ("".equals(clientId) && fallbackToDefault)
            clientId = getClientIdDefault();
        return clientId;
    }

    public static String getClientIdDefault() {
        String clientID=getUsername()+"/"+getDeviceId(true);
        return clientID.replaceAll("[^a-zA-Z0-9/]+", "").toLowerCase();
    }

    @Import(key =Keys.CLIENT_ID)
    public static void setClientId(String clientId) {
        setString(Keys.CLIENT_ID, clientId);
    }

    @Import(key =Keys.PUB_TOPIC_BASE)
    public static void setDeviceTopicBase(String deviceTopic) {
        setString(Keys.PUB_TOPIC_BASE, deviceTopic, false);
    }

    public static String getPubTopicLocations() {
        return getPubTopicBase(true);
    }

    public static String getPubTopicWaypoints() {
        return getPubTopicBase(true) +getPubTopicWaypointsPart();
    }

    public static String getPubTopicWaypointsPart() {
        return "/waypoint";
    }

    public static String getPubTopicEvents() {
        return getPubTopicBase(true) + getPubTopicEventsPart();
    }

    public static String getPubTopicEventsPart() {
        return "/event";
    }
    public static String getPubTopicInfoPart() {
        return "/info";
    }
    public static String getPubTopicCommands() {
        return getPubTopicBase(true) +getPubTopicCommandsPart();
    }
    public static String getPubTopicCommandsPart() {
        return "/cmd";
    }


    public static String getPubTopicBaseDefault() {
        String formatString;
        String username;
        String deviceId = getDeviceId(true); // will use App.SESSION_UUID on public



        if(isModeMqttPublic()) {
            username = "user";
            formatString = getStringRessource(R.string.valPubTopicPublic);
        } else {
            username = getUsername();
            formatString = getStringRessource(R.string.valPubTopic);
        }

        return String.format(formatString, username, deviceId);
    }

    @Export(key =Keys.PUB_TOPIC_BASE, exportModeMqttPrivate =true)
    public static String getPubTopicBase() {
        return getPubTopicBase(false);
    }
    public static String getPubTopicBase(boolean fallbackToDefault) {
        if(!isModeMqttPrivate()) {
            return getPubTopicBaseDefault();
        }
        String topic = getString(Keys.PUB_TOPIC_BASE, R.string.valEmpty);
        if (topic.equals("") && fallbackToDefault)
            topic = getPubTopicBaseDefault();

        return topic;
    }

    @Export(key =Keys.SUB_TOPIC, exportModeMqttPrivate =true)
    public static String getSubTopic() {
        return getString(Keys.SUB_TOPIC, R.string.valSubTopic, R.string.valSubTopicPublic, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.SUB, exportModeMqttPrivate =true)
    public static boolean getSub() {
        return getBoolean(Keys.SUB, R.bool.valSub, R.bool.valSubPublic, true);
    }

    @Import(key =Keys.SUB)
    private static void setSub(boolean sub) {
        setBoolean(Keys.SUB, sub, false);
    }


    @Export(key =Keys.TRACKER_ID, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static String getTrackerId() {
        return getTrackerId(false);

    }
    public static String getTrackerId(boolean fallback) {

        String tid = getString(Keys.TRACKER_ID, R.string.valEmpty);

        if(tid==null || tid.isEmpty())
            return fallback ? getTrackerIdDefault() : "";
        else
            return tid;
    }

    public static String getTrackerIdDefault(){
        String deviceId = getDeviceId(true);
        if(deviceId!=null && deviceId.length() >= 2)
            return deviceId.substring(deviceId.length() - 2);   // defaults to the last two characters of configured deviceId.
        else
            return "na";  // Empty trackerId won't be included in the message.
    }

    @Import(key =Keys.HOST)
    public static void setHost(String value) {
            setString(Keys.HOST, value, false);
    }

    public static void setPortDefault(int value) {
        clearKey(Keys.PORT);
    }

    @Import(key =Keys.PORT)
    public static void setPort(int value) {
            setInt(Keys.PORT, value, false);
   }

    @Import(key =Keys.TRACKER_ID)
    public static void setTrackerId(String value){
        int len=value.length();
        // value validation - must be max 2 characters, only letters and digits
        if(len>=2){
            value=value.substring(0,2);
            if( Character.isLetterOrDigit(value.charAt(0)) && Character.isLetterOrDigit(value.charAt(1)) )
                setString(Keys.TRACKER_ID, value);
        }
        else {
            if( len >0 && Character.isLetterOrDigit(value.charAt(0)))
                setString(Keys.TRACKER_ID, value);
            else
                setString(Keys.TRACKER_ID,"");
        }

    }

    @Export(key =Keys.PORT, exportModeMqttPrivate =true)
    public static int getPort() {
        return getInt(Keys.PORT, R.integer.valPort, R.integer.valPortPublic, true);
    }


    public static String getIntWithHintSupport(String key) {
        int i = getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public static String getPortWithHintSupport() {
        return getIntWithHintSupport(Keys.PORT);
    }


    @Import(key =Keys.KEEPALIVE)
    public static void setKeepalive(int value) {
        setInt(Keys.KEEPALIVE, value, false);
    }


    public static String getKeepaliveWithHintSupport() {
        return getIntWithHintSupport(Keys.KEEPALIVE);
    }

    @Export(key =Keys.KEEPALIVE, exportModeMqttPrivate =true)
    public static int getKeepalive() {
        return getInt(Keys.KEEPALIVE, R.integer.valKeepalive, R.integer.valKeepalivePublic, true);
    }

    @Import(key =Keys.USERNAME)
    public static void setUsername(String value) {

            setString(Keys.USERNAME, value);
    }

    @Import(key =Keys.PUB_EXTENDED_DATA)
    private static void setPubLocationExtendedData(boolean aBoolean) {
        setBoolean(Keys.PUB_EXTENDED_DATA, aBoolean);
    }

    @Import(key =Keys.PUB)
    private static void setPub(boolean aBoolean) {
        setBoolean(Keys.PUB, aBoolean);
    }

    @Import(key =Keys.NOTIFICATION)
    private static void setNotification(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION, aBoolean);
    }

    @Import(key =Keys.NOTIFICATION_HIGHER_PRIORITY)
    private static void setNotificationHigherPriority(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION_HIGHER_PRIORITY, aBoolean);
    }

    @Import(key =Keys.NOTIFICATION_LOCATION)
    private static void setNotificationLocation(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION_LOCATION, aBoolean);
    }
    @Import(key =Keys.NOTIFICATION_EVENTS)
    public static void setNotificationEvents(boolean notificationEvents) {
        setBoolean(Keys.NOTIFICATION_EVENTS, notificationEvents);
    }

    @Import(key =Keys.SUB_TOPIC)
    private static void setSubTopic(String string) {
        setString(Keys.SUB_TOPIC, string, false);
    }

    @Import(key =Keys.AUTOSTART_ON_BOOT)
    private static void setAutostartOnBoot(boolean aBoolean) {
        setBoolean(Keys.AUTOSTART_ON_BOOT, aBoolean);

    }
    @Import(key =Keys.LOCATOR_ACCURACY_FOREGROUND)
    private static void setLocatorAccuracyForeground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_FOREGROUND, anInt);

    }

    @Import(key =Keys.LOCATOR_ACCURACY_BACKGROUND)
    private static void setLocatorAccuracyBackground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_BACKGROUND, anInt);

    }
    @Import(key =Keys.LOCATOR_INTERVAL)
    private static void setLocatorInterval(int anInt) {
        setInt(Keys.LOCATOR_INTERVAL, anInt);

    }

    @Import(key =Keys.BEACON_BACKGROUND_SCAN_PERIOD)
    private static void setBeaconBackgroundScanPeriod(int anInt) {
        setInt(Keys.BEACON_BACKGROUND_SCAN_PERIOD, anInt);

    }

    @Import(key =Keys.BEACON_FOREGROUND_SCAN_PERIOD)
    private static void setBeaconForegroundScanPeriod(int anInt) {
        setInt(Keys.BEACON_FOREGROUND_SCAN_PERIOD, anInt);

    }

    @Import(key =Keys.LOCATOR_DISPLACEMENT)
    private static void setLocatorDisplacement(int anInt) {
        setInt(Keys.LOCATOR_DISPLACEMENT, anInt);

    }
    @Import(key =Keys.PUB_RETAIN)
    private static void setPubRetain(boolean aBoolean) {
        setBoolean(Keys.PUB_RETAIN, aBoolean, false);

    }
    @Import(key =Keys.PUB_QOS)
    private static void setPubQos(int anInt) {
        setInt(Keys.PUB_QOS, anInt, false);
    }


    @Import(key =Keys.SUB_QOS)
    private static void setSubQos(int anInt) {
        setInt(Keys.SUB_QOS, anInt > 2 ? 2 : anInt, false);
    }


    @Import(key =Keys.PASSWORD)
    public static void setPassword(String password) {
            setString(Keys.PASSWORD, password);
    }

    @Import(key =Keys.DEVICE_ID)
    public static void setDeviceId(String deviceId) {
        setString(Keys.DEVICE_ID, deviceId, false);
    }


    @Import(key =Keys.AUTH)
    public static void setAuth(boolean auth) {
        setBoolean(Keys.AUTH, auth, false);
    }

    @Import(key =Keys.TLS)
    public static void setTls(boolean tlsSpecifier) {
        setBoolean(Keys.TLS, tlsSpecifier, false);
    }

    @Import(key =Keys.WS)
    public static void setWs(boolean wsEnable) {
        setBoolean(Keys.WS, wsEnable, false);
    }

    public static void setTlsCaCrt(String name) {
        setString(Keys.TLS_CA_CRT, name, false);
    }
    public static void setTlsClientCrt(String name) {
        setString(Keys.TLS_CLIENT_CRT, name, false);
    }
    @Export(key =Keys.HOST, exportModeMqttPrivate =true)
    public static String getHost() {
        return getString(Keys.HOST, R.string.valEmpty, R.string.valHostPublic, R.string.valEmpty, true, false);
    }
    @Export(key =Keys.PASSWORD, exportModeMqttPrivate =true)
    public static String getPassword() {
        return getString(Keys.PASSWORD, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.TLS, exportModeMqttPrivate =true)
    public static boolean getTls() {
        return getBoolean(Keys.TLS, R.bool.valTls, R.bool.valTlsPublic, true);
    }
    @Export(key =Keys.WS, exportModeMqttPrivate =true)
    public static boolean getWs() {
        return getBoolean(Keys.WS, R.bool.valWs, R.bool.valWsPublic, true);
    }

    @Export(key =Keys.TLS_CA_CRT, exportModeMqttPrivate =true)
    public static String getTlsCaCrtName() {
        return getString(Keys.TLS_CA_CRT, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty,true, false);
    }

    @Export(key =Keys.TLS_CLIENT_CRT, exportModeMqttPrivate =true)
    public static String getTlsClientCrtName() {
        return getString(Keys.TLS_CLIENT_CRT, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty,true, false);
    }

    @Export(key =Keys.NOTIFICATION, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static boolean getNotification() {
        return getBoolean(Keys.NOTIFICATION, R.bool.valNotification);
    }

    @Export(key =Keys.NOTIFICATION_HIGHER_PRIORITY, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static boolean getNotificationHigherPriority() {
        return getBoolean(Keys.NOTIFICATION_HIGHER_PRIORITY, R.bool.valNotificationHigherPriority);
    }



    @Export(key =Keys.NOTIFICATION_LOCATION, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getNotificationLocation() {
        return getBoolean(Keys.NOTIFICATION_LOCATION, R.bool.valNotificationLocation);
    }

    public static boolean getNotificationEvents() {
        return getBoolean(Keys.NOTIFICATION_EVENTS, R.bool.valNotificationEvents);
    }

    @Export(key =Keys.PUB_QOS, exportModeMqttPrivate =true)
    public static int getPubQos() {
        return getInt(Keys.PUB_QOS, R.integer.valPubQos, R.integer.valPubQosPublic, true);
    }

    @Export(key =Keys.SUB_QOS, exportModeMqttPrivate =true)
    public static int getSubQos() {
        return getInt(Keys.SUB_QOS, R.integer.valSubQos, R.integer.valSubQosPublic, true);
    }

    @Export(key =Keys.PUB_RETAIN, exportModeMqttPrivate =true)
    public static boolean getPubRetain() {
        return getBoolean(Keys.PUB_RETAIN, R.bool.valPubRetain, R.bool.valPubRetainPublic, true);
    }

    @Export(key =Keys.PUB, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public static boolean getPub() {
        return getBoolean(Keys.PUB, R.bool.valPub);
    }


    @Export(key =Keys.AUTOSTART_ON_BOOT, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static boolean getAutostartOnBoot() {
        return getBoolean(Keys.AUTOSTART_ON_BOOT, R.bool.valAutostartOnBoot);
    }

    @Export(key =Keys.LOCATOR_ACCURACY_FOREGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static int getLocatorAccuracyForeground() {
        return getInt(Keys.LOCATOR_ACCURACY_FOREGROUND, R.integer.valLocatorAccuracyForeground);
    }

    @Export(key =Keys.BEACON_BACKGROUND_SCAN_PERIOD, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true )
    public static int getBeaconBackgroundScanPeriod() {
        return getInt(Keys.BEACON_BACKGROUND_SCAN_PERIOD, R.integer.valBeaconBackgroundScanPeriod);
    }

    @Export(key =Keys.BEACON_FOREGROUND_SCAN_PERIOD, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static int getBeaconForegroundScanPeriod() {
        return getInt(Keys.BEACON_FOREGROUND_SCAN_PERIOD, R.integer.valBeaconForegroundScanPeriod);
    }

    @Export(key =Keys.LOCATOR_ACCURACY_BACKGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static int getLocatorAccuracyBackground() {
        return getInt(Keys.LOCATOR_ACCURACY_BACKGROUND, R.integer.valLocatorAccuracyBackground);
    }

    @Export(key =Keys.BEACON_LAYOUT, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static String getCustomBeaconLayout() {
        return getString(Keys.BEACON_LAYOUT, R.string.valEmpty);
    }

    @Export(key =Keys.BEACON_MODE, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static int getBeaconMode() {
        return getInt(Keys.BEACON_MODE, R.integer.valBeaconMode);
    }

    @Import(key =Keys.BEACON_MODE)
    public static void setBeaconMode(int beaconMode) {
        if(beaconMode >= 0 && beaconMode <= 2)
            setInt(Keys.BEACON_MODE, beaconMode);
    }


    @Export(key =Keys.BEACON_RANGING, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static boolean getBeaconRangingEnabled() {
        return getBoolean(Keys.BEACON_RANGING, R.bool.valBeaconRangingEnabled);
    }

    @Import(key =Keys.BEACON_RANGING)
    public static void setBeaconRangingEnabled(boolean val) {
        setBoolean(Keys.BEACON_RANGING, val);
    }


    // Enable cards
    public static boolean getInfo() {
        return getBoolean(Keys.INFO, R.bool.valInfo, R.bool.valInfoPublic, false);
    }

    @Import(key =Keys.INFO)
    public static void setInfo(boolean info) {
        setBoolean(Keys.INFO, info);
    }

    public static String getTlsClientCrtPassword() {
        return getString(Keys.TLS_CLIENT_CRT_PASSWORD, R.string.valEmpty);
    }




    @Export(key =Keys.HTTP_SCHEDULER_DIRECT, exportModeMqttPrivate =false, exportModeMqttPublic = false, exportModeHttpPrivate =true)
    public static boolean getHttpSchedulerAllowDirectStrategy() {
        return getBoolean(Keys.HTTP_SCHEDULER_DIRECT, R.bool.valTrue);
    }

    @Import(key =Keys.HTTP_SCHEDULER_DIRECT)
    public static void setHttpSchedulerAllowDirectStrategy(boolean aBoolean) {
        setBoolean(Keys.HTTP_SCHEDULER_DIRECT, aBoolean, false);
    }




    @Import(key =Keys.URL)
    public static void setUrl(String url) {
        setString(Keys.URL, url);
    }

    @Export(key = Keys.URL, exportModeHttpPrivate = true)
    public static String getUrl() {
        return getString(Keys.URL, R.string.valEmpty);
    }
    public static void setTlsClientCrtPassword(String password) {
        setString(Keys.TLS_CLIENT_CRT_PASSWORD, password);
    }

    public static String getEncryptionKey() {
        return getString(Keys._ENCRYPTION_KEY, R.string.valEmpty);
    }



    public static boolean getSetupCompleted() {
        // sharedPreferences because the value is independent from the selected mode
        return !sharedPreferences.getBoolean(Keys._SETUP_NOT_COMPLETED, false);
    }

    public static void setSetupCompleted() {
        sharedPreferences.edit().putBoolean(Keys._SETUP_NOT_COMPLETED , false).apply();

    }

    @Export(key = Keys.PLAY_OVERRIDE, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public static boolean getPlayOverride() {
        return activeSharedPreferences.getBoolean(Keys.PLAY_OVERRIDE, false);
    }

    @Import(key = Keys.PLAY_OVERRIDE)
    public static void setPlayOverride(boolean playOverride) {
        activeSharedPreferences.edit().putBoolean(Keys.PLAY_OVERRIDE, playOverride).apply();
    }


    // Maybe make this configurable
    // For now it makes things easier to change
    public static int getPubQosEvents() {
        return getPubQos();
    }

    public static boolean getPubRetainEvents() {
        return false;
    }

    public static int getPubQosCommands() {
        return getPubQos();
    }

    public static boolean getPubRetainCommands() {
        return false;
    }

    public static int getPubQosWaypoints() {
        return 0;
    }

    public static boolean getPubRetainWaypoints() {
        return false;
    }

    public static int getPubQosLocations() {
        return getPubQos();
    }

    public static boolean getPubRetainLocations() {
        return getPubRetain();
    }


    public static MessageConfiguration exportToMessage() {
        List<Method> methods = getExportMethods();
        MessageConfiguration cfg = new MessageConfiguration();
        for(Method m : methods) {
            m.setAccessible(true);

            Timber.v("method for config key: %s, name:%s, type:%s", m.getAnnotation(Export.class).key(), m.getName(), m.getReturnType());

            try {
                //If the underlying method is static, then the specified obj argument is ignored. It may be null.
                cfg.set(m.getAnnotation(Export.class).key(), m.invoke(null));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        cfg.setWaypoints(waypointsToJSON());

        return cfg;
    }

    public static class Keys {
        public static final String AUTH                             = "auth";
        public static final String AUTOSTART_ON_BOOT                = "autostartOnBoot";
        public static final String BEACON_BACKGROUND_SCAN_PERIOD    = "beaconBackgroundScanPeriod";
        public static final String BEACON_FOREGROUND_SCAN_PERIOD    = "beaconForegroundScanPeriod";
        public static final String BEACON_LAYOUT                    = "beaconLayout";
        public static final String BEACON_RANGING                   = "ranging";
        public static final String BEACON_MODE                      = "beaconMode";
        public static final String CLEAN_SESSION                    = "cleanSession";
        public static final String CLIENT_ID                        = "clientId";
        public static final String DEVICE_ID                        = "deviceId";
        public static final String HOST                             = "host";
        public static final String HTTP_SCHEDULER_DIRECT            = "httpSchedulerConsiderStrategyDirect";
        public static final String IGNORE_INACCURATE_LOCATIONS      = "ignoreInaccurateLocations";
        public static final String IGNORE_STALE_LOCATIONS           = "ignoreStaleLocations";
        public static final String INFO                             = "info";
        public static final String KEEPALIVE                        = "keepalive";
        public static final String LOCATOR_ACCURACY_BACKGROUND      = "locatorAccuracyBackground";
        public static final String LOCATOR_ACCURACY_FOREGROUND      = "locatorAccuracyForeground";
        public static final String LOCATOR_DISPLACEMENT             = "locatorDisplacement";
        public static final String LOCATOR_INTERVAL                 = "locatorInterval";
        public static final String MODE_ID                          = "mode";
        public static final String NOTIFICATION                     = "notification";
        public static final String NOTIFICATION_EVENTS              = "notificationEvents";
        public static final String NOTIFICATION_HIGHER_PRIORITY     = "notificationHigherPriority";
        public static final String NOTIFICATION_LOCATION            = "notificationLocation";
        public static final String PASSWORD                         = "password";
        public static final String PLAY_OVERRIDE                    = "playOverride";
        public static final String PORT                             = "port";
        public static final String PUB                              = "pub";
        public static final String PUB_EXTENDED_DATA                = "pubExtendedData";
        public static final String PUB_QOS                          = "pubQos";
        public static final String PUB_RETAIN                       = "pubRetain";
        public static final String PUB_TOPIC_BASE                   = "pubTopicBase";
        public static final String REMOTE_COMMAND                   = "cmd";
        public static final String REMOTE_CONFIGURATION             = "remoteConfiguration";
        public static final String SUB                              = "sub";
        public static final String SUB_TOPIC                        = "subTopic";
        public static final String SUB_QOS                          = "subQos";
        public static final String TLS                              = "tls";
        public static final String TLS_CA_CRT                       = "tlsCaCrt";
        public static final String TLS_CLIENT_CRT                   = "tlsClientCrt";
        public static final String TLS_CLIENT_CRT_PASSWORD          = "tlsClientCrtPassword";
        public static final String TRACKER_ID                       = "tid";
        public static final String USERNAME                         = "username";
        public static final String WS                               = "ws";
        public static final String URL                              = "url";

        // Internal keys
        public static final String _DEVICE_UUID                     = "deviceUUID";
        public static final String _ENCRYPTION_KEY                  = "encryptionKey";
        public static final String _FIRST_START                     = "firstStart";
        public static final String _SETUP_NOT_COMPLETED             = "setupNotCompleted";


    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Export {
        String key();
        boolean exportModeMqttPrivate() default false;
        boolean exportModeMqttPublic() default false;
        boolean exportModeHttpPrivate() default false;
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Import {
        String key();
    }

    public static List<Method> getExportMethods() {
        final List<Method> methods = new ArrayList<>();
        Class<?> klass  = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Export.class) ) {
                    Export annotInstance = method.getAnnotation(Export.class);
                    if(getModeId() == App.MODE_ID_MQTT_PRIVATE && annotInstance.exportModeMqttPrivate() || getModeId() == App.MODE_ID_MQTT_PUBLIC && annotInstance.exportModeMqttPublic() ||getModeId() == App.MODE_ID_HTTP_PRIVATE && annotInstance.exportModeHttpPrivate()) {
                        methods.add(method);
                    }
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    public static HashMap<String, Method> getImportMethods() {
        final HashMap<String, Method> methods = new HashMap<>();
        Class<?> klass  = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Import.class) ) {
                    methods.put(method.getAnnotation(Import.class).key(), method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    @Nullable
    public static String getStringOrNull(@NonNull String key) {
        String st = Preferences.getString(key, R.string.valEmpty);
        return (st != null && !st.isEmpty()) ? st : null;
    }


}
