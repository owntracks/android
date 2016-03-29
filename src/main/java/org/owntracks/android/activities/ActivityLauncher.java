package org.owntracks.android.activities;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceApplication;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class ActivityLauncher extends ActivityBase {
	private static final String TAG = "ActivityLauncher";

	private static final int RESULT_WIZZARD = 1;
	private static final int RESULT_PLAY_SERVICES = 2;

	private boolean autostart = false;

	private ServiceConnection serviceApplicationConnection;
	private Context context;
	private boolean playServicesOk = false;


	public static class ErrorDialogFragment extends DialogFragment {
		private Dialog mDialog;

		public ErrorDialogFragment() {
			super();
			this.mDialog = null;
		}

		public void setDialog(Dialog dialog) {
			this.mDialog = dialog;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return this.mDialog;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			Log.e(TAG, "Killing application");
			getActivity().finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;

		if ((savedInstanceState != null) && savedInstanceState.getBoolean("autostart"))
			this.autostart = true;

		setContentView(R.layout.activity_launcher);

	}

	@Override
	protected void onResume() {
		super.onResume();
		runChecks();
	}

	private boolean checkSetup() {
		if(Preferences.getSetupCompleted()) {
			return true;
		} else {
			startActivityWelcome();
			return false;
		}
	}



	private void runChecks() {
		checkSetup();
		checkPlayServices();


		if (checkSetup() && checkPlayServices())
			launchChecksComplete();

	}


	private void checkPermissions() {
	}

	private boolean checkPlayServices() {

		if (ServiceApplication.checkPlayServices()) {
			return true;
		} else {
			GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();


			int result = googleAPI.isGooglePlayServicesAvailable(this);
			if (googleAPI.isUserResolvableError(result)) {
				//googleAPI.getErrorDialog(this, result, RESULT_PLAY_SERVICES).show();
				googleAPI.showErrorDialogFragment(this, result, RESULT_PLAY_SERVICES);
			} else {
				showQuitError(GoogleApiAvailability.getInstance().getErrorString(result));
			}

			return false;
		}
	}







	private void showQuitError(String error) {
		AlertDialog.Builder popupBuilder = new AlertDialog.Builder(this);
		popupBuilder.setMessage(error);
		popupBuilder.setTitle("Error");
		popupBuilder.setNegativeButton("Quit application",
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						quitApplication();
					}
				});
		ErrorDialogFragment errorFragment = new ErrorDialogFragment();
		errorFragment.setDialog(popupBuilder.create());

		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.add(errorFragment, "playServicesErrorFragmentNotAvailable");
		transaction.commitAllowingStateLoss();
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.v(TAG, "onActivityResult. RequestCode = " + requestCode + ", resultCode " + resultCode);
		if (requestCode == RESULT_PLAY_SERVICES) {
			if (resultCode != RESULT_OK) {
				showQuitError("Google Play Services must be installed");
			} else {
				Log.v(TAG, "Play services activated successfully");
				runChecks();
			}
			return;
		} else if (resultCode == RESULT_WIZZARD) {
			runChecks();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void quitApplication() {
		Log.e(TAG, "Killing application");
		finish();
	}

	private void launchChecksComplete() {
		Log.v(TAG, "Launch checks complete");
		Intent i = new Intent(this, ServiceProxy.class);
		startService(i);
		this.serviceApplicationConnection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
                if (!ActivityLauncher.this.autostart)
                    startActivityMain();
            }
		};

		bindService(i, this.serviceApplicationConnection, Context.BIND_AUTO_CREATE);
	}
	private void startActivityWelcome() {
		Intent intent = new Intent(this.context, ActivityWelcome.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivityForResult(intent, RESULT_WIZZARD);

	}

	private void startActivityMain() {
		startActivityFromClass(App.getRootActivityClass());
	}

	private void startActivityFromClass(Class<?> c) {
		Intent intent = new Intent(this.context, c);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	protected void onDestroy() {
		if (this.serviceApplicationConnection != null)
			unbindService(this.serviceApplicationConnection);
		super.onDestroy();
	}
}
