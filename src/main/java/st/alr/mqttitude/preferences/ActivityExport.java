package st.alr.mqttitude.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.EnumSet;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.messages.ConfigurationMessage;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.StringifiedJSONObject;

public class ActivityExport extends Activity {
    private static final String TEMP_FILE_NAME = "config.otrc";
    CheckBox includePreferences;
    CheckBox includeConnection;
    CheckBox includeCredentials;
    CheckBox includeDeviceIdentification;
    CheckBox includeWaypoints;
    Button exportButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        includeConnection = (CheckBox) findViewById(R.id.includeConnection);
        includeCredentials = (CheckBox) findViewById(R.id.includeUsernamePassword);
        includeDeviceIdentification = (CheckBox) findViewById(R.id.includeDeviceIdentification);
        includeWaypoints = (CheckBox) findViewById(R.id.includeWaypoints);
        exportButton = (Button) findViewById(R.id.exportButton);

        includeConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setUsernameDeviceExport(isChecked);
            }
        });


        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                export();
            }
        });


        // Look for Import Preference File Intent
        final Intent intent = getIntent();
        final String action = intent.getAction();

        if(Intent.ACTION_VIEW.equals(action)) {

            Uri uri = intent.getData();

            if (uri != null) {
                if(uri.getPath().endsWith(".otrc")){
                    importPreferenceDialog(uri.getPath());
                }
                else{
                    /* Because of the way the Intent filter is defined, this activity could receive Intents that do not contain an otrc file.
                     * In that case, I am ignoring the intent and closing the activity. The user won't even notice that the owntracks app got notified
                     * however this isn't a good a solution and a better way of associating .otrc file to the app should be found.   */

                    this.finish(); // close activity
                }
            }
        }

    }


    private void setUsernameDeviceExport(boolean isChecked) {
        includeCredentials.setEnabled(isChecked);
        includeDeviceIdentification.setEnabled(isChecked);

        // Uncheck Credentials and DeviceIdentification if Connection is not checked
        if(!isChecked) {
            includeCredentials.setChecked(isChecked);
            includeDeviceIdentification.setChecked(isChecked);
        }
    }

    private void export() {
        Log.v("Export", "Export includes: connection=" + includeConnection.isChecked() + ", username/password=" + includeCredentials.isChecked() + ", device identification=" + includeDeviceIdentification.isChecked() + ", waypoints=" + includeWaypoints.isChecked());



        EnumSet<ConfigurationMessage.Includes> includes = EnumSet.noneOf(ConfigurationMessage.Includes.class);

        if (includeConnection.isChecked() && includeConnection.isEnabled())
            includes.add(ConfigurationMessage.Includes.CONNECTION);
        if (includeCredentials.isChecked()&& includeCredentials.isEnabled())
            includes.add(ConfigurationMessage.Includes.CREDENTIALS);
        if (includeDeviceIdentification.isChecked() && includeCredentials.isEnabled())
            includes.add(ConfigurationMessage.Includes.IDENTIFICATION);
        if (includeWaypoints.isChecked())
            includes.add(ConfigurationMessage.Includes.WAYPOINTS);

        Log.v(this.toString(), "Export includes: " + includes);

        ConfigurationMessage config = new ConfigurationMessage(includes);
        Log.v("Export", "Config: \n" + config.toString());




        File cDir = getBaseContext().getCacheDir();
        File tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

        String strLine="";
        StringBuilder text = new StringBuilder();


        FileWriter writer=null;
        try {
            writer = new FileWriter(tempFile);

            writer.write(config.toString());

            writer.close();

            Toast.makeText(getBaseContext(), "Saved config to " + tempFile.getPath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri configUri = FileProvider.getUriForFile(this, "st.alr.mqttitude.fileprovider", tempFile);
        //configUri = Uri.parse(configUri.toString() + ".otrc");



        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //sendIntent.putExtra(Intent.EXTRA_TEXT, config.toString());
        sendIntent.putExtra(Intent.EXTRA_STREAM, configUri);
        sendIntent.setType("text/plain");


        startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));
    }

    private void importPreferenceDialog(String filePath){

        try{
            final String fileContent=readFile(filePath);

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.preferencesImportFile))
                    .setMessage(filePath)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // ???
                        }
                    })
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {

                                Preferences.fromJsonObject(new StringifiedJSONObject(fileContent));
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        ServiceProxy.getServiceBroker().reconnect();
                                    }
                                };
                                new Thread(r).start();

                            } catch (JSONException e) {
                                importPreferenceResultDialog("Preferences import failed!");
                            }

                            importPreferenceResultDialog("Preferences imported successfully !");
                        }
                    });

            Dialog dialog = builder.create();
            dialog.show();

        }catch(Exception e){

            Log.d("Export", "importPreferenceDialog exception");
            importPreferenceResultDialog("Preferences import failed!");
        }

    }

    private void importPreferenceResultDialog(String message){

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.preferencesImportFile))
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        Dialog dialog = builder.create();
        dialog.show();
    }

    private String readFile( String file ) throws IOException {

        BufferedReader reader = new BufferedReader( new FileReader (file));
        String         line = null;
        StringBuilder  stringBuilder = new StringBuilder();
        String         ls = System.getProperty("line.separator");

        while( ( line = reader.readLine() ) != null ) {
            stringBuilder.append( line );
            stringBuilder.append( ls );
        }

        reader.close();

        return stringBuilder.toString();
    }
}