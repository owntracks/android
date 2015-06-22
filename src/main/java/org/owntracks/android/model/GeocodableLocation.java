package org.owntracks.android.model;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;


public class GeocodableLocation extends Location {
	private static final String TAG = "GeocodableLocation";

	private String geocoder;
    private LatLng latlng;
    private String tag;
    private Object extra;
    private Date date;

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

    public Object getExtra() {
        return extra;
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public GeocodableLocation(JSONObject json) throws JSONException{
        super("owntracks-deserialized");

        try {
            String type = json.getString("_type");
            if (!type.equals("location"))
                throw new JSONException("wrong type");
        } catch (JSONException e) {
            Log.e("GeocodableLocation", "Unable to deserialize GeocodableLocation object from JSON");
            throw e;
        }

        try {
            setLatitude( json.getDouble("lat"));
        } catch (Exception e) {
            setLatitude(0);
        }

        try {
            setLongitude( json.getDouble("lon"));
        } catch (Exception e) {
            setLongitude(0);
        }
        try {
            setAccuracy(json.getInt("acc"));
        } catch (Exception e) {
            setAccuracy(0);
        }

        try {
            setTime(TimeUnit.SECONDS.toMillis(Long.parseLong(json.getString("tst"))));
        } catch (Exception e) {
            setTime(0);
        }

        try {
            setAltitude(json.getDouble("alt"));
        } catch (Exception e) {
            setAltitude((double) 0);
        }
    }


    public GeocodableLocation(Location location) {
		this(location, null);
	}

	public GeocodableLocation(String provider) {
		super(provider != null ? provider : "unknown");
		this.geocoder = null;
	}

	public GeocodableLocation(Location location, String geocoder) {
		super(location);
		this.geocoder = geocoder;
		if (location != null)
			this.latlng = new LatLng(location.getLatitude(),
					location.getLongitude());
	}

	public static GeocodableLocation fromJsonObject(JSONObject json) {
		Double lat;
		Double lon;
		Integer acc;
		Long tst;
		Double alt;

		try {
			String type = json.getString("_type");
			if (!type.equals("location"))
				throw new JSONException("wrong type");
		} catch (JSONException e) {
			Log.e("GeocodableLocation", "Unable to deserialize GeocodableLocation object from JSON");
			return null;
		}

		try {
			lat = json.getDouble("lat");
		} catch (Exception e) {
			lat = (double) 0;
		}

		try {
			lon = json.getDouble("lon");
		} catch (Exception e) {
			lon = (double) 0;
		}

		try {
			acc = json.getInt("acc");
		} catch (Exception e) {
			acc = 0;
		}

		try {
			tst = TimeUnit.SECONDS.toMillis(json.getLong("tst"));

		} catch (Exception e) {
			tst = (long) 0;
		}

		try {
			alt = json.getDouble("alt");
		} catch (Exception e) {
			alt = (double) 0;
		}

		GeocodableLocation l = new GeocodableLocation("mqttitude-deserialized");
		l.setLatitude(lat);
		l.setLongitude(lon);
		l.setAccuracy(acc);
		l.setTime(tst);
		l.setAltitude(alt);

		return l;
	}

	public String getGeocoder() {
		return this.geocoder;
	}

	public Location getLocation() {
		return this; // compatibility fix
	}

	@Override
	public void setLatitude(double latitude) {
		super.setLatitude(latitude);
		this.latlng = new LatLng(latitude, getLongitude());
        this.setGeocoder(null);
	}

	@Override
	public void setLongitude(double longitude) {
		super.setLongitude(longitude);
		this.latlng = new LatLng(getLatitude(), longitude);
        this.setGeocoder(null);
	}

	public void setGeocoder(String geocoder) {
		this.geocoder = geocoder;
	}

	@Override
	public String toString() {
		if (this.geocoder != null)
			return this.geocoder;
		else
			return toLatLonString();
	}

	public String toLatLonString() {
		return getLatitude() + " : " + getLongitude();
	}

	public LatLng getLatLng() {
		return this.latlng;
	}

	public Date getDate() {
		return new Date(getLocation().getTime());
	}

}
