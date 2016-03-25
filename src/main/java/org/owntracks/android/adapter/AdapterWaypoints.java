package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.WaypointDao;
import org.w3c.dom.Text;

import java.util.Date;


public class AdapterWaypoints extends AdapterCursorLoader {
    private String labelGeofence;
    private String labelBeacon = "Beacon active";
    private String labelGeofenceAndBeacon = "Geofence and beacon active";
    private String labelNothing = "Geofence and beacon inactive";

    public AdapterWaypoints(Context context) {
        super(context);
        labelGeofence = context.getString(R.string.geofenceActive);
        labelBeacon = context.getString(R.string.beaconActive);
        labelGeofenceAndBeacon = context.getString(R.string.geofenceAndBeaconActive);
        labelNothing = context.getString(R.string.geofenceAndBeaconInactive);

    }


    public static class ItemViewHolder extends ClickableViewHolder {
        public TextView mTitle;
        public TextView mText;
        public TextView  mMeta;

        public ItemViewHolder(View view) {
            super(view);
            mTitle = (TextView)view.findViewById(R.id.title);
            mText =  (TextView)view.findViewById(R.id.text);
            mMeta =  (TextView)view.findViewById(R.id.meta);
        }
    }


    @Override
    public ClickableViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_waypoint, parent, false);
        ItemViewHolder vh = new ItemViewHolder(itemView);
        return vh;
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position) {
        ((ItemViewHolder)viewHolder).mTitle.setText(cursor.getString(cursor.getColumnIndex(WaypointDao.Properties.Description.columnName)));

        //((ItemViewHolder) viewHolder).mDetails.setReferenceTime(cursor.getLong(cursor.getColumnIndex(WaypointDao.Properties.LastTriggered.columnName)) * 1000);
        //((ItemViewHolder) viewHolder).mTime.setPrefix("#" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Channel.columnName)) + ", ");
        boolean geofence = cursor.getInt(cursor.getColumnIndex(WaypointDao.Properties.GeofenceRadius.columnName)) > 0;
        String uuid = cursor.getString(cursor.getColumnIndex(WaypointDao.Properties.BeaconUUID.columnName));
        boolean beaconUUID = uuid != null && uuid.length() > 0;
        Log.v("AdapterWaypoints", "geofence " +geofence +  " uuid " + beaconUUID);

        if(geofence || beaconUUID) {
            long lastTriggered = cursor.getLong(cursor.getColumnIndex(WaypointDao.Properties.LastTriggered.columnName));

            if (geofence && !beaconUUID) {
                ((ItemViewHolder) viewHolder).mText.setText(labelGeofence);
            } else if (!geofence) {
                ((ItemViewHolder) viewHolder).mText.setText(labelBeacon);
            } else {
                ((ItemViewHolder) viewHolder).mText.setText(labelGeofenceAndBeacon);
            }

            if(lastTriggered != 0) {
                ((ItemViewHolder) viewHolder).mMeta.setText(App.formatDate(new Date(cursor.getLong(cursor.getColumnIndex(WaypointDao.Properties.LastTriggered.columnName)))));
                ((ItemViewHolder) viewHolder).mMeta.setVisibility(View.VISIBLE);

            } else {
                ((ItemViewHolder) viewHolder).mMeta.setVisibility(View.GONE);
            }

        } else {
            ((ItemViewHolder) viewHolder).mText.setText(labelNothing);
            ((ItemViewHolder) viewHolder).mMeta.setVisibility(View.GONE);

        }
    }
}