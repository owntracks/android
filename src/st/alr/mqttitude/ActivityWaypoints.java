
package st.alr.mqttitude;

import java.util.Date;

import st.alr.mqttitude.adapter.WaypointAdapter;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.services.ServiceProxy;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
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
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

public class ActivityWaypoints extends FragmentActivity {
    private ListView listView;
    private WaypointAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        this.listAdapter = new WaypointAdapter(this);

        this.listView = (ListView) findViewById(R.id.waypoints);
        this.listView.addHeaderView(getLayoutInflater().inflate(R.layout.listview_header, null));
        this.listView.addFooterView(getLayoutInflater().inflate(R.layout.listview_footer, null));
        this.listView.setAdapter(this.listAdapter);

        this.listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        this.listView.setMultiChoiceModeListener(this.multiChoiceListener);
        this.listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                AddDialog addDialog = AddDialog.newInstance(position - 1);
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();

            }
        });
        if (this.listAdapter.getCount() == 0)
            this.listView.setVisibility(View.GONE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void add(Waypoint w) {
        this.listAdapter.add(w);

        if (this.listView.getVisibility() == View.GONE)
            this.listView.setVisibility(View.VISIBLE);

    }

    protected void update(Waypoint w) {
        this.listAdapter.update(w);
    }

    protected void remove() {
        final SparseBooleanArray checkedItems = this.listView.getCheckedItemPositions();

        if (checkedItems != null) {
            final int checkedItemsCount = checkedItems.size();

            this.listView.setAdapter(null);
            for (int i = checkedItemsCount - 1; i >= 0; i--) {
                final int position = checkedItems.keyAt(i);

                final boolean isChecked = checkedItems.valueAt(i);
                if (isChecked) {
                    this.listAdapter.remove(this.listAdapter.getItem(position - 1));

                }
            }
            this.listView.setAdapter(this.listAdapter);
            this.listAdapter.notifyDataSetChanged();
        }
        if (this.listAdapter.getCount() == 0)
            this.listView.setVisibility(View.GONE);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                AddDialog addDialog = new AddDialog();
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private MultiChoiceModeListener multiChoiceListener = new MultiChoiceModeListener() {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            final int checkedCount = ActivityWaypoints.this.listView.getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setTitle(null);
                    break;
                case 1:
                    mode.setTitle(getResources().getString(R.string.actionModeOneSelected));
                    break;
                default:
                    mode.setTitle(checkedCount + " " + getResources().getString(R.string.actionModeMoreSelected));
                    break;
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.discard:
                    Log.v(this.toString(), "discard");
                    remove();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.activity_waypoint_actionmode, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    public static class AddDialog extends DialogFragment {
        TextView description;
        TextView latitude;
        TextView longitude;
        TextView radius;
        Spinner transitionType;
        CheckBox notification;
        TextView notificationTitle;
        Button useCurrent;
        CheckBox share;
        Waypoint w;

        private void show(Waypoint w) {
            this.description.setText(w.getDescription());
            this.latitude.setText(w.getLatitude().toString());
            this.longitude.setText(w.getLongitude().toString());
            if (w.getRadius() != null)
                this.radius.setText(w.getRadius().toString());

            Log.v(this.toString(), "w.getTransitionType() " + w.getTransitionType());
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

            this.notification.setChecked(w.getNotification());
            this.notification.setText(w.getNotificationTitle());

            this.share.setChecked((w.getShared() != null) && (w.getShared() == true));
        }

        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_waypoint, null);

            this.description = (TextView) view.findViewById(R.id.description);
            this.latitude = (TextView) view.findViewById(R.id.latitude);
            this.longitude = (TextView) view.findViewById(R.id.longitude);
            this.radius = (TextView) view.findViewById(R.id.radius);
            this.transitionType = (Spinner) view.findViewById(R.id.transitionType);
            this.notification = (CheckBox) view.findViewById(R.id.notification);
            this.notificationTitle = (TextView) view.findViewById(R.id.notificationTitle);
            this.useCurrent = (Button) view.findViewById(R.id.useCurrent);
            this.share = (CheckBox) view.findViewById(R.id.share);

            if (this.w != null)
                show(this.w);

            TextWatcher t = new TextWatcher() {

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    conditionallyEnableSaveButton();
                }
            };

            this.description.addTextChangedListener(t);
            this.latitude.addTextChangedListener(t);
            this.longitude.addTextChangedListener(t);

            this.useCurrent.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                    if (l != null) {
                        AddDialog.this.latitude.setText(l.getLatitude() + "");
                        AddDialog.this.longitude.setText(l.getLongitude() + "");
                    } else {
                        Toast.makeText(getActivity(), "No current location is available", Toast.LENGTH_SHORT)
                                .show();
                    }

                }
            });

            return view;
        }

        public static AddDialog newInstance(int position) {
            AddDialog f = new AddDialog();
            Bundle args = new Bundle();
            args.putInt("position", position);
            f.setArguments(args);
            return f;

        }

        private void conditionallyEnableSaveButton() {
            View v = getDialog().findViewById(android.R.id.button1);

            if (v == null)
                return;

            if ((this.description.getText().toString().length() > 0) && (this.latitude.getText().toString().length() > 0) && (this.longitude.getText().toString().length() > 0))
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
                this.w = ((ActivityWaypoints) getActivity()).getListAdapter().getItem(b.getInt("position"));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(this.w == null ? R.string.waypointAdd : R.string.waypointEdit))
                    .setView(getContentView())
                    .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.save), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            boolean update;
                            if (AddDialog.this.w == null) {
                                AddDialog.this.w = new Waypoint();
                                update = false;
                            } else {
                                update = true;
                            }
                            AddDialog.this.w.setDescription(AddDialog.this.description.getText().toString());
                            try {

                                AddDialog.this.w.setLatitude(Double.parseDouble(AddDialog.this.latitude.getText().toString()));
                                AddDialog.this.w.setLongitude(Double.parseDouble(AddDialog.this.longitude.getText().toString()));
                            } catch (NumberFormatException e) {
                            }

                            AddDialog.this.w.setNotification(AddDialog.this.notification.isChecked());
                            AddDialog.this.w.setNotificationTitle(AddDialog.this.notificationTitle.getText().toString());
                            try {
                                AddDialog.this.w.setRadius(Float.parseFloat(AddDialog.this.radius.getText().toString()));
                            } catch (NumberFormatException e) {
                            }

                            switch (AddDialog.this.transitionType.getSelectedItemPosition()) {
                                case 0:
                                    AddDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER);
                                    break;
                                case 1:
                                    AddDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);
                                    break;
                                default:
                                    AddDialog.this.w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
                                    break;
                            }

                            AddDialog.this.w.setShared(AddDialog.this.share.isChecked());

                            if (update)
                                ((ActivityWaypoints) getActivity()).update(AddDialog.this.w);
                            else {
                                AddDialog.this.w.setDate(new Date());
                                ((ActivityWaypoints) getActivity()).add(AddDialog.this.w);
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
