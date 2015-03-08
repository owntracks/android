package org.owntracks.android.messages;

import org.altbeacon.beacon.Identifier;
import org.json.JSONException;

import org.owntracks.android.support.StringifiedJSONObject;

import android.util.Log;

public class BeaconMessage extends Message {

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
    private long time;
    private int proximity;

    public BeaconMessage(Identifier uuid, Identifier major, Identifier minor, int rssi,
                         double distance, String bluetoothName, int manufacturer,
                         String bluetoothAddress, int beaconTypeCode, int txPower, long time,
                         int proximity) {
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
        this.time = time;
        this.proximity = proximity;
    }

    @Override
    public String toString() {
        return this.toJSONObject().toString();
    }

    public StringifiedJSONObject toJSONObject() {
        StringifiedJSONObject json = new StringifiedJSONObject();

        try {
            json.put("_type", "beacon")

                    // Same as iBeacon report
                    .put("uuid", this.uuid)
                    .put("major", this.major)
                    .put("minor", this.minor)
                    .put("tst", this.time)
                    .put("rssi", this.rssi)
                    .put("acc", -1)
                    .put("prox", proximity)

                    // Provide some additional info
                    .put("dist", this.distance) // distance in meters
                    .put("txpwr", this.txPower); // txPower + rssi can be used to calc distance

                    // Potentially useful for debugging
                    //.put("name", this.bluetoothName)
                    //.put("mfr", this.manufacturer)
                    //.put("addr", this.bluetoothAddress)
                    //.put("type", this.beaconTypeCode)
        } catch (JSONException e) {

        }

        return json;
    }

    public BeaconMessage(StringifiedJSONObject json) throws JSONException{
        // Not implemented

        try {
            String type = json.getString("_type");
            if (!type.equals("beacon"))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e("BeaconMessage",
                    "Unable to deserialize LocationMessage object from JSON "
                            + json.toString());
            throw e;
        }


    }
}

