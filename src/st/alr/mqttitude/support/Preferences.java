package st.alr.mqttitude.support;

import java.util.concurrent.TimeUnit;

import com.google.android.gms.location.LocationRequest;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences {

    
    
    public static SharedPreferences getSharedPreferences(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext());
    }
    
    public static String getKey(int resId){
        return App.getContext().getString(resId);
    }

    public static boolean getBoolean(int resId, int defId){
        return getSharedPreferences().getBoolean(getKey(resId), App.getContext().getResources().getBoolean(defId));
    }
    public static int getInt(int resId, int defId){
        return getSharedPreferences().getInt(getKey(resId), App.getContext().getResources().getInteger(defId));
    }
    public static String getString(int resId, int defId){
        String s = getSharedPreferences().getString(getKey(resId), App.getContext().getResources().getString(defId));
        if(s == null || s.equals(""))
            s = App.getContext().getResources().getString(defId);
        
        return s;
    }
    public static void setString(int resId, String value){
        getSharedPreferences().edit().putString(getKey(resId), value).apply();
    }
    
    public static void setInt(int resId, int value) {
        getSharedPreferences().edit().putInt(getKey(resId), value).apply();
    }

    
    public static String getAndroidId(){
        return App.getAndroidId();
    }
    
    public static boolean isAdvancedModeEnabled() {
        return getBoolean(R.string.keyAdvancedConnectionPreferencesEnabled, R.bool.valAdvancedConnectionPreferencesEnabled);
    }


    public static boolean includeBattery() {
        return getBoolean(R.string.keyPubIncludeBattery, R.bool.valPubIncludeBattery);
    }

    public static boolean isSubEnabled() {
        return getBoolean(R.string.keySubEnabled, R.bool.valSubEnabled);
    }

    public static boolean isContactLinkCloudStorageEnabled() {
        return getBoolean(R.string.keyContactsLinkCloudStorageEnabled, R.bool.valContactsLinkCloudStorageEnabled);
    }

    public static String getTrackingUsername() {
        return getString(R.string.keyTrackingUsername, R.string.valEmpty);
    }

    public static int getLocatorBackgroundDisplacement() {
        return Integer.parseInt(getString(R.string.keyLocatorBackgroundDisplacement, R.string.valLocatorBackgroundDisplacement));
    }

    public static long getLocatorBackgroundInterval() {
        return TimeUnit.MINUTES.toMillis(Long.parseLong(getString(R.string.keyLocatorBackgroundInterval, R.string.valLocatorBackgroundInterval)));
    }

    
    public static void setTrackingUsername(String topic) {
        setString(R.string.keyTrackingUsername, topic);
    }

    
    public static String getBrokerUsername() {
        return getString(R.string.keyBrokerUsername, R.string.valEmpty);
    }

    public static int getBrokerAuthType() {
        return getInt(R.string.keyBrokerAuth, R.integer.valBrokerAuthTypeUsernamePw);

    }
    
    public static String getDeviceName(boolean androidIdFallback)
    {
        String name = getString(R.string.keyDeviceName, R.string.valEmpty);
        if (name.equals("") && androidIdFallback)
            name = App.getAndroidId();
        return name;
    }

    public static String getSubTopic(boolean defaultTopicFallback) {
        String topic = getString(R.string.keySubTopic, R.string.valSubTopic);
        if (topic.equals("") && defaultTopicFallback)
            topic = App.getContext().getString(R.string.valSubTopic);
        return topic;
    }

    public static String getPubTopic(boolean defaultFallback) {
        String topic = getString(R.string.keyPubTopic, R.string.valEmpty);
        if (topic.equals("") && defaultFallback)
            topic = getPubTopicFallback();

        return topic;
    }

    public static String getWaypointPubTopicPart() {
        return App.getContext().getString(R.string.valPubTopicWaypointsPart);
    }

    
    public static String getPubTopicFallback() {
        String deviceName = getDeviceName(true);
        String userUsername = getBrokerUsername();

        return deviceName.equals("") || userUsername.equals("") ? "" : String.format(App.getContext().getString(R.string.valPubTopic), userUsername, deviceName);
    }

    public static String getBrokerHost() {
        return getString(R.string.keyBrokerHost, R.string.valBrokerHost);
    }

    public static String getBrokerPort() {
        return getString(R.string.keyBrokerPort, R.string.valBrokerPort);
    }

    public static String getBrokerPassword() {
        return getString(R.string.keyBrokerPassword, R.string.valEmpty);
    }

    public static int getBrokerSecurityType() {
        return getInt(R.string.keyBrokerSecurity, R.integer.valBrokerSecurityTypeTls);
    }

    public static String getBrokerSslCaPath() {
        return getString(R.string.keyBrokerSecuritySslCaPath, R.string.valEmpty);
    }

    public static boolean isNotificationEnabled() {
        return getBoolean(R.string.keyNotificationEnabled, R.bool.valNotificationEnabled);
    }

    public static boolean notificationOnGeofenceTransition() {
        return getBoolean(R.string.keyNotificationTickerGeofence, R.bool.valNotificationTickerGeofence);
    }

    public static boolean notificationTickerOnPublish() {
        return getBoolean(R.string.keyNotificationTickerPublish, R.bool.valNotificationTickerPublish);
    }

    public static boolean isNotificationGeocoderEnabled(){
        return getBoolean(R.string.keyNotificationGeocoderEnabled, R.bool.valNotificationGeocoderEnabled);
    }
    
    public static boolean isNotificationLocationEnabled() {
        return getBoolean(R.string.keyNotificationLocationEnabled, R.bool.valNotificationLocationEnabled);
    }


    
    public static int getPubQos() {
        try {
            return Integer.parseInt(getString(R.string.keyPubQos, R.string.valPubQos));
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean getPubRetain() {
        return getBoolean(R.string.keyPubRetain, R.bool.valPubRetain);
    }
    
    public static int getPubAutoInterval(){
        int ui;
        try {
            ui = Integer.parseInt(getString(R.string.keyPubAutoInterval, R.string.valPubAutoInterval));
        } catch (Exception e) {
            ui = 30;
        }

        return ui;

    }

    public static boolean isPubAutoEnabled() {
        return getBoolean(R.string.keyPubAutoEnabled, R.bool.valPubAutoEnabled);
    }

    public static boolean isAutostartOnBootEnabled() {
        return getBoolean(R.string.keyAutostartOnBootEnabled, R.bool.valAutostartOnBootEnabled);
    }

    
    public static String getBugsnagApiKey(){
        return App.getContext().getString(R.string.valBugsnagApiKey);
    }
    public static String getRepoUrl(){
        return App.getContext().getString(R.string.valRepoUrl);
        
    }
    public static String getIssuesMail(){
        return App.getContext().getString(R.string.valIssuesMail);
        
    }
    public static String getTwitterUrl(){
        return App.getContext().getString(R.string.valTwitterUrl);
        
    }
    public static String getBitcoinAddress(){
        return App.getContext().getString(R.string.valBitcoinAddress);
        
    }

    public static CharSequence getBrokerPortDefault() {
        return App.getContext().getString(R.string.valBrokerPort);
    }

    public static int getLocatorForegroundAccuracy(){
        return getLocatorAccuracy(Integer.parseInt(getString(R.string.keyLocatorAccuracyForeground, R.string.valLocatorAccuracyForeground)));
    }
    public static int getLocatorBackgroundAccuracy(){
        return getLocatorAccuracy(Integer.parseInt(getString(R.string.keyLocatorAccuracyBackground, R.string.valLocatorAccuracyBackground)));
 
    }
    
    private static int getLocatorAccuracy(int val){
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
    
    private static String b2s(boolean b){
        return b? "1" : "0";
    }
    
    // WORK IN PROGRESS for https://github.com/owntracks/android/issues/6
    private static void export() {
        StringBuilder sb = new StringBuilder();

        //        {
        //            "_type": "configuration",
        sb.append("{\"_type\":\"configuration\",");        
        //            "deviceid": "phone",
        sb.append("{\"deviceid\":\"").append(getDeviceName(true)).append("\",");        
        //            "clientid": "jane-phone",
        sb.append("{\"clientid\":\"").append(getDeviceName(true)).append("\",");        
        //            "subscription": "mqttitude/#",
        sb.append("{\"subscription\":\"").append(getSubTopic(true)).append("\",");        
        //            "topic": "mqttitude/jane/phone",
        sb.append("{\"topic\":\"").append(getPubTopic(true)).append("\",");        
        //            "host": "broker.my.net",
        sb.append("{\"host\":\"").append(getBrokerHost()).append("\",");        
        //            "user": "jane",
        sb.append("{\"user\":\"").append(getBrokerUsername()).append("\",");        
        //            "subscriptionqos": "1",
        sb.append("{\"qos\":\"").append(getPubQos()).append("\",");        
        //            "port": "8883",
        sb.append("{\"port\":\"").append(getBrokerPort()).append("\",");        
        //            "retain": "1",
        sb.append("{\"retain\":\"").append(b2s(getPubRetain())).append("\",");        
        //            "tls": "1",
        sb.append("{\"tls\":\"").append(b2s(getBrokerSecurityType() > 0)).append("\",");        
        //            "auth": "1",
        sb.append("{\"auth\":\"").append(b2s(getBrokerAuthType() > 0)).append("\",");        
        //            "mindist": "200",
        sb.append("{\"mindist\":\"").append("tbd").append("\",");        
        //            "mintime": "180",
        sb.append("{\"mintime\":\"").append("tbd").append("\",");        
        //            "monitoring": "1",
        sb.append("{\"monitoring\":\"").append("tbd").append("\",");        
        //            "ab": "0"
        sb.append("{\"ab\":\"").append("tbd").append("\",");        
        //        }
        sb.append("}");

    }
    
    
}
