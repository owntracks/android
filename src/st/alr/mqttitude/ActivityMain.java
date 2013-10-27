
package st.alr.mqttitude;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceBindable;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.ContactAdapter;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract;
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
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity implements ActionBar.TabListener {

    PagerAdapter pagerAdapter;
    static ViewPager viewPager;
    ServiceApplication serviceApplication;
    ServiceConnection serviceApplicationConnection;
    
    @Override
    protected void onDestroy() {
        unbindService(serviceApplicationConnection);
        super.onDestroy();
    }

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
                serviceApplication = (ServiceApplication) ((ServiceBindable.ServiceBinder) service)
                        .getService();
            }

        };

        bindService(new Intent(this, ServiceApplication.class), serviceApplicationConnection,
                Context.BIND_AUTO_CREATE);

        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int j = 0; j < pagerAdapter.getCount(); j++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(pagerAdapter.getPageTitle(j))
                            .setTabListener(this));
        }

        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
        }

   //    parseContacts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        Log.v(this.toString(), "here");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(this.toString(), "here");

        int itemId = item.getItemId();
        Log.v(this.toString(), itemId + " " + R.id.menu_preferences);
        if (itemId == R.id.menu_preferences) {
            Log.v(this.toString(), "here");
            Intent intent1 = new Intent(this, ActivityPreferences.class);
            startActivity(intent1);
            return true;
        } else if (itemId == R.id.menu_publish) {
            if (serviceApplication.getServiceLocator() != null)
                serviceApplication.getServiceLocator().publishLastKnownLocation();
            return true;
        } else if (itemId == R.id.menu_share) {
            if (serviceApplication.getServiceLocator() != null)
                this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void share(View view) {
        GeocodableLocation l = serviceApplication.getServiceLocator().getLastKnownLocation();
        if (l == null) {
            // TODO: signal to user
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
        viewPager.setCurrentItem(tab.getPosition());
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
    public class PagerAdapter extends FragmentPagerAdapter {
        public static final int MAP_FRAGMENT = 0;
        public static final int CONTACT_FRAGMENT = 1;
        public static final int STATUS_FRAGMENT = 2;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case MAP_FRAGMENT:
                    return MapFragment.getInstance();
                case CONTACT_FRAGMENT:
                    return FriendsFragment.getInstance();
                default:
                    return StatusFragment.getInstance();
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
        //EventBus.getDefault().registerSticky(this);
        if (serviceApplication != null)
            serviceApplication.getServiceLocator().enableForegroundMode();

    }

    @Override
    public void onStop() {
       // EventBus.getDefault().unregister(this);

        if (serviceApplication != null)
            serviceApplication.getServiceLocator().enableBackgroundMode();

        super.onStop();
    }

    


    

    public static class MapFragment extends Fragment {
        private static MapFragment instance;
        private Handler handler;
        private Map<Marker, Contact> markerToContacts;

        public static MapFragment newInstance() {
            MapFragment f = new MapFragment();
            return f;
        }

        public static MapFragment getInstance() {
            if (instance == null)
                instance = new MapFragment();

            return instance;
        }

        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().register(this);
        }
        
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                Bundle savedInstanceState) {
            // inflat and return the layout
            View v = inflater.inflate(R.layout.fragment_map, container, false);
            
            markerToContacts = new HashMap<Marker, Contact>();
            
            mMapView = (MapView) v.findViewById(R.id.mapView);
            mMapView.onCreate(savedInstanceState);
            mMapView.onResume();//needed to get the map to display immediately
            
            try {
                MapsInitializer.initialize(getActivity());
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            }
            
            googleMap = mMapView.getMap();
            
            googleMap.setIndoorEnabled(true);
            googleMap.setMyLocationEnabled(true);

            UiSettings s = googleMap.getUiSettings();
            s.setCompassEnabled(false);
            s.setMyLocationButtonEnabled(true);
            s.setTiltGesturesEnabled(false);
            s.setCompassEnabled(false);
            s.setRotateGesturesEnabled(false);
            s.setZoomControlsEnabled(false);

            
            selectedContactDetails = (LinearLayout )v.findViewById(R.id.selectedContactDetails);
            selectedContactName =(TextView )v.findViewById(R.id.selectedContactName);
            selectedContactLocation = (TextView )v.findViewById(R.id.selectedContactLocation);
            selectedContactDetails.setVisibility(View.GONE);
            

//            mMapView.getMap().setOnCameraChangeListener(new OnCameraChangeListener() {
//                
//                @Override
//                public void onCameraChange(CameraPosition arg0) {
//                    if(getCurrentlyTrackedContact()== null && selectedContactDetails != null) {
//                        selectedContactDetails.setVisibility(View.GONE);
//                        return;
//                    }
//                    Log.v("onCameraChange", "trackedContact: " + getCurrentlyTrackedContact().getMarker().getPosition());
//                    Log.v("onCameraChange", "target: " + arg0.target);
//
//                    if(!arg0.target.equals(getCurrentlyTrackedContact().getMarker().getPosition())) {
//                        selectedContactDetails.setVisibility(View.GONE);
//                    }                    
//                }
//            });
            mMapView.getMap().setOnMarkerClickListener(new OnMarkerClickListener() {
                
                @Override
                public boolean onMarkerClick(Marker m) {
                    Contact c = markerToContacts.get(m);
                    Log.v(this.toString(), "Focused contact for: " + c.getTopic());
                    
                    if(c != null) {
                        
                        
                        focus(c);
                        return true;
                    } 
                    
                    return false;
                 }
            });
            
            mMapView.getMap().setOnMapClickListener(new OnMapClickListener() {
                
                @Override
                public void onMapClick(LatLng arg0) {
                    selectedContactDetails.setVisibility(View.GONE);
                    
                }
            });
            
            //Perform any camera updates here
            
            return v;
        }
        
        @Override
        public void onResume() {
            super.onResume();
            mMapView.onResume();
            Log.v(this.toString(), "Adding all existing contact markers to map");
            for (Contact c : ServiceApplication.getContactsAdapter().getValues())
                updateContactLocation(c);

            focus(getCurrentlyTrackedContact());

        }
        
        @Override
        public void onPause() {
            mMapView.onPause();
            super.onPause();
        }
        
        @Override
        public void onDestroy() {
            mMapView.onDestroy();
            super.onDestroy();
        }
        
        @Override
        public void onLowMemory() {
            mMapView.onLowMemory();
            super.onLowMemory();
        }

        
        
        public Contact getCurrentlyTrackedContact() {
            Contact c = ServiceApplication.getContactsAdapter().get(ActivityPreferences.getTrackingUsername());   
            if(c != null)
                Log.v(this.toString(), "getCurrentlyTrackedContact == " + c.getTopic());
            else 
                Log.v(this.toString(), "getCurrentlyTrackedContact == null" );

            return c;
        }

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
                    // locationPrimary.setText(((GeocodableLocation)
                    // msg.obj).getGeocoder());
                    break;
                case ReverseGeocodingTask.GEOCODER_NORESULT:
                    // locationPrimary.setText(((GeocodableLocation)
                    // msg.obj).toLatLonString());
                    break;

            }
        }
        
        public void onEventMainThread(Events.ContactUpdated e) {
            updateContactLocation(e.getContact());
        }


        public void updateContactLocation(Contact c) {

            if (c.getMarker() != null) {
                Log.v(this.toString(), "updating marker position of " + c.getTopic());
                c.updateMarkerPosition();
            } else {
                Log.v(this.toString(), "creating marker for " + c.getTopic());
                Marker m = googleMap.addMarker(
                        new MarkerOptions().position(c.getLocation().getLatLng()).icon(
                                c.getUserImageDescriptor()));
                markerToContacts.put(m, c);
                c.setMarker(m);
            }


            if (c == getCurrentlyTrackedContact())
                focus(c);
        }

        public void centerMap(LatLng latlon) {
            CameraUpdate center = CameraUpdateFactory.newLatLng(latlon);
            googleMap.animateCamera(center);
        }

//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            
//            
//            View v = super.onCreateView(inflater, container, savedInstanceState);
//
//            getMap().setIndoorEnabled(true);
//            getMap().setMyLocationEnabled(true);
//
//            UiSettings s = getMap().getUiSettings();
//            s.setCompassEnabled(false);
//            s.setMyLocationButtonEnabled(true);
//            s.setTiltGesturesEnabled(false);
//            s.setCompassEnabled(false);
//            s.setRotateGesturesEnabled(false);
//            s.setZoomControlsEnabled(true);
//
//            
//               
//            return v;
//        }


        @Override
        public void onStop() {
           EventBus.getDefault().unregister(this);
            super.onStop();
        }
        
        public void focus(Contact c) {
            if (c == null)
                return;

            ActivityPreferences.setTrackingUsername(c.getTopic());
            centerMap(c.getLocation().getLatLng());
            selectedContactName.setText(c.toString());
            selectedContactLocation.setText(c.getLocation().getGeocoder());
            selectedContactDetails.setVisibility(View.VISIBLE);

        }

        public void setLocation(GeocodableLocation location) {
            Location l = location.getLocation();
            Log.v(this.toString(), "Setting location");

            if (l == null) {
                Log.v(this.toString(), "location not available");
                // showLocationUnavailable();
                return;
            }

            LatLng latlong = new LatLng(l.getLatitude(), l.getLongitude());

            if (location.getGeocoder() != null) {
                Log.v(this.toString(), "Reusing geocoder");
                // locationPrimary.setText(location.getGeocoder());
            } else {
                // Start async geocoder lookup and display latlon until geocoder
                // reeturns something
                if (Geocoder.isPresent()) {
                    Log.v(this.toString(), "Requesting geocoder");
                    (new ReverseGeocodingTask(getActivity(), handler))
                            .execute(new GeocodableLocation[] {
                                location
                            });

                } else {
                    // locationPrimary.setText(location.toLatLonString());
                }
            }
            // locationMeta.setText(App.getInstance().formatDate(new Date()));

            // showLocationAvailable();
        }

    }

    public static class FriendsFragment extends Fragment {
        ListView friendsListView;
        TextView currentLocation;
        
        private static FriendsFragment instance;

        public static FriendsFragment getInstance() {
            if (instance == null)
                instance = new FriendsFragment();

            return instance;
        }

        @Override
        public void onStart() {
            super.onStart();
            // Reload the list with data that might have arrived while the
            // activity was stopped
            EventBus.getDefault().register(this);
            ServiceApplication.getContactsAdapter().notifyDataSetChanged();
        }

        public void onEventMainThread(Events.LocationUpdated e) {
            currentLocation.setText(e.getGeocodableLocation().toString());
        }

        
        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_friends, container, false);
            currentLocation = (TextView)v.findViewById(R.id.currentLocation);
            
            friendsListView = (ListView) v.findViewById(R.id.friendsListView);                                   
            friendsListView.setAdapter(ServiceApplication.getContactsAdapter());
            friendsListView.setOnItemClickListener(new OnItemClickListener() {
                
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Contact c = (Contact) ServiceApplication.getContactsAdapter().getItem(position); 
                    if(c == null || c.getLocation() == null)
                        return;
                    
                    MapFragment.getInstance().focus(c);
                    viewPager.setCurrentItem(PagerAdapter.MAP_FRAGMENT);
                }
            });

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

        private static StatusFragment instance;

        public static StatusFragment getInstance() {
            if (instance == null)
                instance = new StatusFragment();

            return instance;
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
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
            locatorCurAccuracy.setText("±" + e.getGeocodableLocation().getLocation().getAccuracy()
                    + "m");
            locatorCurLatLonTime.setText(ServiceApplication.getInstance().formatDate(e.getDate()));
        }

        public void onEventMainThread(Events.PublishSuccessfull e) {
            if (e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
                GeocodableLocation l = (GeocodableLocation) e.getExtra();
                locatorLastPubLatLon.setText(l.toLatLonString());
                locatorLastPubAccuracy.setText("±" + l.getLocation().getAccuracy() + "m");
                locatorLastPubLatLonTime.setText(ServiceApplication.getInstance().formatDate(
                        e.getDate()));
            }
        }

        public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
            locatorStatus.setText(Defaults.State.toString(e.getState()));
        }

        public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
            brokerStatus.setText(Defaults.State.toString(e.getState()));
            if (e.getExtra() != null && e.getExtra() instanceof Exception
                    && e.getExtra().getClass() != null) {
                brokerError.setText(((Exception) e.getExtra()).getCause().getLocalizedMessage());
            } else {
                brokerError.setText(getString(R.string.na));
            }
        }

    }

}
