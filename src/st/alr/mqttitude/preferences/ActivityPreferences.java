
package st.alr.mqttitude.preferences;

import st.alr.mqttitude.R.xml;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start service if it is not already started
        Intent service = new Intent(this, ServiceMqtt.class);
        startService(service);

        // Register for connection changed events
        EventBus.getDefault().register(this);

        // Replace content with fragment for custom preferences
        getFragmentManager().beginTransaction().replace(android.R.id.content, new CustomPreferencesFragment()).commit();

    }

    public static class CustomPreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            PackageManager pm = this.getActivity().getPackageManager();
            Preference version = findPreference("versionReadOnly");

            
            findPreference(Defaults.SETTINGS_KEY_BACKGROUND_UPDATES_INTERVAL).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Log.v(this.toString(), newValue.toString());
                    if (newValue.toString().equals("0")) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        editor.putString(preference.getKey(), "1");
                        editor.commit();                                                
                        return false;
                    }
                    return true;
                }});
       

                    
            try {
                version.setSummary(pm.getPackageInfo(this.getActivity().getPackageName(), 0).versionName);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            serverPreference = findPreference("brokerPreference");
            setServerPreferenceSummary();

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
