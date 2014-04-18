package st.alr.mqttitude.support;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.LocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.model.ConfigurationMessage;

public class Preferences {
    public static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(App.getContext());
	}

	public static String getKey(int resId) {
		return App.getContext().getString(resId);
	}

	public static boolean getBoolean(int resId, int defId) {
		return getSharedPreferences().getBoolean(getKey(resId),getBooleanRessource(defId));
	}

    public static boolean getBooleanRessource(int resId) {
        return App.getContext().getResources().getBoolean(resId);
    }

	public static int getInt(int resId, int defId) {
		return getSharedPreferences().getInt(getKey(resId), getIntResource(defId));
	}

    public static int getIntResource(int resId) {
        return App.getContext().getResources().getInteger(resId);
    }

	public static String getString(int resId, int defId) {
		String s = getSharedPreferences().getString(getKey(resId), getStringRessource(defId));
		if ((s == null) || s.equals(""))
			s = getStringRessource(defId);

		return s;
	}

    public static String getStringRessource(int resId) {
        return App.getContext().getResources().getString(resId);
    }


	public static void setString(int resId, String value) {
		getSharedPreferences().edit().putString(getKey(resId), value).commit();
	}

	public static void setInt(int resId, int value) {
		getSharedPreferences().edit().putInt(getKey(resId), value).commit();
	}
    public static void setBoolean(int resId, boolean value) {
        getSharedPreferences().edit().putBoolean(getKey(resId), value).commit();
    }

    public static String getAndroidId() {
		return App.getAndroidId();
	}

	public static boolean getConnectionAdvancedMode() {
		return getBoolean(R.string.keyConnectionAdvancedMode, R.bool.valConnectionAdvancedMode);
	}

	public static boolean getPubIncludeBattery() {
		return getBoolean(R.string.keyPubIncludeBattery,
				R.bool.valPubIncludeBattery);
	}

	public static boolean getSub() {
		return getBoolean(R.string.keySub, R.bool.valSub);
	}

	public static boolean getUpdateAdressBook() {
		return getBoolean(R.string.keyUpdateAddressBook,
				R.bool.valUpdateAddressBook);
	}

	public static String getTrackingUsername() {
		return getString(R.string.keyTrackingUsername, R.string.valEmpty);
	}

    public static void setTrackingUsername(String topic) {
        setString(R.string.keyTrackingUsername, topic);
    }

	public static int getLocatorDisplacement() {
        return getInt(R.string.keyLocatorDisplacement, R.integer.valLocatorDisplacement);
	}

	public static long getLocatorInterval() {
		return TimeUnit.MINUTES.toMillis(getInt(
                R.string.keyLocatorInterval,
                R.integer.valLocatorInterval));
	}



	public static String getBrokerUsername() {
		return getString(R.string.keyUsername, R.string.valEmpty);
	}

	public static boolean getAuth() {
		return getBoolean(R.string.keyAuth, R.bool.valAuth);

	}

	public static String getDeviceId(boolean androidIdFallback) {
		String name = getString(R.string.keyDeviceId, R.string.valEmpty);
		if (name.equals("") && androidIdFallback)
			name = App.getAndroidId();
		return name;
	}

    public static boolean getZeroLenghClientId(){
        return getBoolean(R.string.keyZeroLenghClientIdEnabled, R.bool.valZeroLenghClientIdEnabled);
    }

    public static void setZeroLenghClientId(boolean enabled) {
        Preferences.setBoolean(R.string.keyZeroLenghClientIdEnabled, enabled);
    }

    public static String getClientId(boolean androidIdFallback) {
        String name = getString(R.string.keyClientId, R.string.valEmpty);
        if (name.equals("") && androidIdFallback)
            name = App.getAndroidId();
        return name;
    }

    public static void setClientId(String clientId) {
        setString(R.string.keyClientId, clientId);
    }

	public static String getSubTopic(boolean defaultTopicFallback) {
		String topic = getString(R.string.keySubTopic, R.string.valSubTopic);
		if (topic.equals("") && defaultTopicFallback)
			topic = App.getContext().getString(R.string.valSubTopic);
		return topic;
	}

	public static String getPubTopicBase(boolean defaultFallback) {
		String topic = getString(R.string.keyPubTopicBase, R.string.valEmpty);
		if (topic.equals("") && defaultFallback)
			topic = getPubTopicFallback();

		return topic;
	}

	public static String getPubTopicPartWaypoints() {
		return getStringRessource(R.string.valPubTopicPartWaypoints);
	}

	public static String getPubTopicFallback() {
		String deviceId = getDeviceId(true);
		String userUsername = getBrokerUsername();

		return deviceId.equals("") || userUsername.equals("") ? "" : String.format(getStringRessource(R.string.valPubTopicBase), userUsername, deviceId);
	}

	public static void setHost(String value) {
		if (!value.equals(getBrokerHost())) {
			brokerChanged();
			setString(R.string.keyHost, value);
		}
	}

	public static void setPort(int value) {
		if (value != getPort()) {
			setInt(R.string.keyPort, value);
			brokerChanged();
		}
	}

    public static int getPort() {
        return getInt(R.string.keyPort, R.integer.valPort);
    }


    public static void setKeepalive(int value) {
        setInt(R.string.keyKeepalive, value);
    }
    public static int getKeepalive() {
        return getInt(R.string.keyKeepalive, R.integer.valKeepalive);
    }

	public static void setUsername(String value) {
		if (!value.equals(getBrokerHost())) {

			setString(R.string.keyUsername, value);
			brokerChanged();
		}
	}

	private static void brokerChanged() {
		Log.v("Preferences", "broker changed");
		EventBus.getDefault().post(new Events.BrokerChanged());
	}

	public static String getBrokerHost() {
		return getString(R.string.keyHost, R.string.valHost);
	}

	public static int getBrokerPort() {
		return getInt(R.string.keyPort, R.integer.valPort);
	}

	public static String getBrokerPassword() {
		return getString(R.string.keyPassword, R.string.valEmpty);
	}

	public static int getTls() {
		return getInt(R.string.keyTls, R.integer.valTls);
	}

	public static String getTlsCrtPath() {
		return getString(R.string.keyTlsCrtPath, R.string.valEmpty);
	}

	public static boolean getNotification() {
		return getBoolean(R.string.keyNotification,
				R.bool.valNotification);
	}

	public static boolean getNotificationTickerOnWaypointTransition() {
		return getBoolean(R.string.keyNotificationTickerOnWaypointTransition,
				R.bool.valNotificationTickerOnWaypointTransition);
	}

	public static boolean getNotificationTickerOnPublish() {
		return getBoolean(R.string.keyNotificationTickerOnPublish,
				R.bool.valNotificationTickerOnPublish);
	}

	public static boolean getNotificationGeocoder() {
		return getBoolean(R.string.keyNotificationGeocoder,
				R.bool.valNotificationGeocoder);
	}

	public static boolean getNotificationLocation() {
		return getBoolean(R.string.keyNotificationLocation,
				R.bool.valNotificationLocation);
	}

	public static int getPubQos() {
			return getInt(
                    R.string.keyPubQos,
					R.integer.valPubQos);
	}

	public static boolean getPubRetain() {
		return getBoolean(R.string.keyPubRetain, R.bool.valPubRetain);
	}

	public static int getPubInterval() {
        return getInt(R.string.keyPubInterval, R.integer.valPubInterval);
	}

	public static boolean getPub() {
		return getBoolean(R.string.keyPub, R.bool.valPub);
	}

	public static boolean getAutostartOnBoot() {
		return getBoolean(R.string.keyAutostartOnBoot,
				R.bool.valAutostartOnBoot);
	}

	public static String getBugsnagApiKey() {
		return App.getContext().getString(R.string.valBugsnagApiKey);
	}

	public static String getRepoUrl() {
		return App.getContext().getString(R.string.valRepoUrl);

	}

	public static String getIssuesMail() {
		return App.getContext().getString(R.string.valIssuesMail);

	}

	public static String getTwitterUrl() {
		return App.getContext().getString(R.string.valTwitterUrl);

	}

	public static String getBitcoinAddress() {
		return App.getContext().getString(R.string.valBitcoinAddress);

	}

	public static int getPortDefault() {
		return getIntResource(R.integer.valPort);
	}

	public static int getLocatorAccuracyForeground() {
        return getInt(R.string.keyLocatorAccuracyForeground, R.integer.valLocatorAccuracyForeground);
	}

	public static int getLocatorAccuracyBackground() {
        return getInt(R.string.keyLocatorAccuracyBackground, R.integer.valLocatorAccuracyBackground);
	}

	private static int getLocatorAccuracy(int val) {
		switch (val) {
		case 0:
			return LocationRequest.PRIORITY_HIGH_ACCURACY;
		case 1:
			return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
		case 2:
			return LocationRequest.PRIORITY_LOW_POWER;
		case 3:
			return LocationRequest.PRIORITY_NO_POWER;
		default:
			return LocationRequest.PRIORITY_HIGH_ACCURACY;
		}
	}

    public static boolean canConnect() {

        return     !getBrokerHost().equals("")
                && !getBrokerUsername().equals("")
                && ((getAuth() && !getBrokerPassword().equals("")) || !getAuth());
    }

    private static void addJsonValue(HashMap<String, String> map, JSONObject json, String key) {
        try {
            map.put(key, json.getString(key));
        } catch (JSONException e) {
            map.put(key, null);
        }
    }

    private static void fromJson(JSONObject json) {
        if(!Defaults.isPropperMessageType(json, "configuration"))
            return;

        HashMap<String, String> values = new HashMap<String, String>();

//      "deviceId": "phone",
        addJsonValue(values, json, "deviceId");
//      "clientId": "jane-phone",
        addJsonValue(values, json, "clientId");
//      "subTopic": "owntracks/#",
        addJsonValue(values, json, "subTopic");
//      "pubTopicBase": "owntracks/jane/phone",
        addJsonValue(values, json, "pubTopicBase");
//      "host": "broker.my.net",
        addJsonValue(values, json, "host");
//      "username": "jane",
        addJsonValue(values, json, "username");
//      "password": "secr3t",
        addJsonValue(values, json, "password");
//      "pubQos": "2",
        addJsonValue(values, json, "pubQos");
//      "port": "8883",
        addJsonValue(values, json, "port");
//      "keepalive": "60",
        addJsonValue(values, json, "keepalive");
//      "pubRetain": "1",
        addJsonValue(values, json, "pubRetain");
//      "tls": "1",
        addJsonValue(values, json, "tls");
//      "tlsCrtPath" : "/foo/bar",
        addJsonValue(values, json, "tlsCrtPath");
//      "auth": "1",
        addJsonValue(values, json, "auth");
//      "locatorDisplacement": "200",
        addJsonValue(values, json, "locatorDisplacement");
//      "locatorInterval": "180",
        addJsonValue(values, json, "locatorInterval");
//      "connectionAdvancedMode" : "0",            "comment": "Android only",
        addJsonValue(values, json, "connectionAdvancedMode");
//      "pubIncludeBattery" : "0",                 "comment": "Android only, in IOS alway on",
        addJsonValue(values, json, "pubIncludeBattery");
//      "sub" : "0",                               "comment": "Android only, subscription enabled for contacts, in IOS always subscribed",
        addJsonValue(values, json, "sub");
//      "pub" : "0",                               "comment": "Android only, auto publish, in IOS controlled by 'monitoring'",
        addJsonValue(values, json, "pub");
//      "updateAddressBook" : "0",                 "comment": "was 'ab' in IOS pre 6.2",
        addJsonValue(values, json, "updateAddressBook");
//      "notification" : "0",                      "comment": "Android only, show notifications",
        addJsonValue(values, json, "notification");
//      "notificationLocation" : "0",              "comment": "Android only, show last reported location in notification, off in IOS",
        addJsonValue(values, json, "notificationLocation");
//      "notificationGeocoder" : "0",              "comment": "Android only, resolve location in notification to address, in IOS only resolved when in show details",
        addJsonValue(values, json, "notificationGeocoder");
//      "notificationTickerOnPublish" : "0",       "comment": "Android only, show a ticker on successful publishes, always off in IOS",
        addJsonValue(values, json, "notificationTickerOnPublish");
//      "notificationTickerOnGeofenceTransition" : "0", "comment": "Android only, show a ticker when the devices enters or leaves a geofence, always on in IOS"
        addJsonValue(values, json, "notificationTickerOnGeofenceTransition");

    }


    private static JSONObject toJson(){
        return null;
    }

    public static int getPreferencesVersion() {
        return getInt(R.string.keyPreferencesVersion, 0);
    }

    public static void setPreferencesVersion(int preferencesVersion) {
        setInt(R.string.keyPreferencesVersion, preferencesVersion);
    }

    public static void setPassword(String password) {
        setString(R.string.keyPassword, password);
    }

    public static void setDeviceId(String deviceId) {
        setString(R.string.keyDeviceId, deviceId);
    }

    public static void setAuth(boolean auth) {
        setBoolean(R.string.keyAuth, auth);
    }

    public static void setTls(int tlsSpecifier) {
        setInt(R.string.keyTls, tlsSpecifier);
    }

    public static void setTlsCrtPath(String tlsCrtPath) {
        setString(R.string.keyTlsCrtPath, tlsCrtPath);
    }

    //TODO
    public static ConfigurationMessage getConfigurationMessage() {
        return null;
    }
}
