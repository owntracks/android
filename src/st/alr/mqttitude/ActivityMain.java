package st.alr.mqttitude;

import java.util.Date;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceBindable;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity implements ActionBar.TabListener {


    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    ServiceApplication serviceApplication;
    ServiceConnection serviceApplicationConnection;

    
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent i = new Intent(this, ServiceApplication.class);
        startService(i);
        serviceApplicationConnection = new ServiceConnection() {
            
            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceApplication = null;                
            }
            
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.v(this.toString(), "application service bound");
                serviceApplication = (ServiceApplication) ((ServiceBindable.ServiceBinder)service).getService();                
            }

   
        };
        
        bindService(new Intent(this, ServiceApplication.class), serviceApplicationConnection, Context.BIND_AUTO_CREATE);

        
        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int j = 0; j < mSectionsPagerAdapter.getCount(); j++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(j))
                            .setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_settings) {
            Intent intent1 = new Intent(this, ActivityPreferences.class);
            startActivity(intent1);
            return true;
        } else if (itemId == R.id.menu_publish) {           
            if(ServiceApplication.getServiceLocator() != null)
                ServiceApplication.getServiceLocator().publishLastKnownLocation();
            return true;
        } else if (itemId == R.id.menu_share) {
            if(ServiceApplication.getServiceLocator() != null)
                this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    public void share(View view) {
        GeocodableLocation l = ServiceApplication.getServiceLocator().getLastKnownLocation();
        if(l == null) {
            //TODO: signal to user
            return;            
        }
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(
                Intent.EXTRA_TEXT,
                "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + ","
                        + Double.toString(l.getLongitude()));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.shareLocation)));

    }

    
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
//                    Bundle args = new Bundle();
//                    args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
//                    fragment.setArguments(args);
//                    return fragment;

                    return new MapFragment();
                case 1:
                    return FriendsFragment.newInstance();
                default:
                    return new StatusFragment();
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.titleMap);
                case 1:
                    return getString(R.string.titleFriends);
                case 2:
                    return getString(R.string.titleStatus);
            }
            return null;
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEventMainThread(Events.ContactLocationUpdated e) {
        Log.v(this.toString(), "Contact location updated: " + e.getTopic() + " ->" + e.getGeocodableLocation().toString() + " @ " + new Date(e.getGeocodableLocation().getLocation().getTime() * 1000));
    }
    
    public static class MapFragment extends SupportMapFragment {
        
        public static MapFragment newInstance() {
            MapFragment f = new MapFragment();
            return f;
        }


        private Handler handler;
        private Marker mMarker;
        private Circle mCircle;
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            handler = new Handler() {
                public void handleMessage(Message msg) {
                    onHandlerMessage(msg);
                }
            };

            super.onCreate(savedInstanceState);
        }

        private void onHandlerMessage(Message msg) {
            switch (msg.what) {
                case ReverseGeocodingTask.GEOCODER_RESULT:
                    Log.v(this.toString(), "Geocoder result_ " + ((GeocodableLocation) msg.obj).getGeocoder());
                   // locationPrimary.setText(((GeocodableLocation) msg.obj).getGeocoder());
                    break;
                case ReverseGeocodingTask.GEOCODER_NORESULT:
                   // locationPrimary.setText(((GeocodableLocation) msg.obj).toLatLonString());
                    break;

            }
        }   

        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        
        public void onEventMainThread(Events.LocationUpdated e) {
            setLocation(e.getGeocodableLocation());
        }

        public void centerMap(double lat, double lon) {
            centerMap(new LatLng(lat, lon));
        }
        public void centerMap(LatLng latlon) {
            CameraUpdate center = CameraUpdateFactory.newLatLng(latlon);
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
            getMap().moveCamera(center);
            getMap().animateCamera(zoom);
        }
        
        public void setLocation(GeocodableLocation location) {
            Location l = location.getLocation();
            Log.v(this.toString(), "Setting location");

           if(l == null) {
               Log.v(this.toString(), "location not available");
               //showLocationUnavailable();
               return;
           } 
           
            LatLng latlong = new LatLng(l.getLatitude(), l.getLongitude());

            if (mMarker != null)
                mMarker.remove();

            if (mCircle != null)
                mCircle.remove();

            
            mMarker = getMap().addMarker(new MarkerOptions().position(latlong).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
            centerMap(latlong);

            
             if(l.getAccuracy() >= 50) {
                     mCircle = getMap().addCircle(new
                     CircleOptions().center(latlong).radius(l.getAccuracy()).strokeColor(0xff1082ac).fillColor(0x1c15bffe).strokeWidth(3));
             }


            if(location.getGeocoder() != null) {
                Log.v(this.toString(), "Reusing geocoder");
                //locationPrimary.setText(location.getGeocoder());            
            } else {
                // Start async geocoder lookup and display latlon until geocoder reeturns something
                if (Geocoder.isPresent()) {
                    Log.v(this.toString(), "Requesting geocoder");
                    (new ReverseGeocodingTask(getActivity(), handler)).execute(new GeocodableLocation[] {location});
                
                } else {
                    //locationPrimary.setText(location.toLatLonString());                
                }
            }
            //locationMeta.setText(App.getInstance().formatDate(new Date()));            

           // showLocationAvailable();
        }

        
//      @Override
//      public View onCreateView(LayoutInflater inflater, ViewGroup container,
//              Bundle savedInstanceState) {
//          View rootView = inflater.inflate(R.layout.fragment_activity_main_dummy, container, false);
//          TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
//          dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
//          return rootView;
//      }
//  
  }
    
    
    
    public static class FriendsFragment extends Fragment {
        int mNum;

        static FriendsFragment newInstance() {
            FriendsFragment f = new FriendsFragment();
//            // Supply num input as an argument.
//            Bundle args = new Bundle();
//            args.putInt("num", num);
//            f.setArguments(args);

            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_friends, container, false);
            return v;
        }
    }


    public static class StatusFragment extends Fragment {
        private TextView locatorStatus;
        private TextView locatorCurLatLon;
        private TextView locatorCurAccuracy;
        private TextView locatorCurLatLonTime;

        private TextView locatorLastPubLatLon;
        private TextView locatorLastPubAccuracy;
        private TextView locatorLastPubLatLonTime;

        private TextView brokerStatus;
        private TextView brokerError;

        static StatusFragment newInstance() {
            StatusFragment f = new StatusFragment();
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }
        
        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_status, container, false);
            locatorStatus = (TextView) v.findViewById(R.id.locatorStatus);
            locatorCurLatLon = (TextView) v.findViewById(R.id.locatorCurLatLon);
            locatorCurAccuracy = (TextView) v.findViewById(R.id.locatorCurAccuracy);
            locatorCurLatLonTime = (TextView) v.findViewById(R.id.locatorCurLatLonTime);

            locatorLastPubLatLon = (TextView) v.findViewById(R.id.locatorLastPubLatLon);
            locatorLastPubAccuracy = (TextView) v.findViewById(R.id.locatorLastPubAccuracy);
            locatorLastPubLatLonTime = (TextView) v.findViewById(R.id.locatorLastPubLatLonTime);

            brokerStatus = (TextView) v.findViewById(R.id.brokerStatus);
            brokerError = (TextView) v.findViewById(R.id.brokerError);

            return v;
        }
        
        public void onEventMainThread(Events.LocationUpdated e) {
            locatorCurLatLon.setText(e.getGeocodableLocation().toLatLonString());
            locatorCurAccuracy.setText("±" + e.getGeocodableLocation().getLocation().getAccuracy()+"m");
            locatorCurLatLonTime.setText(ServiceApplication.getInstance().formatDate(e.getDate()));
        }

        
        
        public void onEventMainThread(Events.PublishSuccessfull e) {
            if(e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
                GeocodableLocation l = (GeocodableLocation)e.getExtra();
                locatorLastPubLatLon.setText(l.toLatLonString());
                locatorLastPubAccuracy.setText("±" + l.getLocation().getAccuracy()+"m");
                locatorLastPubLatLonTime.setText(ServiceApplication.getInstance().formatDate(e.getDate()));            
            }
        }

        public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
           locatorStatus.setText(Defaults.State.toString(e.getState()));
        }

        public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
            brokerStatus.setText(Defaults.State.toString(e.getState()));
            if(e.getExtra() != null && e.getExtra() instanceof Exception && e.getExtra().getClass() != null) {
                brokerError.setText( ((Exception)e.getExtra()).getCause().getLocalizedMessage());
            } else {
                brokerError.setText(getString(R.string.na));
            }
        }

    }


}
