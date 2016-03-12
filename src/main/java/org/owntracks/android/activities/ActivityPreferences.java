package org.owntracks.android.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.EditIntegerPreference;
import org.owntracks.android.support.EditStringPreference;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.ListIntegerPreference;
import org.owntracks.android.support.Preferences;

import de.greenrobot.event.EventBus;

public class ActivityPreferences extends ActivityBase {
    private static final String TAG = "ActivityPreferences";

    private static final int REQUEST_CODE_CONNECTION = 1310 ;


    // Return from ActivityPreferencesConnection to see if we need to reload the preferences because a mode change occured
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Log.v(TAG, "onActivityResult: RequestCode: " + requestCode + " resultCode: " + resultCode);
        switch(requestCode) {
            case (REQUEST_CODE_CONNECTION) : {
                Log.v(TAG, "onActivityResult with REQUEST_CODE_CONNECTION");
                if(resultIntent != null && resultIntent.getBooleanExtra(ActivityPreferencesConnection.KEY_MODE_CHANGED, false)) {
                    Log.v(TAG,"recreating ActivityPreferences due to mode change");
                 //   this.recreate();
                 //   this.overridePendingTransition(0, 0);

                    Intent intent = getIntent();
                    overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);

                }

                break;
            }
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        setupSupportToolbar();
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        toolbar = (Toolbar)findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences(), "preferences").commit();

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect:

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().reconnect();
                    }
                };
                new Thread(r).start();
                goToRoot();
                return true;
            case R.id.disconnect:
                Runnable s = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().disconnect(true);

                    }
                };
                new Thread(s).start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

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

            Log.v(TAG, "Prepping preferences: " + Preferences.getModeId());

            if (Preferences.isModeMqttPrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
                addPreferencesFromResource(R.xml.preferences_root);
                PreferenceScreen root = (PreferenceScreen) findPreference("root");
                populatePreferencesScreen(root);
            } else if(Preferences.isModeMqttPublic()){
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
                addPreferencesFromResource(R.xml.preferences_root);
                PreferenceScreen root = (PreferenceScreen) findPreference("root"); 
                populatePreferencesScreen(root); 
            } else {
                throw new RuntimeException("Unknown application mode");
            }


            export = findPreference("export");



            repo = findPreference("repo");
            twitter = findPreference("twitter");
            community = findPreference("community");
            version = findPreference("versionReadOnly");
            documentation = findPreference("versionReadOnly");



            try { ver = pm.getPackageInfo(a.getPackageName(), 0).versionName; } catch (PackageManager.NameNotFoundException e) { ver = a.getString(R.string.na);}
            version.setSummary(ver);

            export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), ActivityExport.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    getActivity().startActivity(intent);
                    return true;
                }
            });

            repo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getDocumentationUrl()));
                    a.startActivity(intent);
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

            community.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getSupportUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });
            documentation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Preferences.getDocumentationUrl()));
                    a.startActivity(intent);
                    return false;
                }
            });

            setServerPreferenceSummary(this);





            Preference.OnPreferenceClickListener connectionListener = new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.v(TAG, "startActivityForResult ActivityPreferencesConnection");
                    Intent intent = new Intent(getActivity(), ActivityPreferencesConnection.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    getActivity().startActivityForResult(intent, REQUEST_CODE_CONNECTION);
                    return true;
                }
            };

            //Fix toolbars for PreferenceScreens on demand
            Preference.OnPreferenceClickListener genericListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final android.preference.Preference preference) {
                    if(!(preference instanceof PreferenceScreen))
                        return false;

                    final Dialog dialog = ((PreferenceScreen)preference).getDialog();

                    LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
                    final Toolbar bar = (Toolbar) LayoutInflater.from(preference.getContext()).inflate(R.layout.toolbar, root, false);
                    root.addView(bar, 0); // insert at top

                    bar.setTitle(preference.getTitle());
                    bar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    return false;
                }
            };
            findPreference("connectionScreen").setOnPreferenceClickListener(connectionListener);
            findPreference("reportingScreen").setOnPreferenceClickListener(genericListener);
            findPreference("notificationScreen").setOnPreferenceClickListener(genericListener);
            findPreference("advancedScreen").setOnPreferenceClickListener(genericListener);
            findPreference("informationScreen").setOnPreferenceClickListener(genericListener);
        }

        private void populatePreferencesScreen(PreferenceScreen root) {

            populateScreenReporting((PreferenceScreen)root.findPreference("reportingScreen"));
            populateScreenNotification((PreferenceScreen)root.findPreference("notificationScreen"));
            populateScreenAdvanced((PreferenceScreen)root.findPreference("advancedScreen"));
            setupDependencies(root);
        }


        private void populateScreenReporting(PreferenceScreen screen) {
            addSwitchPreference(screen, Preferences.Keys.PUB, R.string.preferencesBackroundUpdates, R.string.preferencesBackgroundUpdatesSummary, R.bool.valPub);
            addSwitchPreference(screen, Preferences.Keys.PUB_EXTENDED_DATA, R.string.preferencesPubExtendedDataSummary, R.string.preferencesPubExtendedData, R.bool.valPubExtendedData);
        }


        private void populateScreenAdvanced(PreferenceScreen screen) {
            PreferenceCategory services = getCategory(R.string.preferencesCategoryAdvancedServices);
            screen.addPreference(services);
            addSwitchPreference(services, Preferences.Keys.REMOTE_COMMAND_REPORT_LOCATION, R.string.preferencesRemoteCommandReportLocation, R.string.preferencesRemoteCommandReportLocationSummary, R.bool.valRemoteCommandReportLocation);

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


        }

        private void populateScreenNotification(PreferenceScreen screen) {
            PreferenceCategory ongoing = getCategory(R.string.preferencesCategoryNotificationOngoing);
            screen.addPreference(ongoing);
            addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION, R.string.preferencesNotification, R.string.preferencesNotificationSummary, R.bool.valNotification);
            addSwitchPreference(ongoing, Preferences.Keys.NOTIFICATION_LOCATION, R.string.preferencesNotificationLocation, R.string.preferencesNotificationLocationSummary, R.bool.valNotificationLocation);


            PreferenceCategory background = getCategory(R.string.preferencesCategoryNotificatinBackground);
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
            } catch (IllegalStateException e) {Log.e(TAG, "Preference dependency could not be setup from: " + dependingKey + " to " + dependsOnKey);}
        }



        private PreferenceCategory getCategory(@StringRes int titleRes) {
            PreferenceCategory c = new PreferenceCategory(getActivity());
            c.setTitle(titleRes);
            return c;
        }

        private boolean addSwitchPreference(PreferenceGroup parent, String key, @StringRes int titleRes, @StringRes int summaryRes, @BoolRes int defaultValueAllModes) {
            return addSwitchPreference(parent, key, titleRes, summaryRes, defaultValueAllModes, defaultValueAllModes);
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
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @SuppressWarnings("unused")
        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if ((e != null) && (e.getExtra() != null) && (e.getExtra() instanceof Exception)) {
                if ((((Exception) e.getExtra()).getCause() != null))
                    setServerPreferenceSummary(this, getResources().getString(R.string.error) + ": " + ((Exception) e.getExtra()).getCause().getLocalizedMessage());
                else
                    setServerPreferenceSummary(this, getResources().getString(R.string.error) + ": " + e.getExtra().toString());

            } else {
                setServerPreferenceSummary(this);
            }
        }

        private  void setServerPreferenceSummary(PreferenceFragment c) {
            setServerPreferenceSummary(c, ServiceBroker.getStateAsString(c.getActivity()));
        }


        private  void setServerPreferenceSummary(PreferenceFragment f, String s) {

            f.findPreference("connectionScreen").setSummary(getModeIdReadable(getActivity()) + " - "+ s);
            ((BaseAdapter) ((PreferenceScreen) f.findPreference("root")).getRootAdapter()).notifyDataSetChanged(); //Have to redraw the list to reflect summary change
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
            default:
                mode = c.getString(R.string.mode_mqtt_private_label);
                break;
        }
        return mode;

    }



}




