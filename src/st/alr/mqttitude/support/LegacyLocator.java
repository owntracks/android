//
//package st.alr.mqttitude.support;
//
//import java.util.Calendar;
//
//import st.alr.mqttitude.App;
//
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
//import android.location.GpsStatus;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.os.Bundle;
//import android.preference.PreferenceManager;
//import android.util.Log;
//import android.app.AlarmManager;
//
//public class LegacyLocator implements Locator
//{
//    private Listener locationListener;
//    private LocationManager locationManager;
//    private Context context;
//    LocationProviderCallback callback;
//    AlarmManager aMgr;
//    PendingIntent updateIntend;
//    UpdateReceiver receiver;
//    
//    public LegacyLocator(Context context) {
//        locationListener = new Listener();
//        aMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        UpdateReceiver receiver = new UpdateReceiver();
//        context.registerReceiver(receiver, new IntentFilter(st.alr.mqttitude.support.Defaults.UPDATE_INTEND_ID));
//
//        OnSharedPreferenceChangeListener preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
//                if (key.equals(Defaults.SETTINGS_KEY_UPDATE_INTERVAL))
//                    handleUpdateInteval();
//            }
//        };
//    }
//    
//    protected void handleUpdateInteval() {
//        if(updateIntend != null)
//            aMgr.cancel(updateIntend);
//        
//        scheduleNextUpdate();
//    }
//    
//    private void scheduleNextUpdate()
//    {
//        updateIntend = PendingIntent.getBroadcast(context, 0, new Intent(st.alr.mqttitude.support.Defaults.UPDATE_INTEND_ID), PendingIntent.FLAG_UPDATE_CURRENT);
//        Integer updateInterval;
//
//        try {
//            updateInterval = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("updateIntervall",
//                            st.alr.mqttitude.support.Defaults.VALUE_UPDATE_INTERVAL));
//            updateInterval = updateInterval < 1 ? 1 : updateInterval; 
//            
//        } catch (NumberFormatException e) {
//            updateInterval = 30;
//        }
//
//        Calendar wakeUpTime = Calendar.getInstance();
//        wakeUpTime.add(Calendar.MINUTE, updateInterval);
//        aMgr.set(AlarmManager.RTC_WAKEUP, wakeUpTime.getTimeInMillis(), updateIntend);
//    }
//
//    private class UpdateReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context arg0, Intent intent) {
//            if (intent.getAction() != null && intent.getAction().equals(Defaults.UPDATE_INTEND_ID)) {
//                updateIntend = null;
//                App.getInstance().publishLocation(true);
//                scheduleNextUpdate();
//            }
//        }
//
//    }
//
//    public void getLocation(LocationProviderCallback cb) {
//        Log.v(this.toString(), "get");
//        this.callback =  cb;
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
//    }
//
//    public void stop() {
//        locationManager.removeUpdates(locationListener);
//        Log.v("stopLocationListener", "locationManager stopped");
//    }
//
//    
//    
//    
//    private class Listener implements LocationListener {
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//            Log.v(this.toString(), "position receiver onStatusChanged");
//
//            try {
//                String strStatus = "";
//                switch (status) {
//                    case GpsStatus.GPS_EVENT_FIRST_FIX:
//                        strStatus = "GPS_EVENT_FIRST_FIX";
//                        break;
//                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
//                        strStatus = "GPS_EVENT_SATELLITE_STATUS";
//                        break;
//                    case GpsStatus.GPS_EVENT_STARTED:
//                        strStatus = "GPS_EVENT_STARTED";
//                        break;
//                    case GpsStatus.GPS_EVENT_STOPPED:
//                        strStatus = "GPS_EVENT_STOPPED";
//                        break;
//                    default:
//                        strStatus = String.valueOf(status);
//                        break;
//                }
//                Log.v(this.toString(), "onStatusChanged: " + strStatus);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//        }
//
//        @Override
//        public void onProviderDisabled(String provider) {
//        }
//
//        @Override
//        public void onLocationChanged(Location location) {
//            try {
//                if(callback != null)
//                    callback.onLocationRespone(location);
//           
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                locationManager.removeUpdates(this);
//                callback = null;
//            }
//        }
//    }
//}
