
package st.alr.mqttitude.preferences;

import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.R;

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
import android.widget.Spinner;

public class PreferencesBroker extends DialogPreference {
    private Context context;
    private EditText host;
    private EditText port;
    private EditText username;
    private EditText password;
    private EditText clientId;
    private Spinner brokerSecurity;
    private View brokerSecuritySSLOptions;
    private View brokerSecurityNoneOptions;

    private EditText brokerSecuritySSLCaCrtPath;

    private boolean hostOk = false;
    private boolean portOk = false;
    private int securityOptions = 0;
    
    public PreferencesBroker(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        setDialogLayoutResource(R.layout.preferences_broker);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected View onCreateDialogView() {
        View root = super.onCreateDialogView();
        host = (EditText) root.findViewById(R.id.brokerHost);
        port = (EditText) root.findViewById(R.id.brokerPort);
        clientId = (EditText) root.findViewById(R.id.brokerClientId);
        username = (EditText) root.findViewById(R.id.brokerUsername);
        password = (EditText) root.findViewById(R.id.brokerPassword);
        brokerSecurity = (Spinner) root.findViewById(R.id.brokerSecurity);
        brokerSecurityNoneOptions = root.findViewById(R.id.brokerSecurityNoneOptions);
        brokerSecuritySSLOptions = root.findViewById(R.id.brokerSecuritySSLOptions);
        brokerSecuritySSLCaCrtPath = (EditText) root.findViewById(R.id.brokerSecuritySSLCaCrtPath);

        return root;
    }

    @Override
    protected void onBindDialogView(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        host.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_HOST, Defaults.VALUE_BROKER_HOST));
        port.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_PORT, Defaults.VALUE_BROKER_PORT));
        clientId.setHint(ServiceMqtt.getDefaultClientId());
        
        String cid = prefs.getString(Defaults.SETTINGS_KEY_BROKER_CLIENT_ID, "");
        if(!cid.equals(""))
            clientId.setText(cid);
        username.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_USERNAME, ""));
        password.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, ""));
        brokerSecurity.setSelection(PreferenceManager.getDefaultSharedPreferences(context).getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, Defaults.VALUE_BROKER_SECURITY_NONE));
        brokerSecuritySSLCaCrtPath.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, ""));

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        validateHost(PreferenceManager.getDefaultSharedPreferences(context).getString(
                Defaults.SETTINGS_KEY_BROKER_HOST, Defaults.VALUE_BROKER_HOST));
        validatePort(PreferenceManager.getDefaultSharedPreferences(context).getString(
                Defaults.SETTINGS_KEY_BROKER_PORT, Defaults.VALUE_BROKER_PORT));
        validateBrokerSecurity(PreferenceManager.getDefaultSharedPreferences(context).getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, Defaults.VALUE_BROKER_SECURITY_NONE));
        
        conditionalyEnableConnectButton();
        conditionallyEnableDisconnectButton();
        conditionallyEnableBrokerSecurityOptions();

        host.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateHost(s.toString());
                conditionalyEnableConnectButton();
            }
        });

        port.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePort(s.toString());
                conditionalyEnableConnectButton();
            }
        });

        brokerSecurity.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                validateBrokerSecurity(brokerSecurity.getSelectedItemPosition());
                conditionallyEnableBrokerSecurityOptions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                brokerSecurity.setSelection(Defaults.VALUE_BROKER_SECURITY_NONE);
            }
        });
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: // Clicked connect

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString(Defaults.SETTINGS_KEY_BROKER_HOST, host.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_PORT, port.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_USERNAME, username.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, password.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_CLIENT_ID, clientId.getText().toString());
                editor.putInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, brokerSecurity.getSelectedItemPosition());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, brokerSecuritySSLCaCrtPath.getText().toString());
                
                
                editor.apply();
                Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        ServiceMqtt.getInstance().reconnect();                        
                    }
                };
                new Thread( r ).start();

                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Runnable s = new Runnable() {
                    
                    @Override
                    public void run() {
                        ServiceMqtt.getInstance().disconnect(true);
                        
                    }
                };
                new Thread( s ).start();
        }
        super.onClick(dialog, which);
    }

    private void validateHost(String s) {
        hostOk = s.length() > 0 ? true : false;
    }

    private void validatePort(String s) {
        Integer p = 0;
        try {
            p = Integer.parseInt(s.toString());
        } catch (NumberFormatException e) {
        }

        portOk = (p > 0) && (p <= 65535) ? true : false;
    }

    private void conditionalyEnableConnectButton() {
        View v = getDialog().findViewById(android.R.id.button1);
        if (v == null)
            return;
        Log.v("hostOk", "" + hostOk);
        Log.v("portOk", "" + portOk);

        if (hostOk && portOk)
            v.setEnabled(true);
        else
            v.setEnabled(false);

    }
    
    private void validateBrokerSecurity(int val) {
        securityOptions = val;
    }

    private void conditionallyEnableDisconnectButton() {
        View v = getDialog().findViewById(android.R.id.button2);
        if (v == null)
            return;

        if (ServiceMqtt.getState() == Defaults.State.ServiceMqtt.CONNECTING
                || ServiceMqtt.getState() == Defaults.State.ServiceMqtt.CONNECTED) {
            v.setEnabled(true);
        } else {
            v.setEnabled(false);
        }

    }

    private void conditionallyEnableBrokerSecurityOptions() {
        switch (securityOptions) {
            case Defaults.VALUE_BROKER_SECURITY_NONE:
                brokerSecuritySSLOptions.setVisibility(View.GONE);
                brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                break;
            case Defaults.VALUE_BROKER_SECURITY_SSL:
                brokerSecuritySSLOptions.setVisibility(View.GONE);
                brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                break;
            case Defaults.VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT:
                brokerSecuritySSLOptions.setVisibility(View.VISIBLE);
                brokerSecurityNoneOptions.setVisibility(View.GONE);
                break;

            default:
                
        }
    }
}
