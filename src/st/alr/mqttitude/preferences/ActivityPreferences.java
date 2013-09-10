
package st.alr.mqttitude.preferences;

import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.R;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import de.greenrobot.event.EventBus;

public class ActivityPreferences extends PreferenceActivity {
    private static Preference serverPreference;
    private static Preference backgroundUpdatesIntervall;
    private static Preference version;
    private static PreferenceActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        
        // Thanks Google for not providing a support version of the PreferenceFragment for older API versions
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
        version = findPreference("versionReadOnly");
        serverPreference = findPreference("brokerPreference");
        backgroundUpdatesIntervall = findPreference(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL);
        onSetupCommon();
    }

    @TargetApi(11)
    private void onCreatePreferenceFragment() {
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new CustomPreferencesFragment()).commit();
    }

    @TargetApi(11)
    private static void onSetupPreferenceFragment(PreferenceFragment f) {
        version = f.findPreference("versionReadOnly");
        serverPreference = f.findPreference("brokerPreference");
        backgroundUpdatesIntervall = f
                .findPreference(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL);
        onSetupCommon();
    }

    private static void onSetupCommon() {
        PackageManager pm = activity.getPackageManager();

        backgroundUpdatesIntervall.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.v(this.toString(), newValue.toString());
                if (newValue.toString().equals("0")) {
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(activity).edit();
                    editor.putString(preference.getKey(), "1");
                    editor.commit();
                    return false;
                }
                return true;
            }
        });

        try {
            version.setSummary(pm.getPackageInfo(activity.getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            version.setSummary(activity.getString(R.string.na));
        }

        setServerPreferenceSummary();

        // Register for connection changed events
        EventBus.getDefault().register(activity);
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

    public void onEventMainThread(Events.MqttConnectivityChanged event) {
        setServerPreferenceSummary();
    }

    private static void setServerPreferenceSummary() {
        serverPreference.setSummary(ServiceMqtt.getConnectivityText());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

}
