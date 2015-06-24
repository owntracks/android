package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
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

    public MessageAdapter(Context context, int headerLayout, String groupColumn) {
        super(context, headerLayout, groupColumn);

        priorities[0] = context.getResources().getColor(R.color.priority0);
        priorities[1] = context.getResources().getColor(R.color.priority1);
        priorities[2] = context.getResources().getColor(R.color.priority2);

    }



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

        return view;    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((IconTextView) view.getTag(R.id.image)).setText("{"+cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Icon.columnName))+"}");
        ((IconTextView) view.getTag(R.id.image)).setBackgroundColor(priorities[cursor.getInt(cursor.getColumnIndex(MessageDao.Properties.Priority.columnName))]);

        ((TextView) view.getTag(R.id.title)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Title.columnName)));
        ((TextView) view.getTag(R.id.description)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Description.columnName)));
        ((RelativeTimeTextView) view.getTag(R.id.time)).setReferenceTime(cursor.getLong(cursor.getColumnIndex(MessageDao.Properties.Tst.columnName))*1000);

    }
}