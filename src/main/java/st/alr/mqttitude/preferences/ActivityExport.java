package st.alr.mqttitude.preferences;

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

public class ActivityExport extends PreferenceActivity {
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
                JSONObject json = config.toJSONObject();


                if (!includeConnection.isChecked()) {
                    json.remove(Preferences.getStringRessource(R.string.keyHost));
                    json.remove(Preferences.getStringRessource(R.string.keyPort));
                    json.remove(Preferences.getStringRessource(R.string.keyAuth));
                    json.remove(Preferences.getStringRessource(R.string.keyTls));
                    json.remove(Preferences.getStringRessource(R.string.keyTlsCrtPath));
                    json.remove(Preferences.getStringRessource(R.string.keyConnectionAdvancedMode));

                    jsonRemoveUsernamePassword(json);
                    jsonRemoveDeviceIdentification(json);

                } else {
                    if (!includeUsernamePassword.isChecked())
                        jsonRemoveUsernamePassword(json);
                    if (!includeDeviceIdentification.isChecked())
                        jsonRemoveDeviceIdentification(json);

                }

                if(includeWaypoints.isChecked())
                    try { jsonAddWaypoints(json); } catch (JSONException e) { e.printStackTrace(); }

                Log.v("Export", "Config: \n" + json.toString());

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, json.toString());
                sendIntent.setType("text/json");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.exportConfiguration)));
            }
        });
    }

    private void jsonAddWaypoints(JSONObject json) throws JSONException {
        JSONArray waypoints = new JSONArray();

        for(Waypoint waypoint : App.getWaypointDao().loadAll()) {
            JSONObject w = new JSONObject();
            try { w.put("_type", "waypoint"); } catch (JSONException e) { }
            try { w.put("tst", waypoint.getDate().getTime()); } catch (JSONException e) { }
            try { w.put("lat", waypoint.getLatitude()); } catch (JSONException e) { }
            try { w.put("lon", waypoint.getLongitude()); } catch (JSONException e) { }
            try { w.put("rad", waypoint.getRadius()); } catch (JSONException e) { }
            try { w.put("shared", waypoint.getShared() ? 1 : 0); } catch (JSONException e) { }
            try { w.put("desc", waypoint.getDescription()); } catch (JSONException e) { }
            try { w.put("transition", waypoint.getTransitionType()); } catch (JSONException e) { }
            waypoints.put(w);
        }

        json.put("waypoints", waypoints);


    }

    private void jsonRemoveUsernamePassword(JSONObject json) {
        json.remove(Preferences.getStringRessource(R.string.keyUsername));
        json.remove(Preferences.getStringRessource(R.string.keyPassword));
    }

    private void jsonRemoveDeviceIdentification(JSONObject json) {
        json.remove(Preferences.getStringRessource(R.string.keyDeviceId));
        json.remove(Preferences.getStringRessource(R.string.keyClientId));
    }


}
