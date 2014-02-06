
package st.alr.mqttitude.preferences;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceBroker;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Preferences;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class PreferencesBroker extends DialogPreference {
    private EditText host;
    private EditText port;
    private EditText password;

    private EditText deviceName;
    private static EditText userName;

    private EditText brokerSecuritySSLCaCrtPath;

    private Spinner brokerSecurity;
    private View brokerSecuritySSLOptions;
    private View brokerSecurityNoneOptions;
    private Spinner brokerAuth;

    private LinearLayout securityWrapper;
    private LinearLayout brokerPasswordWrapper;
    private LinearLayout brokerAuthWrapper;

    private enum RequireablePreferences {
        USER_NAME, DEVICE_NAME, BROKER_HOST, BROKER_PORT, BROKER_PASSWORD, CACRT
    };

    Set<RequireablePreferences> okPreferences = Collections.synchronizedSet(EnumSet.noneOf(RequireablePreferences.class));
    Set<RequireablePreferences> requiredPreferences = Collections.synchronizedSet(EnumSet.of(RequireablePreferences.BROKER_HOST, RequireablePreferences.USER_NAME));

    public PreferencesBroker(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.preferences_broker);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected View onCreateDialogView() {
        View root = super.onCreateDialogView();

        this.brokerAuthWrapper = (LinearLayout) root.findViewById(R.id.brokerAuthWrapper);
        this.brokerPasswordWrapper = (LinearLayout) root.findViewById(R.id.brokerPasswordWrapper);
        this.securityWrapper = (LinearLayout) root.findViewById(R.id.securityWrapper);

        this.host = (EditText) root.findViewById(R.id.brokerHost);
        this.port = (EditText) root.findViewById(R.id.brokerPort);
        this.deviceName = (EditText) root.findViewById(R.id.deviceName);
        userName = (EditText) root.findViewById(R.id.userName);

        this.password = (EditText) root.findViewById(R.id.brokerPassword);
        this.brokerSecurity = (Spinner) root.findViewById(R.id.brokerSecurity);
        this.brokerAuth = (Spinner) root.findViewById(R.id.brokerAuth);

        this.brokerSecurityNoneOptions = root.findViewById(R.id.brokerSecurityNoneOptions);
        this.brokerSecuritySSLOptions = root.findViewById(R.id.brokerSecuritySSLOptions);
        this.brokerSecuritySSLCaCrtPath = (EditText) root.findViewById(R.id.brokerSecuritySSLCaCrtPath);

        return root;
    }

    private void showHideAdvanced() {
        int visibility = Preferences.isAdvancedModeEnabled() ? View.VISIBLE : View.GONE;

        for (View v : new View[] {
                this.securityWrapper, this.brokerAuthWrapper
        })
            v.setVisibility(visibility);

    }

    @Override
    protected void onBindDialogView(View view) {
        this.host.setText(Preferences.getBrokerHost());
        this.port.setText(Preferences.getBrokerPort());
        this.port.setHint(Defaults.VALUE_BROKER_PORT);

        userName.setText(Preferences.getBrokerUsername());

        this.deviceName.setHint(Preferences.getAndroidId());
        this.deviceName.setText(Preferences.getDeviceName(false));

        this.password.setText(Preferences.getBrokerPassword());

        this.brokerAuth.setSelection(Preferences.getBrokerAuthType());

        this.brokerSecurity.setSelection(Preferences.getBrokerSecurityType());
        this.brokerSecuritySSLCaCrtPath.setText(Preferences.getBrokerSslCaPath());

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        handleHost();
        handleBrokerAuth();
        handleUserName();
        handleBrokerSecurity();
        handleCaCrt();

        showHideAdvanced();

        conditionalyEnableConnectButton();
        conditionallyEnableDisconnectButton();

        this.host.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handleHost();
            }
        });

        this.port.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handlePort();
            }
        });

        this.brokerSecurity.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                handleBrokerSecurity();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        this.brokerAuth.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                handleBrokerAuth();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                PreferencesBroker.this.brokerSecurity.setSelection(Defaults.VALUE_BROKER_SECURITY_SSL);
            }
        });

        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handleUserName();
            }
        });
        this.brokerSecuritySSLCaCrtPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handleCaCrt();
            }
        });

    }

    private void handleCaCrt() {
        handleState(RequireablePreferences.CACRT, this.brokerSecuritySSLCaCrtPath.getText().toString().length() > 0);

    }

    private void handleBrokerAuth() {
        switch (this.brokerAuth.getSelectedItemPosition()) {
            case Defaults.VALUE_BROKER_AUTH_ANONYMOUS:
                this.brokerPasswordWrapper.setVisibility(View.GONE);

                break;
            default:
                // We do not require a passwort as it might be empty (stupid but
                // possible)
                this.brokerPasswordWrapper.setVisibility(View.VISIBLE);

                break;

        }
        conditionalyEnableConnectButton();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: // Clicked connect

                Preferences.setString(R.string.keyBrokerHost, this.host.getText().toString());
                Preferences.setString(R.string.keyBrokerPort, this.port.getText().toString());
                Preferences.setString(R.string.keyBrokerUsername, userName.getText().toString());
                Preferences.setString(R.string.keyBrokerPassword, this.password.getText().toString());
                Preferences.setString(R.string.keyDeviceName, this.deviceName.getText().toString());
                Preferences.setInt(R.string.keyBrokerAuth, this.brokerAuth.getSelectedItemPosition());
                Preferences.setInt(R.string.keyBrokerSecurity, this.brokerSecurity.getSelectedItemPosition());
                Preferences.setString(R.string.keyBrokerSecuritySslCaPath, this.brokerSecuritySSLCaCrtPath.getText().toString());
                
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().reconnect();
                    }
                };
                new Thread(r).start();

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Runnable s = new Runnable() {

                    @Override
                    public void run() {
                        ServiceProxy.getServiceBroker().disconnect(true);

                    }
                };
                new Thread(s).start();
        }
        super.onClick(dialog, which);
    }

    private void handleState(RequireablePreferences p, boolean ok) {
        if (ok)
            this.okPreferences.add(p);
        else
            this.okPreferences.remove(p);

        conditionalyEnableConnectButton();
    }

    private void handleHost() {
        try {
            handleState(RequireablePreferences.BROKER_HOST, this.host.getText().toString().length() > 0);
        } catch (Exception e) {
            handleState(RequireablePreferences.BROKER_HOST, false);
        }
    }

    private void handlePort() {
        String portStr = this.port.getText().toString();
        if (portStr.equals(""))
            handleState(RequireablePreferences.BROKER_PORT, true); 
        // If port is not empty, we have to validate if port is 0 <= p <= 65535
        try {
            Integer p = Integer.parseInt(this.port.getText().toString());
            handleState(RequireablePreferences.BROKER_PORT, (p > 0) && (p <= 65535));
        } catch (Exception e) {

            // If we cannot parse an integer from the input, it won't be a valid
            // port anyway
            handleState(RequireablePreferences.BROKER_PORT, false);
        }
    }

    private void handleUserName() {
        handleState(RequireablePreferences.USER_NAME, !userName.getText().toString().equals(""));
    }

    private void conditionalyEnableConnectButton() {
        View v = getDialog().findViewById(android.R.id.button1);
        if (v == null)
            return;

        Log.v("Required for connect: ", this.requiredPreferences.toString());
        Log.v("Currently set", this.okPreferences.toString());

        v.setEnabled(this.okPreferences.containsAll(this.requiredPreferences));
    }

    private void conditionallyEnableDisconnectButton() {
        View v = getDialog().findViewById(android.R.id.button2);
        if (v == null)
            return;

        if ((ServiceBroker.getState() == Defaults.State.ServiceBroker.CONNECTING)
                || (ServiceBroker.getState() == Defaults.State.ServiceBroker.CONNECTED)) {
            v.setEnabled(true);
        } else {
            v.setEnabled(false);
        }

    }

    private void handleBrokerSecurity() {
        switch (this.brokerSecurity.getSelectedItemPosition()) {
            case Defaults.VALUE_BROKER_SECURITY_NONE:
                this.brokerSecuritySSLOptions.setVisibility(View.GONE);
                this.brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                this.requiredPreferences.remove(RequireablePreferences.CACRT);
                break;
            case Defaults.VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT:
                this.brokerSecuritySSLOptions.setVisibility(View.VISIBLE);
                this.brokerSecurityNoneOptions.setVisibility(View.GONE);
                this.requiredPreferences.add(RequireablePreferences.CACRT);
                break;

            default:
                this.brokerSecuritySSLOptions.setVisibility(View.GONE);
                this.brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                this.requiredPreferences.remove(RequireablePreferences.CACRT);
                break;
        }
        conditionalyEnableConnectButton();

    }
}
