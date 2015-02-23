package org.owntracks.android.preferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;

import org.owntracks.android.R;
import org.owntracks.android.messages.ConfigurationMessage;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.StringifiedJSONObject;

public class ActivityExport extends ActionBarActivity {
    private static final String TEMP_FILE_NAME = "config.otrc";
    private CheckBox includePreferences;
    private CheckBox includeConnection;
    private CheckBox includeCredentials;
    private CheckBox includeDeviceIdentification;
    private CheckBox includeWaypoints;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getTitle());

        includeConnection = (CheckBox) findViewById(R.id.includeConnection);
        includeCredentials = (CheckBox) findViewById(R.id.includeUsernamePassword);
        includeDeviceIdentification = (CheckBox) findViewById(R.id.includeDeviceIdentification);
        includeWaypoints = (CheckBox) findViewById(R.id.includeWaypoints);

        includeConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setUsernameDeviceExport(isChecked);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_export, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                export();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

        try {
            FileWriter writer = new FileWriter(tempFile);

            writer.write(config.toString());
            writer.close();

            Log.v(this.toString(), "Saved temporary config file for export to " + tempFile.getPath());

        } catch (IOException e) {
            e.printStackTrace();
        }
        Uri configUri = FileProvider.getUriForFile(this, "org.owntracks.android.fileprovider", tempFile);
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
