package st.alr.mqttitude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import st.alr.mqttitude.adapter.ContactAdapter;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.preferences.ActivityPreferences;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.Preferences;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final int MENU_CONTACT_SHOW = 0;
    private static final int MENU_CONTACT_DETAILS = 1;
    private static final int MENU_CONTACT_NAVIGATE = 2;
    private static final int MENU_CONTACT_FOLLOW = 3;
    private static final int MENU_CONTACT_UNFOLLOW = 4;

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
		if (savedInstanceState != null)
			savedInstanceState.remove("android:support:fragments");

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		FragmentHandler.getInstance().init(ContactsFragment.class);
		FragmentHandler.getInstance().showCurrentOrRoot(this);

        MapsInitializer.initialize(this);
	}

	public static class FragmentHandler extends Fragment {
		private Class<?> current;
		private Class<?> root;
        public static final int DIRECTION_NONE = 0;
        public static final int DIRECTION_FORWARD = 1;
        public static final int DIRECTION_BACK = 2;

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
				FragmentActivity fa, int direction) {
			Fragment f = getFragment(c);
			Fragment prev = getFragment(this.current);

			handleFragmentArguments(c, extras);
			FragmentTransaction ft = fa.getSupportFragmentManager()
					.beginTransaction();

            if(direction == DIRECTION_FORWARD)
                ft.setCustomAnimations(R.anim.enter_from_right, R.anim.zoom_out);
            else if(direction == DIRECTION_BACK)
                ft.setCustomAnimations(R.anim.zoom_in, R.anim.exit_to_right);

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
			return showFragment(popBackStack(), null, fa, DIRECTION_BACK);
		}

		public Fragment forward(Class<?> c, Bundle extras, FragmentActivity fa) {
			pushBackStack(this.current);
			return showFragment(c, extras, fa, DIRECTION_FORWARD);
		}

		public void init(Class<?> c) {
			this.root = c;
		}

		public void showCurrentOrRoot(FragmentActivity fa) {
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

		public void removeAll(FragmentActivity fa) {
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

        if(!App.isDebugBuild()) {
            menu.findItem(R.id.menu_develop_1).setVisible(false);
            menu.findItem(R.id.menu_develop_2).setVisible(false);

        }
		return true;
	}

    private void onCreateContactContextMenu(ContextMenu menu, View v, int viewId) {
        if (v.getId()==viewId) {
            Log.v(this.toString(), "onCreateContactContextMenu");
            Log.v(this.toString(), FragmentHandler.getInstance().getCurrentFragmentClass().toString());

            if(FragmentHandler.getInstance().getCurrentFragmentClass() == ContactsFragment.class) {
                Log.v(this.toString(), "ContactsFragment");
                menu.add(Menu.NONE, MENU_CONTACT_SHOW, 1, R.string.menuContactShow);
                menu.add(Menu.NONE, MENU_CONTACT_DETAILS, 2, R.string.menuContactDetails);
                menu.add(Menu.NONE, MENU_CONTACT_NAVIGATE, 3, R.string.menuContactNavigate);
            } else if(FragmentHandler.getInstance().getCurrentFragmentClass() == MapFragment.class) {
                Log.v(this.toString(), "MapFragment");
                menu.add(Menu.NONE, MENU_CONTACT_SHOW, 1, R.string.menuContactShow);

                if(Preferences.getFollowingSelectedContact())
                    menu.add(Menu.NONE, MENU_CONTACT_UNFOLLOW, 2, R.string.menuContactUnfollow);
                else
                    menu.add(Menu.NONE, MENU_CONTACT_FOLLOW, 2, R.string.menuContactFollow);

                menu.add(Menu.NONE, MENU_CONTACT_DETAILS, 3, R.string.menuContactDetails);
                menu.add(Menu.NONE, MENU_CONTACT_NAVIGATE, 4, R.string.menuContactNavigate);

            }

        }
    }

    private boolean onContactContextItemSelected(MenuItem item, Contact c) {
        switch (item.getItemId()) {
            case MENU_CONTACT_SHOW:
                transitionToContactMap(c);
                break;
            case MENU_CONTACT_DETAILS:
                transitionToContactDetails(c);
                break;
            case MENU_CONTACT_NAVIGATE:
                Log.v(this.toString(), "Navigate for " +c);
                launchNavigation(c);
        }
        return true;

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
        final FragmentActivity that = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, that)).selectCurrentLocation(MapFragment.SELECT_CENTER_AND_ZOOM, true);
            }
        });
    }

    private void transitionToContactMap(final Contact c) {
        final FragmentActivity that = this;
        ServiceProxy.runOrBind(this, new Runnable() {
            @Override
            public void run() {
                ((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, that)).selectContact(c, MapFragment.SELECT_CENTER_AND_ZOOM, true);
            }
        });
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
						ServiceProxy.getServiceLocator()
								.publishLocationMessage();
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
        } else if (itemId == R.id.menu_develop_1) {
            devMenu1();
            return true;
        } else if (itemId == R.id.menu_develop_2) {
            devMenu2();
            return true;
        } else {
			return super.onOptionsItemSelected(item);
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
		// bindService(new Intent(this, ServiceProxy.class), serviceConnection,
		// Context.BIND_AUTO_CREATE);
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableForegroundMode();
                ServiceProxy.getServiceBeacon().setBackgroundMode(false);
            }
        });
	}

	@Override
	public void onStop() {
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.getServiceLocator().enableBackgroundMode();
                ServiceProxy.getServiceBeacon().setBackgroundMode(true);
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


	public static class MapFragment extends Fragment implements StaticHandlerInterface {
        //private GeocodableLocation currentLocation;
		private MapView mMapView;
		private GoogleMap googleMap;
		private LinearLayout selectedContactDetails;
		private TextView selectedContactName;
		private TextView selectedContactLocation;
		//private ImageView selectedContactImage;
		private Map<String, Contact> markerToContacts;
		private static final String KEY_CURRENT_LOCATION = "+CURRENTLOCATION+";
        private static final String KEY_NOTOPIC = "+NOTOPIC+";

        private static final int SELECT_UPDATE = 0;
        private static final int SELECT_CENTER = 1;
        private static final int SELECT_CENTER_AND_ZOOM = 2;


        private static Handler handler;


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

            if (c != null)
                selectContact(c, SELECT_CENTER_AND_ZOOM);
            else if (isFollowingCurrentLocation())
                selectCurrentLocation(SELECT_CENTER_AND_ZOOM);
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
            registerForContextMenu(this.selectedContactDetails);

            this.selectedContactName = (TextView) v.findViewById(R.id.name);
			this.selectedContactLocation = (TextView) v.findViewById(R.id.location);
			//this.selectedContactImage = (ImageView) v.findViewById(R.id.image);

            hideSelectedContactDetails();

			this.mMapView = (MapView) v.findViewById(R.id.mapView);
			this.mMapView.onCreate(savedInstanceState);
			this.mMapView.onResume(); // needed to get the map to display immediately
			this.googleMap = this.mMapView.getMap();

			// Check if we were successful in obtaining the map.
			if (this.mMapView != null) {
                MapsInitializer.initialize(getActivity());
				setUpMap();
			}

			return v;
		}

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
            ((ActivityMain)getActivity()).onCreateContactContextMenu(menu, v, R.id.contactDetails);
        }


        @Override
        public boolean onContextItemSelected(MenuItem item)
        {
            Contact c = getSelectedContact();
            switch (item.getItemId()) {
                case MENU_CONTACT_SHOW:
                    selectContact(c, MapFragment.SELECT_CENTER_AND_ZOOM, false);
                    break;
                case MENU_CONTACT_FOLLOW:
                    selectContact(c, MapFragment.SELECT_CENTER, true);
                    break;
                case MENU_CONTACT_UNFOLLOW:
                    setFollowingSelectedContact(false);
                    break;
                case MENU_CONTACT_DETAILS:
                    ((ActivityMain)getActivity()).transitionToContactDetails(c);
                    break;
                case MENU_CONTACT_NAVIGATE:
                    ((ActivityMain)getActivity()).launchNavigation(c);
                    break;
            }
            return true;

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
			this.googleMap.setIndoorEnabled(false);
            this.googleMap.setBuildingsEnabled(true);

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
								selectContact(c, SELECT_UPDATE, false);

                            // Event was handled by our code do not launch default behaviour that would center the map on the marker
                            return true;
						}
					});

			this.mMapView.getMap().setOnMapClickListener(
                    new OnMapClickListener() {

						@Override
						public void onMapClick(LatLng arg0) {
                            setFollowingSelectedContact(false);
                            Preferences.setSelectedContactTopic(KEY_NOTOPIC);

                            hideSelectedContactDetails();
						}
					});
		}


        public void showSelectedContactDetails() {
            this.selectedContactDetails.setVisibility(View.VISIBLE);
        }
        public void hideSelectedContactDetails() {
            this.selectedContactDetails.setVisibility(View.GONE);
        }

		public void centerMap(LatLng latlon, int centerMode) {
            Log.v(this.toString(), "centerMap with mode: " + centerMode);
            if(centerMode!=SELECT_UPDATE) {
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(latlon, centerMode == SELECT_CENTER ? this.mMapView.getMap().getCameraPosition().zoom : 15f);
                this.mMapView.getMap().animateCamera(center);
            }
		}

		public void updateContactLocation(Contact c) {

			if (c.getMarker() != null) {
                this.markerToContacts.remove(c.getMarker().getId());
                c.getMarker().remove();
            }

			Marker m = this.googleMap.addMarker(new MarkerOptions().position(c.getLocation().getLatLng()).icon(c.getMarkerImageDescriptor()));
			this.markerToContacts.put(m.getId(), c);
			c.setMarker(m);

			if (c == getSelectedContact())
                selectContact(c, isFollowingSelectedContact() ? SELECT_CENTER : SELECT_UPDATE);

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

        public void selectCurrentLocation(final int centerMode, final boolean follow) {
            setFollowingSelectedContact(follow);
            selectCurrentLocation(centerMode);
        }
        public void selectCurrentLocation(final int centerMode) {
            ServiceProxy.runOrBind(getActivity(), new Runnable() {

                @Override
                public void run() {
                    GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                    if (l == null)
                        return;
                    hideSelectedContactDetails();
                    Preferences.setSelectedContactTopic(KEY_CURRENT_LOCATION);
                    centerMap(l.getLatLng(), centerMode);
                }
            });

        }


        public void selectContact(final Contact c, int centerMode, boolean follow) {
             setFollowingSelectedContact(follow);
             selectContact(c, centerMode);
         }

         public void selectContact(final Contact c, int centerMode) {

			if (c == null) {
				Log.v(this.toString(), "no contact, abandon ship!");
				return;
			}

			Preferences.setSelectedContactTopic(c.getTopic());

            centerMap(c.getLocation().getLatLng(), centerMode);

			this.selectedContactName.setText(c.toString());
			this.selectedContactLocation.setText(c.getLocation().toString());

/*
			this.selectedContactImage.setImageBitmap(c.getUserImage());
			this.selectedContactImage.setTag(c.getTopic());
			this.selectedContactImage.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Bundle b = new Bundle();
					b.putString(DetailsFragment.KEY_TOPIC, c.getTopic());
					FragmentHandler.getInstance().forward(
							DetailsFragment.class, b, getActivity());
				}
			});
*/

            showSelectedContactDetails();
/*
            this.selectedContactDetails.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
*/

			if (c.getLocation().getGeocoder() == null)
				(new ReverseGeocodingTask(getActivity(), handler)).execute(c.getLocation());

		}

		public void onEventMainThread(Events.ContactUpdated e) {
            updateContactLocation(e.getContact());
		}

        public void onEventMainThread(Events.ContactAdded e) {
            updateContactLocation(e.getContact());
        }


        public void onEventMainThread(Events.CurrentLocationUpdated e) {
			if (isFollowingCurrentLocation())
				selectCurrentLocation(SELECT_CENTER_AND_ZOOM);
		}

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if(e.getState() == Defaults.State.ServiceBroker.CONNECTING)
                clearMap();

        }

        public void clearMap() {
                Log.v(this.toString(), "Clearing map");
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
            Log.v(this.toString(), "following in getter: " + Preferences.getFollowingSelectedContact());
            return Preferences.getFollowingSelectedContact();
        }
    }

	public static class ContactsFragment extends Fragment implements
			StaticHandlerInterface {

        private static final String TAG_CURRENTLOCATION = "TAG_CURRENTLOCATION";

        private static Handler handler;

		private ListView list;
        private ContactAdapter listAdapter;
        private Button currentLocation;
        private ArrayList<Contact> contacts;
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

            this.list = (ListView) v.findViewById(R.id.list);
            setListAdapter(true);
            this.currentLocation = (Button) v.findViewById(R.id.currentLocation);
            this.currentLocation.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ServiceProxy.runOrBind(getActivity(), new Runnable() {

                        @Override
                        public void run() {
                            if(ServiceProxy.getServiceLocator().getLastKnownLocation() != null) {
                                ((ActivityMain) getActivity()).transitionToCurrentLocationMap();
                            } else {
                                App.showLocationNotAvailableToast();
                            }
                        }
                    });

                }
            });

            this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {
                    ((ActivityMain)getActivity()).transitionToContactMap((Contact) listAdapter.getItem(position));
                }
            });

            registerForContextMenu(this.list);
            EventBus.getDefault().register(this);


            return v;
        }


        private void setListAdapter(boolean fromCache) {
            this.listAdapter = new ContactAdapter(this.getActivity(), fromCache ? new ArrayList<Contact>(App.getCachedContacts().values()) : null);
            this.list.setAdapter(this.listAdapter);
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

            registerForContextMenu(this.list);

			ServiceProxy.runOrBind(getActivity(), new Runnable() {

				@Override
				public void run() {
					updateCurrentLocation(ServiceProxy.getServiceLocator().getLastKnownLocation(), true);

				}
			});

		}

        @Override
        public void onPause() {
            unregisterForContextMenu(this.list);

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

		}

		private void onHide() {

		}

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
            ((ActivityMain)getActivity()).onCreateContactContextMenu(menu, v, R.id.list);
        }


        @Override
        public boolean onContextItemSelected(MenuItem item)
        {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            Contact c = (Contact) listAdapter.getItem(info.position);


            return ((ActivityMain)getActivity()).onContactContextItemSelected(item, c);
        }




		public void onEventMainThread(Events.CurrentLocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
		}

		public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            if(e.getState() == Defaults.State.ServiceBroker.CONNECTING)
                setListAdapter(false); // Ignore cached values. Either they're removed already or are invalid and will be removed soon
		}

		public void updateCurrentLocation(GeocodableLocation l, boolean resolveGeocoder) {
			if (l == null)
				return;


            currentLocation.setText(l.toString());

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
			return new DetailsFragment();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			this.preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
				@Override
				public void onSharedPreferenceChanged(
						SharedPreferences sharedPreference, String key) {
					if (key.equals(Preferences
							.getKey(R.string.keyUpdateAddressBook)))
						showHideAssignContactButton();
				}
			};
			PreferenceManager.getDefaultSharedPreferences(getActivity())
					.registerOnSharedPreferenceChangeListener(
							this.preferencesChangedListener);

		}

		@Override
		public void onStart() {
			super.onStart();
			EventBus.getDefault().registerSticky(this);
		}

		void showHideAssignContactButton() {
			if (!Preferences.getUpdateAdressBook())
				this.assignContact.setVisibility(View.VISIBLE);
			else
				this.assignContact.setVisibility(View.GONE);
		}

		@Override
		public void onDestroy() {
			PreferenceManager.getDefaultSharedPreferences(getActivity())
					.unregisterOnSharedPreferenceChangeListener(
							this.preferencesChangedListener);

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
			Bundle extras = FragmentHandler.getInstance().getBundle(DetailsFragment.class);

			this.contact = App.getContact((String) extras.get(KEY_TOPIC));
			//this.contact = App.getContacts().get(extras.get(KEY_TOPIC));

			this.name.setText(this.contact.getName());
			this.topic.setText(this.contact.getTopic());
			this.location.setText(this.contact.getLocation().toString());
			this.accuracy
					.setText("" + this.contact.getLocation().getAccuracy());
			this.time.setText(App.formatDate(this.contact.getLocation()
					.getDate()));

			this.assignContact.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					startActivityForResult(new Intent(Intent.ACTION_PICK,
							Contacts.CONTENT_URI), CONTACT_PICKER_RESULT);
				}
			});
		}

		private void onHide() {

		}

		// Called when contact is picked
		@Override
		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			super.onActivityResult(requestCode, resultCode, data);

			if ((requestCode == CONTACT_PICKER_RESULT)
					&& (resultCode == RESULT_OK))
				assignContact(data);
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
			this.assignContact = (Button) v.findViewById(R.id.assignContact);
			showHideAssignContactButton();
			onShow();
			return v;
		}

		public void onEventMainThread(Events.ContactUpdated e) {
			if (e.getContact() == this.contact)
				onShow();
		}

        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            // Contact will be cleared, close this view
            if(e.getState() == Defaults.State.ServiceBroker.CONNECTING)
                FragmentHandler.getInstance().back(getActivity());

        }


        @Override
		public void onSaveInstanceState(Bundle b) {
			super.onSaveInstanceState(b);
			b.putString(KEY_TOPIC, this.contact.getTopic());
			FragmentHandler.getInstance().setBundle(DetailsFragment.class, b);
		}


	}

    // Developer menu options to quickly trigger code in order to test things
    private void devMenu1() {
        Log.i(this.toString(), "Developer quickaccess option 1 selected");
        ServiceProxy.getServiceBroker().reconnect();
    }
    private void devMenu2() {
        Log.i(this.toString(), "Developer quickaccess option 2 selected");
    }
}
