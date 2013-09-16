
package st.alr.mqttitude.preferences;

import java.util.prefs.Preferences;

import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.ActivityStatus;
import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.greenrobot.event.EventBus;

public class ActivityPreferences extends PreferenceActivity {
    private static Preference serverPreference;
    private static Preference backgroundUpdatesIntervall;
    private static Preference version;
    private static Preference repo;
    private static Preference mail;
    private static PreferenceActivity activity;
    static String ver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;

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
        onSetupCommon();
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
        onSetupCommon();
    }

    private static void onSetupCommon() {
        PackageManager pm = activity.getPackageManager();
        try {
            ver = pm.getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            ver = activity.getString(R.string.na);
        }

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

        version.setSummary(ver);

        repo.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Defaults.VALUE_REPO_URL));
                        activity.startActivity(intent);
                        return false;
                    }
                });

        mail.setOnPreferenceClickListener(
                new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        // intent.setType("text/html");
                        intent.setType("message/rfc822");

                        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {
                            Defaults.VALUE_ISSUES_MAIL
                        });
                        intent.putExtra(Intent.EXTRA_SUBJECT, "MQTTitude (Version: " + ver + ")");
                        activity.startActivity(Intent.createChooser(intent, "Send Email"));
                        return false;
                    }
                });

        setServerPreferenceSummary();

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_status, menu);
        return true;
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
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_status) {
                Intent intent1 = new Intent(this, ActivityStatus.class);
                startActivity(intent1);
                return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
