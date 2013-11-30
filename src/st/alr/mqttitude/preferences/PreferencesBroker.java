
package st.alr.mqttitude.preferences;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceMqtt;
import st.alr.mqttitude.support.Defaults;
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
    private Context context;
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
    
    
    
    
    private enum RequireablePreferences { USER_NAME, DEVICE_NAME, BROKER_HOST, BROKER_PORT, BROKER_PASSWORD, CACRT};
    
    Set<RequireablePreferences> okPreferences = Collections.synchronizedSet(EnumSet.noneOf(RequireablePreferences.class));
    Set<RequireablePreferences> requiredPreferences = Collections.synchronizedSet(EnumSet.of(RequireablePreferences.BROKER_HOST, RequireablePreferences.USER_NAME));

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

        brokerAuthWrapper = (LinearLayout) root.findViewById(R.id.brokerAuthWrapper);
        brokerPasswordWrapper = (LinearLayout) root.findViewById(R.id.brokerPasswordWrapper);
        securityWrapper = (LinearLayout) root.findViewById(R.id.securityWrapper);

        

        
        host = (EditText) root.findViewById(R.id.brokerHost);
        port = (EditText) root.findViewById(R.id.brokerPort);
        deviceName = (EditText) root.findViewById(R.id.deviceName);
        userName = (EditText) root.findViewById(R.id.userName);

        password = (EditText) root.findViewById(R.id.brokerPassword);
        brokerSecurity = (Spinner) root.findViewById(R.id.brokerSecurity);
        brokerAuth = (Spinner) root.findViewById(R.id.brokerAuth);

        brokerSecurityNoneOptions = root.findViewById(R.id.brokerSecurityNoneOptions);
        brokerSecuritySSLOptions = root.findViewById(R.id.brokerSecuritySSLOptions);
        brokerSecuritySSLCaCrtPath = (EditText) root.findViewById(R.id.brokerSecuritySSLCaCrtPath);        
        
        return root;
    }

    private void showHideAdvanced() {
        int visibility = ActivityPreferences.isAdvancedModeEnabled() ? View.VISIBLE : View.GONE; 

        
        for(View v : new View[]{ securityWrapper, brokerAuthWrapper})
            v.setVisibility(visibility);        
        
    }

    @Override
    protected void onBindDialogView(View view) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        host.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_HOST, ""));
        port.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_PORT, ""));
        port.setHint(Defaults.VALUE_BROKER_PORT);
        
        userName.setText(ActivityPreferences.getUsername());
        
        deviceName.setHint(ActivityPreferences.getAndroidId());
        deviceName.setText(ActivityPreferences.getDeviceName(false));
                
        password.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, ""));

        brokerAuth.setSelection(PreferenceManager.getDefaultSharedPreferences(context).getInt(Defaults.SETTINGS_KEY_BROKER_AUTH, Defaults.VALUE_BROKER_AUTH_USERNAME));

        brokerSecurity.setSelection(PreferenceManager.getDefaultSharedPreferences(context).getInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, Defaults.VALUE_BROKER_SECURITY_SSL));        
        brokerSecuritySSLCaCrtPath.setText(prefs.getString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, ""));

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

        
        host.addTextChangedListener(new TextWatcher() {
            
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

        port.addTextChangedListener(new TextWatcher() {
            
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
        
        
        
        
        brokerSecurity.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                handleBrokerSecurity();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
 
        
        brokerAuth.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                handleBrokerAuth();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                brokerSecurity.setSelection(Defaults.VALUE_BROKER_SECURITY_SSL);
            }
        });
        
        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                handleUserName();                
            }
        });
        brokerSecuritySSLCaCrtPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                handleCaCrt();                
            }
        });

    }
    
    private void handleCaCrt() {
        handleState(RequireablePreferences.CACRT, brokerSecuritySSLCaCrtPath.getText().toString().length() > 0);
        
    }

    private void handleBrokerAuth() {
        switch (brokerAuth.getSelectedItemPosition()) {
            case Defaults.VALUE_BROKER_AUTH_ANONYMOUS:
                brokerPasswordWrapper.setVisibility(View.GONE);
                break;
                default:
                // We do not require a passwort as it might be empty (stupid but possible)
                brokerPasswordWrapper.setVisibility(View.VISIBLE);
                break;

        }        
        conditionalyEnableConnectButton();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: // Clicked connect

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                editor.putString(Defaults.SETTINGS_KEY_BROKER_HOST, host.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_PORT, port.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_USER_NAME, userName.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_PASSWORD, password.getText().toString());
                editor.putString(Defaults.SETTINGS_KEY_DEVICE_NAME, deviceName.getText().toString());
                editor.putInt(Defaults.SETTINGS_KEY_BROKER_SECURITY, brokerSecurity.getSelectedItemPosition());
                editor.putInt(Defaults.SETTINGS_KEY_BROKER_AUTH, brokerAuth.getSelectedItemPosition());
                editor.putString(Defaults.SETTINGS_KEY_BROKER_SECURITY_SSL_CA_PATH, brokerSecuritySSLCaCrtPath.getText().toString());
                

                editor.apply()
                
                ;
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
    
    private void handleState(RequireablePreferences p, boolean ok) {
        if(ok)
            okPreferences.add(p);
        else
            okPreferences.remove(p);
        
        conditionalyEnableConnectButton();
    }


    private void handleHost() {
        try {
            handleState(RequireablePreferences.BROKER_HOST, host.getText().toString().length() > 0);
        } catch (Exception e) {
            handleState(RequireablePreferences.BROKER_HOST, false);
        }
    }

    private void handlePort() {
        String portStr = port.getText().toString();
        if(portStr.equals(""))
            handleState(RequireablePreferences.BROKER_PORT, true); // empty port is ok and defaults to 8883. 
        
        
        //If port is not empty, we have to validate if port is 0 <= p <= 65535
        try {            
            Integer p = Integer.parseInt(port.getText().toString());
            handleState(RequireablePreferences.BROKER_PORT, (p > 0) && (p <= 65535));
        } catch (Exception e) {
            
            // If we cannot parse an integer from the input, it won't be a valid port anyway
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
                
        Log.v("Required for connect: ", requiredPreferences.toString() );
        Log.v("Currently set",okPreferences.toString());

        v.setEnabled(okPreferences.containsAll(requiredPreferences));
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

    private void handleBrokerSecurity() {
        switch (brokerSecurity.getSelectedItemPosition()) {
            case Defaults.VALUE_BROKER_SECURITY_NONE:
                brokerSecuritySSLOptions.setVisibility(View.GONE);
                brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                requiredPreferences.remove(RequireablePreferences.CACRT);
                break;
            case Defaults.VALUE_BROKER_SECURITY_SSL_CUSTOMCACRT:
                brokerSecuritySSLOptions.setVisibility(View.VISIBLE);
                brokerSecurityNoneOptions.setVisibility(View.GONE);
                requiredPreferences.add(RequireablePreferences.CACRT);
                break;

            default:
                brokerSecuritySSLOptions.setVisibility(View.GONE);
                brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
                requiredPreferences.remove(RequireablePreferences.CACRT);
                break;
        }
        conditionalyEnableConnectButton();

    }
}
