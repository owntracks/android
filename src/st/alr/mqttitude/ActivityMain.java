
package st.alr.mqttitude;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
    private static final int CONTACT_PICKER_RESULT = 1001;  

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        startService(new Intent(this, ServiceProxy.class));

        // delete previously stored fragments after orientation change
        if (savedInstanceState != null)
            savedInstanceState.remove("android:support:fragments");

        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        FragmentHandler.getInstance().init(this, ContactsFragment.class);
        FragmentHandler.getInstance().showCurrentOrRoot(this);      
        
        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {}

    }
    
    protected ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) 
            {
                Log.v(this.toString(), "Service disconnected");
                isConnected = false;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                isConnected = true;
                Log.v(this.toString(), "Service connected");
                ServiceProxy.getServiceLocator().enableForegroundMode();                
            }
    };
    private boolean isConnected;


    public static class FragmentHandler extends Fragment {
        private Class<?> current;
        private Class<?> root;

        static FragmentHandler instance;
        
        private static HashMap<Class<?>, Bundle> store = new HashMap<Class<?>, Bundle>();
        private static HashMap<Class<?>, Fragment> fragments = new HashMap<Class<?>, Fragment>();
        
        private static LinkedList<Class<?>> backStack = new LinkedList<Class<?>>();


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public static FragmentHandler getInstance() {
            if (instance == null)
                instance = new FragmentHandler();
            return instance;
        }

        
        public Fragment getCurrentFragment(Bundle extras) {
            handleFragmentArguments(getCurrentFragmentClass(), extras);
            return getFragment(getCurrentFragmentClass());
        }

        public Class<?> getCurrentFragmentClass() {
            return current;
        }
        
        public void setRoot(Class<?> c) {
            root = c;
        }

        public Class<?> getRoot() {
            return root;
        }
        
        public boolean atRoot() {
            return getBackStackSize() == 0; 
        }


        
        public Fragment showFragment(Class<?> c, Bundle extras, FragmentActivity fa) {
            Fragment f = getFragment(c);
            Fragment prev = getFragment(current);
            
            handleFragmentArguments(c, extras);
            FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();

            if (prev != null && prev.isAdded() && prev.isVisible()) {
                Log.v(this.toString(), "hiding " + prev);
                ft.hide(prev);
            }
            if (f.isAdded()) {
                ft.show(f);
                Log.v(this.toString(), "showed " + f);
            }  else{
                ft.add(R.id.main, f, "f:tag:"+c.getName());
                Log.v(this.toString(), "added " + f);
            }

            ft.commitAllowingStateLoss();
            fa.getSupportFragmentManager().executePendingTransactions();
            current = c;

            return f;
        }

        // Shows the previous fragment
        public Fragment back(FragmentActivity fa) {
            return showFragment(popBackStack(), null, fa);
        }

        public Fragment forward(Class<?> c, Bundle extras, FragmentActivity fa) {
            pushBackStack(current);            
            return showFragment(c, extras, fa);
        }
        
        public void init(FragmentActivity a, Class<?> c) {
            this.root = c;
        }

        public void showCurrentOrRoot(FragmentActivity fa){
            if(current != null)
                showFragment(current, null, fa);
            else
                showFragment(getRoot(), null, fa);
            
        }
        
        private Bundle handleFragmentArguments(Class<?> c, Bundle extras) {
            Bundle oldExtras = getBundle(c);
            
            // overwrite old extras
            if (extras != null) {                              
                setBundle(c, extras);
                return extras;
                
            // return previously set extras
            } else if (extras == null && oldExtras != null) { 
                return oldExtras;
            } else {
                return null;
            }
        }

        public Fragment getFragment(Class<?> c) {
            if(c == null)
                return null;
            
            Object f = fragments.get(c);
            
            if (f == null) {
                try {
                    f = c.newInstance();
                } catch (java.lang.InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                fragments.put(c, (Fragment) f);
            }
                
            return (Fragment) f;

        }

        public void removeAll(FragmentActivity fa) {
            Log.v(this.toString(), "Removing all fragments");
            FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();

            Iterator<Fragment> iterator = fragments.values().iterator();
            while(iterator.hasNext()) {
                Fragment f = iterator.next();
                ft.remove(f);

            }
            
            ft.commitAllowingStateLoss();
            fa.getSupportFragmentManager().executePendingTransactions();
        }
        

        public void setBundle(Class<?> c, Bundle b) {
            store.put(c, b);
        }

        public Bundle getBundle(Class<?> c) {
            return store.get(c);
        }
        
        

        public void pushBackStack(Class<?> c) {
            Log.v(this.toString(), "Back stack before push: " + backStack);
            backStack.addLast(c);
            Log.v(this.toString(), "Back stack after push: " + backStack);
        }


        public Class<?> popBackStack() {
            Log.v(this.toString(), "Back stack before pop: " + backStack);           
            return backStack.removeLast();            
        }

        public Integer getBackStackSize() {
            return backStack.size();
        }


    }

    @Override
    public void onBackPressed() {
        if (FragmentHandler.getInstance().atRoot())
            super.onBackPressed();
        else
            FragmentHandler.getInstance().back(this);
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
        } else if (itemId == R.id.menu_report) {
            if(ServiceProxy.getServiceLocator().getLastKnownLocation() == null)
                App.showLocationNotAvailableToast();
            else            
                ServiceProxy.getServiceLocator().publishLocationMessage();
            return true;
        } else if (itemId == R.id.menu_share) {
            this.share(null);
            return true;
        } else if (itemId == R.id.menu_waypoints) {
            Intent intent1 = new Intent(this, ActivityWaypoints.class);
            startActivity(intent1);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    

    


    public void share(View view) {
        GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
        if (l == null) {
            // TODO: Externalize string
            App.showLocationNotAvailableToast();
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + "," + Double.toString(l.getLongitude()));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.shareLocation)));
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, ServiceProxy.class), serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onStop() {
        
        if(isConnected)
            ServiceProxy.getServiceLocator().enableBackgroundMode();

        if(serviceConnection != null)
            unbindService(serviceConnection);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        FragmentHandler.getInstance().removeAll(this);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }


    Bundle fragmentBundle;

    public static class MapFragment extends Fragment {
        // private static MapFragment instance;
        public static final int ID = 2;
        private GeocodableLocation currentLocation;
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        private ImageView selectedContactImage;
        private Map<String, Contact> markerToContacts;
        
        public static MapFragment getInstance(Bundle extras) {
            MapFragment instance = new MapFragment();
            instance.setArguments(extras);
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
            Iterator<Contact> it = App.getContacts().values().iterator();
            while (it.hasNext()) 
                updateContactLocation(it.next());

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
            Log.v(this.toString(), "onCreateView");
            
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
                    // This shouldn't happen as we check things beforehand.
                    // If it does anyway, the launcher activity will take care
                    // of it by displaying handling dialogues
                    startActivity(new Intent(getActivity(), ActivityLauncher.class));
                    return null;
                }

                setUpMap();
            }

            return v;
        }

        public void onHiddenChanged(boolean hidden) {
            super.onHiddenChanged(hidden);
            if (hidden)
                onHide();
            else
                onShow();
            super.onHiddenChanged(hidden);
        }

        private void onShow() {

        }

        private void onHide() {
        }

        private void setUpMap() {
            googleMap.setIndoorEnabled(true);

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
            selectedContactImage.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    Bundle b = new Bundle();
                    b.putString(DetailsFragment.KEY_TOPIC, c.getTopic());
                    FragmentHandler.getInstance().forward(DetailsFragment.class, b, getActivity());
                }
            });

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
            return ActivityPreferences.getTrackingUsername().equals(Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
        }

    }

    public static class ContactsFragment extends Fragment implements StaticHandlerInterface {
        private LinearLayout friendsListView;
        private Button currentLoc;
        private Button report;

        private static Handler handler;

        // private static FriendsFragment instance;

        public static ContactsFragment getInstance() {
            ContactsFragment instance = new ContactsFragment();
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
            handler.removeCallbacksAndMessages(null);
            EventBus.getDefault().unregister(this);
            super.onStop();
        }

        @Override
        public void onResume() {
            super.onResume();

            for (Contact c : ServiceApplication.getContacts().values())
                updateContactView(c);
        }
        
        public void onHiddenChanged(boolean hidden) {
            if(hidden)
                onHide();
            else
                onShow();
        }        

        private void onShow() {
            
        }

        private void onHide() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.v(this.toString(), "onCreateView");
            
            View v = inflater.inflate(R.layout.fragment_friends, container, false);

            friendsListView = (LinearLayout) v.findViewById(R.id.friendsListView);
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);

            currentLoc = (Button) thisdevice.findViewById(R.id.currentLocation);

            currentLoc.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (ServiceProxy.getServiceLocator().getLastKnownLocation() != null) {
                        Log.v(this.toString(), "Focusing the current location");
                        MapFragment f = (MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, getActivity());
                        f.focusCurrentLocation();
                    } else {
                        App.showLocationNotAvailableToast();
                    }
                }
            });

            report = (Button) thisdevice.findViewById(R.id.report);
            report.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ServiceProxy.getServiceLocator().publishLocationMessage();

                }
            });

            Iterator<Contact> it = App.getContacts().values().iterator();
            while (it.hasNext()) 
                updateContactView(it.next());

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

                    if(l.getTag() == null || friendsListView == null)
                        return;
                    
                    TextView tv = (TextView) friendsListView.findViewWithTag(l.getTag())
                            .findViewById(R.id.subtitle);
                    if (tv != null)
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
                    v = getActivity().getLayoutInflater().inflate(R.layout.row_contact, null,
                            false);
                    c.setView(v);
                    v.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Contact c = ServiceApplication.getContacts().get(v.getTag());
                            if (c == null || c.getLocation() == null)
                                return;
                            
                            ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, getActivity())).focus(c);

                            
                        }
                    });
                    
                    v.findViewById(R.id.image).setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            Bundle b = new Bundle();
                            b.putString(DetailsFragment.KEY_TOPIC, c.getTopic());
                            FragmentHandler.getInstance().forward(DetailsFragment.class, b, getActivity());
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
        public static final String KEY_TOPIC = "TOPIC";
       

        Contact contact;
        TextView name;
        TextView topic;
        TextView location;
        TextView accuracy;
        TextView time;
        Button assignContact;
        private OnSharedPreferenceChangeListener preferencesChangedListener;

        public static DetailsFragment getInstance() {
            DetailsFragment instance = new DetailsFragment();
            return instance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            
            preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                    if (key.equals(Defaults.SETTINGS_KEY_CONTACTS_LINK_CLOUD_STORAGE))
                        showHideAssignContactButton();
                }
            };
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(preferencesChangedListener);

            
        }
        
        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        void showHideAssignContactButton(){
            if(!ActivityPreferences.isContactLinkCloudStorageEnabled())
                assignContact.setVisibility(View.VISIBLE);
            else
                assignContact.setVisibility(View.GONE);
        }
        
        @Override
        public void onDestroy() {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(preferencesChangedListener);

            super.onDestroy();
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
        
        public void onHiddenChanged(boolean hidden) {
            if(hidden)
                onHide();
            else
                onShow();
        }
        

        private void onShow() {
            Bundle extras = FragmentHandler.getInstance().getBundle(this.getClass());
                        
            contact = App.getContacts().get(extras.get(KEY_TOPIC));

            name.setText(contact.getName());
            topic.setText(contact.getTopic());
            location.setText(contact.getLocation().toString());
            accuracy.setText("" + contact.getLocation().getAccuracy());
            time.setText(App.formatDate(contact.getLocation().getDate()));

            assignContact.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(Intent.ACTION_PICK,  Contacts.CONTENT_URI), CONTACT_PICKER_RESULT);  
                }
            });
        }

        private void onHide() {

        }

        
        // Called when contact is picked
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            
            Log.v(this.toString(), "onActivityResult requestCode " + requestCode + " resultCode " + resultCode);
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    if(resultCode == RESULT_OK) {
                        Log.v(this.toString(), "assigning");

                        assignContact(data);
                        Log.v(this.toString(), "assigned");

                    }
                        
                    break;

                default:
                    break;
            }
        }  
        

        private void assignContact(Intent intent){            
            Log.v(this.toString(), "Assign contact to with topic " + contact.getTopic());

            Uri result = intent.getData();  
            String contactId = result.getLastPathSegment();
            Log.v(this.toString(), "Got a result: " + result.toString() + " contactId " +contactId);  
            ServiceProxy.getServiceApplication().linkContact(contact, Long.parseLong(contactId));
          
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.v(this.toString(), "onCreateView");

            View v = inflater.inflate(R.layout.fragment_details, container, false);
            name = (TextView) v.findViewById(R.id.name);
            topic = (TextView) v.findViewById(R.id.topic);
            location = (TextView) v.findViewById(R.id.location);
            accuracy = (TextView) v.findViewById(R.id.accuracy);
            time = (TextView) v.findViewById(R.id.time);
            assignContact = (Button) v.findViewById(R.id.assignContact);
            showHideAssignContactButton();
            
            
            onShow();
            
            
            return v;
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            if (e.getContact() == contact)
                onShow();
        }

        
        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putString(KEY_TOPIC, contact.getTopic());
            FragmentHandler.getInstance().setBundle(getClass(), b);
        }

    }
}
