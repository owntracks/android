package st.alr.mqttitude.preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.model.ConfigurationMessage;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Preferences;

public class ActivityExport extends Activity {
    CheckBox includeConnection;
    CheckBox includeUsernamePassword;
    CheckBox includeDeviceIdentification;
    CheckBox includeWaypoints;
    Button exportButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        includeConnection = (CheckBox) findViewById(R.id.includeConnection);
        includeUsernamePassword = (CheckBox) findViewById(R.id.includeUsernamePassword);
        includeDeviceIdentification = (CheckBox) findViewById(R.id.includeDeviceIdentification);
        includeWaypoints = (CheckBox) findViewById(R.id.includeWaypoints);

        includeConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                includeUsernamePassword.setEnabled(isChecked);
                includeDeviceIdentification.setEnabled(isChecked);
            }
        });

        exportButton = (Button) findViewById(R.id.exportButton);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("Export", "Export includes: connection=" + includeConnection.isChecked() + ", username/password=" + includeUsernamePassword.isChecked() + ", device identification=" + includeDeviceIdentification.isChecked() + ", waypoints=" + includeWaypoints.isChecked());
                ConfigurationMessage config = new ConfigurationMessage();


                if (!includeConnection.isChecked()) {
                    config.removeDeviceIdentification();
                    config.removeUsernamePassword();
                    config.remove(Preferences.getStringRessource(R.string.keyHost));
                    config.remove(Preferences.getStringRessource(R.string.keyPort));
                    config.remove(Preferences.getStringRessource(R.string.keyAuth));
                    config.remove(Preferences.getStringRessource(R.string.keyTls));
                    config.remove(Preferences.getStringRessource(R.string.keyTlsCrtPath));
                    config.remove(Preferences.getStringRessource(R.string.keyConnectionAdvancedMode));

                } else {
                    if (!includeUsernamePassword.isChecked())
                        config.removeUsernamePassword();

                    if (!includeDeviceIdentification.isChecked())
                        config.removeDeviceIdentification();

                }

                if(includeWaypoints.isChecked())
                    config.addWaypoints();


                Log.v("Export", "Config: \n" + config.toString());

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, config.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));
            }
        });


    }



}
