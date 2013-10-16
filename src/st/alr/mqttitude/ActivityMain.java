package st.alr.mqttitude;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceApplication;
import st.alr.mqttitude.services.ServiceBindable;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Contact;
import st.alr.mqttitude.support.ContactAdapter;
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
import android.database.DataSetObserver;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
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

    private static Map<String,Contact> contacts = new HashMap<String,Contact>();
    static ContactAdapter contactsAdapter;

    
    
    
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
        
        try {
            MapsInitializer.initialize(this);
        } catch (GooglePlayServicesNotAvailableException e) {
        }

        contactsAdapter = new ContactAdapter(this, contacts);
        parseContacts();        
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
            if(serviceApplication.getServiceLocator() != null)
                serviceApplication.getServiceLocator().publishLastKnownLocation();
            return true;
        } else if (itemId == R.id.menu_share) {
            if(serviceApplication.getServiceLocator() != null)
                this.share(null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    public void share(View view) {
        GeocodableLocation l = serviceApplication.getServiceLocator().getLastKnownLocation();
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
        if(serviceApplication != null)
            serviceApplication.getServiceLocator().enableForegroundMode();

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        
        if(serviceApplication != null)
            serviceApplication.getServiceLocator().enableBackgroundMode();

        super.onStop();
    }

    
    public void onEventMainThread(Events.ContactLocationUpdated e) {
        Log.v(this.toString(), "Contact location updated: " + e.getTopic() + " ->" + e.getGeocodableLocation().toString() + " @ " + new Date(e.getGeocodableLocation().getLocation().getTime() * 1000));

        Contact f = contacts.get(e.getTopic());
        
        if(f == null) {
            f = new Contact();
            f.setTopic(e.getTopic());   
            f.setLocation(e.getGeocodableLocation());

        }
        
        contactsAdapter.addItem(e.getTopic(), f); // automatically fires notifyDatasetChanged to reload with new contact or update position
    }

    
    public void parseContacts(){
        ContentResolver cr = getContentResolver();

        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//                Log.v(this.toString(), "name: " + name);
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    //Query IM details
                    String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
                    String[] imWhereParams = new String[]{id,  ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}; 
                    Cursor imCur = cr.query(ContactsContract.Data.CONTENT_URI, null, imWhere, imWhereParams, null); 
                    imCur.moveToPosition(-1);
                    
                    while(imCur.moveToNext()) {
                    //if (imCur.moveToFirst()) { 
                        String imName = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));
                        String imType;
                        imType = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.TYPE));
                        
                        String label = imCur.getString(imCur.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));
                        
//                        Log.v(this.toString(), "imType: " + imType);
//                        Log.v(this.toString(), "imName: " + imName);
//                        Log.v(this.toString(), "label: " + label);

                        // Check IM attributes with type "Custom" and case-insensitive name "Mqttitude" 
                        if(imType.equalsIgnoreCase("3") &&label != null && label.equalsIgnoreCase("MQTTITUDE")){

                                //create a friend object
                                Contact contact = new Contact();
                                contact.setTopic(imName);
                                contact.setName(name);
                                contact.setUserImage(loadContactPhoto(getContentResolver(), Long.parseLong(id)));
                                
                                contacts.put(imName, contact);
                                
                                Log.v(this.toString(), "New contact created from contacts: " + imName + ", " + name + ", " + imType);
                                EventBus.getDefault().post(new Events.ContactAdded(contact));
                        }
                    } 
                    imCur.close();

                }
            }
        }   
    
    }
    
    public static Bitmap loadContactPhoto(ContentResolver cr, long  id) {
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
        if (input == null) {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }


    
    
    /*
     * We use this to generate markers for each of the different peers/friends
     * we can use a solid colour for each then alter the apha for historical markers
     */
    private BitmapDescriptor createCustomMarker(int colour, float alpha){
        
        float[] hsv = new float[3]; 
        
        hsv[0] = (colour * 50) % 360; //mod 365 so we get variation
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
        
        public void onEventMainThread(Events.ContactLocationUpdated e) {
            Contact f = contacts.get(e.getTopic());
            
            
            
            if(f == null) {
                f = new Contact();
                f.setTopic(e.getTopic());   
                f.setLocation(e.getGeocodableLocation());
                
                //TODO: refresh adapter of list
            } else {
                f.setLocation(e.getGeocodableLocation());
                //TODO: refresh adapter of list
            }

            LatLng ln = new LatLng(e.getGeocodableLocation().getLatitude(), e.getGeocodableLocation().getLongitude());
            Marker m = getMap().addMarker(new MarkerOptions().position(ln).icon(f.getUserImageDescriptor()).title(f.getName()).flat(true));
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

            
         //   mMarker = getMap().addMarker(new MarkerOptions().position(latlong).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            
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
        
        

        
      @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
              Bundle savedInstanceState) {
          View v = super.onCreateView(inflater, container, savedInstanceState);
          
          getMap().setIndoorEnabled(true);
          //getMap().setMyLocationEnabled(true);
          
          return v;
      }
//  
  }
    
    
    
    public static class FriendsFragment extends Fragment {
        private TextView locatorCurLatLon;
        private TextView locatorCurAccuracy;
        private TextView locatorCurLatLonTime;

        private TextView locatorLastPubLatLon;
        private TextView locatorLastPubAccuracy;
        private TextView locatorLastPubLatLonTime;
        ListView friendsListView;
        
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
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_friends, container, false);

            friendsListView = (ListView) v.findViewById(R.id.friendsListView);
            friendsListView.setAdapter(contactsAdapter);
            
            locatorCurLatLon = (TextView) v.findViewById(R.id.locatorCurLatLon);
            locatorCurAccuracy = (TextView) v.findViewById(R.id.locatorCurAccuracy);
            locatorCurLatLonTime = (TextView) v.findViewById(R.id.locatorCurLatLonTime);

            locatorLastPubLatLon = (TextView) v.findViewById(R.id.locatorLastPubLatLon);
            locatorLastPubAccuracy = (TextView) v.findViewById(R.id.locatorLastPubAccuracy);
            locatorLastPubLatLonTime = (TextView) v.findViewById(R.id.locatorLastPubLatLonTime);
            
            return v;
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
