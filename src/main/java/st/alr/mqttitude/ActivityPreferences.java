package st.alr.mqttitude;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.services.ServiceBroker;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;

public class ActivityPreferences extends ActionBarActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences()).commit();
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class FragmentPreferences extends PreferenceFragment {
        private static Preference serverPreference;
        private static Preference backgroundUpdatesIntervall;
        private static Preference version;
        private static Preference repo;
        private static Preference mail;
        private static Preference twitter;
        private static Preference donate;

        private static EditTextPreference topic;
        private static EditTextPreference trackerId;

        static String ver;
        private static SharedPreferences.OnSharedPreferenceChangeListener pubTopicListener;


        private static void setPubTopicHint(EditTextPreference e) {
            e.getEditText().setHint(Preferences.getPubTopicFallback());

        }

        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            final Activity a = getActivity();
            PackageManager pm = a.getPackageManager();

            repo = findPreference("repo");
            mail = findPreference("mail");
            twitter = findPreference("twitter");
            version = findPreference("versionReadOnly");
            donate = findPreference("donate");
            serverPreference = findPreference("brokerPreference");
            backgroundUpdatesIntervall = findPreference(Preferences
                    .getKey(R.string.keyPubInterval));

            topic = (EditTextPreference) findPreference(Preferences
                    .getKey(R.string.keyPubTopicBase));

            trackerId = (EditTextPreference) findPreference(Preferences
                    .getKey(R.string.keyTrackerId));


            try {
                ver = pm.getPackageInfo(a.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                ver = a.getString(R.string.na);
            }

            backgroundUpdatesIntervall
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
                            Log.v("ActivityPreferences", newValue.toString());
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

            repo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getRepoUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });

            mail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("message/rfc822");

                    intent.putExtra(Intent.EXTRA_EMAIL,
                            new String[]{Preferences.getIssuesMail()});
                    intent.putExtra(Intent.EXTRA_SUBJECT,
                            "OwnTracks (Version: " + ver + ")");
                    a.startActivity(Intent.createChooser(intent, "Send Email"));
                    return false;
                }
            });

            twitter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getTwitterUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });

            setServerPreferenceSummary(getActivity());

            backgroundUpdatesIntervall
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(
                                Preference preference, Object newValue) {
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

            pubTopicListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key) {
                    if (key.equals(Preferences.getKey(R.string.keyUsername)) || key.equals(Preferences.getKey(R.string.keyDeviceId))) {
                        setPubTopicHint(topic);
                        setTrackerIdHint(trackerId);
                    }
                }
            };
            PreferenceManager.getDefaultSharedPreferences(a)
                    .registerOnSharedPreferenceChangeListener(pubTopicListener);

            setPubTopicHint(topic);
            setTrackerIdHint(trackerId);
        }

        public void setTrackerIdHint(EditTextPreference e) {
            e.getEditText().setHint(Preferences.getTrackerIdFallback());
        }


        @Override
        public void onDestroy() {
            if (pubTopicListener != null)
                PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(pubTopicListener);
            super.onDestroy();
        }

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if ((e != null) && (e.getExtra() != null) && (e.getExtra() instanceof Exception)) {
                if ((((Exception) e.getExtra()).getCause() != null))
                    setServerPreferenceSummary(getResources().getString(R.string.error) + ": " + ((Exception) e.getExtra()).getCause().getLocalizedMessage());
                else
                    setServerPreferenceSummary(getResources().getString(R.string.error) + ": " + e.getExtra().toString());

            } else {
                setServerPreferenceSummary(getActivity());
            }
        }

        private static void setServerPreferenceSummary(Context c) {
            setServerPreferenceSummary(ServiceBroker.getStateAsString(c));
        }

        private static void setServerPreferenceSummary(String s) {
            serverPreference.setSummary(s);
        }
    }
}




