package org.owntracks.android.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;
import org.owntracks.android.BuildConfig;
import org.owntracks.android.R;
import org.owntracks.android.injection.qualifier.AppContext;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageConfiguration;
import org.owntracks.android.services.LocationProcessor;
import org.owntracks.android.services.MessageProcessorEndpointHttp;
import org.owntracks.android.services.MessageProcessorEndpointMqtt;
import org.owntracks.android.services.worker.Scheduler;

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

import javax.inject.Inject;

import timber.log.Timber;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_DEFAULT;

@SuppressWarnings({"unused", "WeakerAccess"})
@PerApplication
public class Preferences {
    private static final String FILENAME_PRIVATE = "org.owntracks.android.preferences.private";
    private static final String FILENAME_HTTP = "org.owntracks.android.preferences.http";

    private static SharedPreferences activeSharedPreferences;
    private static SharedPreferences sharedPreferences;

    private static SharedPreferences privateSharedPreferences;
    private static SharedPreferences httpSharedPreferences;

    private static int modeId = MessageProcessorEndpointMqtt.MODE_ID;
    private final Context context;
    private final EventBus eventBus;
    private String sharedPreferencesName;
    private boolean isFirstStart = false;

    public String getSharedPreferencesName() {
        return sharedPreferencesName;
    }

    @Inject
    public Preferences(@AppContext Context c, EventBus eventBus) {
        Timber.v("initializing");
        this.context = c;
        this.eventBus = eventBus;
        activeSharedPreferencesChangeListener = new LinkedList<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(c); // only used for modeId and firstStart keys
        privateSharedPreferences = c.getSharedPreferences(FILENAME_PRIVATE, Context.MODE_PRIVATE);
        httpSharedPreferences = c.getSharedPreferences(FILENAME_HTTP, Context.MODE_PRIVATE);

        initMode(sharedPreferences.getInt(getPreferenceKey(R.string.preferenceKeyModeId), getIntResource(R.integer.valModeId)));
    }

    public String getPreferenceKey(int res) {
        return context.getString(res);
    }

    private void initMode(int active) {
        // Check for valid mode IDs and fallback to MQTT if an invalid mode is set
        setMode(active == MessageProcessorEndpointHttp.MODE_ID ? MessageProcessorEndpointHttp.MODE_ID : MessageProcessorEndpointMqtt.MODE_ID, true);
    }

    public void setMode(int active) {
        setMode(active, false);
    }

    public void setMode(int active, boolean init) {
        Timber.v("setMode: %s", active);

        if (!init && modeId == active) {
            Timber.v("mode is already set to requested mode");
            return;
        }

        Timber.v("setting mode to: %s", active);

        detachAllActivePreferenceChangeListeners();
        int oldModeId = modeId;
        modeId = active;
        switch (modeId) {
            case MessageProcessorEndpointMqtt.MODE_ID:
                activeSharedPreferences = privateSharedPreferences;
                sharedPreferencesName = FILENAME_PRIVATE;
                break;
            case MessageProcessorEndpointHttp.MODE_ID:
                activeSharedPreferences = httpSharedPreferences;
                sharedPreferencesName = FILENAME_HTTP;
                break;

        }
        sharedPreferences.edit().putInt(getPreferenceKey(R.string.preferenceKeyModeId), modeId).apply();

        // Mode switcher reads from currently active sharedPreferences, so we commit the value to all
        privateSharedPreferences.edit().putInt(getPreferenceKey(R.string.preferenceKeyModeId), modeId).apply();
        httpSharedPreferences.edit().putInt(getPreferenceKey(R.string.preferenceKeyModeId), modeId).apply();

        attachAllActivePreferenceChangeListeners();

        if (!init) {
            Timber.v("broadcasting mode change event");
            eventBus.post(new Events.ModeChanged(oldModeId, modeId));
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
        for (SharedPreferences.OnSharedPreferenceChangeListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private static void attachAllActivePreferenceChangeListeners() {
        for (OnPreferenceChangedListener listener : activeSharedPreferencesChangeListener) {
            activeSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
            listener.onAttachAfterModeChanged();
        }
    }

    public void checkFirstStart() {
        if (sharedPreferences.getBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), true)) {
            Timber.v("Initial application launch");
            isFirstStart = true;
            sharedPreferences.edit()
                    .putBoolean(getPreferenceKey(R.string.preferenceKeyFirstStart), false)
                    .putBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), true)
                    .apply();
        }

    }

    public interface OnPreferenceChangedListener extends SharedPreferences.OnSharedPreferenceChangeListener {
        void onAttachAfterModeChanged();
    }

    public boolean getBooleanOrDefault(int resKeyId, int defId) {
        return activeSharedPreferences.getBoolean(getPreferenceKey(resKeyId), getBooleanRessource(defId));
    }

    private boolean getBooleanRessource(int resId) {
        return context.getResources().getBoolean(resId);
    }

    public int getIntOrDefault(int resKeyId, int defId) {
        try {
            return activeSharedPreferences.getInt(getPreferenceKey(resKeyId), getIntResource(defId));
        } catch (ClassCastException e) {
            Timber.e("Error retriving string preference %s, returning default", getPreferenceKey(resKeyId));
            return getIntResource(defId);
        }
    }

    private int getIntResource(int resId) {
        return context.getResources().getInteger(resId);
    }

    // Gets the key from specified preferences
    // If the returned value is an empty string or null, the default id is returned
    // This is a quick fix as an empty string does not return the default value
    private String getStringWithFallback(SharedPreferences preferences, String key, int defId) {
        try {
            String s = preferences.getString(key, "");
            return ("".equals(s)) ? getStringRessource(defId) : s;
        } catch (ClassCastException e) {
            Timber.e("Error retriving string preference %s, returning default", key);
            return getStringRessource(defId);
        }
    }

    public String getStringOrDefault(int resKeyId, int defId) {
        return getStringWithFallback(activeSharedPreferences, getPreferenceKey(resKeyId), defId);
    }

    private String getStringRessource(int resId) {
        return context.getResources().getString(resId);
    }

    private void setString(int resKeyId, String value) {
        activeSharedPreferences.edit().putString(getPreferenceKey(resKeyId), value).apply();
    }

    private void setInt(int resKeyId, int value) {
        activeSharedPreferences.edit().putInt(getPreferenceKey(resKeyId), value).apply();
    }

    private void setBoolean(int resKeyId, boolean value) {
        activeSharedPreferences.edit().putBoolean(getPreferenceKey(resKeyId), value).apply();
    }

    public void clearKey(String key) {
        activeSharedPreferences.edit().remove(key).apply();
    }

    public void clearKey(int resKeyId) {
        clearKey(getPreferenceKey(resKeyId));
    }

    @Export(keyResId = R.string.preferenceKeyModeId, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getModeId() {
        return modeId;
    }

    public void importKeyValue(String key, String value) throws IllegalAccessException, IllegalArgumentException {
        Timber.v("setting %s, for key %s", value, key);
        HashMap<String, Method> methods = getImportMethods();

        Method m = methods.get(key);
        if (m == null)
            throw new IllegalAccessException();

        if (value == null) {
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

    private Object convert(Type t, String value) throws IllegalArgumentException {
        if (Boolean.TYPE == t) {
            if (!"true".equals(value) && !"false".equals(value))
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

        if (m.containsKey(getPreferenceKey(R.string.preferenceKeyModeId))) {
            Timber.v("setting mode to %s", m.get(getPreferenceKey(R.string.preferenceKeyModeId)));
            setMode((Integer) m.get(getPreferenceKey(R.string.preferenceKeyModeId)));
            m.removeKey(getPreferenceKey(R.string.preferenceKeyModeId));
        }

        // Don't show setup if a config has been imported
        setSetupCompleted();


        for (String key : m.getKeys()) {
            try {
                Object value = m.get(key);

                Timber.v("load for key %s:%s", key, value);
                if (value == null) {
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
    }

    @Export(keyResId = R.string.preferenceKeyMonitoring, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getMonitoring() {
        return getIntOrDefault(R.string.preferenceKeyMonitoring, R.integer.valMonitoring);
    }

    @Import(keyResId = R.string.preferenceKeyMonitoring)
    public void setMonitoring(int newmode) {
        if (newmode < LocationProcessor.MONITORING_QUIET || newmode > LocationProcessor.MONITORING_MOVE) {
            Timber.e("invalid monitoring mode specified %s", newmode);
            return;
        }
        setInt(R.string.preferenceKeyMonitoring, newmode);
        eventBus.post(new Events.MonitoringChanged(newmode));
    }

    public void setMonitoringNext() {

        int mode = getMonitoring();
        int newmode;
        if (mode < LocationProcessor.MONITORING_MOVE) {
            mode++;
        } else {
            mode = LocationProcessor.MONITORING_QUIET;
        }

        Timber.v("setting monitoring mode %s", mode);

        setMonitoring(mode);

    }

    @Export(keyResId = R.string.preferenceKeyDontReuseHttpClient, exportModeMqttPrivate = false, exportModeHttpPrivate = true)
    public boolean getDontReuseHTTPClient() {
        return getBooleanOrDefault(R.string.preferenceKeyDontReuseHttpClient, R.bool.valFalse);
    }

    @Import(keyResId = R.string.preferenceKeyDontReuseHttpClient)
    public void setDontReuseHTTPClient(boolean bool) {
        setBoolean(R.string.preferenceKeyDontReuseHttpClient, bool);
    }


    @Import(keyResId = R.string.preferenceKeyOpencageGeocoderApiKey)
    public void setOpenCageGeocoderApiKey(String key) {
        setString(R.string.preferenceKeyOpencageGeocoderApiKey, key.trim());
    }

    @Export(keyResId = R.string.preferenceKeyOpencageGeocoderApiKey, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public String getOpenCageGeocoderApiKey() {
        return getStringOrDefault(R.string.preferenceKeyOpencageGeocoderApiKey, R.string.valEmpty);
    }

    public boolean getRemoteConfiguration() {
        return getBooleanOrDefault(R.string.preferenceKeyRemoteConfiguration, R.bool.valRemoteConfiguration);
    }

    @Export(keyResId = R.string.preferenceKeyRemoteCommand, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getRemoteCommand() {
        return getBooleanOrDefault(R.string.preferenceKeyRemoteCommand, R.bool.valRemoteCommand);
    }

    public void setRemoteConfiguration(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyRemoteConfiguration, aBoolean);
    }

    @Import(keyResId = R.string.preferenceKeyRemoteCommand)
    public void setRemoteCommand(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyRemoteCommand, aBoolean);
    }

    @Import(keyResId = R.string.preferenceKeyCleanSession)
    public void setCleanSession(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyCleanSession, aBoolean);
    }

    @Export(keyResId = R.string.preferenceKeyCleanSession, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getCleanSession() {
        return getBooleanOrDefault(R.string.preferenceKeyCleanSession, R.bool.valCleanSession);
    }


    @Export(keyResId = R.string.preferenceKeyPublishExtendedData, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getPubLocationExtendedData() {
        return getBooleanOrDefault(R.string.preferenceKeyPublishExtendedData, R.bool.valPubExtendedData);
    }

    @Export(keyResId = R.string.preferenceKeyPublishExtendedData, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getLocatorDisplacement() {
        return getIntOrDefault(R.string.preferenceKeyPublishExtendedData, R.integer.valLocatorDisplacement);
    }

    @Export(keyResId = R.string.preferenceKeyLocatorInterval, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getLocatorInterval() {
        return getIntOrDefault(R.string.preferenceKeyLocatorInterval, R.integer.valLocatorInterval);
    }

    @Export(keyResId = R.string.preferenceKeyMoveModeLocatorInterval, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getMoveModeLocatorInterval() {
        return getIntOrDefault(R.string.preferenceKeyMoveModeLocatorInterval, R.integer.valMoveModeLocatorInterval);
    }

    @Export(keyResId = R.string.preferenceKeyLocatorPriority, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getLocatorPriority() {
        return getIntOrDefault(R.string.preferenceKeyLocatorPriority, R.integer.valLocatorPriority);
    }

    @Export(keyResId = R.string.preferenceKeyPing, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    // Unit is minutes
    public int getPing() {
        return Math.max(getIntOrDefault(R.string.preferenceKeyPing, R.integer.valPing), (int) TimeUnit.MILLISECONDS.toMinutes(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    // Unit is minutes
    @Import(keyResId = R.string.preferenceKeyPing)
    private void setPing(int anInt) {
        setInt(R.string.preferenceKeyPing, anInt);

    }

    @Export(keyResId = R.string.preferenceKeyUsername, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public String getUsername() {
        return getStringOrDefault(R.string.preferenceKeyUsername, R.string.valEmpty);
    }

    @Export(keyResId = R.string.preferenceKeyDeviceId, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public String getDeviceId() {
        return getDeviceId(true);
    }

    public String getDeviceId(boolean fallbackToDefault) {
        String deviceId = getStringOrDefault(R.string.preferenceKeyDeviceId, R.string.valEmpty);
        if ("".equals(deviceId) && fallbackToDefault)
            return getDeviceIdDefault();
        return deviceId;
    }

    @Export(keyResId = R.string.preferenceKeyIgnoreStaleLocations, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public double getIgnoreStaleLocations() {
        return Double.parseDouble(getStringOrDefault(R.string.preferenceKeyIgnoreStaleLocations, R.string.valIgnoreStaleLocations));
    }

    @Import(keyResId = R.string.preferenceKeyIgnoreStaleLocations)
    public void setIgnoreSTaleLocations(double days) {
        setString(R.string.preferenceKeyIgnoreStaleLocations, String.valueOf(days));
    }

    @Export(keyResId = R.string.preferenceKeyIgnoreInaccurateLocations, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public int getIgnoreInaccurateLocations() {
        return getIntOrDefault(R.string.preferenceKeyIgnoreInaccurateLocations, R.integer.valIgnoreInaccurateLocations);
    }

    @Import(keyResId = R.string.preferenceKeyIgnoreInaccurateLocations)
    public void setIgnoreInaccurateLocations(int meters) {
        setInt(R.string.preferenceKeyIgnoreInaccurateLocations, meters);
    }


    // Not used on public, as many people might use the same device type
    private String getDeviceIdDefault() {
        // Use device name (Mako, Surnia, etc. and strip all non alpha digits)
        return Build.DEVICE.replace(" ", "-").replaceAll("[^a-zA-Z0-9]+", "").toLowerCase();
    }

    @Export(keyResId = R.string.preferenceKeyClientId, exportModeMqttPrivate = true)
    public String getClientId() {
        String clientId = getStringOrDefault(R.string.preferenceKeyClientId, R.string.valEmpty);
        if ("".equals(clientId))
            clientId = getClientIdDefault();
        return clientId;
    }

    private String getClientIdDefault() {
        return (getUsername() + getDeviceId()).replaceAll("\\W", "").toLowerCase();
    }

    @Import(keyResId = R.string.preferenceKeyClientId)
    public void setClientId(String clientId) {
        setString(R.string.preferenceKeyClientId, clientId);
    }

    @Import(keyResId = R.string.preferenceKeyPubTopicBase)
    public void setDeviceTopicBase(String deviceTopic) {
        setString(R.string.preferenceKeyPubTopicBase, deviceTopic);
    }

    public String getPubTopicLocations() {
        return getPubTopicBase();
    }

    public String getPubTopicWaypoints() {
        return getPubTopicBase() + getPubTopicWaypointsPart();
    }

    public String getPubTopicWaypointsPart() {
        return "/waypoints";
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
        return getPubTopicBase() + getPubTopicCommandsPart();
    }

    public String getPubTopicCommandsPart() {
        return "/cmd";
    }


    @Export(keyResId = R.string.preferenceKeyPubTopicBase, exportModeMqttPrivate = true)
    public String getPubTopicBaseFormatString() {
        return getStringOrDefault(R.string.preferenceKeyPubTopicBase, R.string.valPubTopic);
    }

    public String getPubTopicBase() {
        return getPubTopicBaseFormatString().replace("%u", getUsername()).replace("%d", getDeviceId());
    }

    @Export(keyResId = R.string.preferenceKeySubTopic, exportModeMqttPrivate = true)
    public String getSubTopic() {
        return getStringOrDefault(R.string.preferenceKeySubTopic, R.string.valSubTopic);
    }

    @Export(keyResId = R.string.preferenceKeySub, exportModeMqttPrivate = true)
    public boolean getSub() {
        return getBooleanOrDefault(R.string.preferenceKeySub, R.bool.valSub);
    }

    @Import(keyResId = R.string.preferenceKeySub)
    private void setSub(boolean sub) {
        setBoolean(R.string.preferenceKeySub, sub);
    }


    @Export(keyResId = R.string.preferenceKeyTrackerId, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public String getTrackerId() {
        return getTrackerId(false);
    }

    public String getTrackerId(boolean fallback) {

        String tid = getStringOrDefault(R.string.preferenceKeyTrackerId, R.string.valEmpty);

        if (tid == null || tid.isEmpty())
            return fallback ? getTrackerIdDefault() : "";
        else
            return tid;
    }

    private String getTrackerIdDefault() {
        String deviceId = getDeviceId();
        if (deviceId != null && deviceId.length() >= 2)
            return deviceId.substring(deviceId.length() - 2);   // defaults to the last two characters of configured deviceId.
        else
            return "na";  // Empty trackerId won't be included in the message.
    }

    @Import(keyResId = R.string.preferenceKeyHost)
    public void setHost(String value) {
        setString(R.string.preferenceKeyHost, value);
    }

    public void setPortDefault() {
        clearKey(R.string.preferenceKeyPort);
    }

    public void setKeepaliveDefault() {
        clearKey(R.string.preferenceKeyKeepalive);
    }


    @Import(keyResId = R.string.preferenceKeyPort)
    public void setPort(int value) {
        if (value < 1 || value > 65535)
            setPortDefault();
        else
            setInt(R.string.preferenceKeyPort, value);
    }

    @Export(keyResId = R.string.preferenceKeyPort, exportModeMqttPrivate = true)
    public int getPort() {
        return getIntOrDefault(R.string.preferenceKeyPort, R.integer.valPort);
    }

    @Import(keyResId = R.string.preferenceKeyTrackerId)
    public void setTrackerId(String value) {
        int len = value.length();
        // value validation - must be max 2 characters, only letters and digits
        if (len >= 2) {
            value = value.substring(0, 2);
            if (Character.isLetterOrDigit(value.charAt(0)) && Character.isLetterOrDigit(value.charAt(1)))
                setString(R.string.preferenceKeyTrackerId, value);
        } else {
            if (len > 0 && Character.isLetterOrDigit(value.charAt(0)))
                setString(R.string.preferenceKeyTrackerId, value);
            else
                setString(R.string.preferenceKeyTrackerId, "");
        }

    }

    private String getIntWithHintSupport(int resKeyId) {
        int i = getIntOrDefault(resKeyId, R.integer.valInvalid);
        if (i == -1) {
            return "";
        } else {
            return Integer.toString(i);
        }
    }

    public String getPortWithHintSupport() {
        return getIntWithHintSupport(R.string.preferenceKeyPort);
    }

    @Import(keyResId = R.string.preferenceKeyMqttProtocolLevel)
    public void setMqttProtocolLevel(int value) {
        if (value != MQTT_VERSION_DEFAULT && value != MQTT_VERSION_3_1 && value != MQTT_VERSION_3_1_1)
            return;

        setInt(R.string.preferenceKeyMqttProtocolLevel, value);
    }

    @Export(keyResId = R.string.preferenceKeyMqttProtocolLevel, exportModeMqttPrivate = true)
    public int getMqttProtocolLevel() {
        return getIntOrDefault(R.string.preferenceKeyMqttProtocolLevel, R.integer.valMqttProtocolLevel);
    }

    @Import(keyResId = R.string.preferenceKeyKeepalive)
    public void setKeepalive(int value) {
        if (value < 1)
            setKeepaliveDefault();
        else
            setInt(R.string.preferenceKeyKeepalive, value);
    }

    public String getKeepaliveWithHintSupport() {
        return getIntWithHintSupport(R.string.preferenceKeyKeepalive);
    }

    // Unit is seconds
    // Minimum time is 15minutes because work manager cannot schedule any faster
    @Export(keyResId = R.string.preferenceKeyKeepalive, exportModeMqttPrivate = true)
    public int getKeepalive() {
        return Math.max(getIntOrDefault(R.string.preferenceKeyKeepalive, R.integer.valKeepalive), (int) TimeUnit.MILLISECONDS.toSeconds(Scheduler.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Import(keyResId = R.string.preferenceKeyUsername)
    public void setUsername(String value) {
        setString(R.string.preferenceKeyUsername, value);
    }

    @Import(keyResId = R.string.preferenceKeyPublishExtendedData)
    private void setPubLocationExtendedData(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyPublishExtendedData, aBoolean);
    }

    @Import(keyResId = R.string.preferenceKeyNotificationHigherPriority)
    private void setNotificationHigherPriority(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyNotificationHigherPriority, aBoolean);
    }

    @Import(keyResId = R.string.preferenceKeyNotificationLocation)
    private void setNotificationLocation(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyNotificationLocation, aBoolean);
    }

    @Import(keyResId = R.string.preferenceKeyNotificationEvents)
    public void setNotificationEvents(boolean notificationEvents) {
        setBoolean(R.string.preferenceKeyNotificationEvents, notificationEvents);
    }

    @Import(keyResId = R.string.preferenceKeySubTopic)
    private void setSubTopic(String string) {
        setString(R.string.preferenceKeySubTopic, string);
    }

    @Import(keyResId = R.string.preferenceKeyAutostartOnBoot)
    private void setAutostartOnBoot(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyAutostartOnBoot, aBoolean);

    }

    @Import(keyResId = R.string.preferenceKeyLocatorInterval)
    private void setLocatorInterval(int anInt) {
        setInt(R.string.preferenceKeyLocatorInterval, anInt);
    }

    @Import(keyResId = R.string.preferenceKeyLocatorDisplacement)
    private void setLocatorDisplacement(int anInt) {
        setInt(R.string.preferenceKeyLocatorDisplacement, anInt);

    }

    @Import(keyResId = R.string.preferenceKeyLocatorPriority)
    private void setLocatorPriority(int anInt) {
        if(anInt >= 0 && anInt <= 3) {
            setInt(R.string.preferenceKeyLocatorPriority, anInt);
        } else {
            Timber.e("invalid locator priority specified %s", anInt);
        }
    }

    @Import(keyResId = R.string.preferenceKeyPubRetain)
    private void setPubRetain(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyPubRetain, aBoolean);

    }

    @Import(keyResId = R.string.preferenceKeyPubQos)
    private void setPubQos(int anInt) {
        setInt(R.string.preferenceKeyPubQos, anInt);
    }


    @Import(keyResId = R.string.preferenceKeySubQos)
    private void setSubQos(int anInt) {
        setInt(R.string.preferenceKeySubQos, Math.min(anInt, 2));
    }


    @Import(keyResId = R.string.preferenceKeyPassword)
    public void setPassword(String password) {
        setString(R.string.preferenceKeyPassword, password);
    }

    @Import(keyResId = R.string.preferenceKeyDeviceId)
    public void setDeviceId(String deviceId) {
        setString(R.string.preferenceKeyDeviceId, deviceId);
    }

    @Import(keyResId = R.string.preferenceKeyTLS)
    public void setTls(boolean tlsSpecifier) {
        setBoolean(R.string.preferenceKeyTLS, tlsSpecifier);
    }

    @Import(keyResId = R.string.preferenceKeyWS)
    public void setWs(boolean wsEnable) {
        setBoolean(R.string.preferenceKeyWS, wsEnable);
    }

    public void setTlsCaCrt(String name) {
        setString(R.string.preferenceKeyTLSCaCrt, name);
    }

    public void setTlsClientCrt(String name) {
        setString(R.string.preferenceKeyTLSClientCrt, name);
    }

    @Export(keyResId = R.string.preferenceKeyHost, exportModeMqttPrivate = true)
    public String getHost() {
        return getStringOrDefault(R.string.preferenceKeyHost, R.string.valEmpty);
    }

    @Export(keyResId = R.string.preferenceKeyPassword, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public String getPassword() {
        return getStringOrDefault(R.string.preferenceKeyPassword, R.string.valEmpty);
    }

    @Export(keyResId = R.string.preferenceKeyTLS, exportModeMqttPrivate = true)
    public boolean getTls() {
        return getBooleanOrDefault(R.string.preferenceKeyTLS, R.bool.valTls);
    }

    @Export(keyResId = R.string.preferenceKeyWS, exportModeMqttPrivate = true)
    public boolean getWs() {
        return getBooleanOrDefault(R.string.preferenceKeyWS, R.bool.valWs);
    }

    @Export(keyResId = R.string.preferenceKeyTLSCaCrt, exportModeMqttPrivate = true)
    public String getTlsCaCrtName() {
        return getStringOrDefault(R.string.preferenceKeyTLSCaCrt, R.string.valEmpty);
    }

    @Export(keyResId = R.string.preferenceKeyTLSClientCrt, exportModeMqttPrivate = true)
    public String getTlsClientCrtName() {
        return getStringOrDefault(R.string.preferenceKeyTLSClientCrt, R.string.valEmpty);
    }

    @Export(keyResId = R.string.preferenceKeyNotificationHigherPriority, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getNotificationHigherPriority() {
        return getBooleanOrDefault(R.string.preferenceKeyNotificationHigherPriority, R.bool.valNotificationHigherPriority);
    }

    @Export(keyResId = R.string.preferenceKeyNotificationLocation, exportModeMqttPrivate = true)
    public boolean getNotificationLocation() {
        return getBooleanOrDefault(R.string.preferenceKeyNotificationLocation, R.bool.valNotificationLocation);
    }

    public boolean getNotificationEvents() {
        return getBooleanOrDefault(R.string.preferenceKeyNotificationEvents, R.bool.valNotificationEvents);
    }

    @Export(keyResId = R.string.preferenceKeyPubQos, exportModeMqttPrivate = true)
    public int getPubQos() {
        return getIntOrDefault(R.string.preferenceKeyPubQos, R.integer.valPubQos);
    }

    @Export(keyResId = R.string.preferenceKeySubQos, exportModeMqttPrivate = true)
    public int getSubQos() {
        return getIntOrDefault(R.string.preferenceKeySubQos, R.integer.valSubQos);
    }

    @Export(keyResId = R.string.preferenceKeyPubRetain, exportModeMqttPrivate = true)
    public boolean getPubRetain() {
        return getBooleanOrDefault(R.string.preferenceKeyPubRetain, R.bool.valPubRetain);
    }

    @Export(keyResId = R.string.preferenceKeyAutostartOnBoot, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getAutostartOnBoot() {
        return getBooleanOrDefault(R.string.preferenceKeyAutostartOnBoot, R.bool.valAutostartOnBoot);
    }

    public boolean getInfo() {
        return getBooleanOrDefault(R.string.preferenceKeyInfo, R.bool.valInfo);
    }

    @Import(keyResId = R.string.preferenceKeyInfo)
    public void setInfo(boolean info) {
        setBoolean(R.string.preferenceKeyInfo, info);
    }

    public String getTlsClientCrtPassword() {
        return getStringOrDefault(R.string.preferenceKeyTLSClientCrtPassword, R.string.valEmpty);
    }

    @Import(keyResId = R.string.preferenceKeyURL)
    public void setUrl(String url) {
        setString(R.string.preferenceKeyURL, url);
    }

    @Export(keyResId = R.string.preferenceKeyURL, exportModeHttpPrivate = true)
    public String getUrl() {
        return getStringOrDefault(R.string.preferenceKeyURL, R.string.valEmpty);
    }

    public void setTlsClientCrtPassword(String password) {
        setString(R.string.preferenceKeyTLSClientCrtPassword, password);
    }

    String getEncryptionKey() {
        return getStringOrDefault(R.string.preferenceKeyEncryptionKey, R.string.valEmpty);
    }

    public boolean isSetupCompleted() {
        // sharedPreferences because the value is independent from the selected mode
        return !sharedPreferences.getBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), false);
    }

    public void setSetupCompleted() {
        sharedPreferences.edit().putBoolean(getPreferenceKey(R.string.preferenceKeySetupNotCompleted), false).apply();
        isFirstStart = false;
    }

    // Maybe make this configurable
    // For now it makes things easier to change
    public int getPubQosEvents() {
        return getPubQos();
    }

    public boolean getPubRetainEvents() {
        return false;
    }

    public int getPubQosWaypoints() {
        return 0;
    }

    public boolean getPubRetainWaypoints() {
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
        cfg.set(getPreferenceKey(R.string.preferenceKeyVersion), BuildConfig.VERSION_CODE);
        for (Method m : methods) {
            m.setAccessible(true);
            try {
                cfg.set(getPreferenceKey(m.getAnnotation(Export.class).keyResId()), m.invoke(this));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return cfg;
    }

    @Export(keyResId = R.string.preferenceKeyFusedRegionDetection, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getFuseRegionDetection() {
        return getBooleanOrDefault(R.string.preferenceKeyFusedRegionDetection, R.bool.valTrue);
    }

    @Import(keyResId = R.string.preferenceKeyFusedRegionDetection)
    public void setFusedRegionDetection(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyFusedRegionDetection, aBoolean);
    }

    @Export(keyResId = R.string.preferenceKeyDebugLog, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getLogDebug() {
        return getBooleanOrDefault(R.string.preferenceKeyDebugLog, R.bool.valFalse);
    }

    @Import(keyResId = R.string.preferenceKeyDebugLog)
    public void setDebugLog(boolean debug) {
        setBoolean(R.string.preferenceKeyDebugLog, debug);
    }


    public boolean isObjectboxMigrated() {
        return isFirstStart || sharedPreferences.getBoolean(getPreferenceKey(R.string.preferenceKeyObjectboxMigrated), false);
    }

    public void setObjectBoxMigrated() {
        sharedPreferences.edit().putBoolean(getPreferenceKey(R.string.preferenceKeyObjectboxMigrated), true).apply();
    }

    @Export(keyResId = R.string.preferenceKeyGeocodeEnabled, exportModeMqttPrivate = true, exportModeHttpPrivate = true)
    public boolean getGeocodeEnabled() {
        return getBooleanOrDefault(R.string.preferenceKeyGeocodeEnabled, R.bool.valGeocodeEnabled);
    }

    @Import(keyResId = R.string.preferenceKeyGeocodeEnabled)
    public void setGeocodeEnabled(boolean aBoolean) {
        setBoolean(R.string.preferenceKeyGeocodeEnabled, aBoolean);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @SuppressWarnings({"unused", "WeakerAccess"})
    public @interface Export {
        int keyResId() default 0;

        boolean exportModeMqttPrivate() default false;

        boolean exportModeHttpPrivate() default false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Import {
        int keyResId() default 0;
    }

    private List<Method> getExportMethods() {
        int modeId = getModeId();
        final List<Method> methods = new ArrayList<>();
        Class<?> klass = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and insert those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Export.class)) {
                    Export annotInstance = method.getAnnotation(Export.class);
                    if (modeId == MessageProcessorEndpointMqtt.MODE_ID && annotInstance.exportModeMqttPrivate() || modeId == MessageProcessorEndpointHttp.MODE_ID && annotInstance.exportModeHttpPrivate()) {
                        methods.add(method);
                    }
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }

    public List<String> getImportKeys() {
        return new ArrayList<>(getImportMethods().keySet());
    }

    private HashMap<String, Method> getImportMethods() {
        final HashMap<String, Method> methods = new HashMap<>();
        Class<?> klass = Preferences.class;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and insert those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(Import.class)) {
                    methods.put(getPreferenceKey(method.getAnnotation(Import.class).keyResId()), method);
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }
}
