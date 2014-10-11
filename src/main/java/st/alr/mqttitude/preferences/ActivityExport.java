package st.alr.mqttitude.preferences;

import android.app.Activity;
import android.content.Context;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;

import st.alr.mqttitude.R;
import st.alr.mqttitude.messages.ConfigurationMessage;
import st.alr.mqttitude.support.Preferences;

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


    }

    private void setUsernameDeviceExport(boolean isChecked) {
        includeCredentials.setEnabled(isChecked);
        includeDeviceIdentification.setEnabled(isChecked);
    }

    private void export() {
        Log.v("Export", "Export includes: connection=" + includeConnection.isChecked() + ", username/password=" + includeCredentials.isChecked() + ", device identification=" + includeDeviceIdentification.isChecked() + ", waypoints=" + includeWaypoints.isChecked());


        EnumSet<ConfigurationMessage.Includes> includes = EnumSet.noneOf(ConfigurationMessage.Includes.class);
        if (includeConnection.isChecked() && includeConnection.isActivated())
            includes.add(ConfigurationMessage.Includes.CONNECTION);
        if (includeCredentials.isChecked()&& includeCredentials.isActivated())
            includes.add(ConfigurationMessage.Includes.CONNECTION);
        if (includeDeviceIdentification.isChecked() && includeCredentials.isActivated())
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
}
