package org.owntracks.android.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceMessage;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.ContentPathHelper;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.widgets.Toasts;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class ActivityPreferencesConnection extends ActivityBase {
    private static final String TAG = "ActivityPreferencesCon";

    private static boolean modeSwitch = false;
    public static final String KEY_MODE_CHANGED = "modeChanged";
    private WeakReference<FragmentPreferences> preferencesFragment;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);

        setSupportToolbar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferencesFragment = new WeakReference<>(new FragmentPreferences());
        getFragmentManager().beginTransaction().replace(R.id.content_frame, preferencesFragment.get(), "preferences").commit();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK && (requestCode == FILE_SELECT_CODE_TLS_CA_CRT_PATH || requestCode == FILE_SELECT_CODE_TLS_CLIENT_CRT_PATH )) {
            Uri uri = data.getData();
            Log.v(TAG, "onActivityResult() for uri:  " + uri.toString());
                if (requestCode == FILE_SELECT_CODE_TLS_CLIENT_CRT_PATH)
                    new ClientCrtCopyTask(preferencesFragment.get()).execute(uri);
                else
                    new CaCrtCopyTask(preferencesFragment.get()).execute(uri);


        }

        super.onActivityResult(requestCode, resultCode, data);
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


    private static final int FILE_SELECT_CODE_TLS_CLIENT_CRT_PATH = 1;
    private static final int FILE_SELECT_CODE_TLS_CA_CRT_PATH = 2;


    private void showFileChooser(int tag) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), tag);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
        }
    }

    public static class FragmentPreferences extends PreferenceFragment {

        private static Preference mode;
        private static Preference hostPreference;
        private static Preference identificationPreference;
        private static Preference securityPreference;
        private static MaterialDialog securityDialog;
        private static Preference optionsPreference;

        private static boolean tlsVal;
        private static boolean cleansessionVal;
        private static boolean authenticationVal;
        private static boolean wsVal;


        private SharedPreferences.OnSharedPreferenceChangeListener modeListener;

        private static Menu mMenu;
        private MenuInflater mInflater;
        ServiceMessage.EndpointState cachedState = null;
        private String tlsCaCrtName;
        private String tlsClientCrtName;

        private BarcodeScanListener barcodeCallback;

        private interface BarcodeScanListener {
            void onBarcodeScanResult(String url);
        }

        private void loadHostPreferencesMqtt(final Activity a) {
            Preference.OnPreferenceClickListener hostClickListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                        new MaterialDialog.Builder(a)
                            .customView(R.layout.preferences_host_mqtt, true)
                            .title(R.string.preferencesHost)
                            .positiveText(R.string.accept)
                            .negativeText(R.string.cancel)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText host = (MaterialEditText) d.findViewById(R.id.host);
                                    final MaterialEditText port = (MaterialEditText) d.findViewById(R.id.port);
                                    final Switch ws = (Switch)d.findViewById(R.id.ws);

                                    host.setText(Preferences.getHost());
                                    host.setFloatingLabelAlwaysShown(true);

                                    port.setText(Preferences.getPortWithHintSupport());
                                    port.setFloatingLabelAlwaysShown(true);
                                    ws.setChecked(wsVal);
                                    ws.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            wsVal = isChecked;
                                        }
                                    });

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
                                        Preferences.clearKey(Preferences.Keys.PORT);
                                    }

                                    Preferences.setWs(wsVal);
                                }
                            })

                            .show();

                    return true;
                }
            };


            hostPreference = findPreference(getString(R.string.preferencesKeyHost));
            hostPreference.setOnPreferenceClickListener(hostClickListener);
            wsVal = Preferences.getWs();

        }

        private void loadHostPreferencesHttp(final Activity a) {
            Preference.OnPreferenceClickListener hostClickListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new MaterialDialog.Builder(a)
                            .customView(R.layout.preferences_host_http, true)
                            .title(R.string.preferencesHost)
                            .positiveText(R.string.accept)
                            .negativeText(R.string.cancel)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText url = (MaterialEditText) d.findViewById(R.id.url);

                                    url.setText(Preferences.getUrl());
                                    url.setFloatingLabelAlwaysShown(true);

                                }
                            })

                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    final MaterialEditText url = (MaterialEditText) d.findViewById(R.id.url);


                                    Preferences.setUrl(url.getText().toString());
                                }
                            })

                            .show();

                    return true;
                }
            };


            hostPreference = findPreference(getString(R.string.preferencesKeyHost));
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
                                    trackerId.setText(Preferences.getTrackerId(false));
                                    trackerId.setHint(Preferences.getTrackerIdDefault());

                                    deviceId.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                                            if (s.length() >= 2)
                                                trackerId.setHint(s.toString().substring(deviceId.length() - 2));
                                            else
                                                trackerId.setHint(Preferences.getTrackerIdDefault());
                                        }

                                        @Override
                                        public void afterTextChanged(Editable s) {

                                        }
                                    });

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

                                    Preferences.setAuth(authenticationVal);
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
            identificationPreference = findPreference(getString(R.string.preferencesKeyIdentification));
            identificationPreference.setOnPreferenceClickListener(identificationClickListener);

        }




        private void loadSecurityPreferences(final Activity a) {


            Preference.OnPreferenceClickListener securityListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    securityDialog = new MaterialDialog.Builder(a)
                            .customView(R.layout.preferences_security, true)
                            .title(R.string.preferencesSecurity)
                            .positiveText(R.string.accept)
                            .negativeText(R.string.cancel)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    tlsCaCrtName = Preferences.getTlsCaCrtName();
                                    tlsClientCrtName = Preferences.getTlsClientCrtName();

                                    MaterialDialog d = MaterialDialog.class.cast(dialog);
                                    Switch tls = (Switch) d.findViewById(R.id.tls);

                                    final MaterialEditText tlsCaCrtNameView = (MaterialEditText) d.findViewById(R.id.tlsCaCrt);
                                    final MaterialEditText tlsClientCrtNameView = (MaterialEditText) d.findViewById(R.id.tlsClientCrt);
                                    final MaterialEditText tlsClientCrtPasswordView = (MaterialEditText) d.findViewById(R.id.tlsClientCrtPassword);


                                    d.findViewById(R.id.tlsWrapper).setVisibility(Preferences.isModeHttpPrivate() ? View.GONE : View.VISIBLE);
                                    tls.setChecked(tlsVal);
                                    tls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            tlsVal = isChecked;
                                            tlsCaCrtNameView.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                            tlsClientCrtNameView.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                            tlsClientCrtPasswordView.setVisibility(tlsVal && !"".equals(tlsClientCrtNameView.getText().toString()) ? View.VISIBLE : View.GONE);

                                        }
                                    });

                                    tlsCaCrtNameView.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                    tlsCaCrtNameView.setText(tlsCaCrtName);
                                    tlsClientCrtNameView.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                    tlsCaCrtNameView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tlsCaCrtNameView.setFocusable(true);
                                            tlsCaCrtNameView.setFocusableInTouchMode(true);
                                            tlsCaCrtNameView.requestFocus();
                                            PopupMenu popup = new PopupMenu(v.getContext(), tlsCaCrtNameView);
                                            popup.getMenuInflater().inflate(R.menu.picker, popup.getMenu());
                                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                public boolean onMenuItemClick(MenuItem item) {
                                                    if (item.getItemId() == R.id.clear) {
                                                        clearTlsCaCrtName();
                                                    } else if (item.getItemId() == R.id.select) {
                                                        ((ActivityPreferencesConnection) getActivity()).showFileChooser(FILE_SELECT_CODE_TLS_CA_CRT_PATH);
                                                    }

                                                    tlsCaCrtNameView.setFocusable(false);
                                                    tlsCaCrtNameView.setFocusableInTouchMode(false);


                                                    return true;

                                                }
                                            });
                                            popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                                                @Override
                                                public void onDismiss(PopupMenu menu) {
                                                    tlsCaCrtNameView.setFocusable(false);
                                                    tlsCaCrtNameView.setFocusableInTouchMode(false);
                                                }
                                            });


                                            popup.show();//showing popup menu

                                        }
                                    });

                                    tlsClientCrtNameView.setVisibility(tlsVal ? View.VISIBLE : View.GONE);
                                    tlsClientCrtNameView.setText(tlsClientCrtName);
                                    tlsClientCrtNameView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            tlsClientCrtNameView.setFocusable(true);
                                            tlsClientCrtNameView.setFocusableInTouchMode(true);
                                            tlsClientCrtNameView.requestFocus();
                                            PopupMenu popup = new PopupMenu(v.getContext(), tlsClientCrtNameView);
                                            popup.getMenuInflater().inflate(R.menu.picker, popup.getMenu());
                                            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                                public boolean onMenuItemClick(MenuItem item) {
                                                    if (item.getItemId() == R.id.clear) {
                                                        clearTlsClientCrtName();
                                                    } else if (item.getItemId() == R.id.select) {
                                                        ((ActivityPreferencesConnection) getActivity()).showFileChooser(FILE_SELECT_CODE_TLS_CLIENT_CRT_PATH);
                                                    }
                                                    tlsClientCrtNameView.setFocusable(false);
                                                    tlsClientCrtNameView.setFocusableInTouchMode(false);

                                                    return true;

                                                }
                                            });
                                            popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                                                @Override
                                                public void onDismiss(PopupMenu menu) {
                                                    tlsClientCrtNameView.setFocusable(false);
                                                    tlsClientCrtNameView.setFocusableInTouchMode(false);

                                                }
                                            });


                                            popup.show();//showing popup menu

                                        }
                                    });
                                    tlsClientCrtNameView.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                                        }

                                        @Override
                                        public void afterTextChanged(Editable s) {
                                            tlsClientCrtPasswordView.setVisibility("".equals(s.toString()) ? View.GONE : View.VISIBLE);
                                        }
                                    });

                                    tlsClientCrtPasswordView.setVisibility(tlsVal && !"".equals(tlsClientCrtNameView.getText().toString()) ? View.VISIBLE : View.GONE);
                                    tlsClientCrtPasswordView.setText(Preferences.getTlsClientCrtPassword());




                                }
                            })
                            .callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    MaterialDialog d = MaterialDialog.class.cast(dialog);

                                    Preferences.setTls(tlsVal);
                                    Preferences.setTlsCaCrt(tlsCaCrtName);
                                    Preferences.setTlsClientCrt(tlsClientCrtName);

                                    Log.v(TAG, "" + tlsCaCrtName + " " + tlsClientCrtName);

                                    Preferences.setTlsClientCrtPassword(((MaterialEditText) d.findViewById(R.id.tlsClientCrtPassword)).getText().toString());
                                    updateConnectButton();
                                    securityDialog = null;
                                }
                            })
                            .show();

                    return true;
                }
            };
            tlsVal = Preferences.getTls();
            securityPreference = findPreference(getString(R.string.preferencesKeySecurity));
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
                                        Preferences.clearKey(Preferences.Keys.KEEPALIVE);
                                    }

                                    updateConnectButton();

                                }
                            })
                            .show();

                    return true;
                }
            };

            cleansessionVal = Preferences.getCleanSession();
            optionsPreference = findPreference(getString(R.string.preferencesKeyParameters));
            optionsPreference.setOnPreferenceClickListener(optionsClickListener);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Activity a = getActivity();
            setHasOptionsMenu(true);


            Log.v(TAG, "Prepping preferences: " + Preferences.getModeId());


            if (Preferences.isModeMqttPrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PRIVATE);
                addPreferencesFromResource(R.xml.preferences_private_connection);


                loadHostPreferencesMqtt(a);
                loadSecurityPreferences(a);
                loadOptionsPreferences(a);
                loadIdentificationPreferences(a);

            } else if (Preferences.isModeMqttPublic()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_PUBLIC);
                addPreferencesFromResource(R.xml.preferences_public_connection);
            } else if(Preferences.isModeHttpPrivate()) {
                this.getPreferenceManager().setSharedPreferencesName(Preferences.FILENAME_HTTP);
                addPreferencesFromResource(R.xml.preferences_http_connection);
                loadHostPreferencesHttp(a);
                loadIdentificationPreferences(a);
                loadSecurityPreferences(a);

            } else {
                throw new RuntimeException("Unknown application mode");
            }

            mode = findPreference(Preferences.Keys.MODE_ID);
            mode.setSummary(ActivityPreferences.getModeIdReadable(getActivity()));
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



        @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
        public void onEvent(Events.EndpointStateChanged e) {
            Log.v(TAG, "onEventMainThread Events.EndpointStateChanged -> " + e.getState() + " cached " + cachedState);


            cachedState = e.getState(); // this event might arrive before options menu is ready. In this case onCreateOptionsmenu updates the button from the cachedState
            updateDisconnectButton(e.getState());
        }


        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            Log.v(TAG, "onCreateOptionsMenu");
            if (menu != null) {
                mMenu = menu;
                mInflater = inflater;
            } else if (mMenu == null || mInflater == null) {
                return;
            }



            mMenu.clear();
           if(Preferences.isModeHttpPrivate())
               return;

            mInflater.inflate(R.menu.preferences_connection_mqtt, mMenu);

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
                            ServiceProxy.getServiceMessage().reconnect();
                        }
                    };
                    new Thread(r).start();
                    return true;
                case R.id.disconnect:

                    Runnable s = new Runnable() {

                        @Override
                        public void run() {
                            ServiceProxy.getServiceMessage().disconnect();

                        }
                    };
                    new Thread(s).start();
                    return true;
                default:
                    return false;
            }
        }

        public static void updateDisconnectButton(ServiceMessage.EndpointState state) {
            if (mMenu == null || state == null)
                return;

            MenuItem disconnectButton = mMenu.findItem(R.id.disconnect);

            if (disconnectButton == null)
                return;

            disconnectButton.setEnabled(state == ServiceMessage.EndpointState.CONNECTED);
            disconnectButton.getIcon().setAlpha(state == ServiceMessage.EndpointState.CONNECTED ? 255 : 130);
        }

        public static void updateConnectButton() {
            Log.v("ActivityPreferencesCon", "updateConenctionbuttons");

            if (mMenu == null)
                return;

            MenuItem connectButton = mMenu.findItem(R.id.connect);

            if (connectButton == null)
                return;

            boolean canConnect = Preferences.canConnect();

            connectButton.setEnabled(canConnect);
            connectButton.getIcon().setAlpha(canConnect ? 255 : 130);
        }

        public void setTlsCaCrtName(String name) {
            if (securityDialog != null) {
                ((TextView) securityDialog.findViewById(R.id.tlsCaCrt)).setText(name);
                tlsCaCrtName = name;

            }
        }

        public void clearTlsCaCrtName() {
            setTlsCaCrtName("");
        }

        public void setTlsClientCrtName(String name) {
            if (securityDialog != null) {
                ((TextView) securityDialog.findViewById(R.id.tlsClientCrt)).setText(name);
                tlsClientCrtName = name;
            }
        }

        public void clearTlsClientCrtName() {
            setTlsClientCrtName("");
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
        }


    }

    private abstract static class CopyTask extends AsyncTask<Uri, String, String> {
        public static final Integer STATUS_SUCCESS = 1;
        public static final Integer STATUS_ERROR = 2;
        private final WeakReference<FragmentPreferences> reference;


        public CopyTask(FragmentPreferences fragment) {
            this.reference = new WeakReference<>(fragment);
        }

        public FragmentPreferences getFragement() {
            return reference.get();
        }

        @Override
        protected String doInBackground(Uri... params) {
            try {
                Log.v(TAG, "CopyTask with URI: " + params[0]);
                //String path = ContentPathHelper.getPath(App.getContext(), params[0]);
                String filename = ContentPathHelper.uriToFilename(App.getContext(), params[0]);
                Log.v(TAG, "filename for save is: " + filename);

                InputStream inputStream = App.getContext().getContentResolver().openInputStream(params[0]);
                FileOutputStream outputStream = App.getContext().openFileOutput(filename, Context.MODE_PRIVATE);

                byte [] buffer = new byte[256];
                int bytesRead;
                while((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
                Log.v(TAG, "copied file to private storage: " + filename);

                return filename;

            } catch (Exception e) {
                e.printStackTrace();
                this.cancel(true);
                return null;



            }
        }
    }

    private static class CaCrtCopyTask extends CopyTask {
        public CaCrtCopyTask(FragmentPreferences fragment) {
            super(fragment);
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null && getFragement() != null)
                getFragement().setTlsCaCrtName(s);
        }

        @Override
        protected void onCancelled(String s) {
            if (s != null && getFragement() != null)
                getFragement().clearTlsCaCrtName();
            Toasts.showUnableToCopyCertificateToast();
        }
    }

    private static class ClientCrtCopyTask extends CopyTask {
        public ClientCrtCopyTask(FragmentPreferences fragment) {
            super(fragment);
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null && getFragement() != null) {
                getFragement().setTlsClientCrtName(s);
                Toasts.showCopyCertificateSuccessToast();

            }

        }

        @Override
        protected void onCancelled(String s) {
            if (s != null && getFragement() != null)
                getFragement().clearTlsClientCrtName();
            Toasts.showUnableToCopyCertificateToast();
        }
    }

}




