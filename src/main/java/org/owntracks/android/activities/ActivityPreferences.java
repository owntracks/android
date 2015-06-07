package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.rengwuxian.materialedittext.MaterialEditText;

import de.greenrobot.event.EventBus;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ConnectionToolbarPreference;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.EditIntegerPreference;
import org.owntracks.android.support.EditStringPreference;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

public class ActivityPreferences extends ActionBarActivity {
    private static boolean modeSwitch = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;

        setContentView(R.layout.activity_preferences);


        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Drawer.OnDrawerItemClickListener drawerListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        goToRoot();
                        return true;
                    case R.string.idWaypoints:
                        Intent intent = new Intent(context, ActivityWaypoints.class);
                        startActivity(intent);
                        return true;
                    case R.string.idSettings:
                        return true;
                }
                return false;
            }
        };

        DrawerFactory.buildDrawer(this, toolbar, drawerListener, 2);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences(), "preferences").commit();

    }


    private void goToRoot() {
        Intent intent1 = new Intent(this, ActivityMain.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent1);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:     // If the user hits the toolbar back arrow, go back to ActivityMain, no matter where he came from (same as hitting back)
                goToRoot();
                return true;
            case R.id.connect:

                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().reconnect();
                    }
                };
                new Thread(r).start();

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

    // If the user hits back, go back to ActivityMain, no matter where he came from
    @Override
    public void onBackPressed() {
        goToRoot();
    }


    public static class FragmentPreferences extends PreferenceFragment {

        private static ConnectionToolbarPreference serverPreferenceToolbar;

        private static Preference hostPreference;
        private static Preference identificationPreference;
        private static Preference securityPreference;
        private static Preference optionsPreference;

        private static org.owntracks.android.support.EditStringPreference deviceTopic;
        private static org.owntracks.android.support.EditIntegerPreference pubInterval;
        private static org.owntracks.android.support.EditStringPreference baseTopic;
        private static org.owntracks.android.support.EditIntegerPreference locatorDisplacement;
        private static org.owntracks.android.support.EditIntegerPreference locatorInterval;
        private static org.owntracks.android.support.EditIntegerPreference beaconForegroundScanPeriod;
        private static org.owntracks.android.support.EditIntegerPreference beaconBackgroundScanPeriod;

        private static Preference version;
        private static Preference repo;
        private static Preference mail;
        private static Preference twitter;
        private static Preference mode;


        static String ver;
        private static SharedPreferences.OnSharedPreferenceChangeListener deviceTopicListener;
        private SharedPreferences.OnSharedPreferenceChangeListener modeListener;

        private static boolean tlsVal;
        private static boolean cleansessionVal;
        private static boolean authenticationVal;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();
            PackageManager pm = a.getPackageManager();


            Preference.OnPreferenceClickListener identificationClickListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(a)
                            .customView(R.layout.preferences_identification, true)
                            .title(R.string.preferencesIdentification)
                            .positiveText(R.string.accept)
                            .negativeText(R.string.cancel)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    Switch authentication = (Switch) d.findViewById(R.id.authentication);
                                    final MaterialEditText username = (MaterialEditText) d.findViewById(R.id.username);
                                    final MaterialEditText password = (MaterialEditText) d.findViewById(R.id.password);
                                    final MaterialEditText deviceId = (MaterialEditText) d.findViewById(R.id.deviceId);
                                    final MaterialEditText trackerId = (MaterialEditText) d.findViewById(R.id.trackerId);
                                    final MaterialEditText clientId = (MaterialEditText) d.findViewById(R.id.clientId);


                                    authentication.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            authenticationVal = isChecked;
                                            password.setVisibility(authenticationVal ? View.VISIBLE : View.GONE);
                                        }
                                    });

                                    authentication.setChecked(authenticationVal);
                                    username.setText(Preferences.getUsername());
                                    password.setText(Preferences.getPassword());
                                    password.setVisibility(authenticationVal ? View.VISIBLE : View.GONE);
                                    deviceId.setHint(Preferences.getDeviceIdDefault());
                                    deviceId.setText(Preferences.getDeviceId(false));
                                    clientId.setHint(Preferences.getClientIdDefault());
                                    clientId.setText(Preferences.getClientId(false));
                                    trackerId.setText(Preferences.getTrackerId(false));
                                    trackerId.setHint(Preferences.getTrackerIdDefault());

                                }
                            })
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText username = (MaterialEditText) d.findViewById(R.id.username);
                                    final MaterialEditText password = (MaterialEditText) d.findViewById(R.id.password);
                                    final MaterialEditText deviceId = (MaterialEditText) d.findViewById(R.id.deviceId);
                                    final MaterialEditText trackerId = (MaterialEditText) d.findViewById(R.id.trackerId);
                                    final MaterialEditText clientId = (MaterialEditText) d.findViewById(R.id.clientId);

                                    Preferences.setAuth(authenticationVal);
                                    Preferences.setUsername(username.getText().toString());
                                    Preferences.setPassword(password.getText().toString());
                                    Preferences.setDeviceId(deviceId.getText().toString());
                                    Preferences.setClientId(clientId.getText().toString());
                                    Preferences.setTrackerId(trackerId.getText().toString());

                                    serverPreferenceToolbar.conditionallyEnableConnectButton();

                                }
                            })

                            .show();

                    return true;
                }
            };


            if (Preferences.isModePrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
                addPreferencesFromResource(R.xml.privatepreferences);


                deviceTopic = (EditStringPreference) findPreference(Preferences.getKey(R.string.keyDeviceTopic));
                setDeviceTopicHint();

                baseTopic = (EditStringPreference) findPreference(Preferences.getKey(R.string.keyBaseTopic));
                baseTopic.setHint(Preferences.getStringRessource(R.string.valBaseTopic));

                hostPreference = findPreference(getString(R.string.keyHost));
                securityPreference = findPreference(getString(R.string.keySecurity));
                optionsPreference = findPreference(getString(R.string.keyOptions));
                identificationPreference = findPreference(getString(R.string.keyIdentification));

                // Sets the pubTopic hint value when username or deviceId changes
                deviceTopicListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

                    @Override
                    public void onSharedPreferenceChanged(
                            SharedPreferences sharedPreferences, String key) {
                        if (key.equals(Preferences.getKey(R.string.keyUsername)) || key.equals(Preferences.getKey(R.string.keyDeviceId))) {
                            setDeviceTopicHint();
                        }
                    }
                };
                PreferenceManager.getDefaultSharedPreferences(a).registerOnSharedPreferenceChangeListener(deviceTopicListener);

                hostPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        MaterialDialog dialog = new MaterialDialog.Builder(a)
                                .customView(R.layout.preferences_host, true)
                                .title(R.string.preferencesHost)
                                .positiveText(R.string.accept)
                                .negativeText(R.string.cancel)
                                .showListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        final MaterialEditText host = (MaterialEditText) d.findViewById(R.id.host);
                                        final MaterialEditText port = (MaterialEditText) d.findViewById(R.id.port);

                                        host.setText(Preferences.getHost());
                                        host.setFloatingLabelAlwaysShown(true);

                                        port.setText(Preferences.getPortWithHintSupport());
                                        port.setFloatingLabelAlwaysShown(true);

                                    }
                                })
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        final MaterialEditText host = (MaterialEditText) d.findViewById(R.id.host);
                                        final MaterialEditText port = (MaterialEditText) d.findViewById(R.id.port);

                                        Preferences.setHost(host.getText().toString());
                                        try {
                                            Preferences.setPort(Integer.parseInt(port.getText().toString()));
                                        } catch (NumberFormatException e) {
                                            Preferences.clearKey(R.string.keyPort);
                                        }
                                    }
                                })

                                .show();

                        return true;
                    }
                });

                cleansessionVal = Preferences.getCleanSession();
                optionsPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new MaterialDialog.Builder(a)
                                .customView(R.layout.preferences_options, true)
                                .title(R.string.preferencesOptions)
                                .positiveText(R.string.accept)
                                .negativeText(R.string.cancel)
                                .showListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        Switch cleansession = (Switch) d.findViewById(R.id.cleanSession);
                                        final MaterialEditText keepalive = (MaterialEditText) d.findViewById(R.id.keepalive);
                                        cleansession.setChecked(cleansessionVal);
                                        cleansession.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                cleansessionVal = isChecked;
                                            }
                                        });

                                        keepalive.setText(Preferences.getKeepaliveWithHintSupport());

                                    }
                                })
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        Log.v(this.toString(), "saving parameters");
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        final MaterialEditText keepalive = (MaterialEditText) d.findViewById(R.id.keepalive);

                                        Preferences.setCleanSession(cleansessionVal);
                                        try {
                                            Preferences.setKeepalive(Integer.parseInt(keepalive.getText().toString()));

                                        } catch (NumberFormatException e) {
                                            Preferences.clearKey(R.string.keyKeepalive);
                                        }

                                        serverPreferenceToolbar.conditionallyEnableConnectButton();

                                    }
                                })
                                .show();

                        return true;
                    }
                });

                tlsVal = Preferences.getTls();
                securityPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new MaterialDialog.Builder(a)
                                .customView(R.layout.preferences_security, true)
                                .title(R.string.preferencesSecurity)
                                .positiveText(R.string.accept)
                                .negativeText(R.string.cancel)
                                .showListener(new DialogInterface.OnShowListener() {
                                    @Override
                                    public void onShow(DialogInterface dialog) {
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        Switch tls = (Switch) d.findViewById(R.id.tls);
                                        final MaterialEditText tlsCrt = (MaterialEditText) d.findViewById(R.id.tlsCrt);
                                        tls.setChecked(tlsVal);
                                        tlsCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                        tlsCrt.setText(Preferences.getTlsCrtPath());

                                        tls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                            @Override
                                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                                tlsVal = isChecked;
                                                tlsCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                            }
                                        });


                                    }
                                })
                                .callback(new MaterialDialog.ButtonCallback() {
                                    @Override
                                    public void onPositive(MaterialDialog dialog) {
                                        MaterialDialog d = MaterialDialog.class.cast(dialog);
                                        MaterialEditText tlsCrt = (MaterialEditText) d.findViewById(R.id.tlsCrt);

                                        Preferences.setTls(tlsVal);
                                        Preferences.setTlsCrtPath(tlsCrt.getText().toString());

                                        serverPreferenceToolbar.conditionallyEnableConnectButton();

                                    }
                                })
                                .show();

                        return true;
                    }
                });

                identificationPreference.setOnPreferenceClickListener(identificationClickListener);


            } else if(Preferences.isModeHosted()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_HOSTED);
                addPreferencesFromResource(R.xml.hostedpreferences);
                identificationPreference = findPreference(getString(R.string.keyIdentification));
                identificationPreference.setOnPreferenceClickListener(identificationClickListener);

            } else {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
                addPreferencesFromResource(R.xml.publicpreferences);
            }

            if(ActivityPreferences.modeSwitch) {
                Log.v(this.toString(), "simulating click");
                PreferenceScreen screen = getPreferenceScreen(); // gets the main preference screen
                screen.onItemClick(null, null, 0 , 0); // click on the item

            }


            mode = findPreference(Preferences.getKey(R.string.keyModeId));

            String[] modesReadable = getResources().getStringArray(R.array.profileIds_readable);
            mode.setSummary(modesReadable[Preferences.getModeId()]);

            mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Preferences.setMode(Integer.parseInt((String) newValue));
                    ActivityPreferences.modeSwitch = true; // signal that ConnectionPreferences should be shown again after fragment is restored
                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag("preferences")).add(R.id.content_frame, new FragmentPreferences(), "preferences").commit();
                    return false; // Don't save, setMode already did
                }
            });



            repo = findPreference("repo");
            mail = findPreference("mail");
            twitter = findPreference("twitter");
            version = findPreference("versionReadOnly");

            locatorDisplacement = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyLocatorDisplacement));
            locatorDisplacement.setHint(Integer.toString(Preferences.getIntResource(R.integer.valLocatorDisplacement)));

            locatorInterval = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyLocatorInterval));
            locatorInterval.setHint(Integer.toString(Preferences.getIntResource(R.integer.valLocatorInterval)));

            beaconForegroundScanPeriod = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyBeaconForegroundScanPeriod));
            beaconForegroundScanPeriod.setHint(Integer.toString(Preferences.getIntResource(R.integer.valBeaconForegroundScanPeriod)));

            beaconBackgroundScanPeriod = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyBeaconBackgroundScanPeriod));
            beaconBackgroundScanPeriod.setHint(Integer.toString(Preferences.getIntResource(R.integer.valBeaconBackgroundScanPeriod)));


            serverPreferenceToolbar = (ConnectionToolbarPreference) findPreference("brokerPreference");

            try {
                ver = pm.getPackageInfo(a.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                ver = a.getString(R.string.na);
            }
            version.setSummary(ver);

            pubInterval = (EditIntegerPreference) findPreference(Preferences.getKey(R.string.keyPubInterval));
            pubInterval.setHint(Integer.toString(Preferences.getIntResource(R.integer.valPubInterval)));
            pubInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(
                        Preference preference, Object newValue) {
                    Log.v("ActivityPreferences", newValue.toString());
                    if (newValue.toString().equals("0")) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(a).edit();
                        editor.putString(preference.getKey(), "1");
                        editor.commit();
                        return false;
                    }
                    return true;
                }
            });


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
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{Preferences.getIssuesMail()});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "OwnTracks (Version: " + ver + ")");
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

            setServerPreferenceSummary(this);



            authenticationVal = Preferences.getAuth();


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
            if (deviceTopicListener != null)
                PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(deviceTopicListener);


            super.onDestroy();
        }

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

        private static void setServerPreferenceSummary(PreferenceFragment c) {
            setServerPreferenceSummary(c, ServiceBroker.getStateAsString(c.getActivity()));
        }

        private static void setServerPreferenceSummary(PreferenceFragment f, String s) {
            f.findPreference("brokerPreferenceScreen").setSummary(s);
            ((BaseAdapter) ((PreferenceScreen) f.findPreference("root")).getRootAdapter()).notifyDataSetChanged(); //Have to redraw the list to reflect summary change
        }

        private static void setDeviceTopicHint() {
            deviceTopic.setHint(Preferences.getDeviceTopic(true));
        }


    }
}




