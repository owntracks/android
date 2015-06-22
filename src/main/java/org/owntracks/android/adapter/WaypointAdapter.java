package org.owntracks.android.adapter;

import java.util.ArrayList;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.model.Contact;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WaypointAdapter extends MultitypeAdapter {
    private static final String TAG = "WaypointAdapter";

    public WaypointAdapter(Context context) {
        super(context);
    }

    public WaypointAdapter(Context context, ArrayList<Waypoint> waypoints) {
        super(context);

        set(waypoints);
    }
    public DelegateAdapter getItemDelegateAdapter() {
        return new ItemDelegateAdapter();
    }


    public void set(ArrayList<Waypoint> waypoints) {
        if(waypoints != null)
            for (Waypoint w : waypoints)
                addItem(w);
    }

    public static class WaypointHolder {
        TextView description;
        TextView location;
        TextView details;
    }


    public class ItemDelegateAdapter implements DelegateAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Object item) {
            WaypointHolder holder;
            if (convertView == null) {
                holder = new WaypointHolder();
                convertView = inflater.inflate(R.layout.row_waypoint, null);
                holder.description = (TextView)convertView.findViewById(R.id.description);
                holder.location = (TextView)convertView.findViewById(R.id.location);
                holder.details = (TextView)convertView.findViewById(R.id.details);
            } else {
                holder = (WaypointHolder)convertView.getTag();
            }
            convertView.setTag(holder);

            holder.description.setText(((Waypoint) item).getDescription());
            holder.location.setText(((Waypoint) item).getGeocoder() != null ? ((Waypoint) item).getGeocoder()  :(((Waypoint) item).getLatitude() + " : " + ((Waypoint) item).getLongitude()));
            if(((Waypoint) item).getRadius() != null && ((Waypoint) item).getRadius()  > 0) {
                holder.details.setVisibility(View.VISIBLE);
                if(((Waypoint) item).getLastTriggered() != null) {
                    holder.details.setText(context.getString(R.string.geofenceLastTransition) + " " +  App.formatDate(((Waypoint) item).getLastTriggered()));

                } else {
                    holder.details.setText(context.getString(R.string.geofenceActive));
                }
            }else {
                holder.details.setVisibility(View.GONE);
            }

            return convertView;
        }

    }
}
