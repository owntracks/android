
package st.alr.mqttitude;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.services.ServiceProxy.ServiceProxyConnection;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;
import android.content.ComponentName;
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
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                Log.v(this.toString(), "ServiceProxy bound");
            }
        });

        // delete previously stored fragments after orientation change
        if (savedInstanceState != null)
            savedInstanceState.remove("android:support:fragments");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        FragmentHandler.getInstance().init(this, ContactsFragment.class);
        FragmentHandler.getInstance().showCurrentOrRoot(this);

        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
        }

    }

    public static class FragmentHandler extends Fragment {
        private Class<?> current;
        private Class<?> root;

        static FragmentHandler instance;

        private static HashMap<Class<?>, Bundle> store = new HashMap<Class<?>, Bundle>();
        private static ConcurrentHashMap<Class<?>, Fragment> fragments = new ConcurrentHashMap<Class<?>, Fragment>();

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
            return this.current;
        }

        public void setRoot(Class<?> c) {
            this.root = c;
        }

        public Class<?> getRoot() {
            return this.root;
        }

        public boolean atRoot() {
            return getBackStackSize() == 0;
        }

        public Fragment showFragment(Class<?> c, Bundle extras, FragmentActivity fa) {
            Fragment f = getFragment(c);
            Fragment prev = getFragment(this.current);

            handleFragmentArguments(c, extras);
            FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();

            if ((prev != null) && prev.isAdded() && prev.isVisible())
                ft.hide(prev);

            if (f.isAdded())
                ft.show(f);
            else
                ft.add(R.id.main, f, "f:tag:" + c.getName());

            ft.commitAllowingStateLoss();
            fa.getSupportFragmentManager().executePendingTransactions();
            this.current = c;

            return f;
        }

        // Shows the previous fragment
        public Fragment back(FragmentActivity fa) {
            return showFragment(popBackStack(), null, fa);
        }

        public Fragment forward(Class<?> c, Bundle extras, FragmentActivity fa) {
            pushBackStack(this.current);
            return showFragment(c, extras, fa);
        }

        public void init(FragmentActivity a, Class<?> c) {
            this.root = c;
        }

        public void showCurrentOrRoot(FragmentActivity fa) {
            if (this.current != null)
                showFragment(this.current, null, fa);
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
            } else if ((extras == null) && (oldExtras != null)) {
                return oldExtras;
            } else {
                return null;
            }
        }

        public Fragment getFragment(Class<?> c) {
            if (c == null)
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
            FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();

            for (Fragment f : fragments.values())
                ft.remove(f);

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
            backStack.addLast(c);
        }

        public Class<?> popBackStack() {
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
            ServiceProxy.runOrBind(this, new Runnable() {

                @Override
                public void run() {
                    if (ServiceProxy.getServiceLocator().getLastKnownLocation() == null)
                        App.showLocationNotAvailableToast();
                    else
                        ServiceProxy.getServiceLocator().publishLocationMessage();
                }
            });

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
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                if (l == null) {
                    App.showLocationNotAvailableToast();
                    return;
                }

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + Double.toString(l.getLatitude()) + "," + Double.toString(l.getLongitude()));
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.shareLocation)));
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        // bindService(new Intent(this, ServiceProxy.class), serviceConnection,
        // Context.BIND_AUTO_CREATE);
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableForegroundMode();
            }
        });
    }

    @Override
    public void onStop() {
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableBackgroundMode();
            }
        });

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        FragmentHandler.getInstance().removeAll(this);
        ServiceProxy.runOrBind(this, new Runnable() {
            
            @Override
            public void run() {
                ServiceProxy.closeServiceConnection();
                
            }
        });
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    Bundle fragmentBundle;

    public static class MapFragment extends Fragment {
        private GeocodableLocation currentLocation;
        private MapView mMapView;
        private GoogleMap googleMap;
        private LinearLayout selectedContactDetails;
        private TextView selectedContactName;
        private TextView selectedContactLocation;
        private ImageView selectedContactImage;
        private Map<String, Contact> markerToContacts;
        private static final String KEY_TRACKING_CURRENT_DEVICE = "+CURRENTDEVICELOCATION+";

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
            this.mMapView.onResume();

            for (Contact c : App.getContacts().values())
                updateContactLocation(c);

            focusCurrentlyTrackedContact();
        }

        @Override
        public void onPause() {
            this.mMapView.onPause();
            super.onPause();
        }

        @Override
        public void onDestroy() {
            this.mMapView.onDestroy();
            super.onDestroy();
        }

        @Override
        public void onLowMemory() {
            this.mMapView.onLowMemory();
            super.onLowMemory();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.v(this.toString(), "onCreateView");

            View v = inflater.inflate(R.layout.fragment_map, container, false);
            this.markerToContacts = new HashMap<String, Contact>();
            this.selectedContactDetails = (LinearLayout) v.findViewById(R.id.contactDetails);
            this.selectedContactName = (TextView) v.findViewById(R.id.title);
            this.selectedContactLocation = (TextView) v.findViewById(R.id.subtitle);
            this.selectedContactImage = (ImageView) v.findViewById(R.id.image);

            this.selectedContactDetails.setVisibility(View.GONE);

            this.mMapView = (MapView) v.findViewById(R.id.mapView);
            this.mMapView.onCreate(savedInstanceState);
            this.mMapView.onResume(); // needed to get the map to display
                                      // immediately
            this.googleMap = this.mMapView.getMap();

            // Check if we were successful in obtaining the map.
            if (this.mMapView != null) {
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

        @Override
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
            this.googleMap.setIndoorEnabled(true);

            UiSettings s = this.googleMap.getUiSettings();
            s.setCompassEnabled(false);
            s.setMyLocationButtonEnabled(false);
            s.setTiltGesturesEnabled(false);
            s.setCompassEnabled(false);
            s.setRotateGesturesEnabled(false);
            s.setZoomControlsEnabled(false);

            this.mMapView.getMap().setOnMarkerClickListener(new OnMarkerClickListener() {

                @Override
                public boolean onMarkerClick(Marker m) {
                    Contact c = MapFragment.this.markerToContacts.get(m.getId());

                    if (c != null)
                        focus(c);

                    return false;
                }
            });

            this.mMapView.getMap().setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng arg0) {
                    MapFragment.this.selectedContactDetails.setVisibility(View.GONE);

                }
            });
        }

        public void centerMap(LatLng l) {
            centerMap(l, 15.0f);
        }

        public void centerMap(LatLng latlon, float f) {
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latlon, f);
            this.mMapView.getMap().animateCamera(center);
        }

        public void updateContactLocation(Contact c) {
            if (c.getMarker() != null) {
                c.getMarker().remove();
            }

            Marker m = this.googleMap.addMarker(new MarkerOptions()
                    .position(c.getLocation().getLatLng()).icon(c.getMarkerImageDescriptor()));
            this.markerToContacts.put(m.getId(), c);
            c.setMarker(m);

            if (c == getCurrentlyTrackedContact())
                focus(c);
        }

        public void focusCurrentLocation() {
            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                    if (l == null)
                        return;
                    selectedContactDetails.setVisibility(View.GONE);
                    Preferences.setTrackingUsername(KEY_TRACKING_CURRENT_DEVICE);
                    centerMap(l.getLatLng());
                }
            });

        }

        public void focusCurrentlyTrackedContact() {
            Contact c = getCurrentlyTrackedContact();

            if (c != null)
                focus(getCurrentlyTrackedContact());
            else if (isTrackingCurrentLocation())
                focusCurrentLocation();
        }

        public void focus(final Contact c) {

            if (c == null) {
                Log.v(this.toString(), "no contact, abandon ship!");
                return;
            }

            Preferences.setTrackingUsername(c.getTopic());
            centerMap(c.getLocation().getLatLng());

            this.selectedContactName.setText(c.toString());
            this.selectedContactLocation.setText(c.getLocation().toString());

            this.selectedContactImage.setImageBitmap(c.getUserImage());

            this.selectedContactImage.setTag(c.getTopic());
            this.selectedContactImage.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Bundle b = new Bundle();
                    b.putString(DetailsFragment.KEY_TOPIC, c.getTopic());
                    FragmentHandler.getInstance().forward(DetailsFragment.class, b, getActivity());
                }
            });

            this.selectedContactDetails.setVisibility(View.VISIBLE);
            this.selectedContactDetails.setOnClickListener(new OnClickListener() {
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
            if (isTrackingCurrentLocation())
                focusCurrentLocation();
        }

        public Contact getCurrentlyTrackedContact() {
            return ServiceApplication.getContacts().get(Preferences.getTrackingUsername());
        }

        public boolean hasCurrentLocation() {
            return this.currentLocation != null;
        }

        public boolean isTrackingCurrentLocation() {
            return Preferences.getTrackingUsername().equals(KEY_TRACKING_CURRENT_DEVICE);
        }

    }

    public static class ContactsFragment extends Fragment implements StaticHandlerInterface {
        private LinearLayout friendsListView;
        private Button currentLoc;
        private static Handler handler;
        private static final String TAG_MYLOCATION = "++MYLOCATION++";

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

            for (Contact c : App.getContacts().values())
                updateContactView(c);

            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    updateCurrentLocation(ServiceProxy.getServiceLocator().getLastKnownLocation(), true);

                }
            });

        }

        @Override
        public void onHiddenChanged(boolean hidden) {
            if (hidden)
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

            this.friendsListView = (LinearLayout) v.findViewById(R.id.friendsListView);
            LinearLayout thisdevice = (LinearLayout) v.findViewById(R.id.thisdevice);

            this.currentLoc = (Button) thisdevice.findViewById(R.id.currentLocation);

            this.currentLoc.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ServiceProxy.runOrBind(getActivity(), new Runnable() {
                        public void run() {
                            if (ServiceProxy.getServiceLocator().getLastKnownLocation() != null)
                                ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, getActivity())).focusCurrentLocation();
                            else
                                App.showLocationNotAvailableToast();

                        }
                    });

                }
            });

            for (Contact c : App.getContacts().values())
                updateContactView(c);

            return v;
        }

        public void onEventMainThread(Events.LocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
        }

        public void updateCurrentLocation(GeocodableLocation l, boolean resolveGeocoder) {
            if (l == null)
                return;

            // Current location changes often, don't waste resources to resolve
            // the geocoder
            this.currentLoc.setText(l.toString());

            if ((l.getGeocoder() == null) && resolveGeocoder) {
                l.setTag(TAG_MYLOCATION);
                (new ReverseGeocodingTask(getActivity(), handler))
                        .execute(new GeocodableLocation[] {
                                l
                        });
            }
        }

        @Override
        public void handleHandlerMessage(Message msg) {

            if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && (msg.obj != null)) {
                if (((GeocodableLocation) msg.obj).getTag().equals(TAG_MYLOCATION)) {
                    updateCurrentLocation((GeocodableLocation) msg.obj, false);
                } else {
                    GeocodableLocation l = (GeocodableLocation) msg.obj;

                    if ((l.getTag() == null) || (this.friendsListView == null))
                        return;

                    TextView tv = (TextView) this.friendsListView.findViewWithTag(l.getTag()).findViewById(R.id.subtitle);
                    if (tv != null)
                        tv.setText(l.toString());
                }
            }
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            updateContactView(e.getContact());
        }

        public void updateContactView(final Contact c) {
            View v = this.friendsListView.findViewWithTag(c.getTopic());

            if (v == null) {

                if (c.getView() != null) {
                    v = c.getView();
                    // remove from old view first to allow it to be added to the
                    // new view again
                    ((ViewGroup) v.getParent()).removeView(v);
                } else {
                    v = getActivity().getLayoutInflater().inflate(R.layout.row_contact, null, false);
                    c.setView(v);
                    v.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Contact c = ServiceApplication.getContacts().get(v.getTag());
                            if ((c == null) || (c.getLocation() == null))
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
                this.friendsListView.addView(c.getView());
                this.friendsListView.setVisibility(View.VISIBLE);

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

        private Contact contact;
        private TextView name;
        private TextView topic;
        private TextView location;
        private TextView accuracy;
        private TextView time;
        private Button assignContact;
        private OnSharedPreferenceChangeListener preferencesChangedListener;

        public static DetailsFragment getInstance() {
            DetailsFragment instance = new DetailsFragment();
            return instance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreference, String key) {
                    if (key.equals(Preferences.getKey(R.string.keyContactsLinkCloudStorageEnabled)))
                        showHideAssignContactButton();
                }
            };
            PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(this.preferencesChangedListener);

        }

        @Override
        public void onStart() {
            super.onStart();
            EventBus.getDefault().registerSticky(this);
        }

        void showHideAssignContactButton() {
            if (!Preferences.isContactLinkCloudStorageEnabled())
                this.assignContact.setVisibility(View.VISIBLE);
            else
                this.assignContact.setVisibility(View.GONE);
        }

        @Override
        public void onDestroy() {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this.preferencesChangedListener);

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

        @Override
        public void onHiddenChanged(boolean hidden) {
            if (hidden)
                onHide();
            else
                onShow();
        }

        private void onShow() {
            Bundle extras = FragmentHandler.getInstance().getBundle(this.getClass());

            this.contact = App.getContacts().get(extras.get(KEY_TOPIC));

            this.name.setText(this.contact.getName());
            this.topic.setText(this.contact.getTopic());
            this.location.setText(this.contact.getLocation().toString());
            this.accuracy.setText("" + this.contact.getLocation().getAccuracy());
            this.time.setText(App.formatDate(this.contact.getLocation().getDate()));

            this.assignContact.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    startActivityForResult(new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), CONTACT_PICKER_RESULT);
                }
            });
        }

        private void onHide() {

        }

        // Called when contact is picked
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if ((requestCode == CONTACT_PICKER_RESULT) && (resultCode == RESULT_OK))
                assignContact(data);
        }

        private void assignContact(Intent intent) {
            Uri result = intent.getData();
            final String contactId = result.getLastPathSegment();

            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    ServiceProxy.getServiceApplication().linkContact(contact, Long.parseLong(contactId));

                }
            });

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.fragment_details, container, false);
            this.name = (TextView) v.findViewById(R.id.name);
            this.topic = (TextView) v.findViewById(R.id.topic);
            this.location = (TextView) v.findViewById(R.id.location);
            this.accuracy = (TextView) v.findViewById(R.id.accuracy);
            this.time = (TextView) v.findViewById(R.id.time);
            this.assignContact = (Button) v.findViewById(R.id.assignContact);
            showHideAssignContactButton();
            onShow();
            return v;
        }

        public void onEventMainThread(Events.ContactUpdated e) {
            if (e.getContact() == this.contact)
                onShow();
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putString(KEY_TOPIC, this.contact.getTopic());
            FragmentHandler.getInstance().setBundle(getClass(), b);
        }
    }
}
