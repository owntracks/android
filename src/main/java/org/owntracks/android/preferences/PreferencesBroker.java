package org.owntracks.android.preferences;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Defaults;
import org.owntracks.android.support.Preferences;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class PreferencesBroker extends DialogPreference {
	private EditText host;
	private EditText port;
	private EditText password;
	private static EditText userName;
	private EditText brokerSecuritySSLCaCrtPath;
	private Spinner brokerSecurity;
	private View brokerSecuritySSLOptions;
	private View brokerSecurityNoneOptions;
    private Spinner brokerAuth;
    private EditText deviceId;
    private EditText clientId;
    private CheckBox cleanSession;

	private LinearLayout securityWrapper;
	private LinearLayout brokerPasswordWrapper;
	private LinearLayout brokerAuthWrapper;
    private LinearLayout cleanSessionWrapper;
    private LinearLayout clientIdWrapper;

	private enum RequireablePreferences {
		USER_NAME, DEVICE_NAME, BROKER_HOST, BROKER_PORT, BROKER_PASSWORD, CACRT, DEVICE_ID
	};

	Set<RequireablePreferences> okPreferences = Collections.synchronizedSet(EnumSet.noneOf(RequireablePreferences.class));
	Set<RequireablePreferences> requiredPreferences = Collections.synchronizedSet(EnumSet.of(RequireablePreferences.BROKER_HOST, RequireablePreferences.USER_NAME,  RequireablePreferences.USER_NAME));

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
        this.cleanSessionWrapper = (LinearLayout) root.findViewById(R.id.cleanSessionWrapper);
        this.clientIdWrapper = (LinearLayout) root.findViewById(R.id.clientIdWrapper);
        this.host = (EditText) root.findViewById(R.id.brokerHost);
		this.port = (EditText) root.findViewById(R.id.brokerPort);
		this.userName = (EditText) root.findViewById(R.id.userName);
		this.password = (EditText) root.findViewById(R.id.brokerPassword);
		this.brokerSecurity = (Spinner) root.findViewById(R.id.brokerSecurity);
		this.brokerAuth = (Spinner) root.findViewById(R.id.brokerAuth);
        this.deviceId = (EditText) root.findViewById(R.id.deviceId);
        this.clientId = (EditText) root.findViewById(R.id.clientId);
		this.brokerSecurityNoneOptions = root.findViewById(R.id.brokerSecurityNoneOptions);
		this.brokerSecuritySSLOptions = root.findViewById(R.id.brokerSecuritySSLOptions);
		this.brokerSecuritySSLCaCrtPath = (EditText) root.findViewById(R.id.brokerSecuritySSLCaCrtPath);
        this.cleanSession= (CheckBox) root.findViewById(R.id.cleanSession);


		return root;
	}

	private void showHideAdvanced() {
		int visibility = Preferences.getConnectionAdvancedMode() ? View.VISIBLE : View.GONE;

        for (View v : new View[] {this.securityWrapper, this.brokerAuthWrapper, this.clientIdWrapper, this.cleanSessionWrapper })
            v.setVisibility(visibility);


	}

	@Override
	protected void onBindDialogView(View view) {
		this.host.setText(Preferences.getHost());
		this.port.setText(String.valueOf(Preferences.getPort()));

		this.userName.setText(Preferences.getUsername());
		this.password.setText(Preferences.getPassword());

		this.brokerAuth.setSelection(Preferences.getAuth() ? 1 : 0);

		this.brokerSecurity.setSelection(Preferences.getTls());
		this.brokerSecuritySSLCaCrtPath.setText(Preferences.getTlsCrtPath());


        this.deviceId.setText(Preferences.getDeviceId(false));
        this.deviceId.setHint(Preferences.getDeviceIdFallback());

        this.clientId.setText(Preferences.getClientId(false));
        this.clientId.setHint(Preferences.getClientIdFallback());

        this.cleanSession.setChecked(Preferences.getCleanSession());
	}


    private void updateClientIdHint() {
        this.clientId.setHint(getTmpClientIdFallback(userName.getText().toString(), this.deviceId.getText().toString()));
    }

	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);

		handleHost();
		handleBrokerAuth();
		handleUserName();
        handleDeviceId();
		handleBrokerSecurity();
		handleCaCrt();


		showHideAdvanced();

		conditionalyEnableConnectButton();
		conditionallyEnableDisconnectButton();

		this.host.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void afterTextChanged(Editable s) {
				handleHost();
			}
		});

		this.port.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

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
				PreferencesBroker.this.brokerSecurity.setSelection(App.getContext().getResources().getInteger(R.integer.valTls));
			}
		});

		this.userName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void afterTextChanged(Editable s) {
				handleUserName();
			}
		});

        this.deviceId.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                handleDeviceId();
            }
        });

		this.brokerSecuritySSLCaCrtPath.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void afterTextChanged(Editable s) {
                handleCaCrt();
            }
        });

    }

    private void handleDeviceId() {
        handleState(RequireablePreferences.DEVICE_ID, !deviceId.getText().toString().equals(""));
        updateClientIdHint();
    }

    private void handleCaCrt() {
		handleState(RequireablePreferences.CACRT, this.brokerSecuritySSLCaCrtPath.getText().toString().length() > 0);
	}

	private void handleBrokerAuth() {
		if (this.brokerAuth.getSelectedItemPosition() == 0)
			this.brokerPasswordWrapper.setVisibility(View.GONE);
		else
			this.brokerPasswordWrapper.setVisibility(View.VISIBLE);

		conditionalyEnableConnectButton();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE: // Clicked connect
			Preferences.setHost(this.host.getText().toString());
            try {Preferences.setPort(Integer.parseInt(this.port.getText().toString())); } catch (NumberFormatException e) {}
            Preferences.setUsername(userName.getText().toString());
            Preferences.setPassword(this.password.getText().toString());
            Preferences.setDeviceId(this.deviceId.getText().toString());
            Preferences.setClientId(this.clientId.getText().toString());
            Preferences.setAuth(this.brokerAuth.getSelectedItemPosition() == 1);
            Preferences.setTls(this.brokerSecurity.getSelectedItemPosition());
            Preferences.setTlsCrtPath(this.brokerSecuritySSLCaCrtPath.getText().toString());
            Preferences.setCleanSession(this.cleanSession.isChecked());
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
			handleState(RequireablePreferences.BROKER_HOST, this.host.getText()
					.toString().length() > 0);
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
			handleState(RequireablePreferences.BROKER_PORT, (p > 0)
					&& (p <= 65535));
		} catch (Exception e) {

			// If we cannot parse an integer from the input, it won't be a valid
			// port anyway
			handleState(RequireablePreferences.BROKER_PORT, false);
		}
	}

	private void handleUserName() {
		handleState(RequireablePreferences.USER_NAME, !userName.getText().toString().equals(""));
        updateClientIdHint();

	}

	private void conditionalyEnableConnectButton() {
		View v = getDialog().findViewById(android.R.id.button1);
		if (v == null)
			return;

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
		if (this.brokerSecurity.getSelectedItemPosition() == Preferences.getIntResource(R.integer.valTlsNone)) {
			this.brokerSecuritySSLOptions.setVisibility(View.GONE);
			this.brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
			this.requiredPreferences.remove(RequireablePreferences.CACRT);
		} else if (this.brokerSecurity.getSelectedItemPosition() == Preferences.getIntResource(R.integer.valTlsCustom)) {
			this.brokerSecuritySSLOptions.setVisibility(View.VISIBLE);
			this.brokerSecurityNoneOptions.setVisibility(View.GONE);
			this.requiredPreferences.add(RequireablePreferences.CACRT);
		} else {
			this.brokerSecuritySSLOptions.setVisibility(View.GONE);
			this.brokerSecurityNoneOptions.setVisibility(View.VISIBLE);
			this.requiredPreferences.remove(RequireablePreferences.CACRT);
		}
		conditionalyEnableConnectButton();

	}

    // A bit meh as the same logic is also implemented in Preferences, but this needs to read values
    // that are not yet saved to the preferences
    public static String getTmpClientIdFallback(String username, String deviceId) {
        String d;
        if(!"".equals(deviceId))
            d = deviceId;
        else
            d = Preferences.getAndroidId();

        return !"".equals(username) ? username+"/"+d : d;
    }
}
