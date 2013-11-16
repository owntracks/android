
package st.alr.mqttitude.preferences;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import de.greenrobot.event.EventBus;

public class ActivityPreferences extends PreferenceActivity {
    private static Preference serverPreference;
    private static Preference backgroundUpdatesIntervall;
    private static Preference version;
    private static Preference repo;
    private static Preference mail;
    
    private static Preference advanced;
    private static PreferenceScreen topicScreen;
    private static EditTextPreference topic;

    
    static String ver;

    public static boolean isAdvancedModeEnabled(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getBoolean("advancedMode", false);
    }
    
    public static String getAndroidId() {
        
        String id = ServiceApplication.getAndroidId();

        // MQTT specification doesn't allow client IDs longer than 23 chars
        if (id.length() > 22)
            id = id.substring(0, 22);
        
        return id;
    }
    
    public static String getTrackingUsername(){
        String t = PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(Defaults.SETTINGS_KEY_TRACKING, "");
        return t;
    }
    
    public static void setTrackingUsername(String topic){
        Log.v("ActivityPreferences", "Now tracking " + topic);
        PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putString(Defaults.SETTINGS_KEY_TRACKING, topic).apply();
    }


    
    public static String getUserUsername(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(Defaults.SETTINGS_KEY_USER_USERNAME, "");
    }

    public static int getBrokerAuthType(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getInt(Defaults.SETTINGS_KEY_BROKER_AUTH, Defaults.VALUE_BROKER_AUTH_USERUSERNAME);

    }

    public static String getBrokerUsername(boolean userUsernameFallback)
    {
        String username = PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, "");
        if(username.equals("") && userUsernameFallback) 
            username = getUserUsername();
        return username;        
    }
    
    
    public static String getDeviceName(boolean androidIdFallback)
    {
        String name = PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString(Defaults.SETTINGS_KEY_BROKER_DEVICE_NAME, "");
        if(name.equals("") && androidIdFallback) 
            name = getAndroidId();
        return name;        
    }
    
    public static String getTopicFallback(){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        String deviceName = getDeviceName(true);
        String userUsername = getUserUsername();
        
        return deviceName.equals("") || userUsername.equals("") ? "" : String.format(Defaults.VALUE_TOPIC, userUsername, deviceName);
    }
    
    public static String getTopic(boolean defaultFallback){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        String topic = p.getString(Defaults.SETTINGS_KEY_TOPIC, "");
        if(topic.equals("") && defaultFallback)
            topic = getTopicFallback();

        return topic;        
      }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Thanks Google for not providing a support version of the
        // PreferenceFragment for older API versions
        if (supportsFragment())
            onCreatePreferenceFragment();
        else
            onCreatePreferenceActivity();
    }

    private boolean supportsFragment() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    @SuppressWarnings("deprecation")
    private void onCreatePreferenceActivity() {
        addPreferencesFromResource(R.xml.preferences);
        onSetupPreferenceActivity();
    }

    @SuppressWarnings("deprecation")
    private void onSetupPreferenceActivity() {
        repo = findPreference("repo");
        mail = findPreference("mail");
        version = findPreference("versionReadOnly");
        serverPreference = findPreference("brokerPreference");
        backgroundUpdatesIntervall = findPreference(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL);
        topicScreen = (PreferenceScreen) findPreference("topicSettings");
        topic = (EditTextPreference) findPreference("topic");

        onSetupCommon(this);
    }

    @TargetApi(11)
    private void onCreatePreferenceFragment() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new CustomPreferencesFragment()).commit();
    }

    @TargetApi(11)
    private static void onSetupPreferenceFragment(PreferenceFragment f) {
        repo = f.findPreference("repo");
        mail = f.findPreference("mail");
        version = f.findPreference("versionReadOnly");
        serverPreference = f.findPreference("brokerPreference");
        backgroundUpdatesIntervall = f
                .findPreference(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL);
        topicScreen = (PreferenceScreen) f.findPreference("topicSettings");
        topic = (EditTextPreference) f.findPreference(Defaults.SETTINGS_KEY_TOPIC);

        onSetupCommon(f.getActivity());
    }
    
    private static void onSetupCommon(final Activity a) {
        PackageManager pm = a.getPackageManager();
        try {
            ver = pm.getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            ver = a.getString(R.string.na);
        }

        
        backgroundUpdatesIntervall.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.v(this.toString(), newValue.toString());
                if (newValue.toString().equals("0")) {
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(a).edit();
                    editor.putString(preference.getKey(), "1");
                    editor.commit();
                    return false;
                }
                return true;
            }
        });

        version.setSummary(ver);

        repo.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Defaults.VALUE_REPO_URL));
                        a.startActivity(intent);
                        return false;
                    }
                });

        mail.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("message/rfc822");

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                            Defaults.VALUE_ISSUES_MAIL
                        });
                        intent.putExtra(Intent.EXTRA_SUBJECT, "MQTTitude (Version: " + ver + ")");
                        a.startActivity(Intent.createChooser(intent, "Send Email"));
                        return false;
                    }
                });

        
        setServerPreferenceSummary();
        //TODO: set hint when device name changes
        
        
        backgroundUpdatesIntervall.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.v(this.toString(), newValue.toString());
                if (newValue.toString().equals("0")) {
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(a).edit();
                    editor.putString(preference.getKey(), "1");
                    editor.commit();
                    return false;
                }
                return true;
            }
        });
        
        
        OnPreferenceChangeListener topicListener = new OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(preference.equals(Defaults.SETTINGS_KEY_USER_USERNAME) || preference.equals(Defaults.SETTINGS_KEY_USER_USERNAME)) {
                    topic.getEditText().setHint(getTopicFallback());                      
                }
                return true;
            }
        };
        
                Log.v("prefs", "Topic fallback: " + getTopicFallback());
        topic.getEditText().setHint(getTopicFallback());

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @TargetApi(11)
    public static class CustomPreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            onSetupPreferenceFragment(this);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onEventMainThread(Events.StateChanged.ServiceMqtt event) {
        setServerPreferenceSummary();
    }

    private static void setServerPreferenceSummary() {
        serverPreference.setSummary(ServiceMqtt.getStateAsString());
    }

    


    @Override
    public boolean onIsMultiPane() {
        return false;
    }

}
