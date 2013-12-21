
package st.alr.mqttitude;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceBindable;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import android.annotation.SuppressLint;
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
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.data.e;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import de.greenrobot.event.EventBus;

public class ActivityMain extends FragmentActivity {

    PagerAdapter pagerAdapter;
    static ViewPager viewPager;
    //ServiceConnection serviceApplicationConnection;
    
//    @Override
//    protected void onDestroy() {
//        unbindService(serviceApplicationConnection);
//        super.onDestroy();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        // delete previously stored fragments after orientation change (see http://stackoverflow.com/a/13996054/899155). 
        // Without this, two map fragments would exists after rotating the device, of which the visible one would not receive updates. 
        if (savedInstanceState != null) 
        {
           savedInstanceState.remove ("android:support:fragments");
        } 
        super.onCreate(savedInstanceState);
                

        setContentView(R.layout.activity_main);
        
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        pagerAdapter = new PagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
//        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//            @Override
//            public void onPageSelected(int position) {
//                actionBar.setSelectedNavigationItem(position);
//            }
//        });

//        for (int j = 0; j < pagerAdapter.getCount(); j++) {
//            actionBar.addTab(actionBar.newTab().setText(pagerAdapter.getPageTitle(j)).setTabListener(this));
//        }

        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {}

    }
    
    @Override
    public void onBackPressed() {
        if(viewPager.getCurrentItem() == PagerAdapter.MAP_FRAGMENT)
            viewPager.setCurrentItem(PagerAdapter.CONTACT_FRAGMENT);
        else 
            super.onBackPressed();
           
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
        if (itemId == R.id.menu_preferences) {
            Intent intent1 = new Intent(this, ActivityPreferences.class);
            startActivity(intent1);
            return true;
        } else if (itemId == R.id.menu_publish) {
            if (ServiceApplication.getInstance().getServiceLocator() != null)
                ServiceApplication.getInstance().getServiceLocator().publishLastKnownLocation();
            return true;
        } else if (itemId == R.id.menu_share) {
            if (ServiceApplication.getInstance().getServiceLocator() != null)
                this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void share(View view) {
        GeocodableLocation l = ServiceApplication.getInstance().getServiceLocator().getLastKnownLocation();
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

//    @Override
//    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//        viewPager.setCurrentItem(tab.getPosition());
//    }
//
//    @Override
//    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//    }
//
//    @Override
//    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class PagerAdapter extends FragmentPagerAdapter {
        public static final int CONTACT_FRAGMENT = 0;
        public static final int MAP_FRAGMENT = 1;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }
        
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case CONTACT_FRAGMENT:
                    return FriendsFragment.getInstance();
                default:
                    return MapFragment.getInstance();
//                default:
//                    return StatusFragment.getInstance();
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.titleFriends);

                case 1:
                    return getString(R.string.titleMap);
//                case 2:
//                    return getString(R.string.titleStatus);
            }
            return "BUGBUGBUG";
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ServiceApplication.getInstance() != null)
            ServiceApplication.getInstance().enableForegroundMode();

    }

    @Override
    public void onStop() {
        if (ServiceApplication.getInstance() != null)
            ServiceApplication.getInstance().enableBackgroundMode();

        super.onStop();
    }

    

    @Override
    public void onResume(){
        super.onResume();
    }
    

    public static class MapFragment extends Fragment {
        private static MapFragment instance;
        private GeocodableLocation currentLocation;
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        private ImageView selectedContactImage;
        private TextView selectedContactTime;
        private TextView selectedContactAccuracy;
        private Map<String, Contact> markerToContacts;

        public static MapFragment getInstance() {
            if (instance == null) {
                instance = new MapFragment();
            }
            return instance;
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
        public void onResume() {
            super.onResume();
            mMapView.onResume();

            // Initial population of the map with all exisiting contacts
            for (Contact c : ServiceApplication.getContacts().values())
                updateContactLocation(c);

            focusCurrentlyTrackedContact();
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                Bundle savedInstanceState) {
            
            View v = inflater.inflate(R.layout.fragment_map, container, false);
            
            markerToContacts = new HashMap<String, Contact>();
            selectedContactDetails = (LinearLayout )v.findViewById(R.id.contactDetails);
            selectedContactName =(TextView )v.findViewById(R.id.title);
            selectedContactLocation = (TextView )v.findViewById(R.id.subtitle);
            selectedContactTime = (TextView )v.findViewById(R.id.time);
            selectedContactAccuracy = (TextView )v.findViewById(R.id.acc);
            selectedContactImage = (ImageView )v.findViewById(R.id.image);
            selectedContactDetails.setVisibility(View.GONE);
            
            mMapView = (MapView) v.findViewById(R.id.mapView);
            mMapView.onCreate(savedInstanceState);
            mMapView.onResume(); //needed to get the map to display immediately
            googleMap = mMapView.getMap();  
            
            // Check if we were successful in obtaining the map.
            if (mMapView != null) {
                try {
                    MapsInitializer.initialize(getActivity());
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace(); // TODO: Catch not available PlayServices
                }
                
                setUpMap();
            }
                        
            return v;
        }
        
        private void setUpMap() {
            googleMap.setIndoorEnabled(true);
            googleMap.setMyLocationEnabled(true);

            UiSettings s = googleMap.getUiSettings();
            s.setCompassEnabled(false);
            s.setMyLocationButtonEnabled(false);
            s.setTiltGesturesEnabled(false);
            s.setCompassEnabled(false);
            s.setRotateGesturesEnabled(false);
            s.setZoomControlsEnabled(false);
            
            mMapView.getMap().setOnMarkerClickListener(new OnMarkerClickListener() {
                
                @Override
                public boolean onMarkerClick(Marker m) {
                    Contact c = markerToContacts.get(m.getId());
                    
                    if(c != null)
                        focus(c);
                    
                    return false;
                 }
            });
            
            mMapView.getMap().setOnMapClickListener(new OnMapClickListener() {
                
                @Override
                public void onMapClick(LatLng arg0) {
                    selectedContactDetails.setVisibility(View.GONE);
                    
                }
            });
        }
        

        public void centerMap(LatLng l) {
            centerMap(l, 15.0f);
        }
        
        public void centerMap(LatLng latlon, float f) {
            Log.v(this.toString(), "centering map at " + latlon.toString());
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latlon, f);
            mMapView.getMap().animateCamera(center);
        }
        
        public void updateContactLocation(Contact c) {
            if (c.getMarker() != null) {
                c.getMarker().remove();
            }
            
            Marker m = googleMap.addMarker(new MarkerOptions().position(c.getLocation().getLatLng()).icon(c.getMarkerImageDescriptor()));            
            markerToContacts.put(m.getId(), c);            
            c.setMarker(m);

            if (c == getCurrentlyTrackedContact())
                focus(c);
        }



        public void focusCurrentLocation() {
            selectedContactDetails.setVisibility(View.GONE);
            ActivityPreferences.setTrackingUsername("+CURRENTDEVICELOCATION+");
            centerMap(currentLocation.getLatLng());
        }

        public void focusCurrentlyTrackedContact(){
            focus(getCurrentlyTrackedContact());
        }
        
        public void focus(final Contact c) {
            Log.v(this.toString(), "map fragment focussing " +c);

            if (c == null) {
                Log.v(this.toString(), "no contact, abandon ship!");                
                return;
            }
            
            ActivityPreferences.setTrackingUsername(c.getTopic());
            centerMap(c.getLocation().getLatLng());
            Log.v(this.toString(), "map fragment focussing " +c.getTopic());

            selectedContactName.setText(c.toString());
            selectedContactLocation.setText(c.getLocation().toString());
            selectedContactTime.setText(App.formatDate(new Date(c.getLocation().getTime()*1000)));
            selectedContactAccuracy.setText("±" + c.getLocation().getAccuracy());
            
            selectedContactImage.setImageBitmap(c.getUserImage());

            selectedContactDetails.setVisibility(View.VISIBLE);
            selectedContactDetails.setOnClickListener(new OnClickListener() {                
                @Override
                public void onClick(View v) {                    
                    centerMap(c.getLocation().getLatLng());
                }
            });
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            updateContactLocation(e.getContact());
        }

        
        public void onEventMainThread(Events.LocationUpdated e) {
            currentLocation = e.getGeocodableLocation();

            if(ActivityPreferences.getTrackingUsername().equals("+CURRENTDEVICELOCATION+"))
                focusCurrentLocation();
        }
        
        public Contact getCurrentlyTrackedContact() {
            Contact c = ServiceApplication.getContacts().get(ActivityPreferences.getTrackingUsername());   
            if(c != null)
                Log.v(this.toString(), "getCurrentlyTrackedContact == " + c.getTopic());
            else 
                Log.v(this.toString(), "getCurrentlyTrackedContact == null || tracking current location" );

            return c;
        }
        
        public boolean hasCurrentLocation(){
            return currentLocation != null;
        }
    }

    @SuppressLint("NewApi")
    public static class FriendsFragment extends Fragment implements StaticHandlerInterface{
        LinearLayout friendsListView;
        TextView currentLoc;
        TextView currentAcc;
        TextView currentTime;       
        GeocodableLocation currentLocation;
        private static Handler handler;
        
        private static FriendsFragment instance;

        public static FriendsFragment getInstance() {
            if (instance == null)
                instance = new FriendsFragment();

            return instance;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);          
            handler = new StaticHandler(this);
        }
        
        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().register(this);
        }

        @Override
        public void onStop() {
            EventBus.getDefault().unregister(this);
            super.onStop();
        }


        @Override
        public void onResume() {
            super.onResume();
                        
            for (Contact c : ServiceApplication.getContacts().values())
                updateContactView(c);
        }

        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_friends, container, false);
            
            friendsListView = (LinearLayout) v.findViewById(R.id.friendsListView);  
            currentAcc = (TextView) v.findViewById(R.id.currentAccuracy);  
            currentLoc = (TextView) v.findViewById(R.id.currentLocation);  
            currentTime = (TextView) v.findViewById(R.id.currentTime);  
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);
            thisdevice.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                  MapFragment f = MapFragment.getInstance();
                  if(f.hasCurrentLocation()) {
                      Log.v(this.toString(), "Focusing the current location");
                      f.focusCurrentLocation();
                      viewPager.setCurrentItem(PagerAdapter.MAP_FRAGMENT);
                  } else {
                      Log.v(this.toString(), "No current location available");
                  }
                }
            });

            for (Contact c : ServiceApplication.getContacts().values())
                updateContactView(c);

            return v;
        }

        public void onEventMainThread(Events.LocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
        }
        

        public void updateCurrentLocation(GeocodableLocation l, boolean resolveGeocoder) {
            currentLocation = l;
        
            // Current location changes often, don't waste resources to resolve the geocoder
            currentLoc.setText(l.toLatLonString());
            currentAcc.setText("±" + l.getLocation().getAccuracy() );
            currentTime.setText(App.formatDate(new Date(l.getTime())));
        }

        
        
        public void handleHandlerMessage(Message msg) {
            if (msg.what == ReverseGeocodingTask.GEOCODER_RESULT && msg.obj != null) {
                GeocodableLocation l = (GeocodableLocation) msg.obj;
                TextView tv = (TextView)friendsListView.findViewWithTag(l.getTag()).findViewById(R.id.subtitle);                    
                tv.setText(l.toString());
            }
        }

        public void onEventMainThread(Events.ContactUpdated e) {            
            updateContactView(e.getContact());
        }

        public void updateContactView(Contact c){
            View v = friendsListView.findViewWithTag(c.getTopic()); 
            if(v == null) {
            
                if (c.getView() != null) {
                    Log.v(this.toString(), "updating view of " + c.getTopic());
                    v = c.getView();
                    ((ViewGroup)v.getParent()).removeView(v); // remove from old view first to allow it to be added to the new view again

                } else {
                    Log.v(this.toString(), "creating view for " + c.getTopic());
                    v = getActivity().getLayoutInflater().inflate(R.layout.friend_list_item, null, false);
                    c.setView(v);
                    v.setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                          Contact c = ServiceApplication.getContacts().get(v.getTag()); 
                          Log.v(this.toString(), "Focusing " + c);
                          if(c == null || c.getLocation() == null) {
                              Log.v(this.toString(), "No contact or no location ");

                              return;
                          
                          }
                          MapFragment.getInstance().focus(c);
                          viewPager.setCurrentItem(PagerAdapter.MAP_FRAGMENT);

                        }
                    });

                }                
                v.setTag(c.getTopic());
                friendsListView.addView(c.getView());
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT){
                    Transition t = new android.transition.Fade();
                    TransitionManager.beginDelayedTransition(friendsListView, t);

                }
                    friendsListView.setVisibility(View.VISIBLE);
                    
            }
            ((TextView) v.findViewById(R.id.title)).setText(c.toString());
            
            ((TextView) v.findViewById(R.id.subtitle)).setText(c.getLocation().toString());
            ((TextView) v.findViewById(R.id.acc)).setText("±" + c.getLocation().getAccuracy());
            ((TextView) v.findViewById(R.id.time)).setText(App.formatDate(new Date(c.getLocation().getTime()*1000)));

            (new ReverseGeocodingTask(getActivity(), handler)).execute(new GeocodableLocation[] {
                    c.getLocation()
                });

            ((ImageView) v.findViewById(R.id.image)).setImageBitmap(c.getUserImage());

        }
        


    }
    
    

//    public static class StatusFragment extends Fragment {
//        private TextView locatorStatus;
//        private TextView locatorCurLatLon;
//        private TextView locatorCurAccuracy;
//        private TextView locatorCurLatLonTime;
//        private TextView locatorLastPubLatLon;
//        private TextView locatorLastPubAccuracy;
//        private TextView locatorLastPubLatLonTime;
//        private TextView brokerStatus;
//        private TextView brokerError;
//
//        private static StatusFragment instance;
//
//        public static StatusFragment getInstance() {
//            if (instance == null)
//                instance = new StatusFragment();
//
//            return instance;
//        }
//
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//
//        }
//
//        @Override
//        public void onStart() {
//            super.onStart();
//            Log.v(this.toString(), "registering");
//
//            EventBus.getDefault().registerSticky(this);
//        }
//
//        @Override
//        public void onStop() {
//            Log.v(this.toString(), "unregistering");
//            EventBus.getDefault().unregister(this);
//            super.onStop();
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            View v = inflater.inflate(R.layout.fragment_status, container, false);
//            locatorStatus = (TextView) v.findViewById(R.id.locatorStatus);
//            locatorCurLatLon = (TextView) v.findViewById(R.id.locatorCurLatLon);
//            locatorCurAccuracy = (TextView) v.findViewById(R.id.locatorCurAccuracy);
//            locatorCurLatLonTime = (TextView) v.findViewById(R.id.locatorCurLatLonTime);
//
//            locatorLastPubLatLon = (TextView) v.findViewById(R.id.locatorLastPubLatLon);
//            locatorLastPubAccuracy = (TextView) v.findViewById(R.id.locatorLastPubAccuracy);
//            locatorLastPubLatLonTime = (TextView) v.findViewById(R.id.locatorLastPubLatLonTime);
//
//            brokerStatus = (TextView) v.findViewById(R.id.brokerStatus);
//            brokerError = (TextView) v.findViewById(R.id.brokerError);
//
//            return v;
//        }
//
//        public void onEventMainThread(Events.LocationUpdated e) {
//            locatorCurLatLon.setText(e.getGeocodableLocation().toLatLonString());
//            locatorCurAccuracy.setText("±" + e.getGeocodableLocation().getLocation().getAccuracy()
//                    + "m");
//            locatorCurLatLonTime.setText(App.formatDate(e.getDate()));
//        }
//
//        public void onEventMainThread(Events.PublishSuccessfull e) {
//            if (e.getExtra() != null && e.getExtra() instanceof GeocodableLocation) {
//                GeocodableLocation l = (GeocodableLocation) e.getExtra();
//                locatorLastPubLatLon.setText(l.toLatLonString());
//                locatorLastPubAccuracy.setText("±" + l.getLocation().getAccuracy() + "m");
//                locatorLastPubLatLonTime.setText(App.formatDate(
//                        e.getDate()));
//            }
//        }
//
//        public void onEventMainThread(Events.StateChanged.ServiceLocator e) {
//            locatorStatus.setText(Defaults.State.toString(e.getState(), getActivity()));
//        }
//
//        public void onEventMainThread(Events.StateChanged.ServiceMqtt e) {
//            brokerStatus.setText(Defaults.State.toString(e.getState(), getActivity()));
//            if (e.getExtra() != null && e.getExtra() instanceof Exception
//                    && e.getExtra().getClass() != null) {
//                brokerError.setText(((Exception) e.getExtra()).getCause().getLocalizedMessage());
//            } else {
//                brokerError.setText(getString(R.string.na));
//            }
//        }
//
//    }

}
