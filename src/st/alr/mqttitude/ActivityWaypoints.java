
package st.alr.mqttitude;

import java.util.Date;

import com.google.android.gms.location.Geofence;

import st.alr.mqttitude.adapter.WaypointAdapter;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.services.ServiceProxy;
import st.alr.mqttitude.support.ReverseGeocodingTask;
import de.greenrobot.event.EventBus;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityWaypoints extends FragmentActivity {
    private ListView listView;
    private Menu menu;
    private WaypointAdapter listAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waypoint);

        listAdapter = new WaypointAdapter(this);
        
        listView = (ListView) findViewById(R.id.waypoints);        
        listView.addHeaderView(getLayoutInflater().inflate(R.layout.listview_header, null));
        listView.addFooterView(getLayoutInflater().inflate(R.layout.listview_footer, null));
        listView.setAdapter(listAdapter);

        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(multiChoiceListener);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                AddDialog addDialog = AddDialog.newInstance(position-1);
                getFragmentManager().beginTransaction().add(addDialog, "addDialog").commit();

            }
        });
        if(listAdapter.getCount() == 0) 
            listView.setVisibility(View.GONE);
        
     }
    


    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    protected void add(Waypoint w){
        listAdapter.add(w);
         
        if(listView.getVisibility() == View.GONE)
            listView.setVisibility(View.VISIBLE);
        
        
    }
    
    protected void update(Waypoint w){
        listAdapter.update(w);
    }
    
    
    protected void remove() {
        final SparseBooleanArray checkedItems = listView.getCheckedItemPositions();

        if (checkedItems != null) {
            final int checkedItemsCount = checkedItems.size();

            // Lets get the position of the view to scroll to before the first checked 
            // item to restore scroll position
            //          
            int topPosition = checkedItems.keyAt(0) - 1;            

            listView.setAdapter(null);
            for (int i = checkedItemsCount - 1; i >= 0 ; i--) {
                // This tells us the item position we are looking at
                // --
                final int position = checkedItems.keyAt(i);

                // This tells us the item status at the above position
                // --
                final boolean isChecked = checkedItems.valueAt(i);
                if (isChecked) {
                    listAdapter.remove(listAdapter.getItem(position-1));

                }               
            }
            listView.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
            //if topPosition is -1 then item zero is positioned by default.
            //listView.setSelection(topPosition);
        }

        
//        
//        
//        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
//
//        Log.v(this.toString(), "sba size " +checkedItemPositions.size());
//
//        Log.v(this.toString(), "count "  + listAdapter.getCount());
//
//        for (int i=1;i<=listAdapter.getCount();i++){
//            Log.v(this.toString(), "i " + i);
//            if (checkedItemPositions.get(i-1)==true)
//            {
//                Log.v(this.toString(), "removing index "  + (i-1));
//                listAdapter.remove(listAdapter.getItem(i-1));
//
//            } 
//
//        }
//        for (Integer i = 0; i < listAdapter.getCount(); i++) {
//
//            if (sba.valueAt(i)) {
//                Log.v(this.toString(), "removing " + i);
//
//                listAdapter.remove(listAdapter.getItem(i));
//            }
//        }
        if(listAdapter.getCount() == 0) 
            listView.setVisibility(View.GONE);
    }
    
    public WaypointAdapter getListAdapter() {
        return listAdapter;
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_waypoint, menu);
        this.menu = menu;
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
            final int checkedCount = listView.getCheckedItemCount();
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
            description.setText(w.getDescription());
            latitude.setText(w.getLatitude().toString());
            longitude.setText(w.getLongitude().toString());
            if(w.getRadius() != null)
                radius.setText(w.getRadius().toString());
            
            
            Log.v(this.toString(), "w.getTransitionType() " +w.getTransitionType() );
            switch (w.getTransitionType()) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    transitionType.setSelection(0);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    transitionType.setSelection(1);
                    break;
                 default:
                     transitionType.setSelection(2);
                    break;
            }

            
            notification.setChecked(w.getNotification());
            notification.setText(w.getNotificationTitle());                
            
            share.setChecked(w.getShared() != null && w.getShared() == true);
        }
        
        private View getContentView() {
            View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_waypoint, null);

            description = (TextView) view.findViewById(R.id.description);
            latitude =(TextView) view.findViewById(R.id.latitude);
            longitude = (TextView) view.findViewById(R.id.longitude);
            radius = (TextView) view.findViewById(R.id.radius);
            transitionType = (Spinner) view.findViewById(R.id.transitionType);
            notification =(CheckBox) view.findViewById(R.id.notification) ;
            notificationTitle = (TextView) view.findViewById(R.id.notificationTitle);
            useCurrent = (Button) view.findViewById(R.id.useCurrent);
            share = (CheckBox) view.findViewById(R.id.share);

            if(w != null)
                show(w);
            
            
            TextWatcher t = new TextWatcher() {
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    conditionallyEnableSaveButton();
                }
            };
            
            description.addTextChangedListener(t);
            latitude.addTextChangedListener(t);
            longitude.addTextChangedListener(t);
            
            useCurrent.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    GeocodableLocation l = ServiceProxy.getServiceLocator().getLastKnownLocation();
                    if(l != null) {
                        latitude.setText(l.getLatitude()+"");
                        longitude.setText(l.getLongitude()+"");                        
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

             if(v == null)
                return; 
                
            
            if((description.getText().toString().length() > 0) && (latitude.getText().toString().length() > 0) && (longitude.getText().toString().length() > 0)) 
                v.setEnabled(true);
            else
                v.setEnabled(false);                    
        }
            
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.v(this.toString(), "onCreateDialog");

            Bundle b;
            if (savedInstanceState != null)
                b = savedInstanceState;
            else
                b = getArguments();

            if(b != null)
                w = (Waypoint) ((ActivityWaypoints) getActivity()).getListAdapter().getItem(b.getInt("position"));
            
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getString(w == null? R.string.waypointAdd : R.string.waypointEdit))
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
                            if(w == null) {
                                Log.v(this.toString(), "adding waypoint");
                                w = new Waypoint();
                                update = false;
                            } else {
                                Log.v(this.toString(), "updating waypoint");
                                update = true;
                            }
                            w.setDescription(description.getText().toString());
                            try {
                                
                            w.setLatitude(Double.parseDouble(latitude.getText().toString()));
                            w.setLongitude(Double.parseDouble(longitude.getText().toString()));
                            } catch (NumberFormatException e) {}

                            w.setNotification(notification.isChecked());
                            w.setNotificationTitle(notificationTitle.getText().toString());
                            try {
                            w.setRadius(Float.parseFloat(radius.getText().toString()));
                            } catch (NumberFormatException e) {}
                            
                            switch (transitionType.getSelectedItemPosition()) {
                                case 0:
                                    w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER);
                                    break;
                                case 1:
                                    w.setTransitionType(Geofence.GEOFENCE_TRANSITION_EXIT);                                    
                                    break;
                                 default:
                                     w.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);
                                    break;
                            }
                            
                            w.setShared(share.isChecked());
                            
                            if(update)
                                ((ActivityWaypoints) getActivity()).update(w);
                            else {
                                w.setDate(new Date());
                                ((ActivityWaypoints) getActivity()).add(w);
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
