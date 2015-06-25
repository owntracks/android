package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;

import org.owntracks.android.App;
import org.owntracks.android.R;
import org.owntracks.android.db.MessageDao;
import org.owntracks.android.db.Waypoint;
import org.owntracks.android.db.WaypointDao;
import org.owntracks.android.model.GeocodableLocation;
import org.owntracks.android.support.ReverseGeocodingTask;
import org.owntracks.android.support.StaticHandler;
import org.owntracks.android.support.StaticHandlerInterface;


public class AdapterWaypoints extends AdapterCursorLoader {

    public AdapterWaypoints(Context context) {
        super(context);
    }


    public static class ItemViewHolder extends ClickableViewHolder {
        public TextView mTitle;
        public TextView mText;
        public RelativeTimeTextView  mMeta;

        public ItemViewHolder(View view) {
            super(view);
            mTitle = (TextView)view.findViewById(R.id.title);
            mText =  (TextView)view.findViewById(R.id.text);
            mMeta =  (RelativeTimeTextView)view.findViewById(R.id.meta);
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
        boolean geofence = cursor.getInt(cursor.getColumnIndex(WaypointDao.Properties.Radius.columnName)) > 0;

        if(geofence) {
            long lastTriggered = cursor.getLong(cursor.getColumnIndex(WaypointDao.Properties.LastTriggered.columnName));
            ((ItemViewHolder) viewHolder).mText.setText("Geofence active");

            if(lastTriggered != 0) {
                ((ItemViewHolder) viewHolder).mMeta.setReferenceTime(cursor.getLong(cursor.getColumnIndex(WaypointDao.Properties.LastTriggered.columnName)));
                ((ItemViewHolder) viewHolder).mMeta.setPrefix("Last transition: ");

            } else {
                ((ItemViewHolder) viewHolder).mMeta.setText("Last transition: never");
            }
            ((ItemViewHolder) viewHolder).mMeta.setVisibility(View.VISIBLE);

        } else {
            ((ItemViewHolder) viewHolder).mText.setText("Geofence inactive");
            ((ItemViewHolder) viewHolder).mMeta.setVisibility(View.GONE);
        }
    }
}