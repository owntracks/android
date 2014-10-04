package st.alr.mqttitude.messages;

import org.altbeacon.beacon.Identifier;
import org.json.JSONException;

import st.alr.mqttitude.support.StringifiedJSONObject;

import android.util.Log;

public class BeaconMessage {

    private Identifier uuid;
    private Identifier major;
    private Identifier minor;
    private int rssi;
    private double distance;
    private String bluetoothName;
    private int manufacturer;
    private String bluetoothAddress;
    private int beaconTypeCode;
    private int txPower;

    public BeaconMessage(Identifier uuid, Identifier major, Identifier minor, int rssi,
                         double distance, String bluetoothName, int manufacturer,
                         String bluetoothAddress, int beaconTypeCode, int txPower) {
        this.uuid = uuid;
        this.major = major;
        this.minor = minor;
        this.rssi = rssi;
        this.distance = distance;
        this.bluetoothName = bluetoothName;
        this.manufacturer = manufacturer;
        this.bluetoothAddress = bluetoothAddress;
        this.beaconTypeCode = beaconTypeCode;
        this.txPower = txPower;
    }

    @Override
    public String toString() {
        return this.toJSONObject().toString();
    }

    public StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();

        try {
            json.put("_type", "beacon")
                    .put("uuid", this.uuid)
                    .put("major", this.major)
                    .put("minor", this.minor)
                    .put("rssi", this.rssi)
                    .put("dist", this.distance)
                    .put("name", this.bluetoothName)
                    .put("mfr", this.manufacturer)
                    .put("addr", this.bluetoothAddress)
                    .put("type", this.beaconTypeCode)
                    .put("txpwr", this.txPower);
        } catch (JSONException e) {

        }

        return json;
    }

    public static LocationMessage fromJsonObject(StringifiedJSONObject json) {
        // Not implemented

        try {
            String type = json.getString("_type");
            if (!type.equals("beacon"))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e("BeaconMessage",
                    "Unable to deserialize LocationMessage object from JSON "
                            + json.toString());
            return null;
        }

        return null;

    }
}

