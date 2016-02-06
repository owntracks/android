package org.owntracks.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.owntracks.android.App;
import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.adapter.ContactAdapter;
import org.owntracks.android.databinding.ActivityContactsBinding;
import org.owntracks.android.databinding.ActivityMapBinding;
import org.owntracks.android.messages.ClearMessage;
import org.owntracks.android.messages.CommandMessage;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.services.ServiceBroker;
import org.owntracks.android.services.ServiceProxy;
import org.owntracks.android.support.DrawerFactory;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Preferences;
import org.owntracks.android.support.RecyclerViewAdapter;
import org.owntracks.android.support.SnackbarFactory;
import org.owntracks.android.support.Toasts;

import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
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

import com.mapbox.mapboxsdk.api.ILatLng;
import com.mapbox.mapboxsdk.tileprovider.tilesource.MapboxTileLayer;
import com.mapbox.mapboxsdk.views.MapViewListener;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import de.greenrobot.event.EventBus;
import me.tatarka.bindingcollectionadapter.BindingRecyclerViewAdapter;
import me.tatarka.bindingcollectionadapter.ItemViewArg;
import me.tatarka.bindingcollectionadapter.factories.BindingRecyclerViewAdapterFactory;

public class ActivityMain extends ActivityBase {
    private static final String TAG = "ActivityMain";

    private static final int CONTACT_PICKER_RESULT = 1001;
    private static final int MENU_CONTACT_SHOW = 0;
    private static final int MENU_CONTACT_DETAILS = 1;
    private static final int MENU_CONTACT_NAVIGATE = 2;
    private static final int MENU_CONTACT_FOLLOW = 3;
    private static final int MENU_CONTACT_UNFOLLOW = 4;
    private static final int MENU_CONTACT_REQUEST_REPORT_LOCATION = 5;

    private Toolbar toolbar;
    private Drawer drawer;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        startService(new Intent(this, ServiceProxy.class));
        final ActivityMain self = this;
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
        drawer = DrawerFactory.buildDrawerV2(this, toolbar, new DrawerFactory.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick() {
                if (!ActivityMain.FragmentHandler.getInstance().atRoot()) {
                    // We're showing the root and rolling back the complete back stack so we discard it
                    FragmentHandler.getInstance().clearBackStack();
                    FragmentHandler.getInstance().showFragment(FragmentHandler.getInstance().getRoot(), null, (AppCompatActivity) self, FragmentHandler.DIRECTION_BACK);
                }

                drawer.closeDrawer();
                return true;
            }
        });





        FragmentHandler.getInstance().init(ContactsFragment.class, drawer);
		FragmentHandler.getInstance().showCurrentOrRoot(this);

	}
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
       // toggle.syncState();
    }

    @Override
    public View getSnackbarTargetView() {
        Log.v(TAG, "getSnackbarTargetView");
        Fragment f = FragmentHandler.getInstance().getCurrentFragmentInstance();
        if(f instanceof SnackbarFactory.SnackbarFactoryDelegate) {
            Log.v(TAG, "returing specifcdelegate");

            return ((SnackbarFactory.SnackbarFactoryDelegate) f).getSnackbarTargetView();
        }else {
            Log.v(TAG, "returing general delegate");

            return super.getSnackbarTargetView();
        }
    }


    private void requestReportLocation(final Contact c) {
        ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                //ServiceProxy.getServiceBroker().publish(new CommandMessage(CommandMessage.ACTION_REPORT_LOCATION), c.getCommandTopic(), Preferences.getPubQosCommands(), Preferences.getPubRetainCommands(), null, null);
            }
        });
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

        public Fragment getCurrentFragmentInstance() {
            return getFragment(current);
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
            Toasts.showLocationNotAvailable();
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
                //((MapFragment) FragmentHandler.getInstance().forward(MapFragment.class, null, that)).selectCurrentLocation(MapFragment.SELECT_CENTER_AND_ZOOM, true, false);
                ((MapboxMapFragment) FragmentHandler.getInstance().forward(MapboxMapFragment.class, null, that)).selectCurrentLocation(MapboxMapFragment.SELECT_CENTER_AND_ZOOM, true, false, MapboxMapFragment.ZOOM_LEVEL_NEIGHBORHOOD);

            }
        });
    }

    private void transitionToContactMap(FusedContact c) {
        FragmentHandler.getInstance().forward(MapboxMapFragment.class, null, this);
    }


    public  void showLocationNotAvailableToast() {
        Toast.makeText(this, getString(R.string.currentLocationNotAvailable), Toast.LENGTH_SHORT).show();
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
                        showLocationNotAvailableToast();
                    else
                        ServiceProxy.getServiceLocator().publishManualLocationMessage();
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
                        showLocationNotAvailableToast();
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
                Location l = ServiceProxy.getServiceLocator()
                        .getLastKnownLocation();
                if (l == null) {
                    showLocationNotAvailableToast();
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



    public static class  MapboxMapFragment extends Fragment implements SnackbarFactory.SnackbarFactoryDelegate {
        private static final java.lang.String KEY_SELECTED_CONTACT_TOPIC = "KEY_SELECTED_CONTACT_TOPIC";
        private com.mapbox.mapboxsdk.views.MapView mapView;
        private static final String KEY_CURRENT_LOCATION = "+CURRENTLOCATION+";
        private static final String KEY_NOTOPIC =          "+NOTOPIC+";
        private static final String KEY_POSITION =         "+POSITION+";
        private static final String KEY_ZOOM =              "+ZOOM+";
        private static final int SELECT_UPDATE = 0;
        private static final int SELECT_CENTER = 1;
        private static final int SELECT_CENTER_AND_ZOOM = 2;

        private static final long ZOOM_LEVEL_COUNTRY = 6;
        private static final long ZOOM_LEVEL_CITY = 11;
        private static final long ZOOM_LEVEL_NEIGHBORHOOD = 17;

        private Menu mMenu;
        private static Handler handler;
        private MenuInflater mInflater;
        private com.mapbox.mapboxsdk.overlay.Marker currentLocationMarker;
        ActivityMapBinding binding;
        private HashMap<String, com.mapbox.mapboxsdk.overlay.Marker> markers;

        public View getSnackbarTargetView() {
            return null;
           // return selectedContactDetails;
        }



        public static MapboxMapFragment getInstance(Bundle extras) {
            MapboxMapFragment instance = new MapboxMapFragment();
            instance.setArguments(extras);
            return instance;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            this.markers = new HashMap<>();

            this.binding = DataBindingUtil.inflate(inflater, R.layout.activity_map, container, false);
            this.mapView = binding.mapView;
            this.mapView.setAccessToken(getString(R.string.MAPBOX_API_KEY));
            this.mapView.setTileSource(new MapboxTileLayer("binarybucks.nfc70b7d"));
            this.mapView.setDiskCacheEnabled(true);
            this.mapView.setUserLocationEnabled(false);
            this.mapView.setMapViewListener(new MapViewListener() {
                @Override
                public void onShowMarker(com.mapbox.mapboxsdk.views.MapView mapView, com.mapbox.mapboxsdk.overlay.Marker marker) {

                }

                @Override
                public void onHideMarker(com.mapbox.mapboxsdk.views.MapView mapView, com.mapbox.mapboxsdk.overlay.Marker marker) {

                }

                @Override
                public void onTapMarker(com.mapbox.mapboxsdk.views.MapView mapView, com.mapbox.mapboxsdk.overlay.Marker marker) {

                }

                @Override
                public void onLongPressMarker(com.mapbox.mapboxsdk.views.MapView mapView, com.mapbox.mapboxsdk.overlay.Marker marker) {

                }

                @Override
                public void onTapMap(com.mapbox.mapboxsdk.views.MapView mapView, ILatLng iLatLng) {
                    unfollowContact();
                }

                @Override
                public void onLongPressMap(com.mapbox.mapboxsdk.views.MapView mapView, ILatLng iLatLng) {
                    unfollowContact();
                }
            });

            setHasOptionsMenu(true);
            onShow();

            return binding.getRoot();
        }



        public void selectCurrentLocation(final int centerMode, final boolean follow, boolean animate, float zoom) {
            // TODO
        }

        public void centerMap(ILatLng l, boolean animate, float zoom) {
                if(animate)
                    this.mapView.setCenter(l, false);
                else
                    //this.mMapView.getMap().moveCamera(center);
                    this.mapView.setCenter(l, false);

                if(zoom != -1)
                    this.mapView.setZoom(zoom);

        }


        @Override
        public void onResume() {
            super.onResume();

            for(FusedContact c : App.getFusedContacts().values()) {
                updateContactMarker(c);
            }

            Bundle extras = FragmentHandler.getInstance().getBundle(MapboxMapFragment.class);
            // TODO: Restore state

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
            onCreateOptionsMenu(mMenu, mInflater);
        }

        private void onHide() {

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


        public void selectContact(FusedContact c) {
            // TODO
        }

        public void updateContactMarker(FusedContact c) {
            // TODO
        }


        public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
            // TOOD
        }

        public void onEventMainThread(Events.ModeChanged e) {
            // TOOD
        }


        private void unfollowContact(){
            // TODO
        }

        @Override
        public void onSaveInstanceState(Bundle b) {
            super.onSaveInstanceState(b);
            b.putParcelable(KEY_POSITION, this.mapView.getCenter());
            b.putFloat(KEY_ZOOM, this.mapView.getZoomLevel());
            FragmentHandler.getInstance().setBundle(MapboxMapFragment.class, b);
        }


    }


    public static class ContactsFragment extends Fragment implements RecyclerViewAdapter.ClickHandler, RecyclerViewAdapter.LongClickHandler, BindingRecyclerViewAdapterFactory

    {

        private static final String TAG_CURRENTLOCATION = "TAG_CURRENTLOCATION";

        private Menu mMenu;
        private MenuInflater mInflater;

        public static ContactsFragment getInstance() {
			return new ContactsFragment();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ActivityContactsBinding v = DataBindingUtil.inflate(inflater, R.layout.activity_contacts, container, false);


            v.setVariable(BR.adapterFactory, this);
            v.setViewModel(App.getContactsViewModel());


            setHasOptionsMenu(true);

            onShow();
            return v.getRoot();
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
            super.onDestroy();
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
        public boolean onOptionsItemSelected(MenuItem item) {
            return super.onOptionsItemSelected(item);
        }

		public void onEventMainThread(Events.CurrentLocationUpdated e) {
		}

		public void onEventMainThread(Events.StateChanged.ServiceBroker e) {
		}

        public void onEventMainThread(Events.ModeChanged e) {
        }



        @Override
        public void onClick(View v, Object viewModel) {
            Log.v(TAG, "onClick" + viewModel);
            FragmentHandler.getInstance().forward(MapboxMapFragment.class, null, (AppCompatActivity)getActivity());

        }

        @Override
        public void onLongClick(View v, Object viewModel) {
           // registerForContextMenu(v);
            //openContextMenu(v);
           // unregisterForContextMenu(v);

        }

        @Override
        public <T> BindingRecyclerViewAdapter<T> create(RecyclerView recyclerView, ItemViewArg<T> arg) {
            Log.v("BindingRecyclerViewAdap", "create");
            return new RecyclerViewAdapter<>(this, this, arg);
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

            Contact.unlinkContact(c);


        }

		private void assignContact(Intent intent) {
			Uri result = intent.getData();
			final String contactId = result.getLastPathSegment();

					Contact.linkContact(getActivity(),
                            DetailsFragment.this.contact,
                            Long.parseLong(contactId));


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
                    //ServiceProxy.getServiceBroker().publish(new ClearMessage(contact.getTopic()));
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
                FragmentHandler.getInstance().back((AppCompatActivity) getActivity());
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
