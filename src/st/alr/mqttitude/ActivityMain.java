
package st.alr.mqttitude;

import java.io.InputStream;
import java.util.Date;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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
        EventBus.getDefault().registerSticky(this);
        if (serviceApplication != null)
            serviceApplication.getServiceLocator().enableForegroundMode();

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        if (serviceApplication != null)
            serviceApplication.getServiceLocator().enableBackgroundMode();

        super.onStop();
    }

    public void onEventMainThread(Events.ContactLocationUpdated e) {
        Log.v(this.toString(), "Contact location updated: " + e.getTopic() + " ->"
                + e.getGeocodableLocation().toString() + " @ "
                + new Date(e.getGeocodableLocation().getLocation().getTime() * 1000));

        Contact c = updateContact(e.getTopic(), e.getGeocodableLocation());
        MapFragment m = (MapFragment) pagerAdapter.getItem(PagerAdapter.MAP_FRAGMENT);
        m.updateContactLocation(c);
    }

    private Contact updateContact(String topic, GeocodableLocation location) {
        Contact c = App.getContactsAdapter().get(topic);

        if (c == null) {
            Log.v(this.toString(), "Allocating new contact for " + topic);
            c = new Contact(topic);
            Log.v(this.toString(), "looking for contact picture");
            findContactData(c);
        }

        c.setLocation(location);
        App.getContactsAdapter().addItem(topic, c);

        return c;
    }

    public void findContactData(Contact c){
        
                
                String imWhere = ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL + " = ? AND " + ContactsContract.CommonDataKinds.Im.DATA + " = ?";
                String[] imWhereParams = new String[] {"Mqttitude", c.getTopic() };
                Cursor imCur = getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
                
                while (imCur.moveToNext()) {
                    Long cId = imCur.getLong(imCur.getColumnIndex(ContactsContract.Data.CONTACT_ID));                    
                    Log.v(this.toString(), "found matching contact with id "+ cId + " to be associated with topic " + imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA)));
                    c.setUserImage(loadContactPhoto(getContentResolver(), cId));
                    c.setName(imCur.getString(imCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));               
                }
                imCur.close();
                Log.v(this.toString(), "search finished");
                
    }
    
    public void parseContacts() {

        Log.v(this.toString(), "Parsing contacts for marker images");
        ContentResolver cr = getContentResolver();

        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur
                        .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                // Log.v(this.toString(), "name: " + name);
//                if (Integer.parseInt(cur.getString(cur
//                        .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    // Query IM details
                    String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?";
                    String[] imWhereParams = new String[] { id, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE };
                    Cursor imCur = cr.query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null);
                    imCur.moveToPosition(-1);

                    while (imCur.moveToNext()) {
                        // if (imCur.moveToFirst()) {
                        String imName = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));
                        String imType = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.TYPE));         
                        String imProtocolType = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));

                         Log.v(this.toString(), "imType: " + imType);
                         Log.v(this.toString(), "imName: " + imName);
                         Log.v(this.toString(), "imProtocolType: " + imProtocolType);

                        // Check IM attributes with type "Custom" and
                        // case-insensitive name "Mqttitude"
                        if (imType.equalsIgnoreCase("3") && imProtocolType != null
                                && imProtocolType.equalsIgnoreCase("MQTTITUDE")) {

                            // create a friend object
                            Contact contact = new Contact(imName);
                            contact.setName(name);
                            contact.setUserImage(loadContactPhoto(getContentResolver(),
                                    Long.parseLong(id)));

                            App.getContactsAdapter().addItem(imName, contact);

                            Log.v(this.toString(), "New contact created from contacts: " + imName
                                    + ", " + name + ", " + imType);
                            EventBus.getDefault().post(new Events.ContactAdded(contact));
                        }
                    }
                    imCur.close();

                }
//            }
        }

        Log.v(this.toString(), "Parsing contacts completed");

    }

    public static Bitmap loadContactPhoto(ContentResolver cr, long id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        Log.v("loadContactPhoto", "using URI " + uri);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    /*
     * We use this to generate markers for each of the different peers/friends
     * we can use a solid colour for each then alter the apha for historical
     * markers
     */
    private BitmapDescriptor createCustomMarker(int colour, float alpha) {

        float[] hsv = new float[3];

        hsv[0] = (colour * 50) % 360; // mod 365 so we get variation
        hsv[1] = 1;
        hsv[2] = alpha;

        Bitmap bm = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas();
        c.setBitmap(bm);

        Paint p = new Paint();
        p.setColor(Color.HSVToColor(hsv));

        c.drawCircle(20, 20, 10, p);
        return BitmapDescriptorFactory.fromBitmap(bm);

    }

    public static class MapFragment extends Fragment {
        private static MapFragment instance;
        private Handler handler;

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
        }
        
//        
//        public void onResume() {
//            super.onResume();
//            for (Contact c : App.getContacts().values())
//                updateContactLocation(c);
//
//            focus(getCurrentlyTrackedContact());
//
//        
//        }


        private MapView mMapView;
        private GoogleMap googleMap;
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, 
                Bundle savedInstanceState) {
            // inflat and return the layout
            View v = inflater.inflate(R.layout.fragment_map, container, false);
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

            
            //Perform any camera updates here
            
            return v;
        }
        
        @Override
        public void onResume() {
            super.onResume();
            mMapView.onResume();
            for (Contact c : App.getContacts().values())
                updateContactLocation(c);

            focus(getCurrentlyTrackedContact());

        }
        
        @Override
        public void onPause() {
            super.onPause();
            mMapView.onPause();
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
            mMapView.onDestroy();
        }
        
        @Override
        public void onLowMemory() {
            super.onLowMemory();
            mMapView.onLowMemory();
        }

        
        
        public Contact getCurrentlyTrackedContact() {
            return App.getContactsAdapter().get(ActivityPreferences.getTrackingUsername());
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

        public void updateContactLocation(Contact c) {

            if (c.getMarker() != null) {
                Log.v(this.toString(), "updating marker position of " + c.getTopic());
                c.updateMarkerPosition();
            } else {
                Log.v(this.toString(), "creating marker for " + c.getTopic());
                c.setMarker(googleMap.addMarker(
                        new MarkerOptions().position(c.getLocation().getLatLng()).icon(
                                c.getUserImageDescriptor())));
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

        public void focus(Contact c) {
            if (c == null)
                return;

            ActivityPreferences.setTrackingUsername(c.getTopic());
            centerMap(c.getLocation().getLatLng());
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
            ((ContactAdapter) friendsListView.getAdapter()).notifyDataSetChanged();
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

            friendsListView = (ListView) v.findViewById(R.id.friendsListView);
            friendsListView.setAdapter(App.getContactsAdapter());
            friendsListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Contact c = (Contact) App.getContactsAdapter().getItem(position); 
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
