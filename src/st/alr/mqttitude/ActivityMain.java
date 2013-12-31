
package st.alr.mqttitude;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.GeocodableLocation;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
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

//    PagerAdapter pagerAdapter;
//    static ViewPager viewPager;
//    ServiceConnection serviceConnection;
    
//    @Override
//    protected void onDestroy() {
//        unbindService(serviceConnection);
//        super.onDestroy();
//    }
    static FragmentManager fragmentManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        startService(new Intent(this, ServiceProxy.class));
        int fragmentId = FragmentManager.CONTACT_FRAGMENT;
        if (savedInstanceState != null) 
        {
            // delete previously stored fragments after orientation change (see http://stackoverflow.com/a/13996054/899155). 
            // Without this, two map fragments would exists after rotating the device, of which the visible one would not receive updates. 
           savedInstanceState.remove ("android:support:fragments");
           fragmentId = savedInstanceState.getInt("currentFragment");          
        } 
        super.onCreate(savedInstanceState);
                
        setContentView(R.layout.activity_main);
        fragmentManager = new FragmentManager();
        Log.v(this.toString(), "fragmentId" +fragmentId );
        fragmentManager.showFragment(fragmentId, this);
        

        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {}

    }
    
    public static class FragmentManager{
        public static final int CONTACT_FRAGMENT = 1;
        public static final int MAP_FRAGMENT = 2;
        public static final int DETAIL_FRAGMENT = 3;
        
        private final int COUNT = 3;
        public Fragment[] fragments;
        private int current; 

        public FragmentManager() {
            current = 0;
            fragments = new Fragment[COUNT+1];
        }
        public Fragment getCurrentFragment(){
            return getFragment(getCurrentFragmentId());
        }
        public int getCurrentFragmentId(){
            return current;
        }
        public Fragment showFragment(int id, FragmentActivity activity) {
          Log.v(this.toString(), "Showing fragment with id " + id);

            Fragment f = activity.getSupportFragmentManager().findFragmentByTag("f:"+id);
            Fragment prev = activity.getSupportFragmentManager().findFragmentByTag("f:"+current);
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();

            if(prev != null && prev.isVisible())
                ft.hide(prev);
            
            if(f!= null && f.isAdded()) {
                ft.show(f);                
            } else {
                f=getFragment(id);
                ft.add(R.id.main, f, "f:"+id);                            
            }

            ft.commit();
            activity.getSupportFragmentManager().executePendingTransactions();

            current = id;

            return f;
        }
        
        public Fragment getFragment(int id) {
            Fragment f = fragments[id];
            
            if(f == null) {
                if(id == CONTACT_FRAGMENT)
                    f = FriendsFragment.getInstance();
                else if (id == MAP_FRAGMENT)
                    f = MapFragment.getInstance();
                else if (id == DETAIL_FRAGMENT)
                    f = DetailsFragment.getInstance();

                fragments[id] = f;
            }
                        
            return f;
                            
        }
     
    }
    
    
    @Override
    public void onBackPressed() {
        if(fragmentManager.getCurrentFragmentId() == FragmentManager.CONTACT_FRAGMENT)
            super.onBackPressed();
        else
            fragmentManager.showFragment(FragmentManager.CONTACT_FRAGMENT, this);           
    }


    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
//        } else if (itemId == R.id.menu_publish) {
//                ServiceProxy.getServiceLocator().publishLastKnownLocation();
//                return true;
        } else if (itemId == R.id.menu_share) {
                this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void share(View view) {
        GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
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
    public void onStart() {
        super.onStart();
            ServiceProxy.getServiceLocator().enableForegroundMode();

    }

    @Override
    public void onStop() {
            ServiceProxy.getServiceLocator().enableBackgroundMode();

        super.onStop();
    }

    

    @Override
    public void onResume(){
        super.onResume();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("currentFragment", fragmentManager.getCurrentFragmentId());
        super.onSaveInstanceState(savedInstanceState);
    }

    public void contactImageClicked(View v) {
        View parent = (View) v.getParent();
        String topic = (String) parent.getTag();
        Log.v(this.toString(), "topic " + topic);
        DetailsFragment f = (DetailsFragment) fragmentManager.showFragment(FragmentManager.DETAIL_FRAGMENT, this);
        f.show(App.getContacts().get(topic));

    }
    Bundle fragmentBundle;
    
    public static class MapFragment extends Fragment {
        private static MapFragment instance;
        private GeocodableLocation currentLocation;
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        private ImageView selectedContactImage;
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
            System.out.println("map fragment inflated");

            markerToContacts = new HashMap<String, Contact>();
            selectedContactDetails = (LinearLayout )v.findViewById(R.id.contactDetails);
            selectedContactName =(TextView )v.findViewById(R.id.title);
            selectedContactLocation = (TextView )v.findViewById(R.id.subtitle);
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
            if(currentLocation == null)
                return;
            selectedContactDetails.setVisibility(View.GONE);
            ActivityPreferences.setTrackingUsername(Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
            centerMap(currentLocation.getLatLng());
        }

        public void focusCurrentlyTrackedContact(){
            Contact c = getCurrentlyTrackedContact();
            
            if(c != null)
                focus(getCurrentlyTrackedContact()); 
            else if (isTrackingCurrentLocation())
                focusCurrentLocation();            
        }
        
        public void focus(final Contact c) {
            Log.v(this.toString(), "focussing " +c.getTopic());

            if (c == null) {
                Log.v(this.toString(), "no contact, abandon ship!");                
                return;
            }
            
            ActivityPreferences.setTrackingUsername(c.getTopic());
            centerMap(c.getLocation().getLatLng());

            selectedContactName.setText(c.toString());
            selectedContactLocation.setText(c.getLocation().toString());
            
            selectedContactImage.setImageBitmap(c.getUserImage());

            selectedContactImage.setTag(c.getTopic());
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

            if(isTrackingCurrentLocation())
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
        
        public boolean isTrackingCurrentLocation(){
            return ActivityPreferences.getTrackingUsername().equals(Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
        }

    }

    @SuppressLint("NewApi")
    public static class FriendsFragment extends Fragment implements StaticHandlerInterface{
        LinearLayout friendsListView;
        TextView currentLocTitle;
        TextView currentLoc;

//        TextView currentAcc;
//        TextView currentTime;       
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
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);

            currentLoc = (TextView) thisdevice.findViewById(R.id.currentLocation);  
            
            
            currentLoc.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                  MapFragment f = MapFragment.getInstance();
                  if(f.hasCurrentLocation()) {
                      Log.v(this.toString(), "Focusing the current location");
                      f.focusCurrentLocation();
                      //viewPager.setCurrentItem(PagerAdapter.MAP_FRAGMENT);
                      
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
            currentLoc.setText(l.toString());
            
            if(l.getGeocoder() == null && resolveGeocoder)
                (new ReverseGeocodingTask(getActivity(), handler)).execute(new GeocodableLocation[] { l });

//            currentAcc.setText("±" + l.getLocation().getAccuracy() );
//            currentTime.setText(App.formatDate(new Date(l.getTime())));
        }

        
        
        @Override
        public void handleHandlerMessage(Message msg) {
            
            if (msg.what == ReverseGeocodingTask.GEOCODER_RESULT && msg.obj != null) {
                if((GeocodableLocation) msg.obj == currentLocation) {
                    updateCurrentLocation((GeocodableLocation) msg.obj, false);
                }               else {
                GeocodableLocation l = (GeocodableLocation) msg.obj;
                Log.v("", "loc: " + l);
                Log.v("", "friendsListView: " + friendsListView);
                Log.v("", "l.getTag(): " + l.getTag());
                Log.v("", "l.getTag(): " + l.getTag());

                TextView tv = (TextView)friendsListView.findViewWithTag(l.getTag()).findViewById(R.id.subtitle);                    
                tv.setText(l.toString());
                }
            }
        }

        public void onEventMainThread(Events.ContactUpdated e) {            
            updateContactView(e.getContact());
        }

        public void updateContactView(final Contact c){
            View v = friendsListView.findViewWithTag(c.getTopic()); 
            if(v == null) {
            
                if (c.getView() != null) {
                    v = c.getView();
                    ((ViewGroup)v.getParent()).removeView(v); // remove from old view first to allow it to be added to the new view again
                } else {
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
                          
                          fragmentManager.showFragment(FragmentManager.MAP_FRAGMENT, getActivity());
                          ((MapFragment) fragmentManager.getCurrentFragment()).focus(c);

                        }
                    });


                }                
                v.setTag(c.getTopic());
                friendsListView.addView(c.getView());
                friendsListView.setVisibility(View.VISIBLE);
                    
            }
            
            ((TextView) v.findViewById(R.id.title)).setText(c.toString());            
            ((TextView) v.findViewById(R.id.subtitle)).setText(c.getLocation().toString());
            ImageView img = ((ImageView) v.findViewById(R.id.image));
            img.setImageBitmap(c.getUserImage());

            (new ReverseGeocodingTask(getActivity(), handler)).execute(new GeocodableLocation[] { c.getLocation() });

            

        }
        


    }
        
    
    public static class DetailsFragment extends Fragment {
        Contact contact;
        
        TextView location;
        TextView accuracy;
        TextView time;


        public static DetailsFragment getInstance() {
            DetailsFragment instance = new DetailsFragment();
            return instance;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);     
            
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
        }
        
        public void show(Contact c){
            contact = c;
            location.setText(c.getLocation().toString());
            accuracy.setText(""+c.getLocation().getAccuracy());
            time.setText(App.formatDate(new Date(c.getLocation().getTime())));


        }

        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_details, container, false);
            location = (TextView) v.findViewById(R.id.location);
            accuracy = (TextView) v.findViewById(R.id.accuracy);
            time = (TextView) v.findViewById(R.id.time);
            
            if(savedInstanceState != null)
                show(App.getContacts().get(savedInstanceState.getString("topic")));

                return v;
        }

        public void onEventMainThread(Events.ContactUpdated e) {            
            if(e.getContact() == contact)
                show(e.getContact());
        }
            
        
        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putString("topic", contact.getTopic());
          }

   }
    
    
    
}
