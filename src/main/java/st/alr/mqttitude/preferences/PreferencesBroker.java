package st.alr.mqttitude.preferences;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.services.ServiceBroker;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Preferences;
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

	private EditText deviceName;
	private static EditText userName;

	private EditText brokerSecuritySSLCaCrtPath;

	private Spinner brokerSecurity;
	private View brokerSecuritySSLOptions;
	private View brokerSecurityNoneOptions;
    private View clientIdNegotiationManualOptions;
    private View clientIdNegotiationAutoOptions;


    private Spinner brokerAuth;
    private Spinner clientIdNegotiation;
    private EditText clientId;
    private CheckBox cleanSession;


	private LinearLayout securityWrapper;
	private LinearLayout brokerPasswordWrapper;
	private LinearLayout brokerAuthWrapper;
    private LinearLayout clientIdNegotiationWrapper;
    private LinearLayout cleanSessionWrapper;

	private enum RequireablePreferences {
		USER_NAME, DEVICE_NAME, BROKER_HOST, BROKER_PORT, BROKER_PASSWORD, CACRT, CLIENT_ID
	};

	Set<RequireablePreferences> okPreferences = Collections
			.synchronizedSet(EnumSet.noneOf(RequireablePreferences.class));
	Set<RequireablePreferences> requiredPreferences = Collections
			.synchronizedSet(EnumSet.of(RequireablePreferences.BROKER_HOST,
					RequireablePreferences.USER_NAME));

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
        this.clientIdNegotiationWrapper =(LinearLayout) root.findViewById(R.id.clientIdNegotiationWrapper);
        this.cleanSessionWrapper = (LinearLayout) root.findViewById(R.id.cleanSessionWrapper);
        this.host = (EditText) root.findViewById(R.id.brokerHost);
		this.port = (EditText) root.findViewById(R.id.brokerPort);
		this.deviceName = (EditText) root.findViewById(R.id.deviceName);
		this.userName = (EditText) root.findViewById(R.id.userName);
		this.password = (EditText) root.findViewById(R.id.brokerPassword);
		this.brokerSecurity = (Spinner) root.findViewById(R.id.brokerSecurity);
		this.brokerAuth = (Spinner) root.findViewById(R.id.brokerAuth);
        this.clientIdNegotiation = (Spinner) root.findViewById(R.id.clientIdNegotiation);
        this.clientId = (EditText) root.findViewById(R.id.clientId);
		this.brokerSecurityNoneOptions = root.findViewById(R.id.brokerSecurityNoneOptions);
		this.brokerSecuritySSLOptions = root.findViewById(R.id.brokerSecuritySSLOptions);
		this.brokerSecuritySSLCaCrtPath = (EditText) root.findViewById(R.id.brokerSecuritySSLCaCrtPath);
        this.clientIdNegotiationAutoOptions = root.findViewById(R.id.clientIdNegotiationAutoOptions);
        this.clientIdNegotiationManualOptions = root.findViewById(R.id.clientIdNegotiationManualOptions);
        this.cleanSession= (CheckBox) root.findViewById(R.id.cleanSession);


		return root;
	}

	private void showHideAdvanced() {
		int visibility = Preferences.getConnectionAdvancedMode() ? View.VISIBLE : View.GONE;

        // TODO: Waiting for paho lib to support zero length client ids
        // for (View v : new View[] {this.clientIdNegotiationWrapper, this.securityWrapper, this.brokerAuthWrapper })
		//	v.setVisibility(visibility);

        for (View v : new View[] {this.securityWrapper, this.brokerAuthWrapper, this.cleanSessionWrapper })
            v.setVisibility(visibility);


	}

	@Override
	protected void onBindDialogView(View view) {
		this.host.setText(Preferences.getHost());
		this.port.setText(String.valueOf(Preferences.getPort()));

		userName.setText(Preferences.getUsername());

		this.deviceName.setHint(Preferences.getAndroidId());
		this.deviceName.setText(Preferences.getDeviceId(false));

		this.password.setText(Preferences.getPassword());

		this.brokerAuth.setSelection(Preferences.getAuth() ? 1 : 0);

		this.brokerSecurity.setSelection(Preferences.getTls());
		this.brokerSecuritySSLCaCrtPath.setText(Preferences.getTlsCrtPath());

        this.clientIdNegotiation.setSelection(Preferences.getZeroLenghClientId() ? 0 : 1);
        this.clientId.setHint(Preferences.getAndroidId());
        this.clientId.setText(Preferences.getClientId(false));

        this.cleanSession.setChecked(Preferences.getCleanSession());
	}

	@Override
	protected void showDialog(Bundle state) {
		super.showDialog(state);

		handleHost();
		handleBrokerAuth();
		handleUserName();
        // TODO: Waiting for paho lib to support zero length client ids
        //handleClientId();
		handleBrokerSecurity();
		handleCaCrt();


		showHideAdvanced();

		conditionalyEnableConnectButton();
		conditionallyEnableDisconnectButton();

		this.host.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				handleHost();
			}
		});

		this.port.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				handlePort();
			}
		});

		this.brokerSecurity
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						handleBrokerSecurity();
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		this.brokerAuth.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				handleBrokerAuth();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				PreferencesBroker.this.brokerSecurity.setSelection(App
						.getContext().getResources()
						.getInteger(R.integer.valTls));
			}
		});

        this.clientIdNegotiation.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleClientId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                PreferencesBroker.this.clientIdNegotiation.setSelection(0);
            }
        });

		userName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				handleUserName();
			}
		});
		this.brokerSecuritySSLCaCrtPath
				.addTextChangedListener(new TextWatcher() {
					@Override
					public void onTextChanged(CharSequence s, int start,
							int before, int count) {
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start,
							int count, int after) {
					}

					@Override
					public void afterTextChanged(Editable s) {
						handleCaCrt();
					}
				});

    }

    private void handleClientId() {
        boolean auto = this.clientIdNegotiation.getSelectedItemPosition() == 1;
        Log.v(this.toString(), "ClientId: auto == " + auto);
        this.clientIdNegotiationManualOptions.setVisibility(auto ? View.GONE : View.VISIBLE);
        this.clientIdNegotiationAutoOptions.setVisibility(auto ? View.VISIBLE : View.GONE);
    }

    private void handleCaCrt() {
		handleState(
				RequireablePreferences.CACRT,
				this.brokerSecuritySSLCaCrtPath.getText().toString().length() > 0);

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
            Preferences.setDeviceId(this.deviceName.getText().toString());
            Preferences.setAuth(this.brokerAuth.getSelectedItemPosition() == 1);
            Preferences.setTls(this.brokerSecurity.getSelectedItemPosition());
            Preferences.setTlsCrtPath(this.brokerSecuritySSLCaCrtPath.getText().toString());
            Preferences.setCleanSession(this.cleanSession.isChecked());

            //Preferences.setClientId(this.clientId.getText().toString());
            //Preferences.setZeroLenghClientId(this.clientIdNegotiation.getSelectedItemPosition() == 1);

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
		handleState(RequireablePreferences.USER_NAME, !userName.getText()
				.toString().equals(""));
	}

	private void conditionalyEnableConnectButton() {
		View v = getDialog().findViewById(android.R.id.button1);
		if (v == null)
			return;

//		Log.v("Required for connect: ", this.requiredPreferences.toString());
//		Log.v("Currently set", this.okPreferences.toString());

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
}
