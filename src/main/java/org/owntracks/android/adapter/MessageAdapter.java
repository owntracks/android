package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.TextView;

import com.github.curioustechizen.ago.RelativeTimeTextView;

import org.owntracks.android.R;
import org.owntracks.android.db.MessageDao;


public class MessageAdapter extends LoaderSectionCursorAdapter {
    private int[] priorities = new int[3];

    public MessageAdapter(Context context) {
        super(context);

        priorities[0] = context.getResources().getColor(R.color.priority0);
        priorities[1] = context.getResources().getColor(R.color.priority1);
        priorities[2] = context.getResources().getColor(R.color.priority2);

    }


    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitle;
        public TextView mDescription;
        public IconTextView mIcon;
        public RelativeTimeTextView  mTime;
        public String objectId;
        public int position;

        public ItemViewHolder(View view) {
            super(view);
            mTitle = (TextView)view.findViewById(R.id.title);
            mDescription =  (TextView)view.findViewById(R.id.description);
            mIcon =  (IconTextView)view.findViewById(R.id.image);
            mTime =  (RelativeTimeTextView)view.findViewById(R.id.time);
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_message, parent, false);
        ItemViewHolder vh = new ItemViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position) {
        ((ItemViewHolder)viewHolder).mIcon.setText("{" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Icon.columnName)) + "}");
        ((ItemViewHolder)viewHolder).mIcon.setBackgroundColor(priorities[cursor.getInt(cursor.getColumnIndex(MessageDao.Properties.Priority.columnName))]);

        ((ItemViewHolder)viewHolder).mTitle.setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Title.columnName)));
        ((ItemViewHolder)viewHolder).mDescription.setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Description.columnName)));
        ((ItemViewHolder) viewHolder).mTime.setReferenceTime(cursor.getLong(cursor.getColumnIndex(MessageDao.Properties.Tst.columnName)) * 1000);
       //((ItemViewHolder) viewHolder).mTime.setPrefix("#" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Channel.columnName)) + ", ");
        ((ItemViewHolder) viewHolder).mTime.setSuffix(" in #" + cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Channel.columnName)));

        ((ItemViewHolder) viewHolder).objectId= cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Id.columnName));

    }

/*


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_message, null);
        IconTextView icon = (IconTextView) view.findViewById(R.id.image);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView description = (TextView) view.findViewById(R.id.description);
        RelativeTimeTextView time = (RelativeTimeTextView) view.findViewById(R.id.time);

        view.setTag(R.id.image, icon);
        view.setTag(R.id.title, title);
        view.setTag(R.id.description, description);
        view.setTag(R.id.time, time);

        return view;    }*/

  /*  @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((IconTextView) view.getTag(R.id.image)).setText("{"+cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Icon.columnName))+"}");
        ((IconTextView) view.getTag(R.id.image)).setBackgroundColor(priorities[cursor.getInt(cursor.getColumnIndex(MessageDao.Properties.Priority.columnName))]);

        ((TextView) view.getTag(R.id.title)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Title.columnName)));
        ((TextView) view.getTag(R.id.description)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Description.columnName)));
        ((RelativeTimeTextView) view.getTag(R.id.time)).setReferenceTime(cursor.getLong(cursor.getColumnIndex(MessageDao.Properties.Tst.columnName))*1000);

    }*/
}