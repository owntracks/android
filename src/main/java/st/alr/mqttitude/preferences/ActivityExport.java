package st.alr.mqttitude.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.EnumSet;

import st.alr.mqttitude.R;
import st.alr.mqttitude.messages.ConfigurationMessage;
import st.alr.mqttitude.support.Preferences;

public class ActivityExport extends Activity {
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

        includePreferences = (CheckBox) findViewById(R.id.includePreferences);
        includeConnection = (CheckBox) findViewById(R.id.includeConnection);
        includeCredentials = (CheckBox) findViewById(R.id.includeUsernamePassword);
        includeDeviceIdentification = (CheckBox) findViewById(R.id.includeDeviceIdentification);
        includeWaypoints = (CheckBox) findViewById(R.id.includeWaypoints);
        exportButton = (Button) findViewById(R.id.exportButton);

        includePreferences.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeConnection.setEnabled(isChecked);

                setUsernameDeviceExport(isChecked && includeConnection.isChecked());
                setExportButon();
            }
        });

        includeConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setUsernameDeviceExport(isChecked);
            }
        });


        includeWaypoints.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setExportButon();
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

    private void setExportButon() {
        exportButton.setEnabled(includePreferences.isChecked() || includeWaypoints.isChecked());
    }

    private void export() {
        Log.v("Export", "Export includes: connection=" + includeConnection.isChecked() + ", username/password=" + includeCredentials.isChecked() + ", device identification=" + includeDeviceIdentification.isChecked() + ", waypoints=" + includeWaypoints.isChecked());


        EnumSet<ConfigurationMessage.Includes> includes = null;
        if (includePreferences.isChecked())
            includes.add(ConfigurationMessage.Includes.PREFERENCES);
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

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, config.toString());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));
    }
}
