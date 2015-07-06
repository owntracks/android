package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;

import de.greenrobot.event.EventBus;

public class ActivityPreferencesConnection extends ActivityBase {
    private static final String TAG = "ActivityPreferencesCon";

    private static boolean modeSwitch = false;
    public static final String KEY_MODE_CHANGED = "modeChanged";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = this;

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getTitle());
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new FragmentPreferences(), "preferences").commit();

    }


    // If the user hits back, go back to ActivityMain, no matter where he came from
    @Override
    public void onBackPressed() {
        handleBack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:     // If the user hits the toolbar back arrow, go back to ActivityMain, no matter where he came from (same as hitting back)
                handleBack();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void handleBack() {
        Log.v("ConnectionPReferences", "handleBack. ModeChange: " + modeSwitch);
        Intent resultIntent = new Intent();
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        resultIntent.putExtra(KEY_MODE_CHANGED, modeSwitch); // signal preferences activity if it has to reload the preferences tree after a mode switch
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(0, 0); //
    }

    public static class FragmentPreferences extends PreferenceFragment {

        private static Preference mode;
        private static Preference hostPreference;
        private static Preference identificationPreference;
        private static Preference securityPreference;
        private static Preference optionsPreference;

        private static boolean tlsVal;
        private static boolean cleansessionVal;
        private static boolean authenticationVal;

        private SharedPreferences.OnSharedPreferenceChangeListener modeListener;

        private static Menu mMenu;
        private MenuInflater mInflater;
        ServiceBroker.State cachedState = null;

        private void loadHostPreferences(final Activity a) {
            Preference.OnPreferenceClickListener hostClickListener = new Preference.OnPreferenceClickListener() {
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
            };


            hostPreference = findPreference(getString(R.string.keyHost));
            hostPreference.setOnPreferenceClickListener(hostClickListener);

        }

        private void loadIdentificationPreferences(final Activity a) {
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

                                    updateConnectButton();
                                }
                            })

                            .show();

                    return true;
                }
            };

            authenticationVal = Preferences.getAuth();
            identificationPreference = findPreference(getString(R.string.keyIdentification));
            identificationPreference.setOnPreferenceClickListener(identificationClickListener);

        }



        private void loadIdentificationPreferencesHosted(final Activity a) {
            Preference.OnPreferenceClickListener identificationClickListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(a)
                            .customView(R.layout.preferences_identification_hosted, true)
                            .title(R.string.preferencesIdentification)
                            .positiveText(R.string.accept)
                            .negativeText(R.string.cancel)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText username = (MaterialEditText) d.findViewById(R.id.usernameHosted);
                                    final MaterialEditText password = (MaterialEditText) d.findViewById(R.id.passwordHosted);
                                    final MaterialEditText deviceId = (MaterialEditText) d.findViewById(R.id.deviceIdHosted);
                                    final MaterialEditText trackerId = (MaterialEditText) d.findViewById(R.id.trackerId);

                                    Log.v(TAG, Preferences.getUsername());
                                    Log.v(TAG, Preferences.getPassword());
                                    Log.v(TAG, Preferences.getDeviceId(false));
                                    username.setText(Preferences.getUsername());
                                    password.setText(Preferences.getPassword());
                                    deviceId.setText(Preferences.getDeviceId(false));
                                    trackerId.setText(Preferences.getTrackerId(false));
                                    trackerId.setHint(Preferences.getTrackerIdDefault());

                                }
                            })
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText username = (MaterialEditText) d.findViewById(R.id.usernameHosted);
                                    final MaterialEditText password = (MaterialEditText) d.findViewById(R.id.passwordHosted);
                                    final MaterialEditText deviceId = (MaterialEditText) d.findViewById(R.id.deviceIdHosted);
                                    final MaterialEditText trackerId = (MaterialEditText) d.findViewById(R.id.trackerId);

                                    Preferences.setUsername(username.getText().toString());
                                    Preferences.setPassword(password.getText().toString());
                                    Preferences.setDeviceId(deviceId.getText().toString());
                                    Preferences.setTrackerId(trackerId.getText().toString());

                                    updateConnectButton();

                                }
                            })
                            .show();

                    return true;
                }
            };

            authenticationVal = Preferences.getAuth();
            identificationPreference = findPreference(getString(R.string.keyIdentification));
            identificationPreference.setOnPreferenceClickListener(identificationClickListener);

        }



        private void loadSecurityPreferences(final Activity a) {
            Preference.OnPreferenceClickListener securityListener = new Preference.OnPreferenceClickListener() {
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
                                    final MaterialEditText tlsCaCrt = (MaterialEditText) d.findViewById(R.id.tlsCaCrt);
                                    final MaterialEditText tlsClientCrt = (MaterialEditText) d.findViewById(R.id.tlsClientCrt);

                                    tls.setChecked(tlsVal);
                                    tlsCaCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                    tlsCaCrt.setText(Preferences.getTlsCaCrtPath());

                                    tlsClientCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                    tlsClientCrt.setText(Preferences.getTlsCaCrtPath());

                                    tls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            tlsVal = isChecked;
                                            tlsCaCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                            tlsClientCrt.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                        }
                                    });


                                }
                            })
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    MaterialEditText tlsCaCrt = (MaterialEditText) d.findViewById(R.id.tlsCaCrt);
                                    MaterialEditText tlsClientCrt = (MaterialEditText) d.findViewById(R.id.tlsClientCrt);

                                    Preferences.setTls(tlsVal);
                                    Preferences.setTlsCaCrtPath(tlsCaCrt.getText().toString());
                                    Preferences.setTlsCaCrtPath(tlsClientCrt.getText().toString());

                                    updateConnectButton();
                                }
                            })
                            .show();

                    return true;
                }
            };
            tlsVal = Preferences.getTls();
            securityPreference = findPreference(getString(R.string.keySecurity));
            securityPreference.setOnPreferenceClickListener(securityListener);


        }

        private void loadOptionsPreferences(final Activity a) {

            Preference.OnPreferenceClickListener optionsClickListener = new Preference.OnPreferenceClickListener() {
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
                                    Log.v(TAG, "saving parameters");
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText keepalive = (MaterialEditText) d.findViewById(R.id.keepalive);

                                    Preferences.setCleanSession(cleansessionVal);
                                    try {
                                        Preferences.setKeepalive(Integer.parseInt(keepalive.getText().toString()));

                                    } catch (NumberFormatException e) {
                                        Preferences.clearKey(R.string.keyKeepalive);
                                    }

                                    updateConnectButton();

                                }
                            })
                            .show();

                    return true;
                }
            };

            cleansessionVal = Preferences.getCleanSession();
            optionsPreference = findPreference(getString(R.string.keyOptions));
            optionsPreference.setOnPreferenceClickListener(optionsClickListener);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();
            PackageManager pm = a.getPackageManager();
            setHasOptionsMenu(true);


            Log.v(TAG, "Prepping preferences: " + Preferences.getModeId());


            if (Preferences.isModePrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
                addPreferencesFromResource(R.xml.preferences_private_connection);


                loadHostPreferences(a);
                loadSecurityPreferences(a);
                loadOptionsPreferences(a);
                loadIdentificationPreferences(a);

            } else if (Preferences.isModeHosted()) {
                Log.v(TAG, "Prepping hosted preferences");
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_HOSTED);
                addPreferencesFromResource(R.xml.preferences_hosted_connection);

                loadIdentificationPreferencesHosted(a);

            } else if (Preferences.isModePublic()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
                addPreferencesFromResource(R.xml.preferences_public_connection);
            } else {
                throw new RuntimeException("Unknown application mode");
            }

            mode = findPreference(Preferences.getKey(R.string.keyModeId));
            String[] modesReadable = getResources().getStringArray(R.array.profileIds_readable);
            mode.setSummary(modesReadable[Preferences.getModeId()]);
            mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Preferences.setMode(Integer.parseInt((String) newValue));
                    ActivityPreferencesConnection.modeSwitch = true; // signal that ConnectionPreferences should be shown again after fragment is restored
                    getFragmentManager().beginTransaction().remove(getFragmentManager().findFragmentByTag("preferences")).add(R.id.content_frame, new FragmentPreferences(), "preferences").commit();
                    return false; // Don't save, setMode already did
                }
            });
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.v(TAG, "onStart registerSticky");
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            Log.v(TAG, "onStop unregister");

            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            Log.v(TAG, "onEventMainThread StateChanged.ServiceBroker -> " + e.getState());
            if(cachedState != null) {
                if(e.getState() == ServiceBroker.State.CONNECTED) {
                    Snackbar.make(getActivity().findViewById(R.id.content_frame), R.string.snackbarConnected, Snackbar.LENGTH_LONG).show(); // Don’t forget to show!
                } else if (e.getState() == ServiceBroker.State.CONNECTING) {
                    Snackbar.make(getActivity().findViewById(R.id.content_frame), R.string.snackbarConnecting, Snackbar.LENGTH_LONG).show(); // Don’t forget to show!

                } else if (e.getState() == ServiceBroker.State.DISCONNECTED  || e.getState() == ServiceBroker.State.DISCONNECTED_USERDISCONNECT) {
                    Snackbar.make(getActivity().findViewById(R.id.content_frame), R.string.snackbarDisconnected, Snackbar.LENGTH_LONG).show(); // Don’t forget to show!
                } else if(e.getState() == ServiceBroker.State.DISCONNECTED_ERROR){
                    Snackbar.make(getActivity().findViewById(R.id.content_frame), R.string.snackbarDisconnectedError, Snackbar.LENGTH_LONG).show(); // Don’t forget to show!

                }

            }
            cachedState = e.getState(); // this event might arrive before options menu is ready. In this case onCreateOptionsmenu updates the button from the cachedState
            updateDisconnectButton(e.getState());
        }


        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            Log.v(TAG, "onCreateOptionsMenu");
            if(menu != null) {
                mMenu = menu;
                mInflater = inflater;
            } else if(mMenu == null || mInflater == null) {
                return;
            }

            mMenu.clear();
            mInflater.inflate(R.menu.preferences_connection, mMenu);

            updateDisconnectButton(cachedState);
            updateConnectButton();
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
                    return false;
            }
        }

        public static void updateDisconnectButton(ServiceBroker.State state) {
            if(mMenu == null || state == null)
                return;

            MenuItem disconnectButton = mMenu.findItem(R.id.disconnect);

            if(disconnectButton == null)
                return;

            disconnectButton.setEnabled(state == ServiceBroker.State.CONNECTED);
            disconnectButton.getIcon().setAlpha(state == ServiceBroker.State.CONNECTED ? 255 : 130);
        }

        public static void updateConnectButton() {
            Log.v("ActivityPreferencesCon", "updateConenctionbuttons");

            if(mMenu == null)
                return;

            MenuItem connectButton = mMenu.findItem(R.id.connect);

            if(connectButton == null )
                return;

            boolean canConnect = Preferences.canConnect();

            connectButton.setEnabled(canConnect);
            connectButton.getIcon().setAlpha(canConnect ? 255 : 130);
        }
    }
}




