
package st.alr.mqttitude;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


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
import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
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
import android.widget.Toast;

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
    private static final int CONTACT_PICKER_RESULT = 1001;  

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        startService(new Intent(this, ServiceProxy.class));
        int fragmentId = ContactsFragment.ID;
        if (savedInstanceState != null)
        {
            // delete previously stored fragments after orientation change (see
            // http://stackoverflow.com/a/13996054/899155).
            // Without this, two map fragments would exists after rotating the
            // device, of which the visible one would not receive updates.
            savedInstanceState.remove("android:support:fragments");
            fragmentId = savedInstanceState.getInt("currentFragment");
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        fragmentHandler = new FragmentHandler(getSupportFragmentManager());
        fragmentHandler.forward(fragmentId, null);
        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
        }

    }

    public static class FragmentHandler {
        private final int COUNT = 3;
        private Fragment[] fragments;
        private int current;
        private FragmentManager fragmentManager;
        private HeadlessFragment store;

        public FragmentHandler(FragmentManager fm) {
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
            //Log.v(this.toString(), "using fragmentManager" + fragmentManager);

            Fragment f = getFragment(id, null);
            Fragment prev = getFragment(current, null);
            FragmentTransaction ft = fragmentManager.beginTransaction();

            handleFragmentArguments(id, extras);

            if (prev != null && prev.isAdded() && prev.isVisible()) {
                ft.hide(prev);
                //Log.v(this.toString(), "hiding fragment");
            }

            if (f.isAdded()) {
                //Log.v(this.toString(), "showing fragment " + f);
                ft.show(f);
            } else {
                //Log.v(this.toString(), "adding fragment " + f);
                ft.add(R.id.main, f, "f:" + id);
            }

            ft.commit();
            fragmentManager.executePendingTransactions();

            current = id;

            return f;
        }

        // Shows the previous fragment
        public Fragment back() {
            HeadlessFragment.getInstance().popBackStack();

            return showFragment(HeadlessFragment.getInstance().getBackStackHead(), null);
        }

        public Fragment forward(Integer id, Bundle extras) {
            if (HeadlessFragment.getInstance().getBackStackHead() != id)
                HeadlessFragment.getInstance().pushBackStack(id);

            return showFragment(id, extras);
        }

        public boolean atRoot() {
            return HeadlessFragment.getInstance().getBackStackSize() == 1;
        }

        private Bundle handleFragmentArguments(int id, Bundle extras) {
            Bundle oldExtras = store.getBundle(id);
            if (extras != null) { // overwrite old extras
                store.setBundle(id, extras);
                return extras;
            } else if (extras == null && oldExtras != null) { // return
                                                              // previously set
                                                              // extras
                return oldExtras;
            } else {
                return null;
            }
        }

        public Fragment getFragment(int id, Bundle extras) {
            Fragment f = fragments[id];

            if (f == null) {
                if (id == ContactsFragment.ID)
                    f = ContactsFragment.getInstance(extras);
                else if (id == MapFragment.ID)
                    f = MapFragment.getInstance(extras);
                else if (id == DetailsFragment.ID)
                    f = DetailsFragment.getInstance(extras);

                fragments[id] = f;
            }

            return f;

        }

        public void removeAll() {
            Log.v(this.toString(), "Removing all fragments");
            FragmentTransaction ft = fragmentManager.beginTransaction();

            for (int i = 0; i < fragments.length; i++)
                if (fragments[i] != null) {
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
        if(!App.isDebugBuild()) {
            MenuItem item = menu.findItem(R.id.menu_waypoints);
            item.setVisible(false);
        }
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
                Toast.makeText(this, "No current location is available", Toast.LENGTH_SHORT).show();
            else            
                ServiceProxy.getServiceLocator().publishLastKnownLocation();
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
            Toast.makeText(this, "No current location to share is available", Toast.LENGTH_SHORT)
                    .show();
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
    public void onDestroy() {
        fragmentHandler.removeAll();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("currentFragment", fragmentHandler.getCurrentFragmentId());
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
        private String selectedContact;
        private Map<String, Contact> markerToContacts;

        public static MapFragment getInstance(Bundle extras) {
            // if (instance == null) {
            MapFragment instance = new MapFragment();
            instance.setArguments(extras);
            // }
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
            Log.v(this.toString(), "onShow");

        }

        private void onHide() {
            Log.v(this.toString(), "onHide");
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
                    b.putString("topic", c.getTopic());
                    fragmentHandler.forward(DetailsFragment.ID, b);
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
            return ActivityPreferences.getTrackingUsername().equals(
                    Defaults.CURRENT_LOCATION_TRACKING_IDENTIFIER);
        }

    }

    public static class ContactsFragment extends Fragment implements StaticHandlerInterface {
        public static final int ID = 1;
        private LinearLayout friendsListView;
        private Button currentLoc;
        private Button report;

        private static Handler handler;

        // private static FriendsFragment instance;

        public static ContactsFragment getInstance(Bundle extras) {
            // if (instance == null) {
            ContactsFragment instance = new ContactsFragment();
            instance.setArguments(extras);
            // }
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
            View v = inflater.inflate(R.layout.fragment_friends, container, false);

            friendsListView = (LinearLayout) v.findViewById(R.id.friendsListView);
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);

            currentLoc = (Button) thisdevice.findViewById(R.id.currentLocation);

            currentLoc.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (ServiceProxy.getServiceLocator().getLastKnownLocation() != null) {
                        Log.v(this.toString(), "Focusing the current location");
                        MapFragment f = (MapFragment) fragmentHandler.forward(MapFragment.ID, null);
                        f.focusCurrentLocation();

                    } else {
                        Toast.makeText(getActivity(), "No current location is available", Toast.LENGTH_SHORT).show();
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

                    if(l.getTag() == null)
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
                            Log.v(this.toString(), "Focusing " + c);
                            if (c == null || c.getLocation() == null) {
                                Log.v(this.toString(), "No contact or no location ");

                                return;

                            }
                            Log.v(this.toString(), "request for fragmenthandler fragment handler"
                                    + fragmentHandler);
                            fragmentHandler.forward(MapFragment.ID, null);
                            ((MapFragment) fragmentHandler.getCurrentFragment(null)).focus(c);

                            
                        }
                    });
                    
                    v.findViewById(R.id.image).setOnClickListener(new OnClickListener() {
                        
                        @Override
                        public void onClick(View v) {
                            Bundle b = new Bundle();
                            b.putString("topic", c.getTopic());
                            fragmentHandler.forward(DetailsFragment.ID, b);
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
        public static final int ID = 3;
        Contact contact;
        TextView name;
        TextView topic;
        TextView location;
        TextView accuracy;
        TextView time;
        Button assignContact;
        private OnSharedPreferenceChangeListener preferencesChangedListener;

        public static DetailsFragment getInstance(Bundle extras) {
            DetailsFragment instance = new DetailsFragment();
            instance.setArguments(extras);
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
            Log.v(this.toString(), "subscribed");
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
            Log.v(this.toString(), "unsubscribed");
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
            Bundle extras = HeadlessFragment.getInstance().getBundle(DetailsFragment.ID);
            contact = App.getContacts().get(extras.get("topic"));
            Log.v(this.toString(), "show for " + contact.getName());

            name.setText(contact.getName());
            topic.setText(contact.getTopic());
            location.setText(contact.getLocation().toString());
            accuracy.setText("" + contact.getLocation().getAccuracy());
            time.setText(App.formatDate(contact.getLocation().getDate()));

            assignContact.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,  
                            Contacts.CONTENT_URI);  
                    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);  

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
            Log.v(this.toString(), "contactUpdated");
            if (e.getContact() == contact)
                onShow();
        }

        
        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putString("topic", contact.getTopic());
            HeadlessFragment.getInstance().setBundle(DetailsFragment.ID, b);
        }

    }

    public static class HeadlessFragment extends Fragment {
        static HeadlessFragment instance;
        private static HashMap<Integer, Bundle> store = new HashMap<Integer, Bundle>();
        private static LinkedList<Integer> backStack = new LinkedList<Integer>();

        public static HeadlessFragment getInstance() {
            if (instance == null) {
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
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

        public Integer getBackStackSize() {
            return backStack.size();
        }

    }
}
