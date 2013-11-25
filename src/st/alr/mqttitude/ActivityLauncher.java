
package st.alr.mqttitude;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceBindable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.AndroidCharacter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class ActivityLauncher extends FragmentActivity{
    private final static int  CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private boolean autostart = false;
    
    private ServiceConnection serviceApplicationConnection;
    private Context context;
    private boolean playServicesOk = false; 
    private boolean settingsOK = false;
    
    public static class ErrorDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
        
        @Override
        public void onCancel(DialogInterface dialog){
            Log.e(this.toString(), "Killing application");
            getActivity().finish();
        }
    }


    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        
        if(savedInstanceState != null && savedInstanceState.getBoolean("autostart")) 
            autostart = true;
        
        
        setContentView(R.layout.activity_launcher);

        
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        checkSettings();
        
        if(playServicesOk && settingsOK)
            launchChecksComplete();
        else
            showQuitError();
    }
    
    
    
    private void checkSettings() {

        // check if username, devicename and host is set. 

        // until the wizzard works
        settingsOK = true;
        if(!settingsOK)
            startActivityWizzard();
        
        Log.v(this.toString(), "Preflight check passed: " + settingsOK);
    }

    private void checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d("Location Updates", "Google Play services is available.");
            playServicesOk  = true;
            
        } else {
            Log.e("Location Updates", "Google Play services not available.");

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
    
                if (errorDialog != null) {
                    ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                    errorFragment.setDialog(errorDialog);
                     
                    
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.add(errorFragment, "playServicesErrorFragmentEnable");
                    transaction.commitAllowingStateLoss();  
    
                }
            } else {
                showQuitError();
            }
        }        
    }


    private void showQuitError() {
        AlertDialog.Builder popupBuilder = new AlertDialog.Builder(this);
        popupBuilder.setMessage("Unable to activate Google Play Services");
        popupBuilder.setTitle("Error");
        popupBuilder.setNegativeButton("Quit application", new OnClickListener() {
            
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
        Log.v(this.toString(), "onActivityResult");
      if(requestCode==CONNECTION_FAILURE_RESOLUTION_REQUEST) {
        if (resultCode == RESULT_CANCELED) {
          Toast.makeText(this, "Google Play Services must be installed.", Toast.LENGTH_SHORT).show();
          quitApplication();
        } else {
            Log.v(this.toString(), "Play services activated successfully");
        }
        return;
      } 

      super.onActivityResult(requestCode, resultCode, data);
    }


    
    private void quitApplication() {
        Log.e(this.toString(), "Killing application");
        finish();
    }


    
    

    private void launchChecksComplete() {
        Log.v(this.toString(), "Launch checks complete. Starting application service");
        Intent i = new Intent(this, ServiceApplication.class);
        startService(i);
        serviceApplicationConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.v(this.toString(), "Application service started and bound. ");
                if(!autostart)
                    startActivityMain();
            }
        };

        bindService(new Intent(this, ServiceApplication.class), serviceApplicationConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void startActivityMain(){
        startActivityFromClass(ActivityMain.class);                   
    }
    
    public void startActivityWizzard(){
        startActivityFromClass(ActivityWizzard.class);                   
    }
    
    public void startActivityFromClass(Class<?> c) {
        Log.v(this.toString(), "Starting activity for class" + c.toString());

        Intent intent = new Intent(context, c);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); 
        startActivity(intent);                   
        
    }
    
    @Override
    protected void onDestroy() {
        if(serviceApplicationConnection != null)
            unbindService(serviceApplicationConnection);
        super.onDestroy();
    }

}

