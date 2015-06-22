package org.owntracks.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
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

public class ActivityExport extends AppCompatActivity {
    private static final String TAG = "ActivityExport";

    private static final String TEMP_FILE_NAME = "config.otrc";
    private Switch includeConnection;
    private Switch includeCredentials;
    private Switch includeDeviceIdentification;
    private Switch includeWaypoints;

    boolean includeConnectionVal=true;
    boolean includeCredentialsVal=false;
    boolean includeDeviceIdentificationVal=false;
    boolean includeWaypointsVal=true;
    private TextView exportUsernameLabel;
    private TextView exportIdentificationLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        Toolbar toolbar = (Toolbar) findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.export));

        includeConnection = (Switch) findViewById(R.id.includeConnection);
        includeCredentials = (Switch) findViewById(R.id.includeUsernamePassword);
        includeDeviceIdentification = (Switch) findViewById(R.id.includeDeviceIdentification);
        includeWaypoints = (Switch) findViewById(R.id.includeWaypoints);

        exportUsernameLabel = (TextView) findViewById(R.id.exportUsernameLabel);
        exportIdentificationLabel = (TextView) findViewById(R.id.exportIdentificationLabel);

        setUsernameDeviceExport(true);

        includeConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeConnectionVal = isChecked;
                setUsernameDeviceExport(includeConnectionVal);
            }
        });

        includeCredentials.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeCredentialsVal = isChecked;
            }
        });

        includeDeviceIdentification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeDeviceIdentificationVal = isChecked;
            }
        });
        includeWaypoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeWaypointsVal = isChecked;
            }
        });
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
        int primaryColor = getResources().getColor(R.color.textPrimary);
        int textColor = Color.argb(isChecked? 255 : 128,
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor));

        exportUsernameLabel.setTextColor(textColor);
        exportIdentificationLabel.setTextColor(textColor);

        // Uncheck Credentials and DeviceIdentification if Connection is not checked
        if(!isChecked) {
            includeCredentials.setChecked(false);
            includeCredentialsVal =false;
            includeDeviceIdentification.setChecked(false);
            includeDeviceIdentificationVal = false;
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

        Log.v(TAG, "Export includes: " + includes);

        ConfigurationMessage config = new ConfigurationMessage(includes);
        Log.v("Export", "Config: \n" + config.toString());




        File cDir = getBaseContext().getCacheDir();
        File tempFile = new File(cDir.getPath() + "/" + TEMP_FILE_NAME) ;

        try {
            FileWriter writer = new FileWriter(tempFile);

            writer.write(config.toString());
            writer.close();

            Log.v(TAG, "Saved temporary config file for export to " + tempFile.getPath());

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
}
