package org.owntracks.android.activities;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.services.ServiceApplication;
import org.owntracks.android.services.ServiceProxy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class ActivityLauncher extends ActivityBase {
	private static final String TAG = "ActivityLauncher";

	public final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	public static boolean playServicesAvailable;
	private boolean autostart = false;

	private ServiceConnection serviceApplicationConnection;
	private Context context;

	private static void showPlayServicesNotAvilableNotification() {
		NotificationCompat.Builder nb = new NotificationCompat.Builder(
				App.getContext());
		NotificationManager nm = (NotificationManager) App.getContext()
				.getSystemService(NOTIFICATION_SERVICE);

		nb.setContentTitle(App.getContext().getString(R.string.app_name))
				.setSmallIcon(R.drawable.ic_notification)
				.setContentText("Google Play Services are not available")
				.setPriority(NotificationCompat.PRIORITY_MIN);
		nm.notify(ServiceApplication.NOTIFCATION_ID, nb.build());

	}

	public void showGooglePlayServicesError(int resultCode){
		Dialog errorDialog = GooglePlayServicesUtil
				.getErrorDialog(resultCode, this,
						ActivityLauncher.CONNECTION_FAILURE_RESOLUTION_REQUEST);

		if (errorDialog != null) {
			// Log.v(TAG, "Showing error recovery dialog");
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();
			errorFragment.setDialog(errorDialog);

			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.add(errorFragment,
					"playServicesErrorFragmentEnable");
			transaction.commitAllowingStateLoss();
		}
	}



	public static boolean checkPlayServices(ActivityLauncher caller) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(App.getContext());
		playServicesAvailable = ConnectionResult.SUCCESS == resultCode;
		if (playServicesAvailable) {
//            App.mapFragmentClass=ActivityMain.GoogleMapFragment.class;
			App.mapFragmentClass=ActivityMain.MapFragment.class;

		} else {
            Log.e("checkPlayServices", "Google Play services not available. Result code " + resultCode);
            showPlayServicesNotAvilableNotification();

			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Log.v(TAG, "Showing error recovery dialog");
				if(caller!=null){
					caller.showGooglePlayServicesError(resultCode);
				}
			} else {
				if(caller!=null) {

					caller.showQuitError();
				}
			}
        }

        playServicesAvailable=true;

        return playServicesAvailable;
	}


	public static class ErrorDialogFragment extends DialogFragment {
		private Dialog mDialog;

		public ErrorDialogFragment() {
			super();
			this.mDialog = null;
		}

		public void setDialog(Dialog dialog) {
			this.mDialog = dialog;
		}

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

		if (checkPlayServices(this))
			launchChecksComplete();
	}

	private void showQuitError() {
		AlertDialog.Builder popupBuilder = new AlertDialog.Builder(this);
		popupBuilder.setMessage("Unable to activate Google Play Services");
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

		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		transaction.add(errorFragment, "playServicesErrorFragmentNotAvailable");
		transaction.commitAllowingStateLoss();
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		Log.v(TAG, "onActivityResult. RequestCode = " + requestCode + ", resultCode " + resultCode);
		if (requestCode == CONNECTION_FAILURE_RESOLUTION_REQUEST) {
			if (resultCode != RESULT_OK) {
				Toast.makeText(this, "Google Play Services must be installed.",
						Toast.LENGTH_SHORT).show();
				checkPlayServices(this);
			} else {
				Log.v(TAG, "Play services activated successfully");
			}
			return;
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

	public void startActivityMain() {
		startActivityFromClass(ActivityMain.class);
	}

	public void startActivityFromClass(Class<?> c) {
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
