package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.ArrayRes;
import android.support.annotation.BoolRes;
import android.support.annotation.IntegerRes;
import android.support.annotation.StringRes;

import org.greenrobot.eventbus.Subscribe;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.support.widgets.EditIntegerPreference;
import org.owntracks.android.support.widgets.EditStringPreference;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.widgets.ListIntegerPreference;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.ToolbarPreference;

import timber.log.Timber;

public class ActivityPreferences extends ActivityBase {
    private static final int REQUEST_CODE_CONNECTION = 1310 ;


    // Return from ActivityPreferencesConnection to see if we need to reload the preferences because a mode change occured
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Timber.v("onActivityResult: RequestCode: " + requestCode + " resultCode: " + resultCode);
        switch(requestCode) {
            case (REQUEST_CODE_CONNECTION) : {
                Timber.v("onActivityResult with REQUEST_CODE_CONNECTION");
                if(resultIntent != null && resultIntent.getBooleanExtra(ActivityPreferencesConnection.KEY_MODE_CHANGED, false)) {
                    Timber.v("recreating ActivityPreferences due to mode change");
                 //   this.recreate();
                 //   this.overridePendingTransition(0, 0);
                    loadFragment();
                }

                break;
            }
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);



        setSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadFragment();


    }

    private void loadFragment() {
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences(), "preferences").commit();
    }


    public static class FragmentPreferences extends PreferenceFragment {
        private static Preference version;
        private static Preference repo;
        private static Preference twitter;
        private static Preference community;
        private Preference export;

        static String ver;
        private Preference documentation;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();
            PackageManager pm = a.getPackageManager();

            Timber.v("Prepping preferences: " + Preferences.getModeId());

            if (Preferences.isModeMqttPrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
            } else if(Preferences.isModeMqttPublic()){
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
            }  else if(Preferences.isModeHttpPrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_HTTP);
            } else {
                throw new RuntimeException("Unknown application mode");
            }

            addPreferencesFromResource(R.xml.preferences_root);
            PreferenceScreen root = (PreferenceScreen) findPreference("root");
            populatePreferencesScreen(root);

            export = findPreference("export");

            repo = findPreference("repo");
            twitter = findPreference("twitter");
            community = findPreference("community");
            documentation = findPreference("documentation");
            version = findPreference("versionReadOnly");



            try {
                ver = pm.getPackageInfo(a.getPackageName(), 0).versionName + " (" + pm.getPackageInfo(a.getPackageName(), 0).versionCode+")";
            } catch (PackageManager.NameNotFoundException e) { ver = a.getString(R.string.na);}
            version.setSummary(ver);

            export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), ActivityExport.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.putExtra(ActivityBase.DISABLES_ANIMATION, true);
                    getActivity().startActivity(intent);

                    return true;
                }
            });

            repo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getString(R.string.valRepoUrl)));
                    a.startActivity(intent);
                    return false;
                }
            });


            twitter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getString(R.string.valTwitterUrl)));
                    a.startActivity(intent);
                    return false;
                }
            });

            community.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getString(R.string.valCommunityUrl)));
                    a.startActivity(intent);
                    return false;
                }
            });
            documentation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getString(R.string.valDocumentationUrl)));
                    a.startActivity(intent);
                    return false;
                }
            });

            setServerPreferenceSummary(this);





            Preference.OnPreferenceClickListener connectionListener = new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Timber.v("startActivityForResult ActivityPreferencesConnection");
                    Intent intent = new Intent(getActivity(), ActivityPreferencesConnection.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.putExtra(ActivityBase.DISABLES_ANIMATION, true);

                    getActivity().startActivityForResult(intent, REQUEST_CODE_CONNECTION);

                    return true;
                }
            };


            findPreference("connectionScreen").setOnPreferenceClickListener(connectionListener);
        }

        private void populatePreferencesScreen(PreferenceScreen root) {

            populateScreenReporting((PreferenceScreen)root.findPreference("reportingScreen"));
            populateScreenNotification((PreferenceScreen)root.findPreference("notificationScreen"));
            populateScreenAdvanced((PreferenceScreen)root.findPreference("advancedScreen"));
            setupDependencies(root);
        }


        private void populateScreenReporting(PreferenceScreen screen) {
            addToolbar(screen);
            addSwitchPreference(screen, Preferences.Keys.PUB, R.string.preferencesBackgroundUpdates, R.string.preferencesBackgroundUpdatesSummary, R.bool.valPub);
            addSwitchPreference(screen, Preferences.Keys.PUB_EXTENDED_DATA, R.string.preferencesPubExtendedData, R.string.preferencesPubExtendedDataSummary, R.bool.valPubExtendedData);
        }


        private void populateScreenAdvanced(PreferenceScreen screen) {
            addToolbar(screen);
            PreferenceCategory services = getCategory(R.string.preferencesCategoryAdvancedServices);
            screen.addPreference(services);
            addSwitchPreference(services, Preferences.Keys.REMOTE_COMMAND, R.string.preferencesRemoteCommand, R.string.preferencesRemoteCommandSummary, R.bool.valRemoteCommand);

            PreferenceCategory locator = getCategory(R.string.preferencesCategoryAdvancedLocator);
            screen.addPreference(locator);
            addListIntegerPreference(locator, Preferences.Keys.LOCATOR_ACCURACY_FOREGROUND,  R.string.preferencesLocatorAccuracyForeground, R.string.preferencesLocatorAccuracyForegroundSummary, R.array.locatorAccuracy_readable, R.array.locatorAccuracy, R.integer.valLocatorAccuracyForeground);
            addListIntegerPreference(locator, Preferences.Keys.LOCATOR_ACCURACY_BACKGROUND, R.string.preferencesLocatorAccuracyBackground, R.string.preferencesLocatorAccuracyBackgroundSummary, R.array.locatorAccuracy_readable, R.array.locatorAccuracy, R.integer.valLocatorAccuracyForeground);
            addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_DISPLACEMENT, R.string.preferencesLocatorDisplacement, R.string.preferencesLocatorDisplacementSummary, R.integer.valLocatorDisplacement);
            addEditIntegerPreference(locator, Preferences.Keys.LOCATOR_INTERVAL, R.string.preferencesLocatorInterval, R.string.preferencesLocatorIntervalSummary, R.integer.valLocatorInterval);

            PreferenceCategory encryption = getCategory(R.string.preferencesCategoryAdvancedEncryption);
            screen.addPreference(encryption);
            addEditStringPreference(encryption, Preferences.Keys._ENCRYPTION_KEY, R.string.preferencesEncryptionKey, R.string.preferencesEncryptionKeySummary, R.string.valEmpty);

            PreferenceCategory misc = getCategory(R.string.preferencesCategoryAdvancedMisc);
            screen.addPreference(misc);
            addSwitchPreference(misc, Preferences.Keys.AUTOSTART_ON_BOOT, R.string.preferencesAutostart, R.string.preferencesAutostartSummary, R.bool.valAutostartOnBoot);
            //addSwitchPreference(misc, Preferences.Keys.PLAY_OVERRIDE, R.string.preferencesPlayOverride, R.string.preferencesPlayOverrideSummary, R.bool.valPlayOverride);


        }

        private void populateScreenNotification(PreferenceScreen screen) {
            addToolbar(screen);

            PreferenceCategory ongoing = getCategory(R.string.preferencesCategoryNotificationOngoing);
            screen.addPreference(ongoing);
            addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION, R.string.preferencesNotification, R.string.preferencesNotificationSummary, R.bool.valNotification);
            addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION_LOCATION, R.string.preferencesNotificationLocation, R.string.preferencesNotificationLocationSummary, R.bool.valNotificationLocation);


            PreferenceCategory background = getCategory(R.string.preferencesCategoryNotificationBackground);
            screen.addPreference(background);
            addSwitchPreference(background, Preferences.Keys.NOTIFICATION_EVENTS, R.string.preferencesNotificationEvents, R.string.preferencesNotificationEventsSummary, R.bool.valNotificationEvents);

        }

        private void setupDependencies(PreferenceScreen root) {
            setDependency(root, Preferences.Keys.NOTIFICATION_LOCATION, Preferences.Keys.NOTIFICATION);
            setDependency(root, Preferences.Keys.NOTIFICATION_EVENTS, Preferences.Keys.NOTIFICATION);
        }

        private void setDependency(PreferenceScreen root, String dependingKey, String dependsOnKey) {
            try {
                root.findPreference(dependingKey).setDependency(dependsOnKey);
            } catch (IllegalStateException e) {
                Timber.e("Preference dependency could not be setup from: " + dependingKey + " to " + dependsOnKey);}
        }



        private PreferenceCategory getCategory(@StringRes int titleRes) {
            PreferenceCategory c = new PreferenceCategory(getActivity());
            c.setTitle(titleRes);
            return c;
        }

        private boolean addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueAllModes) {
            return addSwitchPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
        }

        private void addToolbar(PreferenceScreen parent) {
            ToolbarPreference t = new ToolbarPreference(getActivity(), parent);
            t.setTitle(parent.getTitle());
            parent.addPreference(t);
        }

        private boolean addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueResPrivate, @BoolRes int defaultValueResPublic) {
            // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
            if((Preferences.isModeMqttPrivate() && defaultValueResPrivate == 0) || (Preferences.isModeMqttPublic() && defaultValueResPublic == 0)) {
                return false;
            }

            SwitchPreference p = new SwitchPreference(getActivity());
            p.setKey(key);
            p.setTitle(titleRes);
            p.setSummary(summaryRes);
            p.setPersistent(false);
            p.setChecked(Preferences.getBoolean(key, defaultValueResPrivate, defaultValueResPublic, false));
            p.setPersistent(true);
            parent.addPreference(p);
            return true;
        }

        private boolean addEditStringPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @StringRes int defaultValueAllModes) {
            return addEditStringPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
        }

        private boolean addEditStringPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @StringRes int defaultValueResPrivate, @StringRes int defaultValueResPublic) {
            // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
            if((Preferences.isModeMqttPrivate() && defaultValueResPrivate == 0) || (Preferences.isModeMqttPublic() && defaultValueResPublic == 0)) {
                return false;
            }

            EditStringPreference p = new EditStringPreference(getActivity());
            p.setKey(key);
            p.setTitle(titleRes);
            p.setSummary(summaryRes);

            p.setPersistent(false);
            p.setText(getEditStringPreferenceTextValueWithHintSupport(key));
            p.setHint(Preferences.getStringDefaultValue(defaultValueResPrivate, defaultValueResPublic));
            p.setPersistent(true);

            parent.addPreference(p);
            return true;
        }





        private boolean addEditIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @IntegerRes int defaultValueAllModes) {
            return addEditIntegerPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
        }

        private boolean addEditIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @IntegerRes int defaultValueResPrivate, @IntegerRes int defaultValueResPublic) {
            // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
            if((Preferences.isModeMqttPrivate() && defaultValueResPrivate == 0) || (Preferences.isModeMqttPublic() && defaultValueResPublic == 0)) {
                return false;
            }

            EditIntegerPreference p = new EditIntegerPreference(getActivity());
            p.setKey(key);
            p.setTitle(titleRes);
            p.setSummary(summaryRes);

            p.setPersistent(false);
            p.setText(getEditIntegerPreferenceTextValueWithHintSupport(key));
            p.setHint(Integer.toString(Preferences.getIntegerDefaultValue(defaultValueResPrivate, defaultValueResPublic)));
            p.setPersistent(true);

            parent.addPreference(p);
            return true;
        }

        private String getEditStringPreferenceTextValueWithHintSupport(String key) {
            return Preferences.getString(key, R.string.valEmpty);
        }

        // returns an empty string if no key value is found so that a hint can be displayed
        private String getEditIntegerPreferenceTextValueWithHintSupport(String key) {
            int i = Preferences.getInt(key, R.integer.valInvalid);
            if (i == -1) {
                return "";
            } else {
                return Integer.toString(i);
            }
        }


        private boolean addListIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @ArrayRes int entriesRes, @ArrayRes int entryValuesRes, @IntegerRes int defaultValueAllModes) {
            return addListIntegerPreference(parent, key, titleRes, summaryRes, entriesRes, entryValuesRes, defaultValueAllModes, defaultValueAllModes);
        }

        private boolean addListIntegerPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @ArrayRes int entriesRes, @ArrayRes int entryValuesRes, @IntegerRes int defaultValueResPrivate, @IntegerRes int defaultValueResPublic) {
            // Skip if no default value exists for current mode. Can be used to exclude preferences in some modes
            if((Preferences.isModeMqttPrivate() && defaultValueResPrivate == 0) || (Preferences.isModeMqttPublic() && defaultValueResPublic == 0)) {
                return false;
            }

            ListIntegerPreference p = new ListIntegerPreference(parent.getContext());
            p.setKey(key);
            p.setTitle(titleRes);
            p.setSummary(summaryRes);
            p.setEntries(entriesRes);
            p.setEntryValues(entryValuesRes);

            p.setPersistent(false);
            p.setValueIndex(Preferences.getInt(key, defaultValueResPrivate, defaultValueResPublic, false));
            p.setPersistent(true);

            parent.addPreference(p);
            return true;
        }





        @Override
        public void onStart() {
            super.onStart();
            App.getEventBus().register(this);
        }

        @Override
        public void onStop() {
            App.getEventBus().unregister(this);
            super.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Subscribe(sticky = true)
        public void onEvent(Events.ModeChanged event) {
            setServerPreferenceSummary(this);

        }



        private  void setServerPreferenceSummary(PreferenceFragment f) {

            f.findPreference("connectionScreen").setSummary(getModeIdReadable(getActivity()));
            //((BaseAdapter) ((PreferenceScreen) f.findPreference("root")).getRootAdapter()).notifyDataSetChanged(); //Have to redraw the list to reflect summary change
        }
    }


    public static String getModeIdReadable(Context c) {
        String mode;
        switch (Preferences.getModeId()) {
            case App.MODE_ID_MQTT_PRIVATE:
                mode = c.getString(R.string.mode_mqtt_private_label);
                break;
            case App.MODE_ID_MQTT_PUBLIC:
                mode = c.getString(R.string.mode_mqtt_public_label);
                break;
            case App.MODE_ID_HTTP_PRIVATE:
                mode = c.getString(R.string.mode_http_private_label);
                break;

            default:
                mode = c.getString(R.string.mode_mqtt_private_label);
                break;
        }
        return mode;

    }


}




