package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;

import org.owntracks.android.R;
import org.owntracks.android.db.MessageDao;


public class MessageAdapter extends AdapterCursorLoader {
    private static final String TAG = "MessageAdapter";
    private int[] priorities = new int[3];

    public MessageAdapter(Context context) {
        super(context);

        priorities[0] = R.drawable.circle_priority0;
        priorities[1] = R.drawable.circle_priority1;
        priorities[2] = R.drawable.circle_priority2;

    }



    public static class ItemViewHolder extends ClickableViewHolder {
        public TextView mTitle;
        public TextView mDescription;
        public IconTextView mIcon;
        public RelativeTimeTextView  mTime;
        public String objectId;
        public String url;
        public View mUrlIndicator;
        public int position;

        public ItemViewHolder(View view) {
            super(view);
            mTitle = (TextView)view.findViewById(R.id.title);
            mDescription =  (TextView)view.findViewById(R.id.description);
            mIcon =  (IconTextView)view.findViewById(R.id.image);
            mTime =  (RelativeTimeTextView)view.findViewById(R.id.time);
            mUrlIndicator = (IconTextView)view.findViewById(R.id.mUrlIndicator);
        }
    }


    @Override
    public ClickableViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_message, parent, false);
        ItemViewHolder vh = new ItemViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position) {
        ((ItemViewHolder) viewHolder).mIcon.setText("{" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Icon.columnName)) + "}");

        ((ItemViewHolder) viewHolder).mIcon.setBackgroundResource(priorities[cursor.getInt(cursor.getColumnIndex(MessageDao.Properties.Priority.columnName))]);
        ((ItemViewHolder) viewHolder).mTitle.setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Title.columnName)));
        ((ItemViewHolder) viewHolder).mDescription.setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Description.columnName)));
        ((ItemViewHolder) viewHolder).mTime.setReferenceTime(cursor.getLong(cursor.getColumnIndex(MessageDao.Properties.Tst.columnName)) * 1000);
       //((ItemViewHolder) viewHolder).mTime.setPrefix("#" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Channel.columnName)) + ", ");
        ((ItemViewHolder) viewHolder).mTime.setSuffix(" in #" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Channel.columnName)));

        ((ItemViewHolder) viewHolder).url= cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Url.columnName));
        if(((ItemViewHolder) viewHolder).url != null) {
            ((ItemViewHolder) viewHolder).mUrlIndicator.setVisibility(View.VISIBLE);
        }
    }
}