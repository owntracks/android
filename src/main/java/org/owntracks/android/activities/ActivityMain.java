package org.owntracks.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.adapter.ContactAdapter;
import org.owntracks.android.messages.ClearMessage;
import org.owntracks.android.messages.CommandMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.SnackbarFactory;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;

import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import de.greenrobot.event.EventBus;

public class ActivityMain extends ActivityBase {
    private static final String TAG = "ActivityMain";

    private static final int CONTACT_PICKER_RESULT = 1001;
    private static final int MENU_CONTACT_SHOW = 0;
    private static final int MENU_CONTACT_DETAILS = 1;
    private static final int MENU_CONTACT_NAVIGATE = 2;
    private static final int MENU_CONTACT_FOLLOW = 3;
    private static final int MENU_CONTACT_UNFOLLOW = 4;
    private static final int MENU_CONTACT_REQUEST_REPORT_LOCATION = 5;

    private static Drawer.OnDrawerItemClickListener drawerClickListener;
    private static Drawer.OnDrawerNavigationListener drawerNavigationListener;

    private Toolbar toolbar;
    private Drawer drawer;
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
		ServiceProxy.runOrBind(this, new Runnable() {
			@Override
			public void run() {
				Log.v("ActivityMain", "ServiceProxy bound");
			}
		});

		// delete previously stored fragments after orientation change
		if (savedInstanceState != null) {
            savedInstanceState.remove("android:support:fragments");
        }

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
        toolbar = (Toolbar)findViewById(R.id.fragmentToolbar);
        setSupportActionBar(toolbar);




        final ActivityMain context = this;
        drawerClickListener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                Log.v(TAG, "" +drawerItem.getIdentifier());

                switch (drawerItem.getIdentifier()) {
                    case R.string.idLocations:
                        if (!ActivityMain.FragmentHandler.getInstance().atRoot()) {
                            // We're showing the root and rolling back the complete back stack so we discard it
                            FragmentHandler.getInstance().clearBackStack();
                            FragmentHandler.getInstance().showFragment(FragmentHandler.getInstance().getRoot(), null, (AppCompatActivity) context, FragmentHandler.DIRECTION_BACK);
                        }
                        return true;
                    case R.string.idPager:
                        Intent intent3 = new Intent(context, ActivityMessages.class);
                        startActivity(intent3);
                        return true;
                    case R.string.idWaypoints:
                        Intent intent1 = new Intent(context, ActivityWaypoints.class);
                        startActivity(intent1);
                        return true;
                    case R.string.idStatistics:
                        Intent intent2 = new Intent(context, ActivityStatistics.class);
                        startActivity(intent2);
                        return true;

                    case R.string.idSettings:
                        Intent intent4 = new Intent(context, ActivityPreferences.class);
                        startActivity(intent4);
                        return true;

                }
                return false;
            }
        };
        drawerNavigationListener = new Drawer.OnDrawerNavigationListener() {
            @Override
            public boolean onNavigationClickListener(View view) {
                if(!FragmentHandler.getInstance().atRoot()) {
                    FragmentHandler.getInstance().back(context);
                    return true;
                }
                return false;
            }
        };

        drawer = DrawerFactory.buildDrawer(this, toolbar, drawerClickListener, drawerNavigationListener, 0);


        FragmentHandler.getInstance().init(ContactsFragment.class, drawer);
        Log.v(TAG, "Fragment show current or root");
		FragmentHandler.getInstance().showCurrentOrRoot(this);

	}
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
       // toggle.syncState();
    }




	public static class FragmentHandler extends Fragment {
		private Class<?> current;
		private Class<?> root;
        private Drawer drawer;
        public static final int DIRECTION_NONE = 0;
        public static final int DIRECTION_FORWARD = 1;
        public static final int DIRECTION_BACK = 2;

        static FragmentHandler instance;

		private static HashMap<Class<?>, Bundle> store = new HashMap<>();
		private static ConcurrentHashMap<Class<?>, Fragment> fragments = new ConcurrentHashMap<>();

		private static LinkedList<Class<?>> backStack = new LinkedList<>();

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(false);
		}

		public static FragmentHandler getInstance() {
			if (instance == null)
				instance = new FragmentHandler();
			return instance;
		}

        public Class<?> getCurrentFragmentClass() {
            return current;
        }

		public Class<?> getRoot() {
			return this.root;
		}

		public boolean atRoot() {
			return getBackStackSize() == 0;
		}

		public Fragment showFragment(Class<?> c, Bundle extras,
                                     AppCompatActivity fa, int direction) {
			Fragment f = getFragment(c);
			Fragment prev = getFragment(this.current);


			handleFragmentArguments(c, extras);
			FragmentTransaction ft = fa.getSupportFragmentManager().beginTransaction();

            if(direction == DIRECTION_FORWARD)
                ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out  );
            else if(direction == DIRECTION_BACK)
                ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);

            if ((prev != null) && prev.isAdded() && prev.isVisible())
				ft.hide(prev);

			if (f.isAdded())
				ft.show(f);
			else
				ft.add(R.id.main, f, "f:tag:" + c.getName());



			ft.commitAllowingStateLoss();
			fa.getSupportFragmentManager().executePendingTransactions();

            // Disable drawer indicator if we're not showing the root fragment
            // Instead, this shows the back arrow and calls the drawer navigation listener where we can handle back
            // or show the drawer manually
            if(drawer != null && drawer.getActionBarDrawerToggle() != null)
                drawer.getActionBarDrawerToggle().setDrawerIndicatorEnabled(c == getRoot());

            this.current = c;

			return f;
		}

		// Shows the previous fragment
		public Fragment back(AppCompatActivity fa) {
			return showFragment(popBackStack(), null, fa, DIRECTION_BACK);
		}

		public Fragment forward(Class<?> c, Bundle extras, AppCompatActivity fa) {
			pushBackStack(this.current);
			return showFragment(c, extras, fa, DIRECTION_FORWARD);
		}

		public void init(Class<?> c, Drawer drawer) {
			this.root = c;
            this.drawer = drawer;
		}

		public void showCurrentOrRoot(AppCompatActivity fa) {
			if (this.current != null)
				showFragment(this.current, null, fa, DIRECTION_NONE);
			else
				showFragment(getRoot(), null, fa, DIRECTION_NONE);

		}

		private Bundle handleFragmentArguments(Class<?> c, Bundle extras) {
			Bundle oldExtras = getBundle(c);

			// overwrite old extras
			if (extras != null) {
				setBundle(c, extras);
				return extras;

				// return previously set extras
			} else if (oldExtras != null) {
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

		public void removeAll(AppCompatActivity fa) {
			if(fa == null)
				return;

			FragmentTransaction ft = fa.getSupportFragmentManager()
					.beginTransaction();

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
            if(backStack != null && backStack.size() > 0 && backStack.getLast() == c)
                return;

            backStack.addLast(c);
		}
        public void clearBackStack() {
            backStack.clear();
            return;
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


    private void launchNavigation(Contact c) {
        if(c.getLocation() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + c.getLocation().getLatitude() + "," + c.getLocation().getLongitude()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(
                    App.getContext(),
                    App.getContext()
                            .getString(R.string.contactLocationUnknown),
                    Toast.LENGTH_SHORT
            ).show();

        }
    }

    private void transitionToContactDetails(Contact c) {
        Bundle b = new Bundle();
        b.putString(DetailsFragment.KEY_TOPIC, c.getTopic());
        FragmentHandler.getInstance().forward(DetailsFragment.class, b, this);
    }

    private void transitionToCurrentLocationMap() {
        final AppCompatActivity that = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, that)).selectCurrentLocation(MapFragment.SELECT_CENTER_AND_ZOOM, true, false);
            }
        });
    }

    private void transitionToContactMap(final Contact c) {
        final AppCompatActivity that = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, that)).selectContact(c, MapFragment.SELECT_CENTER_AND_ZOOM, true, false);
            }
        });
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_report) {
            Log.v(TAG, "report");
            ServiceProxy.runOrBind(this, new Runnable() {

                @Override
                public void run() {
                    if (ServiceProxy.getServiceLocator().getLastKnownLocation() == null)
                        App.showLocationNotAvailableToast();
                    else
                        ServiceProxy.getServiceLocator()
                                .publishManualLocationMessage();
                }
            });

            return true;
        } else if( itemId == R.id.menu_mylocation) {
            ServiceProxy.runOrBind(this, new Runnable() {

                @Override
                public void run() {
                    if(ServiceProxy.getServiceLocator().getLastKnownLocation() != null) {
                        transitionToCurrentLocationMap();
                    } else {
                        App.showLocationNotAvailableToast();
                    }
                }
            });
            return true;

        } else if( itemId == android.R.id.home) {
            FragmentHandler.getInstance().back(this);
            return true;
        } else {
			return false;
		}
	}


    public void share(View view) {
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                GeocodableLocation l = ServiceProxy.getServiceLocator()
                        .getLastKnownLocation();
                if (l == null) {
                    App.showLocationNotAvailableToast();
                    return;
                }

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "http://maps.google.com/?q="
                                + Double.toString(l.getLatitude()) + ","
                                + Double.toString(l.getLongitude())
                );
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent,
                        getString(R.string.shareLocation)));
            }
        });

	}



	@Override
	public void onStart() {
		super.onStart();
        Log.v("ActivityMain", "app onStartCalled called");

        // bindService(new Intent(this, ServiceProxy.class), serviceConnection,
		// Context.BIND_AUTO_CREATE);
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                Log.v("ActivityMain", "runOrBind onStart");

               // ServiceProxy.getServiceLocator().enableForegroundMode();
                //ServiceProxy.getServiceBeacon().setBackgroundMode(false);
            }
        });
	}

	@Override
	public void onStop() {
        Log.v("ActivityMain", "app onStop called");
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                Log.v("ActivityMain", "runOrBind onStop");

                //  ServiceProxy.getServiceLocator().enableBackgroundMode();
                //ServiceProxy.getServiceBeacon().setBackgroundMode(true);
            }
        });

		super.onStop();
	}



	@Override
	public void onDestroy() {
		FragmentHandler.getInstance().removeAll(this);
		ServiceProxy.closeServiceConnection();
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}


	public static class  MapFragment extends Fragment implements StaticHandlerInterface {
        private static final String KEY_CURRENT_LOCATION = "+CURRENTLOCATION+";
        private static final String KEY_NOTOPIC =          "+NOTOPIC+";
        private static final String KEY_POSITION =         "+POSITION+";
        private static final String KEY_ZOOM =              "+ZOOM+";
        private static final int SELECT_UPDATE = 0;
        private static final int SELECT_CENTER = 1;
        private static final int SELECT_CENTER_AND_ZOOM = 2;


        private MapView mMapView;
		private GoogleMap googleMap;
		private LinearLayout selectedContactDetails;
		private TextView selectedContactName;
		private TextView selectedContactLocation;
		private ImageView selectedContactImage;
		private Map<String, Contact> markerToContacts;
        private Menu mMenu;
        private static Handler handler;
        private MenuInflater mInflater;
        private BitmapDescriptor currentLocationMarkerBitmap;
        private Marker currentLocationMarker;
        private Circle currentLocationPrecision;

        public static MapFragment getInstance(Bundle extras) {
			MapFragment instance = new MapFragment();
			instance.setArguments(extras);
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
			this.mMapView.onResume();


            HashMap<String, Contact> contacts = new HashMap<String, Contact>(App.getCachedContacts());
            for(Contact c : contacts.values())
				updateContactLocation(c);

            Contact c = getSelectedContact();

            Bundle extras = FragmentHandler.getInstance().getBundle(MapFragment.class);
            CameraPosition position = null;
            float zoom = -1;

            if(extras != null) {
                position = extras.getParcelable(KEY_POSITION);
                zoom = position.zoom;

            }

            if (c != null) {
                selectContact(c, SELECT_UPDATE, true, false, zoom);
                if(position != null)
                    centerMap(position.target, SELECT_CENTER, false, zoom);

            } else if (isFollowingCurrentLocation())
                selectCurrentLocation(SELECT_CENTER, true, false, zoom);
            else if(position != null)
                centerMap(position.target, SELECT_CENTER, false, zoom);
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
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

			View v = inflater.inflate(R.layout.fragment_map, container, false);



            this.markerToContacts = new HashMap<String, Contact>();
			this.selectedContactDetails = (LinearLayout) v.findViewById(R.id.contactDetails);
            registerForContextMenu(this.selectedContactDetails);

            this.selectedContactName = (TextView) v.findViewById(R.id.name);
			this.selectedContactLocation = (TextView) v.findViewById(R.id.location);
			this.selectedContactImage = (ImageView) v.findViewById(R.id.image);


            Drawable markerDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.current_location_marker);
            Bitmap bitmap = Bitmap.createBitmap(markerDrawable.getIntrinsicWidth(), markerDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            markerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            markerDrawable.draw(canvas);

            MapsInitializer.initialize(App.getContext());

            this.currentLocationMarkerBitmap = BitmapDescriptorFactory.fromBitmap(bitmap);

            hideSelectedContactDetails();

			this.mMapView = (MapView) v.findViewById(R.id.mapView);
			this.mMapView.onCreate(savedInstanceState);
			this.mMapView.onResume(); // needed to get the map to display immediately
			this.googleMap = this.mMapView.getMap();

			// Check if we were successful in obtaining the map.
			if (this.mMapView != null) {
                //MapsInitializer.initialize(getActivity());
				setUpMap();
			}

            setHasOptionsMenu(true);
            onShow();

			return v;
		}

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
                if(menu != null) {
                    mMenu = menu;
                    mInflater = inflater;
                } else if(mMenu == null || mInflater == null) {
                    return;
                }

                mMenu.clear();
                mInflater.inflate(R.menu.fragment_map, mMenu);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
            if (v.getId()==R.id.contactDetails) {
                menu.add(Menu.NONE, MENU_CONTACT_SHOW, 1, R.string.menuContactShow);

                if(Preferences.getFollowingSelectedContact())
                    menu.add(Menu.NONE, MENU_CONTACT_UNFOLLOW, 2, R.string.menuContactUnfollow);
                else
                    menu.add(Menu.NONE, MENU_CONTACT_FOLLOW, 2, R.string.menuContactFollow);

                menu.add(Menu.NONE, MENU_CONTACT_DETAILS, 3, R.string.menuContactDetails);
                menu.add(Menu.NONE, MENU_CONTACT_NAVIGATE, 4, R.string.menuContactNavigate);
                menu.add(Menu.NONE, MENU_CONTACT_REQUEST_REPORT_LOCATION, 5, R.string.menuContactRequestReportLocation);


            }
        }


        @Override
        public boolean onContextItemSelected(MenuItem item)
        {
            Contact c = getSelectedContact();
            switch (item.getItemId()) {
                case MENU_CONTACT_SHOW:
                    selectContact(c, MapFragment.SELECT_CENTER_AND_ZOOM, true);
                    return true;
                case MENU_CONTACT_FOLLOW:
                    selectContact(c, MapFragment.SELECT_CENTER, true, true);
                    return true;
                case MENU_CONTACT_UNFOLLOW:
                    setFollowingSelectedContact(false);
                    return true;
                case MENU_CONTACT_DETAILS:
                    ((ActivityMain)getActivity()).transitionToContactDetails(c);
                    return true;
                case MENU_CONTACT_NAVIGATE:
                    ((ActivityMain)getActivity()).launchNavigation(c);
                    return true;
                case MENU_CONTACT_REQUEST_REPORT_LOCATION:
                    ((ActivityMain)getActivity()).requestReportLocation(c);
                    return true;
            }
            return false;

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
            ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
            onCreateOptionsMenu(mMenu, mInflater);

        }

		private void onHide() {
		}

		private void setUpMap() {
			this.googleMap.setIndoorEnabled(false);
            this.googleMap.setBuildingsEnabled(true);
            this.googleMap.setMyLocationEnabled(false);

			UiSettings s = this.googleMap.getUiSettings();
			s.setCompassEnabled(false);
			s.setMyLocationButtonEnabled(false);
			s.setTiltGesturesEnabled(false);
			s.setCompassEnabled(false);
			s.setRotateGesturesEnabled(false);
			s.setZoomControlsEnabled(false);

			this.mMapView.getMap().setOnMarkerClickListener(
					new OnMarkerClickListener() {

						@Override
						public boolean onMarkerClick(Marker m) {
                            setFollowingSelectedContact(false);
							Contact c = MapFragment.this.markerToContacts.get(m.getId());

							if (c != null)
								selectContact(c, SELECT_UPDATE, false,true);

                            // Event was handled by our code do not launch default behaviour that would center the map on the marker
                            return true;
						}
					});

			this.mMapView.getMap().setOnMapClickListener(
                    new OnMapClickListener() {

                        @Override
                        public void onMapClick(LatLng arg0) {
                            unfollowContact();
                        }
                    });
		}


        public void showSelectedContactDetails() {
            this.selectedContactDetails.setVisibility(View.VISIBLE);
        }
        public void hideSelectedContactDetails() {
            this.selectedContactDetails.setVisibility(View.GONE);
        }

        public void centerMap(LatLng latlon, int centerMode, boolean animate) {
            centerMap(latlon, centerMode, animate, -1);
        }
		public void centerMap(LatLng latlon, int centerMode, boolean animate, float zoom) {
            if(centerMode!=SELECT_UPDATE) {
                CameraUpdate center;
                if(zoom == -1) {
                     center = CameraUpdateFactory.newLatLngZoom(latlon, centerMode == SELECT_CENTER && zoom != -1? this.mMapView.getMap().getCameraPosition().zoom : 15f);
                } else {
                     center = CameraUpdateFactory.newLatLngZoom(latlon, zoom);
                }

                if(animate)
                    this.mMapView.getMap().animateCamera(center);
                else
                    this.mMapView.getMap().moveCamera(center);
            }
		}

		public void updateContactLocation(Contact c) {

            removeContactMarker(c);

			Marker m = this.googleMap.addMarker(
                    new MarkerOptions().position(c.getLocation().getLatLng()).icon(c.getFaceDescriptor()).anchor(0.5F, 0.5F));
			this.markerToContacts.put(m.getId(), c);
			c.setMarker(m);

			if (c == getSelectedContact())
                selectContact(c, isFollowingSelectedContact() ? SELECT_CENTER : SELECT_UPDATE, true, true);

		}

        private void removeContactMarker(Contact c) {
            if (c.getMarker() != null) {
                this.markerToContacts.remove(c.getMarker().getId());
                c.getMarker().remove();
            }
        }

        private void removeContactLocation(Contact c) {
            removeContactMarker(c);
            Log.v(TAG, Preferences.getSelectedContactTopic());
            if(c.getTopic() == Preferences.getSelectedContactTopic()) {
                unfollowContact();
            }
        }

        private void unfollowContact(){
            setFollowingSelectedContact(false);
            Preferences.setSelectedContactTopic(KEY_NOTOPIC);
            hideSelectedContactDetails();
        }

		@Override
		public void handleHandlerMessage(Message msg) {

			if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && (msg.obj != null)) {
				GeocodableLocation l = (GeocodableLocation) msg.obj;
				if ((l.getTag() == null) || (this.selectedContactLocation == null) || !l.getTag().equals(Preferences.getSelectedContactTopic()))
					return;

				this.selectedContactLocation.setText(l.toString());

			}
		}


        public void selectCurrentLocation(final int centerMode, final boolean follow, boolean animate) {
            selectCurrentLocation(centerMode, follow, animate, -1);
        }

        public void selectCurrentLocation(final int centerMode, final boolean follow, boolean animate, float zoom) {
            setFollowingSelectedContact(follow);
            selectCurrentLocation(centerMode, animate, zoom);
        }
        public void selectCurrentLocation(final int centerMode, final boolean animate, final float zoom) {
            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();

                    if (l == null)
                        return;

                    hideSelectedContactDetails();
                    Preferences.setSelectedContactTopic(KEY_CURRENT_LOCATION);
                    centerMap(l.getLatLng(), centerMode, animate, zoom);
                }
            });

        }

        public void selectContact(final Contact c, int centerMode, boolean follow, boolean animate) {
            selectContact(c, centerMode, follow, animate, -1);
        }

        public void selectContact(final Contact c, int centerMode, boolean follow, boolean animate, float zoom) {
             setFollowingSelectedContact(follow);
             selectContact(c, centerMode, animate, zoom);
         }

        public void selectContact(final Contact c, int centerMode, boolean animate) {
            selectContact(c, centerMode, animate, -1);
        }

        public void selectContact(final Contact c, int centerMode, boolean animate, float zoom) {
			if (c == null)
				return;

			Preferences.setSelectedContactTopic(c.getTopic());

            centerMap(c.getLocation().getLatLng(), centerMode, animate, zoom);

			this.selectedContactName.setText(c.getDisplayName());
			this.selectedContactLocation.setText(c.getLocation().toString());
            this.selectedContactImage.setImageBitmap(c.getFace());

            showSelectedContactDetails();

			if (c.getLocation().getGeocoder() == null)
				(new ReverseGeocodingTask(getActivity(), handler)).execute(c.getLocation());

		}

		public void onEventMainThread(Events.ContactUpdated e) {
            updateContactLocation(e.getContact());
		}

        public void onEventMainThread(Events.ContactAdded e) {
            updateContactLocation(e.getContact());
        }

        public void onEventMainThread(Events.ContactRemoved e) {
            removeContactLocation(e.getContact());
        }



        public void onEventMainThread(Events.CurrentLocationUpdated e) {
            Log.v(TAG,"CurrentLocationUpdated" );
            if(currentLocationMarker != null)
                this.currentLocationMarker.remove();

            if(currentLocationPrecision != null)
                this.currentLocationPrecision.remove();
            this.currentLocationMarker = this.googleMap.addMarker(new MarkerOptions().position(e.getGeocodableLocation().getLatLng()).icon(this.currentLocationMarkerBitmap).draggable(false).flat(true).anchor(0.5F, 0.5F));
//.zIndex(10000).fillColor(R.color.currentLocationRadiusFill).strokeColor(R.color.currentLocationRadiusStroke).
            CircleOptions circleOptions = new CircleOptions().center(e.getGeocodableLocation().getLatLng()).radius(e.getGeocodableLocation().getAccuracy()).strokeWidth(2).strokeColor(0x883f72b5).fillColor(0x110000FF);
            this.currentLocationPrecision = this.googleMap.addCircle(circleOptions);



            if (isFollowingCurrentLocation())
				selectCurrentLocation(SELECT_CENTER_AND_ZOOM, true, true);
		}

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if(e.getState() == ServiceBroker.State.CONNECTING)
                clearMap();

        }

        public void onEventMainThread(Events.ModeChanged e) {
            clearMap();
        }

        public void clearMap() {
                markerToContacts.clear();
                mMapView.getMap().clear();
                hideSelectedContactDetails();
        }

        public Contact getSelectedContact() {
			return App.getContact(Preferences.getSelectedContactTopic());
		}

        public boolean isFollowingCurrentLocation() {
            return Preferences.getSelectedContactTopic().equals(KEY_CURRENT_LOCATION);
        }

//		public boolean hasCurrentLocation() {
//            return this.currentLocation != null;
//		}

        public void setFollowingSelectedContact(boolean followingSelectedContact) {
            Preferences.setFollowingSelectedContact(followingSelectedContact);
        }

        public boolean isFollowingSelectedContact() {
            return Preferences.getFollowingSelectedContact();
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putParcelable(KEY_POSITION, this.mMapView.getMap().getCameraPosition());
            FragmentHandler.getInstance().setBundle(MapFragment.class, b);
        }
    }

    private void requestReportLocation(final Contact c) {
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
            ServiceProxy.getServiceBroker().publish(new CommandMessage(CommandMessage.ACTION_REPORT_LOCATION), c.getCommandTopic(), Preferences.getPubQosCommands(), Preferences.getPubRetainCommands(), null, null);
            }
        });
    }

    public static class ContactsFragment extends Fragment implements
			StaticHandlerInterface {

        private static final String TAG_CURRENTLOCATION = "TAG_CURRENTLOCATION";
        private static Handler handler;

		private ListView contactsList;
        private ContactAdapter listAdapter;
        private ArrayList<Contact> contacts;
        private Menu mMenu;
        private MenuInflater mInflater;

        public static ContactsFragment getInstance() {
			return new ContactsFragment();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			handler = new StaticHandler(this);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View v = inflater.inflate(R.layout.fragment_contacts, container,
                    false);
           // LinearLayout subView = (LinearLayout) v.findViewById(R.id.fragmentToolbarChild);
           // ((ViewGroup)subView.getParent()).removeView(subView);
           // Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
           // layoutParams.gravity = Gravity.LEFT  | Gravity.BOTTOM;

            //this.toolbar.addView(subView, layoutParams);



            this.contactsList = (ListView) v.findViewById(R.id.contactsList);

            this.contactsList.setEmptyView((View) v.findViewById(R.id.contactsListPlaceholder));
            setListAdapter(true);


            this.contactsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
                    ((ActivityMain) getActivity()).transitionToContactMap((Contact) listAdapter.getItem(position));
                }
            });

            registerForContextMenu(this.contactsList);
            EventBus.getDefault().register(this);

            setHasOptionsMenu(true);

            onShow();
            return v;
        }

            @Override
            public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
                if(menu != null) {
                    mMenu = menu;
                    mInflater = inflater;
                } else if(mMenu == null || mInflater == null) {
                    return;
                }

                mMenu.clear();
                mInflater.inflate(R.menu.fragment_contacts, mMenu);
            }


        private void setListAdapter(boolean fromCache) {
            this.listAdapter = new ContactAdapter(this.getActivity(), fromCache ? new ArrayList<Contact>(App.getCachedContacts().values()) : null);
            this.contactsList.setAdapter(this.listAdapter);
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
        public void onDestroy() {
            handler.removeCallbacksAndMessages(null);
            EventBus.getDefault().unregister(this);
            super.onDestroy();
        }

		@Override
		public void onResume() {
            super.onResume();

            registerForContextMenu(this.contactsList);

            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    updateCurrentLocation(ServiceProxy.getServiceLocator().getLastKnownLocation(), true);

                }
            });

		}

        @Override
        public void onPause() {
            unregisterForContextMenu(this.contactsList);

            super.onPause();

        }



        @Override
		public void onHiddenChanged(boolean hidden) {
			if (hidden)
				onHide();
			else
				onShow();
		}

		private void onShow() {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Locations");
            onCreateOptionsMenu(mMenu, mInflater);

        }

		private void onHide() {

		}



        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
            if (v.getId()==R.id.contactsList) {
                menu.add(Menu.NONE, MENU_CONTACT_SHOW, 1, R.string.menuContactShow);
                menu.add(Menu.NONE, MENU_CONTACT_DETAILS, 2, R.string.menuContactDetails);
                menu.add(Menu.NONE, MENU_CONTACT_NAVIGATE, 3, R.string.menuContactNavigate);
                menu.add(Menu.NONE, MENU_CONTACT_REQUEST_REPORT_LOCATION, 5, R.string.menuContactRequestReportLocation);

            }



        }

        @Override
        public boolean onContextItemSelected(MenuItem item)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            if(info == null)
                return true;

            Contact c = (Contact) listAdapter.getItem(info.position);

            switch (item.getItemId()) {
                case MENU_CONTACT_SHOW:
                    ((ActivityMain)getActivity()).transitionToContactMap(c);
                    break;
                case MENU_CONTACT_DETAILS:
                    ((ActivityMain)getActivity()).transitionToContactDetails(c);
                    break;
                case MENU_CONTACT_NAVIGATE:
                    ((ActivityMain)getActivity()).launchNavigation(c);
                    break;
                case MENU_CONTACT_REQUEST_REPORT_LOCATION:
                    ((ActivityMain)getActivity()).requestReportLocation(c);

            }
            return true;
        }



        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            return super.onOptionsItemSelected(item);
        }

		public void onEventMainThread(Events.CurrentLocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
		}

		public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if(e.getState() == ServiceBroker.State.CONNECTING)
                setListAdapter(false); // Ignore cached values. Either they're removed already or are invalid and will be removed soon
		}

        public void onEventMainThread(Events.ModeChanged e) {
            setListAdapter(false);
        }


        public void updateCurrentLocation(GeocodableLocation l, boolean resolveGeocoder) {
			if (l == null)
				return;



			if ((l.getGeocoder() == null) && resolveGeocoder) {
				l.setTag(TAG_CURRENTLOCATION);
				(new ReverseGeocodingTask(getActivity(), handler)).execute(l);
			}
		}

        public void updateContactLocation(){
            updateContactLocation(null, false);
        }

        public void updateContactLocation(Contact c, boolean resolveGeocoder) {

            this.listAdapter.notifyDataSetChanged();

            if (resolveGeocoder && c != null && c.getLocation() != null && c.getLocation().getGeocoder() == null)
                (new ReverseGeocodingTask(getActivity(), handler)).execute(c.getLocation());

        }


		@Override
		public void handleHandlerMessage(Message msg) {

			if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && (msg.obj != null))
				if (((GeocodableLocation) msg.obj).getTag().equals(TAG_CURRENTLOCATION))
					updateCurrentLocation((GeocodableLocation) msg.obj, false);
				else
                    updateContactLocation();
		}

		public void onEventMainThread(Events.ContactUpdated e) {
            updateContactLocation(e.getContact(), true);
		}
        public void onEventMainThread(Events.ContactAdded e) {

            listAdapter.addItem(e.getContact());
            updateContactLocation(e.getContact(), true);
        }

        public void onEventMainThread(Events.ContactRemoved e) {
            listAdapter.removeItem(e.getContact());
        }

    }



    public static class DetailsFragment extends Fragment  {
		public static final String KEY_TOPIC = "TOPIC";
        private static final int MENU_CONTACT_DETAILS_LINK = 0;
        private static final int MENU_CONTACT_DETAILS_UNLINK = 1;

		private Contact contact;
		private TextView name;
		private TextView topic;
		private TextView location;
		private TextView accuracy;
		private TextView time;
		private OnSharedPreferenceChangeListener preferencesChangedListener;
        private Menu mMenu;
        private MenuInflater mInflater;

        public static DetailsFragment getInstance() {
			return new DetailsFragment();
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
		public void onDestroy() {

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
        public void onPause() {
            super.onPause();
        }

		@Override
		public void onHiddenChanged(boolean hidden) {
			if (hidden)
				onHide();
			else
				onShow();
		}

		private void onShow() {
			Bundle extras = FragmentHandler.getInstance().getBundle(DetailsFragment.class);

			this.contact = App.getContact((String) extras.get(KEY_TOPIC));
            if(this.contact == null)
                FragmentHandler.getInstance().back((AppCompatActivity)getActivity());

            this.name.setText(this.contact.getDisplayName() );
            this.topic.setText(this.contact.getTopic());
			this.location.setText(this.contact.getLocation().toString());
            this.accuracy.setText("± " + this.contact.getLocation().getAccuracy() + "m");
			this.time.setText(App.formatDate(this.contact.getLocation().getDate()));

            ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Details");


            onCreateOptionsMenu(mMenu, mInflater);
		}

		private void onHide() {

		}

		// Called when contact is picked
		@Override
		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			super.onActivityResult(requestCode, resultCode, data);

			if ((requestCode == CONTACT_PICKER_RESULT) && (resultCode == RESULT_OK))
				assignContact(data);
		}

        private void unassignContact(Contact c) {

            if(c == null)
                return;

            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    ServiceProxy.getServiceApplication().unlinkContact(DetailsFragment.this.contact);

                }
            });

        }

		private void assignContact(Intent intent) {
			Uri result = intent.getData();
			final String contactId = result.getLastPathSegment();

			ServiceProxy.runOrBind(getActivity(), new Runnable() {

				@Override
				public void run() {
					ServiceProxy.getServiceApplication().linkContact(
                            DetailsFragment.this.contact,
                            Long.parseLong(contactId));

				}
			});

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			View v = inflater.inflate(R.layout.fragment_details, container,
                    false);
			this.name = (TextView) v.findViewById(R.id.name);
			this.topic = (TextView) v.findViewById(R.id.topic);
			this.location = (TextView) v.findViewById(R.id.location);
			this.accuracy = (TextView) v.findViewById(R.id.accuracy);
			this.time = (TextView) v.findViewById(R.id.time);



            setHasOptionsMenu(true);
    		onShow();
			return v;
		}

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            if(menu != null) {
                mMenu = menu;
                mInflater = inflater;
            } else if(mMenu == null || mInflater == null) {
                return;
            }


            mMenu.clear();
            mInflater.inflate(R.menu.fragment_details, mMenu);

            mMenu.findItem(R.id.action_assign).setVisible(!this.contact.hasLink());
            mMenu.findItem(R.id.action_unassign).setVisible(this.contact.hasLink());


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_assign:
                    startActivityForResult(new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI), CONTACT_PICKER_RESULT);
                    return true;
                case R.id.action_unassign:
                    unassignContact(this.contact);
                    return true;
                case R.id.action_clear:
                    clearTopic(this.contact);
                    return true;
            }
            return false;
        }

        private void clearTopic(final Contact contact) {
            ServiceProxy.runOrBind(getActivity(), new Runnable() {
                @Override
                public void run() {
                    ServiceProxy.getServiceBroker().publish(new ClearMessage(contact.getTopic()));
                }
            });
        }


        public void onEventMainThread(Events.ContactUpdated e) {
			if (e.getContact() == this.contact && isVisible())
				onShow();
		}

        public void onEventMainThread(Events.ContactRemoved e) {
            // Contact will be cleared, close this view
            if (e.getContact() == this.contact);
                FragmentHandler.getInstance().back((AppCompatActivity)getActivity());
        }

        public void onEventMainThread(Events.ModeChanged e) {
            FragmentHandler.getInstance().back((AppCompatActivity)getActivity());
        }


        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            // Contact will be cleared, close this view
            if(e.getState() == ServiceBroker.State.CONNECTING)
                FragmentHandler.getInstance().back((AppCompatActivity)getActivity());

        }


        @Override
		public void onSaveInstanceState(Bundle b) {
			super.onSaveInstanceState(b);
            if(this.contact != null)
                b.putString(KEY_TOPIC, this.contact.getTopic());
			FragmentHandler.getInstance().setBundle(DetailsFragment.class, b);
		}

    }


}
