package org.owntracks.android.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.owntracks.android.App;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.messages.MessageWaypoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private String sharedPreferencesName;

    public boolean isModeMqttPrivate(){ return modeId == App.MODE_ID_MQTT_PRIVATE; }

    public boolean isModeMqttPublic(){ return modeId == App.MODE_ID_MQTT_PUBLIC; }

    public boolean isModeHttpPrivate(){ return modeId == App.MODE_ID_HTTP_PRIVATE; }

    public String getSharedPreferencesName() { return sharedPreferencesName; }

    private static String getDeviceUUID() {
        return deviceUUID;
    }

    public Preferences(Context c){
        Timber.v("initializing");
        activeSharedPreferencesChangeListener = new LinkedList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c); // only used for modeId and firstStart keys
        privateSharedPreferences = c.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE);
        httpSharedPreferences = c.getSharedPreferences(FILENAME_HTTP, Context.MODE_PRIVATE);
        publicSharedPreferences = c.getSharedPreferences(FILENAME_PUBLIC, Context.MODE_PRIVATE);

        deviceUUID = sharedPreferences.getString(Keys._DEVICE_UUID, "undefined-uuid");
        initMode(sharedPreferences.getInt(Keys.MODE_ID, getIntResource(R.integer.valModeId)));
    }


    private void initMode(int active) {
        // Check for valid mode IDs and fallback to Private if an invalid mode is set
        if(active == App.MODE_ID_MQTT_PRIVATE || active == App.MODE_ID_MQTT_PUBLIC || active == App.MODE_ID_HTTP_PRIVATE) {
            setMode(active, true);
        } else {
            setMode(App.MODE_ID_MQTT_PRIVATE, true);
        }
    }

    public void setMode(int active) {
        setMode(active, false);
    }
    public void setMode(int active, boolean init){
        Timber.v("setMode: " + active);

        if(!init && modeId == active) {
            Timber.v("mode is already set to requested mode");
            return;
        }

        Timber.v("setting mode to: " + active);

        detachAllActivePreferenceChangeListeners();
        int oldModeId = modeId;
        modeId = active;
        switch (modeId) {
            case App.MODE_ID_MQTT_PRIVATE:
                activeSharedPreferences = privateSharedPreferences;
                sharedPreferencesName = FILENAME_PRIVATE;
                break;
            case App.MODE_ID_MQTT_PUBLIC:
                activeSharedPreferences = publicSharedPreferences;
                sharedPreferencesName = FILENAME_PUBLIC;
                break;
            case App.MODE_ID_HTTP_PRIVATE:
                activeSharedPreferences = httpSharedPreferences;
                sharedPreferencesName = FILENAME_HTTP;
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

    public void registerOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
        activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        activeSharedPreferencesChangeListener.push(listener);
    }

    public void unregisterOnPreferenceChangedListener(OnPreferenceChangedListener listener) {
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

    private boolean getBoolean(String key,  int defId) {
        return getBoolean(key, defId, defId, false);
    }

    public boolean getBoolean(String key, int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getBooleanRessource(defIdPublic) :  activeSharedPreferences.getBoolean(key, getBooleanRessource(defIdPublic));
        }

        return activeSharedPreferences.getBoolean(key, getBooleanRessource(defIdPrivate));
    }

    private boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

    // For keys that do not need overrides in any modes
    public int getInt(String key,  int defId) {
        return getInt(key, defId, defId, false);
    }
    public int getInt(String key,  int defIdPrivate, int defIdPublic, boolean forceDefIdPublic) {
        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getIntResource(defIdPublic) :  activeSharedPreferences.getInt(key, getIntResource(defIdPublic));
        }

        return activeSharedPreferences.getInt(key, getIntResource(defIdPrivate));
    }
    private int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }


    public int getIntegerDefaultValue(int defaultValueResPrivate, int defaultValueResPublic) {
        return isModeMqttPrivate() ? getIntResource(defaultValueResPrivate) : getIntResource(defaultValueResPublic);
    }



    // Gets the key from specified preferences
    // If the returned value is an empty string or null, the default id is returned
    // This is a quick fix as an empty string does not return the default value
    private String getStringWithFallback(SharedPreferences preferences, String key, int defId) {
        String s = preferences.getString(key, "");
        return ("".equals(s)) ? getStringRessource(defId) : s;
    }

    public String getString(String key,  int defId) {
        return getString(key, defId, defId, defId, false, false);
    }
    public String getString(String key,  int defIdPrivate, int defIdPublic, int defIdHttp, boolean forceDefIdPublic, boolean forceDefIdHttp) {
        if (isModeHttpPrivate()) {
            return forceDefIdHttp ? getStringRessource(defIdHttp) : getStringWithFallback(httpSharedPreferences, key, defIdHttp);
        }

        if (isModeMqttPublic()) {
            return forceDefIdPublic ? getStringRessource(defIdPublic) : getStringWithFallback(publicSharedPreferences, key, defIdPublic);
        }

        return getStringWithFallback(privateSharedPreferences, key, defIdPrivate);
    }

    public String getStringDefaultValue(@StringRes  int defIdPrivate, @StringRes int defIdPublic) {
        return isModeMqttPrivate() ? getStringRessource(defIdPrivate) : getStringRessource(defIdPublic);
    }

    private String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }

    private void setString(String key, String value) {
        setString(key, value, true);
    }
    private void setString(String key, String value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putString(key, value).apply();
    }

    private void setInt(String key, int value) {
        setInt(key, value, true);
    }
    private void setInt(String key, int value, boolean allowSetWhenPublic) {
        if((isModeMqttPublic() && !allowSetWhenPublic)) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putInt(key, value).apply();
    }
    private void setBoolean(String key, boolean value) {
        setBoolean(key, value, true);
    }
    private void setBoolean(String key, boolean value, boolean allowSetWhenPublic) {
        if(isModeMqttPublic() && !allowSetWhenPublic) {
            Timber.e("setting of key denied in the current mode: " + key);
            return;
        }
        activeSharedPreferences.edit().putBoolean(key, value).apply();
    }

    public void clearKey(String key) {
        activeSharedPreferences.edit().remove(key).apply();
    }

    @Export(key =Keys.MODE_ID, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public int getModeId() { return modeId; }

    @SuppressLint("CommitPrefEdits")
    public void importKeyValue(String key, String value) throws IllegalAccessException, IllegalArgumentException {
        Timber.v("setting %s, for key %s", value, key);
        HashMap<String, Method> methods = getImportMethods();

        Method m = methods.get(key);
        if(m == null)
            throw new IllegalAccessException();


        if(value == null) {
            clearKey(key);
            return;
        }

        try {
            Type t = m.getGenericParameterTypes()[0];
            Timber.v("type of parameter: %s %s", t, t.getClass());
            methods.get(key).invoke(this, convert(t, value));
        } catch (InvocationTargetException e) {
            throw new IllegalAccessException();
        }

    }

    private Object convert( Type t, String value ) throws IllegalArgumentException{
        if( Boolean.TYPE == t ) {
            if(!"true".equals(value)&& !"false".equals(value) )
                throw new IllegalArgumentException();
            return Boolean.parseBoolean(value);
        }
        try {
            if (Integer.TYPE == t) return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        return value;
    }


    @SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
    public void importFromMessage(MessageConfiguration m) {
        Timber.v("importing %s keys ", m.getKeys().size());
        HashMap<String, Method> methods = getImportMethods();

        if(m.containsKey(Keys.MODE_ID)) {
            Timber.v("setting mode to %s", m.get(Keys.MODE_ID));
            setMode((Integer) m.get(Keys.MODE_ID));
            m.removeKey(Keys.MODE_ID);
        }

        // Don't show setup if a config has been imported
        setSetupCompleted();


        for(String key : m.getKeys()) {
            try {
                Object value = m.get(key);

                Timber.v("load for key %s:%s", key, value);
                if(value==null) {
                    Timber.v("clearing value for key %s", key);
                    clearKey(key);
                } else {
                    Timber.v("method: %s", methods.get(key).getName());
                    methods.get(key).invoke(this, m.get(key));
                }
                } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Timber.v("committing to preferences %s", activeSharedPreferences);

        activeSharedPreferences.edit().commit();
        if(m.hasWaypoints()) {
            importWaypointsFromJson(m.getWaypoints());
        }


    }

    public MessageWaypointCollection waypointsToJSON() {

        MessageWaypointCollection messages = new MessageWaypointCollection();
        for(Waypoint waypoint : App.getDao().getWaypointDao().loadAll()) {
            messages.add(MessageWaypoint.fromDaoObject(waypoint));
        }
        return messages;
    }


    public void importWaypointsFromJson(@Nullable  MessageWaypointCollection j) {
        if(j == null)
            return;

        WaypointDao dao = App.getDao().getWaypointDao();
        List<Waypoint> deviceWaypoints =  dao.loadAll();

        for (MessageWaypoint m: j) {
            Waypoint w = m.toDaoObject();

            for(Waypoint e : deviceWaypoints) {
                // remove exisiting waypoint before importing new one
                if(TimeUnit.MILLISECONDS.toSeconds(e.getDate().getTime()) == TimeUnit.MILLISECONDS.toSeconds(w.getDate().getTime())) {
                    Timber.v("removing existing waypoint with same tst before adding it");
                    dao.delete(e);
                    App.getEventBus().post(w);
                }
            }

            dao.insert(w);
            App.getEventBus().post(w);
        }
    }


    @Import(key = Keys.OPENCAGE_GEOCODER_API_KEY)
    public void setOpenCageGeocoderApiKey(String key) {
        setString(Keys.CP, key, true);
    }

    @Export(key = Keys.OPENCAGE_GEOCODER_API_KEY, exportModeMqttPrivate = true, exportModeHttpPrivate = true, exportModeMqttPublic = true)
    public String getOpenCageGeocoderApiKey() {
        return getString(Keys.OPENCAGE_GEOCODER_API_KEY, R.string.OPENCAGE_API_KEY);
    }

    @Import(key = Keys.CP)
    public void setCp(boolean cp) {
        setBoolean(Keys.CP, cp, false);
    }

    @Export(key = Keys.CP, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getCp() {
        return getBoolean(Keys.CP, R.bool.valCp);
    }

    @Export(key =Keys.REMOTE_CONFIGURATION, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public boolean getRemoteConfiguration() {
        return getBoolean(Keys.REMOTE_CONFIGURATION, R.bool.valRemoteConfiguration, R.bool.valRemoteConfigurationPublic, true);
    }

    @Export(key =Keys.REMOTE_COMMAND, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public boolean getRemoteCommand() {
        return getBoolean(Keys.REMOTE_COMMAND, R.bool.valRemoteCommand);
    }

    @Import(key =Keys.REMOTE_CONFIGURATION )
    public void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(Keys.REMOTE_CONFIGURATION, aBoolean, false);
    }

    @Import(key =Keys.REMOTE_COMMAND)
    public void setRemoteCommand(boolean aBoolean) {
        setBoolean(Keys.REMOTE_COMMAND, aBoolean, true);
    }

    @Import(key =Keys.CLEAN_SESSION)
    public void setCleanSession(boolean aBoolean) {
        setBoolean(Keys.CLEAN_SESSION, aBoolean, false);
    }

    @Export(key =Keys.CLEAN_SESSION, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public boolean getCleanSession() {
        return getBoolean(Keys.CLEAN_SESSION, R.bool.valCleanSession,R.bool.valCleanSessionPublic, true);
    }


    @Export(key =Keys.PUB_EXTENDED_DATA, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public boolean getPubLocationExtendedData() {
        return getBoolean(Keys.PUB_EXTENDED_DATA, R.bool.valPubExtendedData, R.bool.valPubExtendedData, false);
    }

    @Export(key =Keys.LOCATOR_DISPLACEMENT, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public int getLocatorDisplacement() {
        return getInt(Keys.LOCATOR_DISPLACEMENT, R.integer.valLocatorDisplacement);
    }

    @Export(key =Keys.LOCATOR_INTERVAL, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public int getLocatorInterval() {
        return getInt(Keys.LOCATOR_INTERVAL, R.integer.valLocatorInterval);
    }

    @Export(key =Keys.PING, exportModeMqttPrivate =true, exportModeMqttPublic =true, exportModeHttpPrivate =true)
    public int getPing() {
        return getInt(Keys.PING, R.integer.valPing);
    }

    @Import(key =Keys.PING)
    private void setPing(int anInt) {
        setInt(Keys.PING, anInt);

    }

    @Export(key =Keys.USERNAME, exportModeMqttPrivate =true, exportModeHttpPrivate = true)
    public String getUsername() {
        // in public, the username is just used to build the topic public/user/$deviceId
        return getString(Keys.USERNAME, R.string.valEmpty, R.string.valUsernamePublic, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.AUTH, exportModeMqttPrivate =true)
    public  boolean getAuth() {
        return getBoolean(Keys.AUTH, R.bool.valAuth, R.bool.valAuthPublic, true);

    }

    @Export(key =Keys.DEVICE_ID, exportModeMqttPrivate =true)
    public String getDeviceId() {
        return getDeviceId(true);
    }

    public String getDeviceId(boolean fallbackToDefault) {
        if(isModeMqttPublic())
            return getDeviceUUID();

        String deviceId = getString(Keys.DEVICE_ID, R.string.valEmpty);
        if ("".equals(deviceId) && fallbackToDefault)
            return getDeviceIdDefault();
        return deviceId;
    }

    @Export(key =Keys.IGNORE_STALE_LOCATIONS, exportModeMqttPrivate =true, exportModeHttpPrivate =true)
    public int getIgnoreStaleLocations() {
        return getInt(Keys.IGNORE_STALE_LOCATIONS, R.integer.valIgnoreStaleLocations);
    }

    @Import(key =Keys.IGNORE_STALE_LOCATIONS )
    public void setIgnoreSTaleLocations(int days) {
        setInt(Keys.IGNORE_STALE_LOCATIONS, days, false);
    }

    @Export(key =Keys.IGNORE_INACCURATE_LOCATIONS, exportModeMqttPrivate =true, exportModeHttpPrivate =true, exportModeMqttPublic = true)
    public int getIgnoreInaccurateLocations() {
        return getInt(Keys.IGNORE_INACCURATE_LOCATIONS, R.integer.valIgnoreInaccurateLocations);
    }

    @Import(key =Keys.IGNORE_INACCURATE_LOCATIONS )
    public void setIgnoreInaccurateLocations(int meters) {
        setInt(Keys.IGNORE_INACCURATE_LOCATIONS, meters, true);
    }




    // Not used on public, as many people might use the same device type
    private String getDeviceIdDefault() {
        // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
        return android.os.Build.DEVICE.replace(" ", "-").replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
    }

    @Export(key =Keys.CLIENT_ID, exportModeMqttPrivate =true)
    public String getClientId() {
        if(isModeMqttPublic())
            return MqttAsyncClient.generateClientId();

        String clientId = getString(Keys.CLIENT_ID, R.string.valEmpty);
        if ("".equals(clientId))
            clientId = getClientIdDefault();
        return clientId;
    }

    private String getClientIdDefault() {
        return (getUsername()+ getDeviceId()).replaceAll("\\W", "").toLowerCase();
    }

    @Import(key =Keys.CLIENT_ID)
    public void setClientId(String clientId) {
        setString(Keys.CLIENT_ID, clientId);
    }

    @Import(key =Keys.PUB_TOPIC_BASE)
    public void setDeviceTopicBase(String deviceTopic) {
        setString(Keys.PUB_TOPIC_BASE, deviceTopic, false);
    }

    public String getPubTopicLocations() {
        return getPubTopicBase();
    }

    public String getPubTopicWaypoints() {
        return getPubTopicBase() +getPubTopicWaypointsPart();
    }

    public String getPubTopicWaypointsPart() {
        return "/waypoint";
    }

    public String getPubTopicEvents() {
        return getPubTopicBase() + getPubTopicEventsPart();
    }

    public String getPubTopicEventsPart() {
        return "/event";
    }
    public String getPubTopicInfoPart() {
        return "/info";
    }
    public String getPubTopicCommands() {
        return getPubTopicBase() +getPubTopicCommandsPart();
    }
    public String getPubTopicCommandsPart() {
        return "/cmd";
    }


    @Export(key =Keys.PUB_TOPIC_BASE, exportModeMqttPrivate =true)
    public String getPubTopicBaseFormatString() {
        return getString(Keys.PUB_TOPIC_BASE, R.string.valPubTopic, R.string.valPubTopicPublic, R.string.valEmpty, true, false);
    }

    public String getPubTopicBase() {
        return getPubTopicBaseFormatString().replace("%u", getUsername()).replace("%d", getDeviceId());
    }

    @Export(key =Keys.SUB_TOPIC, exportModeMqttPrivate =true)
    public String getSubTopic() {
        return getString(Keys.SUB_TOPIC, R.string.valSubTopic, R.string.valSubTopicPublic, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.SUB, exportModeMqttPrivate =true)
    public boolean getSub() {
        return getBoolean(Keys.SUB, R.bool.valSub, R.bool.valSubPublic, true);
    }

    @Import(key =Keys.SUB)
    private void setSub(boolean sub) {
        setBoolean(Keys.SUB, sub, false);
    }


    @Export(key =Keys.TRACKER_ID, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public String getTrackerId() {
        return getTrackerId(false);
    }

    public String getTrackerId(boolean fallback) {

        String tid = getString(Keys.TRACKER_ID, R.string.valEmpty);

        if(tid==null || tid.isEmpty())
            return fallback ? getTrackerIdDefault() : "";
        else
            return tid;
    }

    private String getTrackerIdDefault(){
        String deviceId = getDeviceId();
        if(deviceId!=null && deviceId.length() >= 2)
            return deviceId.substring(deviceId.length() - 2);   // defaults to the last two characters of configured deviceId.
        else
            return "na";  // Empty trackerId won't be included in the message.
    }

    @Import(key =Keys.HOST)
    public void setHost(String value) {
        setString(Keys.HOST, value, false);
        App.getEventBus().post(new Events.EndpointChanged());
    }

    public void setPortDefault() {
        clearKey(Keys.PORT);
    }
    public void setKeepaliveDefault() {
        clearKey(Keys.KEEPALIVE);
    }



    @Import(key =Keys.PORT)
    public void setPort(int value) {
        if(value < 1 || value > 65535)
            setPortDefault();
        else
            setInt(Keys.PORT, value, false);
   }

    @Import(key =Keys.TRACKER_ID)
    public void setTrackerId(String value){
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
    public int getPort() {
        return getInt(Keys.PORT, R.integer.valPort, R.integer.valPortPublic, true);
    }


    private String getIntWithHintSupport(String key) {
        int i = getInt(key, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public String getPortWithHintSupport() {
        return getIntWithHintSupport(Keys.PORT);
    }

    @Import(key =Keys.MQTT_PROTOCOL_LEVEL)
    public void setMqttProtocolLevel(int value) {
        if(value != 0 && value != 3 && value != 4)
            return;

        setInt(Keys.MQTT_PROTOCOL_LEVEL, value, false);
    }

    @Export(key =Keys.MQTT_PROTOCOL_LEVEL, exportModeMqttPrivate =true)
    public int getMqttProtocolLevel() {
        return getInt(Keys.MQTT_PROTOCOL_LEVEL, R.integer.valMqttProtocolLevel, R.integer.valMqttProtocolLevelPublic, true);
    }

    @Import(key =Keys.KEEPALIVE)
    public void setKeepalive(int value)
    {
        if(value < 1)
            setKeepaliveDefault();
        else
            setInt(Keys.KEEPALIVE, value, false);
    }

    public String getKeepaliveWithHintSupport() {
        return getIntWithHintSupport(Keys.KEEPALIVE);
    }

    @Export(key =Keys.KEEPALIVE, exportModeMqttPrivate =true)
    public int getKeepalive() {
        int keepalive = getInt(Keys.KEEPALIVE, R.integer.valKeepalive, R.integer.valKeepalivePublic, true);
        if(keepalive < 30)
            keepalive = 30;
        return keepalive;
    }

    @Import(key =Keys.USERNAME)
    public void setUsername(String value) {
        setString(Keys.USERNAME, value);
    }

    @Import(key =Keys.PUB_EXTENDED_DATA)
    private void setPubLocationExtendedData(boolean aBoolean) {
        setBoolean(Keys.PUB_EXTENDED_DATA, aBoolean);
    }

    @Import(key =Keys.PUB)
    public void setPub(boolean aBoolean) {
        setBoolean(Keys.PUB, aBoolean);
    }

    @Import(key =Keys.NOTIFICATION_HIGHER_PRIORITY)
    private void setNotificationHigherPriority(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION_HIGHER_PRIORITY, aBoolean);
    }

    @Import(key =Keys.NOTIFICATION_LOCATION)
    private void setNotificationLocation(boolean aBoolean) {
        setBoolean(Keys.NOTIFICATION_LOCATION, aBoolean);
    }
    @Import(key =Keys.NOTIFICATION_EVENTS)
    public void setNotificationEvents(boolean notificationEvents) {
        setBoolean(Keys.NOTIFICATION_EVENTS, notificationEvents);
    }

    @Import(key =Keys.SUB_TOPIC)
    private void setSubTopic(String string) {
        setString(Keys.SUB_TOPIC, string, false);
    }

    @Import(key =Keys.AUTOSTART_ON_BOOT)
    private void setAutostartOnBoot(boolean aBoolean) {
        setBoolean(Keys.AUTOSTART_ON_BOOT, aBoolean);

    }
    @Import(key =Keys.LOCATOR_ACCURACY_FOREGROUND)
    private void setLocatorAccuracyForeground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_FOREGROUND, anInt);

    }

    @Import(key =Keys.LOCATOR_ACCURACY_BACKGROUND)
    private void setLocatorAccuracyBackground(int anInt) {
        setInt(Keys.LOCATOR_ACCURACY_BACKGROUND, anInt);

    }
    @Import(key =Keys.LOCATOR_INTERVAL)
    private void setLocatorInterval(int anInt) {
        setInt(Keys.LOCATOR_INTERVAL, anInt);

    }

    @Import(key =Keys.LOCATOR_DISPLACEMENT)
    private void setLocatorDisplacement(int anInt) {
        setInt(Keys.LOCATOR_DISPLACEMENT, anInt);

    }
    @Import(key =Keys.PUB_RETAIN)
    private void setPubRetain(boolean aBoolean) {
        setBoolean(Keys.PUB_RETAIN, aBoolean, false);

    }
    @Import(key =Keys.PUB_QOS)
    private void setPubQos(int anInt) {
        setInt(Keys.PUB_QOS, anInt, false);
    }


    @Import(key =Keys.SUB_QOS)
    private void setSubQos(int anInt) {
        setInt(Keys.SUB_QOS, anInt > 2 ? 2 : anInt, false);
    }


    @Import(key =Keys.PASSWORD)
    public void setPassword(String password) {
            setString(Keys.PASSWORD, password);
    }

    @Import(key =Keys.DEVICE_ID)
    public void setDeviceId(String deviceId) {
        setString(Keys.DEVICE_ID, deviceId, false);
    }


    @Import(key =Keys.AUTH)
    public void setAuth(boolean auth) {
        setBoolean(Keys.AUTH, auth, false);
    }

    @Import(key =Keys.TLS)
    public void setTls(boolean tlsSpecifier) {
        setBoolean(Keys.TLS, tlsSpecifier, false);
    }

    @Import(key =Keys.WS)
    public void setWs(boolean wsEnable) {
        setBoolean(Keys.WS, wsEnable, false);
    }

    public void setTlsCaCrt(String name) {
        setString(Keys.TLS_CA_CRT, name, false);
    }
    public void setTlsClientCrt(String name) {
        setString(Keys.TLS_CLIENT_CRT, name, false);
    }
    @Export(key =Keys.HOST, exportModeMqttPrivate =true)
    public String getHost() {
        return getString(Keys.HOST, R.string.valEmpty, R.string.valHostPublic, R.string.valEmpty, true, false);
    }
    @Export(key =Keys.PASSWORD, exportModeMqttPrivate =true, exportModeHttpPrivate = true)
    public String getPassword() {
        return getString(Keys.PASSWORD, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty, true, false);
    }

    @Export(key =Keys.TLS, exportModeMqttPrivate =true)
    public boolean getTls() {
        return getBoolean(Keys.TLS, R.bool.valTls, R.bool.valTlsPublic, true);
    }
    @Export(key =Keys.WS, exportModeMqttPrivate =true)
    public boolean getWs() {
        return getBoolean(Keys.WS, R.bool.valWs, R.bool.valWsPublic, true);
    }

    @Export(key =Keys.TLS_CA_CRT, exportModeMqttPrivate =true)
    public String getTlsCaCrtName() {
        return getString(Keys.TLS_CA_CRT, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty,true, false);
    }

    @Export(key =Keys.TLS_CLIENT_CRT, exportModeMqttPrivate =true)
    public String getTlsClientCrtName() {
        return getString(Keys.TLS_CLIENT_CRT, R.string.valEmpty, R.string.valEmpty, R.string.valEmpty,true, false);
    }

    @Export(key =Keys.NOTIFICATION_HIGHER_PRIORITY, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public boolean getNotificationHigherPriority() {
        return getBoolean(Keys.NOTIFICATION_HIGHER_PRIORITY, R.bool.valNotificationHigherPriority);
    }


    @Export(key =Keys.NOTIFICATION_LOCATION, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public  boolean getNotificationLocation() {
        return getBoolean(Keys.NOTIFICATION_LOCATION, R.bool.valNotificationLocation);
    }

    public boolean getNotificationEvents() {
        return getBoolean(Keys.NOTIFICATION_EVENTS, R.bool.valNotificationEvents);
    }

    @Export(key =Keys.PUB_QOS, exportModeMqttPrivate =true)
    public int getPubQos() {
        return getInt(Keys.PUB_QOS, R.integer.valPubQos, R.integer.valPubQosPublic, true);
    }

    @Export(key =Keys.SUB_QOS, exportModeMqttPrivate =true)
    public int getSubQos() {
        return getInt(Keys.SUB_QOS, R.integer.valSubQos, R.integer.valSubQosPublic, true);
    }

    @Export(key =Keys.PUB_RETAIN, exportModeMqttPrivate =true)
    public boolean getPubRetain() {
        return getBoolean(Keys.PUB_RETAIN, R.bool.valPubRetain, R.bool.valPubRetainPublic, true);
    }

    @Export(key =Keys.PUB, exportModeMqttPrivate =true, exportModeMqttPublic = true)
    public boolean getPub() {
        return getBoolean(Keys.PUB, R.bool.valPub);
    }


    @Export(key =Keys.AUTOSTART_ON_BOOT, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public boolean getAutostartOnBoot() {
        return getBoolean(Keys.AUTOSTART_ON_BOOT, R.bool.valAutostartOnBoot);
    }

    @Export(key =Keys.LOCATOR_ACCURACY_FOREGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public int getLocatorAccuracyForeground() {
        return getInt(Keys.LOCATOR_ACCURACY_FOREGROUND, R.integer.valLocatorAccuracyForeground);
    }

    @Export(key =Keys.LOCATOR_ACCURACY_BACKGROUND, exportModeMqttPrivate =true, exportModeMqttPublic = true, exportModeHttpPrivate = true)
    public int getLocatorAccuracyBackground() {
        return getInt(Keys.LOCATOR_ACCURACY_BACKGROUND, R.integer.valLocatorAccuracyBackground);
    }


    public boolean getInfo() {
        return getBoolean(Keys.INFO, R.bool.valInfo, R.bool.valInfoPublic, false);
    }

    @Import(key =Keys.INFO)
    public void setInfo(boolean info) {
        setBoolean(Keys.INFO, info);
    }

    public String getTlsClientCrtPassword() {
        return getString(Keys.TLS_CLIENT_CRT_PASSWORD, R.string.valEmpty);
    }

    @Import(key =Keys.URL)
    public void setUrl(String url) {
        setString(Keys.URL, url);
    }

    @Export(key = Keys.URL, exportModeHttpPrivate = true)
    public String getUrl() {
        return getString(Keys.URL, R.string.valEmpty);
    }
    public void setTlsClientCrtPassword(String password) {
        setString(Keys.TLS_CLIENT_CRT_PASSWORD, password);
    }

    @Export(key = Keys.DEBUG_VIBRATE, exportModeHttpPrivate = true ,exportModeMqttPrivate = true, exportModeMqttPublic = true)
    public boolean getDebugVibrate() {
        return getBoolean(Keys.DEBUG_VIBRATE, R.bool.valDebugVibrate);
    }

    @Import(key = Keys.DEBUG_VIBRATE)
    public void setDebugVibrate(boolean vibrate) {
        setBoolean(Keys.DEBUG_VIBRATE, vibrate);
    }

    String getEncryptionKey() {
        return getString(Keys._ENCRYPTION_KEY, R.string.valEmpty);
    }

    boolean getSetupCompleted() {
        // sharedPreferences because the value is independent from the selected mode
        return !sharedPreferences.getBoolean(Keys._SETUP_NOT_COMPLETED, false);
    }

    public void setSetupCompleted() {
        sharedPreferences.edit().putBoolean(Keys._SETUP_NOT_COMPLETED , false).apply();

    }

    // Maybe make this configurable
    // For now it makes things easier to change
    public int getPubQosEvents() {
        return getPubQos();
    }

    public static boolean getPubRetainEvents() {
        return false;
    }

    public static int getPubQosWaypoints() {
        return 0;
    }

    public static boolean getPubRetainWaypoints() {
        return false;
    }

    public int getPubQosLocations() {
        return getPubQos();
    }

    public boolean getPubRetainLocations() {
        return getPubRetain();
    }


    public MessageConfiguration exportToMessage() {
        List<Method> methods = getExportMethods();
        MessageConfiguration cfg = new MessageConfiguration();
        cfg.set(Keys._VERSION, BuildConfig.VERSION_CODE);
        for(Method m : methods) {
            m.setAccessible(true);
            Timber.v("key %s", m.getAnnotation(Export.class).key());
            try {
                cfg.set(m.getAnnotation(Export.class).key(), m.invoke(this));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        cfg.setWaypoints(waypointsToJSON());

        return cfg;
    }

    @Export(key =Keys.FUSED_REGION_DETECTION, exportModeMqttPrivate = true, exportModeHttpPrivate =true)
    public boolean getFuseRegionDetection() {
        return getBoolean(Keys.FUSED_REGION_DETECTION, R.bool.valTrue);
    }

    @Import(key =Keys.FUSED_REGION_DETECTION)
    public void setFusedRegionDetection(boolean aBoolean) {
        setBoolean(Keys.FUSED_REGION_DETECTION, aBoolean, false);
    }

    @SuppressWarnings("WeakerAccess")
    public static class Keys {
        public static final String AUTH                             = "auth";
        public static final String AUTOSTART_ON_BOOT                = "autostartOnBoot";
        public static final String CLEAN_SESSION                    = "cleanSession";
        public static final String CLIENT_ID                        = "clientId";
        public static final String CP                               = "cp";
        public static final String DEBUG_VIBRATE                    = "debugVibrate";
        public static final String DEVICE_ID                        = "deviceId";
        public static final String FUSED_REGION_DETECTION           = "fusedRegionDetection";
        public static final String HOST                             = "host";
        public static final String IGNORE_INACCURATE_LOCATIONS      = "ignoreInaccurateLocations";
        public static final String IGNORE_STALE_LOCATIONS           = "ignoreStaleLocations";
        public static final String INFO                             = "info";
        public static final String KEEPALIVE                        = "keepalive";
        public static final String LOCATOR_ACCURACY_BACKGROUND      = "locatorAccuracyBackground";
        public static final String LOCATOR_ACCURACY_FOREGROUND      = "locatorAccuracyForeground";
        public static final String LOCATOR_DISPLACEMENT             = "locatorDisplacement";
        public static final String LOCATOR_INTERVAL                 = "locatorInterval";
        public static final String MODE_ID                          = "mode";
        public static final String MQTT_PROTOCOL_LEVEL              = "mqttProtocolLevel";
        public static final String NOTIFICATION                     = "notification";
        public static final String NOTIFICATION_EVENTS              = "notificationEvents";
        public static final String NOTIFICATION_HIGHER_PRIORITY     = "notificationHigherPriority";
        public static final String NOTIFICATION_LOCATION            = "notificationLocation";
        public static final String OPENCAGE_GEOCODER_API_KEY        = "opencageApiKey";
        public static final String PASSWORD                         = "password";
        public static final String PING                             = "ping";
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
        public static final String _VERSION                         = "_build";


    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @SuppressWarnings({"unused", "WeakerAccess"})
    public @interface Export {
        String key();
        boolean exportModeMqttPrivate() default false;
        boolean exportModeMqttPublic() default false;
        boolean exportModeHttpPrivate() default false;
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @SuppressWarnings({"unused", "WeakerAccess"})
    public @interface Import {
        String key();
    }

    private static List<Method> getExportMethods() {
        int modeId = App.getPreferences().getModeId();
        final List<Method> methods = new ArrayList<>();
        Class<?> klass  = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Export.class) ) {
                    Export annotInstance = method.getAnnotation(Export.class);
                    if(modeId == App.MODE_ID_MQTT_PRIVATE && annotInstance.exportModeMqttPrivate() || modeId == App.MODE_ID_MQTT_PUBLIC && annotInstance.exportModeMqttPublic() ||modeId == App.MODE_ID_HTTP_PRIVATE && annotInstance.exportModeHttpPrivate()) {
                        methods.add(method);
                    }
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    public static List<String> getImportKeys() {
        return new ArrayList<>(getImportMethods().keySet());
    }

    private static HashMap<String, Method> getImportMethods() {
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
    public String getStringOrNull(@NonNull String key) {
        String st = getString(key, R.string.valEmpty);
        return (st != null && !st.isEmpty()) ? st : null;
    }
}
