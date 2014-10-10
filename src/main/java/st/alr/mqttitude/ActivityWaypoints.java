package st.alr.mqttitude;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;
import st.alr.mqttitude.adapter.WaypointAdapter;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import st.alr.mqttitude.support.StaticHandler;
import st.alr.mqttitude.support.StaticHandlerInterface;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

public class ActivityWaypoints extends FragmentActivity implements StaticHandlerInterface {
    private static final int MENU_WAYPOINT_DELETE = 0;
    private ListView listView;
	private WaypointAdapter listAdapter;
    private WaypointDao dao;
    private GeocodableLocation currentLocation;
    private static final String BUNDLE_KEY_POSITION = "position";
    private Handler handler;
    private TextView waypointListPlaceholder;
    private static final Integer WAYPOINT_TYPE_LOCAL = 0;
    private static final Integer WAYPOINT_TYPE_LOCAL_MONITORING = 2;
    private int waypointLocalLastIdx = 0;
    private int waypointLocalMonitoringLastIdx = 0;
    private boolean localHeaderAdded = false;
    private boolean localMonitoringHeaderAdded = false;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this, ServiceProxy.class));
		ServiceProxy.runOrBind(this, new Runnable() {
			@Override
			public void run() {
				Log.v("ActivityWaypoints", "ServiceProxy bound");
			}
		});

		setContentView(R.layout.activity_waypoint);

        this.dao = App.getWaypointDao();
        this.handler = new StaticHandler(this);

		//this.listAdapter = new WaypointAdapter(this, new ArrayList<Waypoint>(dao.loadAll()));

        this.listAdapter = new WaypointAdapter(this);
        this.listAdapter.addHeader("MONITORING"); // initial idx 1;

        List<Waypoint> waypoints = this.dao.queryBuilder().where(WaypointDao.Properties.Id.isNotNull())
                .orderAsc(WaypointDao.Properties.Type, WaypointDao.Properties.Description)
                .list();
        //this.listAdapter.addHeader("LOCAL WAYPOINTS");
        for(Waypoint w : waypoints) {
            addWaypointToList(w);
        }



		this.listView = (ListView) findViewById(R.id.waypoints);
		this.listView.setAdapter(this.listAdapter);

        registerForContextMenu(this.listView);
        this.waypointListPlaceholder = (TextView) findViewById(R.id.waypointListPlaceholder);

		this.listView.setEmptyView(waypointListPlaceholder);
        this.listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (listAdapter.getItemViewType(position) == listAdapter.ROW_TYPE_HEADER)
                    return;

                LocalWaypointDialog localWaypointDialog = LocalWaypointDialog.newInstance(position);
                getFragmentManager().beginTransaction().add(localWaypointDialog, "localWaypointDialog").commit();

            }
        });
        this.listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View v, int position, long id) {
                if (listAdapter.getItemViewType(position) == listAdapter.ROW_TYPE_HEADER)
                    return true;

                listView.showContextMenu(); //to show
                return true;
            }
        });


//		if (this.listAdapter.getCount() == 0)
//			this.listView.setVisibility(View.GONE);

	}


    private void requestWaypointGeocoder(Waypoint w, boolean force){
        if(w.getGeocoder() == null || force) {
            Log.v("handler", "request");

            GeocodableLocation l = new GeocodableLocation("Waypoint");
            l.setLatitude(w.getLatitude());
            l.setLongitude(w.getLongitude());
            l.setExtra(w);
            (new ReverseGeocodingTask(this, handler)).execute(l);
        }


    }

    @Override
    public void handleHandlerMessage(Message msg) {
        Log.v("handler", "handlehandlermessage");
        if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && ((GeocodableLocation)msg.obj).getExtra() instanceof  Waypoint) {
            Log.v("handler", "result");

            // Gets the geocoder from the returned array of [Geocoder, Waypoint] and assigns the geocoder to the waypoint
            Waypoint w = (Waypoint) ((GeocodableLocation)msg.obj).getExtra();
            w.setGeocoder(((GeocodableLocation)msg.obj).getGeocoder());
            this.dao.update(w);
            this.listAdapter.updateItem(w);
         }
    }


    @Override
	public void onDestroy() {
		ServiceProxy.runOrBind(this, new Runnable() {

            @Override
            public void run() {
                ServiceProxy.closeServiceConnection();

            }
        });
		super.onDestroy();
	}

    private void addWaypointToList(Waypoint w) {
        if(w.getType() == WAYPOINT_TYPE_LOCAL) {
            if(!localHeaderAdded) {
                this.listAdapter.addHeaderAtIndex("LOCAL", 0);
                localHeaderAdded = true;
            }

            this.listAdapter.addItemAtIndex(w, ++waypointLocalLastIdx);
        } else if(w.getType() == WAYPOINT_TYPE_LOCAL_MONITORING) {
            if(!localMonitoringHeaderAdded) {
                this.listAdapter.addHeaderAtIndex("MONITORING", waypointLocalMonitoringLastIdx + waypointLocalLastIdx);
                localMonitoringHeaderAdded = true;
            }
            this.listAdapter.addItemAtIndex(w, (waypointLocalMonitoringLastIdx+(++waypointLocalMonitoringLastIdx)));
        }
    }

    private void removeWaypointFromList(Waypoint w) {
        this.listAdapter.removeItem(w);

        if(w.getType() == WAYPOINT_TYPE_LOCAL) {
            waypointLocalLastIdx--;
            if(waypointLocalLastIdx == 0)
                this.listAdapter.removeItem(0);
        } else {
            waypointLocalMonitoringLastIdx--;
            if(waypointLocalMonitoringLastIdx == 0)
                this.listAdapter.removeItem(waypointLocalLastIdx + waypointLocalMonitoringLastIdx);
        }
    }

	protected void add(Waypoint w) {
        this.dao.insert(w);
        addWaypointToList(w);

        requestWaypointGeocoder(w, false);
        if(w.getType() == WAYPOINT_TYPE_LOCAL)
            EventBus.getDefault().post(new Events.WaypointAdded(w));

		if (this.listView.getVisibility() == View.GONE)
			this.listView.setVisibility(View.VISIBLE);

	}

    protected void update(Waypoint w) {
        this.dao.update(w);
        this.listAdapter.updateItem(w);
        EventBus.getDefault().post(new Events.WaypointUpdated(w));
        requestWaypointGeocoder(w, true);
    }


	protected void remove(Waypoint w) {
        this.dao.delete(w);
        this.removeWaypointFromList(w);
        EventBus.getDefault().post(new Events.WaypointRemoved(w));
	}

	public WaypointAdapter getListAdapter() {
		return this.listAdapter;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_waypoint, menu);
		return true;
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {

        if (v.getId()==R.id.waypoints) {
            menu.add(Menu.NONE, MENU_WAYPOINT_DELETE, 0, R.string.menuWaypointDelete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_WAYPOINT_DELETE:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                remove((Waypoint) this.listAdapter.getItem(info.position));
                break;
        }
        return true;

    }



    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add:
			LocalWaypointDialog LocalWaypointDialog = new LocalWaypointDialog();
			getFragmentManager().beginTransaction().add(LocalWaypointDialog, "LocalWaypointDialog")
					.commit();
			return true;
        case R.id.addMonitor:
                LocalWaypointMonitorDialog LocalWaypointMonitorDialog = new LocalWaypointMonitorDialog();
                getFragmentManager().beginTransaction().add(LocalWaypointMonitorDialog, "LocalWaypointMonitorDialog")
                        .commit();
                return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}


    public static class LocalWaypointDialog extends DialogFragment implements StaticHandlerInterface {
        private TextView description;
        private TextView latitude;
        private TextView longitude;
        private TextView radius;
        private Spinner transitionType;
        private CheckBox waypointNotificationOnEnterLeave;
        private TextView notificationMessage;
        private TextView currentLocationText;
        private LinearLayout currentLocationWrapper;
        private LinearLayout waypointGeofenceSettings;

        private static Handler handler;


        CheckBox share;
        GeocodableLocation location;

        Waypoint w;

        private void show(Waypoint w) {
			this.description.setText(w.getDescription());
			this.latitude.setText(w.getLatitude().toString());
			this.longitude.setText(w.getLongitude().toString());
			if (w.getRadius() != null)
				this.radius.setText(w.getRadius().toString());
            this.share.setChecked(w.getShared());

            setWaypointGeofenceSettingsVisibility(w.getRadius() != null);


			Log.v(this.toString(),
					"w.getTransitionType() " + w.getTransitionType());
			switch (w.getTransitionType()) {
			case Geofence.GEOFENCE_TRANSITION_ENTER:
				this.transitionType.setSelection(0);
				break;
			case Geofence.GEOFENCE_TRANSITION_EXIT:
				this.transitionType.setSelection(1);
				break;
			default:
				this.transitionType.setSelection(2);
				break;
			}

            notificationMessage.setText(w.getNotificationMessage());
            waypointNotificationOnEnterLeave.setChecked(w.getNotificationOnEnter() || w.getNotificationOnLeave()); // Both are always set for local waypoints

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

        public void onEventMainThread(Events.CurrentLocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
        }

        private void updateCurrentLocation(GeocodableLocation l, boolean updateGeocoderIfNotResolved){
            if(updateGeocoderIfNotResolved && l!= null && l.getGeocoder() == null) {
                (new ReverseGeocodingTask(getActivity(), handler)).execute(l);
            }

            ((ActivityWaypoints)getActivity()).currentLocation = l;
            currentLocationText.setText(l.toString());

        }

        @Override
        public void handleHandlerMessage(Message msg) {

            if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && (msg.obj != null))
                updateCurrentLocation((GeocodableLocation)msg.obj, false);
        }


        private void setWaypointGeofenceSettingsVisibility(boolean visible) {
            this.waypointGeofenceSettings.setVisibility(visible ? View.VISIBLE : View.GONE);

        }

		private View getContentView() {
			View view = getActivity().getLayoutInflater().inflate(
					R.layout.fragment_waypoint, null);

			this.description = (TextView) view.findViewById(R.id.description);
			this.latitude = (TextView) view.findViewById(R.id.latitude);
			this.longitude = (TextView) view.findViewById(R.id.longitude);
			this.radius = (TextView) view.findViewById(R.id.radius);
			this.transitionType = (Spinner) view
					.findViewById(R.id.transitionType);
			this.waypointNotificationOnEnterLeave = (CheckBox) view.findViewById(R.id.waypointNotificationOnEnterLeave);
			this.notificationMessage = (TextView) view.findViewById(R.id.notificationMessage);
            this.currentLocationWrapper = (LinearLayout) view.findViewById(R.id.currentLocationWrapper);
            this.currentLocationText = (TextView) view.findViewById(R.id.currentLocation);
            this.waypointGeofenceSettings = (LinearLayout) view.findViewById(R.id.waypointGeofenceSettings);
            this.share = (CheckBox) view.findViewById(R.id.share);

			if (this.w != null)
				show(this.w);

			TextWatcher requiredForSave = new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					conditionallyEnableSaveButton();
				}
			};

            TextWatcher requiredForGeofence = new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start,  int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start,  int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setWaypointGeofenceSettingsVisibility(radius.getText().length() > 0);
                }
            };

            this.description.addTextChangedListener(requiredForSave);
			this.latitude.addTextChangedListener(requiredForSave);
			this.longitude.addTextChangedListener(requiredForSave);
            this.radius.addTextChangedListener(requiredForGeofence);

			this.currentLocationWrapper.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
                    if (((ActivityWaypoints)getActivity()).currentLocation != null) {
                        LocalWaypointDialog.this.latitude.setText("" + ((ActivityWaypoints)getActivity()).currentLocation.getLatitude());
                        LocalWaypointDialog.this.longitude.setText("" + ((ActivityWaypoints)getActivity()).currentLocation.getLongitude());
                    } else {
                        Toast.makeText(getActivity(), "No current location is available", Toast.LENGTH_SHORT).show();
                    }
                }
            });


			return view;
		}

        public static LocalWaypointDialog newInstance(int position) {
			LocalWaypointDialog f = new LocalWaypointDialog();
			Bundle args = new Bundle();
			args.putInt(BUNDLE_KEY_POSITION, position);
			f.setArguments(args);
			return f;

		}

		private void conditionallyEnableSaveButton() {
			View v = getDialog().findViewById(android.R.id.button1);

			if (v == null)
				return;

			if ((this.description.getText().toString().length() > 0)
					&& (this.latitude.getText().toString().length() > 0)
					&& (this.longitude.getText().toString().length() > 0))
				v.setEnabled(true);
			else
				v.setEnabled(false);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {

			Bundle b;
			if (savedInstanceState != null)
				b = savedInstanceState;
			else
				b = getArguments();

			if (b != null)
				this.w = (Waypoint)((ActivityWaypoints) getActivity()).getListAdapter().getItem(b.getInt(BUNDLE_KEY_POSITION));

            handler = new StaticHandler(this);


            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
					.setTitle(getResources().getString( this.w == null ? R.string.waypointAdd : R.string.waypointEdit))
					.setView(getContentView())
					.setNegativeButton(getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {dismiss();	}
							})
					.setPositiveButton(getResources().getString(R.string.save),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int which) {
                            boolean update;
                            if (LocalWaypointDialog.this.w == null) {
                                LocalWaypointDialog.this.w = new Waypoint();
                                w.setType(WAYPOINT_TYPE_LOCAL);
                                update = false;
                            } else {
                                update = true;
                            }
                            LocalWaypointDialog.this.w.setDescription(LocalWaypointDialog.this.description.getText().toString());
                            try {
                                LocalWaypointDialog.this.w.setLatitude(Double.parseDouble(LocalWaypointDialog.this.latitude.getText().toString()));
                                LocalWaypointDialog.this.w.setLongitude(Double.parseDouble(LocalWaypointDialog.this.longitude.getText().toString()));
                            } catch (NumberFormatException e) {
                            }

                            try {
                                LocalWaypointDialog.this.w.setRadius(Float.parseFloat(LocalWaypointDialog.this.radius.getText().toString()));
                            } catch (NumberFormatException e) {
                                LocalWaypointDialog.this.w.setRadius(null);
                            }

                            LocalWaypointDialog.this.w.setShared(LocalWaypointDialog.this.share.isChecked());

                            switch (LocalWaypointDialog.this.transitionType.getSelectedItemPosition()) {
                                case 0:
                                    LocalWaypointDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER);
                                    LocalWaypointDialog.this.w.setNotificationOnEnter(LocalWaypointDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                                case 1:
                                    LocalWaypointDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);
                                    LocalWaypointDialog.this.w.setNotificationOnLeave(LocalWaypointDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                                default:
                                    LocalWaypointDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
                                    LocalWaypointDialog.this.w.setNotificationOnEnter(LocalWaypointDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    LocalWaypointDialog.this.w.setNotificationOnLeave(LocalWaypointDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                            }

                            LocalWaypointDialog.this.w.setNotificationMessage(LocalWaypointDialog.this.notificationMessage.getText().toString());


                            if (update)
                                ((ActivityWaypoints) getActivity()).update(LocalWaypointDialog.this.w);
                            else {
                                LocalWaypointDialog.this.w.setDate(new Date());
                                ((ActivityWaypoints) getActivity()).add(LocalWaypointDialog.this.w);
                            }

                            dismiss();
                        }
                    });

			Dialog dialog = builder.create();
			dialog.setOnShowListener(new OnShowListener() {

				@Override
				public void onShow(DialogInterface dialog) {
					conditionallyEnableSaveButton();

				}
			});
			return dialog;
		}

	}




    public static class LocalWaypointMonitorDialog extends DialogFragment implements StaticHandlerInterface {
        private TextView description;
        private TextView latitude;
        private TextView longitude;
        private TextView radius;
        private Spinner transitionType;
        private CheckBox waypointNotificationOnEnterLeave;
        private TextView notificationMessage;
        private TextView currentLocationText;
        private LinearLayout currentLocationWrapper;
        private TextView topic;

        private static Handler handler;


        CheckBox share;
        GeocodableLocation location;

        Waypoint w;

        private void show(Waypoint w) {
            this.description.setText(w.getDescription());
            this.latitude.setText(w.getLatitude().toString());
            this.longitude.setText(w.getLongitude().toString());
            if (w.getRadius() != null)
                this.radius.setText(w.getRadius().toString());
            this.share.setChecked(w.getShared());


            Log.v(this.toString(),
                    "w.getTransitionType() " + w.getTransitionType());
            switch (w.getTransitionType()) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    this.transitionType.setSelection(0);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    this.transitionType.setSelection(1);
                    break;
                default:
                    this.transitionType.setSelection(2);
                    break;
            }

            notificationMessage.setText(w.getNotificationMessage());
            waypointNotificationOnEnterLeave.setChecked(w.getNotificationOnEnter() || w.getNotificationOnLeave()); // Both are always set for local waypoints

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

        public void onEventMainThread(Events.CurrentLocationUpdated e) {
            updateCurrentLocation(e.getGeocodableLocation(), true);
        }

        private void updateCurrentLocation(GeocodableLocation l, boolean updateGeocoderIfNotResolved){
            if(updateGeocoderIfNotResolved && l!= null && l.getGeocoder() == null) {
                (new ReverseGeocodingTask(getActivity(), handler)).execute(l);
            }

            ((ActivityWaypoints)getActivity()).currentLocation = l;
            currentLocationText.setText(l.toString());

        }

        @Override
        public void handleHandlerMessage(Message msg) {

            if ((msg.what == ReverseGeocodingTask.GEOCODER_RESULT) && (msg.obj != null))
                updateCurrentLocation((GeocodableLocation)msg.obj, false);
        }




        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_waypoint, null);

            this.description = (TextView) view.findViewById(R.id.description);
            this.topic = (TextView) view.findViewById(R.id.monitorTopic);
            this.latitude = (TextView) view.findViewById(R.id.latitude);
            this.longitude = (TextView) view.findViewById(R.id.longitude);
            this.radius = (TextView) view.findViewById(R.id.radius);
            this.transitionType = (Spinner) view.findViewById(R.id.transitionType);
            this.waypointNotificationOnEnterLeave = (CheckBox) view.findViewById(R.id.waypointNotificationOnEnterLeave);
            this.notificationMessage = (TextView) view.findViewById(R.id.notificationMessage);
            this.currentLocationWrapper = (LinearLayout) view.findViewById(R.id.currentLocationWrapper);
            this.currentLocationText = (TextView) view.findViewById(R.id.currentLocation);

            if (this.w != null)
                show(this.w);

            TextWatcher requiredForSave = new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    conditionallyEnableSaveButton();
                }
            };

            this.description.addTextChangedListener(requiredForSave);
            this.latitude.addTextChangedListener(requiredForSave);
            this.longitude.addTextChangedListener(requiredForSave);
            this.radius.addTextChangedListener(requiredForSave);
            this.topic.addTextChangedListener(requiredForSave);

            this.currentLocationWrapper.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (((ActivityWaypoints)getActivity()).currentLocation != null) {
                        LocalWaypointMonitorDialog.this.latitude.setText("" + ((ActivityWaypoints)getActivity()).currentLocation.getLatitude());
                        LocalWaypointMonitorDialog.this.longitude.setText("" + ((ActivityWaypoints)getActivity()).currentLocation.getLongitude());
                    } else {
                        Toast.makeText(getActivity(), "No current location is available", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            return view;
        }

        public static LocalWaypointDialog newInstance(int position) {
            LocalWaypointDialog f = new LocalWaypointDialog();
            Bundle args = new Bundle();
            args.putInt(BUNDLE_KEY_POSITION, position);
            f.setArguments(args);
            return f;

        }

        private void conditionallyEnableSaveButton() {
            View v = getDialog().findViewById(android.R.id.button1);

            if (v == null)
                return;

            if ((this.description.getText().toString().length() > 0)
                    && (this.latitude.getText().toString().length() > 0)
                    && (this.longitude.getText().toString().length() > 0)
                    && (this.topic.getText().toString().length() > 0)
                    && (this.radius.getText().toString().length() > 0))

            v.setEnabled(true);
            else
                v.setEnabled(false);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Bundle b;
            if (savedInstanceState != null)
                b = savedInstanceState;
            else
                b = getArguments();

            if (b != null)
                this.w = (Waypoint)((ActivityWaypoints) getActivity()).getListAdapter().getItem(b.getInt(BUNDLE_KEY_POSITION));

            handler = new StaticHandler(this);


            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString( this.w == null ? R.string.waypointAdd : R.string.waypointEdit))
                    .setView(getContentView())
                    .setNegativeButton(getResources().getString(R.string.cancel),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {dismiss();	}
                    })
                    .setPositiveButton(getResources().getString(R.string.save),new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            boolean update;
                            if (LocalWaypointMonitorDialog.this.w == null) {
                                LocalWaypointMonitorDialog.this.w = new Waypoint();
                                w.setType(WAYPOINT_TYPE_LOCAL_MONITORING);
                                update = false;
                            } else {
                                update = true;
                            }
                            LocalWaypointMonitorDialog.this.w.setDescription(LocalWaypointMonitorDialog.this.description.getText().toString());
                            try {
                                LocalWaypointMonitorDialog.this.w.setLatitude(Double.parseDouble(LocalWaypointMonitorDialog.this.latitude.getText().toString()));
                                LocalWaypointMonitorDialog.this.w.setLongitude(Double.parseDouble(LocalWaypointMonitorDialog.this.longitude.getText().toString()));
                            } catch (NumberFormatException e) {
                            }

                            try {
                                LocalWaypointMonitorDialog.this.w.setRadius(Float.parseFloat(LocalWaypointMonitorDialog.this.radius.getText().toString()));
                            } catch (NumberFormatException e) {
                                LocalWaypointMonitorDialog.this.w.setRadius(null);
                            }

                            LocalWaypointMonitorDialog.this.w.setShared(LocalWaypointMonitorDialog.this.share.isChecked());

                            switch (LocalWaypointMonitorDialog.this.transitionType.getSelectedItemPosition()) {
                                case 0:
                                    LocalWaypointMonitorDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER);
                                    LocalWaypointMonitorDialog.this.w.setNotificationOnEnter(LocalWaypointMonitorDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                                case 1:
                                    LocalWaypointMonitorDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);
                                    LocalWaypointMonitorDialog.this.w.setNotificationOnLeave(LocalWaypointMonitorDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                                default:
                                    LocalWaypointMonitorDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
                                    LocalWaypointMonitorDialog.this.w.setNotificationOnEnter(LocalWaypointMonitorDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    LocalWaypointMonitorDialog.this.w.setNotificationOnLeave(LocalWaypointMonitorDialog.this.waypointNotificationOnEnterLeave.isChecked());
                                    break;
                            }

                            LocalWaypointMonitorDialog.this.w.setNotificationMessage(LocalWaypointMonitorDialog.this.notificationMessage.getText().toString());


                            if (update)
                                ((ActivityWaypoints) getActivity()).update(LocalWaypointMonitorDialog.this.w);
                            else {
                                LocalWaypointMonitorDialog.this.w.setDate(new Date());
                                ((ActivityWaypoints) getActivity()).add(LocalWaypointMonitorDialog.this.w);
                            }

                            dismiss();
                        }
                    });

            Dialog dialog = builder.create();
            dialog.setOnShowListener(new OnShowListener() {

                @Override
                public void onShow(DialogInterface dialog) {
                    conditionallyEnableSaveButton();

                }
            });
            return dialog;
        }

    }

}
