
package st.alr.mqttitude;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
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
    static FragmentHandler fragmentHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        startService(new Intent(this, ServiceProxy.class));
        int fragmentId = FragmentHandler.CONTACT_FRAGMENT;
        if (savedInstanceState != null)
        {
            // delete previously stored fragments after orientation change (see
            // http://stackoverflow.com/a/13996054/899155).
            // Without this, two map fragments would exists after rotating the
            // device, of which the visible one would not receive updates.
             savedInstanceState.remove ("android:support:fragments");
            fragmentId = savedInstanceState.getInt("currentFragment");
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Log.v(this.toString(), "getSupportFragmentManager is" + getSupportFragmentManager());

        fragmentHandler = new FragmentHandler(getSupportFragmentManager());
        Log.v(this.toString(), "Instantiated new fragment handler" + fragmentHandler);
        Log.v(this.toString(), "fragmentId" + fragmentId);
        fragmentHandler.forward(fragmentId, null);
        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
        }

    }

    public static class FragmentHandler {
        public static final int CONTACT_FRAGMENT = 1;
        public static final int MAP_FRAGMENT = 2;
        public static final int DETAIL_FRAGMENT = 3;

        private final int COUNT = 3;
        private Fragment[] fragments;
        private int current;
        private FragmentManager fragmentManager;
        private HeadlessFragment store;
        
        public FragmentHandler(FragmentManager fm) {
            Log.v(this.toString(), "Instantiating new fragmenthandler");
            store = HeadlessFragment.getInstance();
            fragmentManager = fm;
            current = 0;
            fragments = new Fragment[COUNT + 1];
        }

        public Fragment getCurrentFragment(Bundle extras) {
            return getFragment(getCurrentFragmentId(), extras);
        }

        public int getCurrentFragmentId() {
            return current;
        }

        public Fragment showFragment(int id, Bundle extras) {
            Log.v(this.toString(), "using fragmentManager" + fragmentManager);

            Fragment f = getFragment(id, null);
            Fragment prev = getFragment(current, null); 
            FragmentTransaction ft = fragmentManager.beginTransaction();

            handleFragmentArguments(id, extras);
            
            if (prev != null && prev.isAdded() && prev.isVisible()) {
                ft.hide(prev);
                Log.v(this.toString(), "hiding fragment");
            }
            
            if (f.isAdded()) {
                Log.v(this.toString(), "showing fragment " + f);
                ft.show(f);
            } else {
                Log.v(this.toString(), "adding fragment " + f);
                ft.add(R.id.main, f, "f:" + id);
            }
            

            ft.commit();
            fragmentManager.executePendingTransactions();

            current = id;

            return f;
        }
        
        // Shows the previous fragment
        public Fragment back(){
            HeadlessFragment.getInstance().popBackStack();
            
            return showFragment(HeadlessFragment.getInstance().getBackStackHead(), null);
        }
        
        
        public Fragment forward(Integer id, Bundle extras) {
            if(HeadlessFragment.getInstance().getBackStackHead()!=id)
                HeadlessFragment.getInstance().pushBackStack(id);
            
            return showFragment(id, extras);
        }
        public boolean atRoot(){
            return HeadlessFragment.getInstance().getBackStackSize() == 1;
        }
        
        


        
        private Bundle handleFragmentArguments(int id, Bundle extras) {
            Bundle oldExtras = store.getBundle(id);
            if(extras != null) { // overwrite old extras
                store.setBundle(id, extras);
                return extras;
            } else if (extras == null && oldExtras != null) { // return previously  set extras
                return oldExtras;                
            } else {
                return null; 
            }                
        }

        public Fragment getFragment(int id, Bundle extras) {
            Fragment f = fragments[id];                    
                              
            if (f == null) {
                if (id == CONTACT_FRAGMENT)
                    f = FriendsFragment.getInstance(extras);
                else if (id == MAP_FRAGMENT)
                    f = MapFragment.getInstance(extras);
                else if (id == DETAIL_FRAGMENT)
                    f = DetailsFragment.getInstance(extras);

                fragments[id] = f;
            }
            
            return f;

        }

        public void removeAll(){
            Log.v(this.toString(), "Removing all fragments");
            FragmentTransaction ft = fragmentManager.beginTransaction();
            
            for (int i = 0; i < fragments.length; i++)
                if(fragments[i] != null) {
                    Log.v(this.toString(), "Removing fragment " + fragments[i]);
                    ft.hide(fragments[i]).remove(fragments[i]);
                }
            ft.commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        
    }

    @Override
    public void onBackPressed() {
        if (fragmentHandler.atRoot())
            super.onBackPressed();
        else
            fragmentHandler.back();
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
            // } else if (itemId == R.id.menu_publish) {
            // ServiceProxy.getServiceLocator().publishLastKnownLocation();
            // return true;
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
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onDestroy(){
        fragmentHandler.removeAll();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("currentFragment", fragmentHandler.getCurrentFragmentId());
        super.onSaveInstanceState(savedInstanceState);
    }

    public void contactImageClicked(View v) {
        View parent = (View) v.getParent();
        String topic = (String) parent.getTag();
        Log.v(this.toString(), "topic " + topic);
        Bundle b = new Bundle();
        b.putString("topic", topic);
        fragmentHandler.forward(FragmentHandler.DETAIL_FRAGMENT, b);

    }

    Bundle fragmentBundle;

    public static class MapFragment extends Fragment {
//        private static MapFragment instance;
        private GeocodableLocation currentLocation;
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        private ImageView selectedContactImage;
        private String selectedContact;
        private Map<String, Contact> markerToContacts;

        public static MapFragment getInstance(Bundle extras) {
//            if (instance == null) {
                MapFragment instance = new MapFragment();
                instance.setArguments(extras);
//            }
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
            selectedContactDetails = (LinearLayout) v.findViewById(R.id.contactDetails);
            selectedContactName = (TextView) v.findViewById(R.id.title);
            selectedContactLocation = (TextView) v.findViewById(R.id.subtitle);
            selectedContactImage = (ImageView) v.findViewById(R.id.image);
            selectedContactDetails.setVisibility(View.GONE);

            mMapView = (MapView) v.findViewById(R.id.mapView);
            mMapView.onCreate(savedInstanceState);
            mMapView.onResume(); // needed to get the map to display immediately
            googleMap = mMapView.getMap();

            // Check if we were successful in obtaining the map.
            if (mMapView != null) {
                try {
                    MapsInitializer.initialize(getActivity());
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace(); // TODO: Catch not available
                                         // PlayServices
                }

                setUpMap();
            }

            return v;
        }
        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            if(hidden)
                onHide();
            else
                onShow();
            super.onHiddenChanged(hidden);
        }
        
        
        private void onShow() {
            Log.v(this.toString(), "onShow");
            
        }
 
       private void onHide() {
            Log.v(this.toString(), "onHide");
        }
 
//       @Override
//       public void onSaveInstanceState(Bundle b) {
//           super.onSaveInstanceState(b);
//           b.putString("topic", contact.getTopic());
//           HeadlessFragment.getInstance().setBundle(FragmentHandler.DETAIL_FRAGMENT, b);
//       }
       
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

                    if (c != null)
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

            Marker m = googleMap.addMarker(new MarkerOptions()
                    .position(c.getLocation().getLatLng()).icon(c.getMarkerImageDescriptor()));
            markerToContacts.put(m.getId(), c);
            c.setMarker(m);

            if (c == getCurrentlyTrackedContact())
                focus(c);
        }

        public void focusCurrentLocation() {
            GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
            if (l == null)
                return;
            selectedContactDetails.setVisibility(View.GONE);
            ActivityPreferences.setTrackingUsername(Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
            centerMap(l.getLatLng());
        }

        public void focusCurrentlyTrackedContact() {
            Contact c = getCurrentlyTrackedContact();

            if (c != null)
                focus(getCurrentlyTrackedContact());
            else if (isTrackingCurrentLocation())
                focusCurrentLocation();
        }

        public void focus(final Contact c) {
            Log.v(this.toString(), "focussing " + c.getTopic());

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
            Log.v(this.toString(), "my location updated");
            if (isTrackingCurrentLocation())
                focusCurrentLocation();
        }

        public Contact getCurrentlyTrackedContact() {
            Contact c = ServiceApplication.getContacts().get(
                    ActivityPreferences.getTrackingUsername());
            if (c != null)
                Log.v(this.toString(), "getCurrentlyTrackedContact == " + c.getTopic());
            else
                Log.v(this.toString(),
                        "getCurrentlyTrackedContact == null || tracking current location");

            return c;
        }

        public boolean hasCurrentLocation() {
            return currentLocation != null;
        }

        public boolean isTrackingCurrentLocation() {
            return ActivityPreferences.getTrackingUsername().equals(
                    Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
        }

    }

    @SuppressLint("NewApi")
    public static class FriendsFragment extends Fragment implements StaticHandlerInterface {
        private LinearLayout friendsListView;
        private Button currentLoc;
        private Button report;

        private static Handler handler;

       // private static FriendsFragment instance;

        public static FriendsFragment getInstance(Bundle extras) {
            //if (instance == null) {
            FriendsFragment  instance = new FriendsFragment();
                instance.setArguments(extras);
         //   }
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

            for (Contact c : ServiceApplication.getContacts().values())
                updateContactView(c);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_friends, container, false);

            friendsListView = (LinearLayout) v.findViewById(R.id.friendsListView);
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);

            
            currentLoc = (Button) thisdevice.findViewById(R.id.currentLocation);

            currentLoc.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (ServiceProxy.getServiceLocator().getLastKnownLocation() != null) {
                        Log.v(this.toString(), "Focusing the current location");
                        MapFragment f = (MapFragment) fragmentHandler.forward(FragmentHandler.MAP_FRAGMENT, null);
                        f.focusCurrentLocation();

                    } else {
                        Log.v(this.toString(), "No current location available");
                    }
                }
            });
            
            report = (Button) thisdevice.findViewById(R.id.report);
            report.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ServiceProxy.getServiceLocator().publishLastKnownLocation();
                    
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

            // Current location changes often, don't waste resources to resolve
            // the geocoder
            currentLoc.setText(l.toString());

            if (l.getGeocoder() == null && resolveGeocoder) {
                l.setTag("++MYLOCATION++");
                (new ReverseGeocodingTask(getActivity(), handler))
                        .execute(new GeocodableLocation[] {
                            l
                        });
            }
        }

        @Override
        public void handleHandlerMessage(Message msg) {

            if (msg.what == ReverseGeocodingTask.GEOCODER_RESULT && msg.obj != null) {
                if (((GeocodableLocation) msg.obj).getTag().equals("++MYLOCATION++")) {
                    updateCurrentLocation((GeocodableLocation) msg.obj, false);
                } else {
                    GeocodableLocation l = (GeocodableLocation) msg.obj;

                    TextView tv = (TextView) friendsListView.findViewWithTag(l.getTag())
                            .findViewById(R.id.subtitle);
                    if(tv != null)
                        tv.setText(l.toString());
                }
            }
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            updateContactView(e.getContact());
        }

        public void updateContactView(final Contact c) {
            View v = friendsListView.findViewWithTag(c.getTopic());
           
            if (v == null) {

                if (c.getView() != null) {
                    v = c.getView();
                    ((ViewGroup) v.getParent()).removeView(v); // remove from
                                                               // old view first
                                                               // to allow it to
                                                               // be added to
                                                               // the new view
                                                               // again
                } else {
                    v = getActivity().getLayoutInflater().inflate(R.layout.friend_list_item, null,
                            false);
                    c.setView(v);
                    v.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Contact c = ServiceApplication.getContacts().get(v.getTag());
                            Log.v(this.toString(), "Focusing " + c);
                            if (c == null || c.getLocation() == null) {
                                Log.v(this.toString(), "No contact or no location ");

                                return;

                            }
                            Log.v(this.toString(), "request for fragmenthandler fragment handler" + fragmentHandler);
                            fragmentHandler.forward(FragmentHandler.MAP_FRAGMENT, null);
                            ((MapFragment) fragmentHandler.getCurrentFragment(null)).focus(c);

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

            (new ReverseGeocodingTask(getActivity(), handler)).execute(new GeocodableLocation[] {
                c.getLocation()
            });

        }

    }

    public static class DetailsFragment extends Fragment {
        Contact contact;
        TextView location;
        TextView accuracy;
        TextView time;

        public static DetailsFragment getInstance(Bundle extras) {
                DetailsFragment instance = new DetailsFragment();
              instance.setArguments(extras);
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

        public void show(Contact c) {

            contact = c;
            location.setText(c.getLocation().toString());
            accuracy.setText("" + c.getLocation().getAccuracy());
            time.setText(App.formatDate(new Date(c.getLocation().getTime())));
            Log.v(this.toString(), "showing details of " + contact);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Bundle extras = HeadlessFragment.getInstance().getBundle(FragmentHandler.DETAIL_FRAGMENT);
            
            View v = inflater.inflate(R.layout.fragment_details, container, false);
            location = (TextView) v.findViewById(R.id.location);
            accuracy = (TextView) v.findViewById(R.id.accuracy);
            time = (TextView) v.findViewById(R.id.time);

            show(App.getContacts().get(extras.get("topic")));
            return v;
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            if (e.getContact() == contact)
                show(e.getContact());
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putString("topic", contact.getTopic());
            HeadlessFragment.getInstance().setBundle(FragmentHandler.DETAIL_FRAGMENT, b);
        }

    }

    
    
    public static class HeadlessFragment extends Fragment {
        static HeadlessFragment instance;
        private static HashMap<Integer, Bundle> store = new HashMap<Integer, Bundle>();
        private static LinkedList<Integer> backStack = new LinkedList<Integer>();
        public static HeadlessFragment getInstance() {
            if(instance == null) {
                instance = new HeadlessFragment();
            }
            return instance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

        }
        
        public void setBundle(Integer id, Bundle b) {
           store.put(id, b);
        }
        
        public Bundle getBundle(Integer id) {
           return store.get(id);
        }

        @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

        @Override
        public void onResume() {
            super.onResume();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return null;
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
        }
        
        public Integer pushBackStack(Integer id) {
            backStack.addLast(id);
            return id;
        }
        public Integer getBackStackHead() {
            
            return getBackStackSize() > 0 ? backStack.getLast() : -1;
        }

        public Integer popBackStack() {
            return backStack.removeLast();
        }
        public Integer getBackStackSize(){
            return backStack.size();
        }
        
    }
}
