package org.owntracks.android.adapter;


import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.IconTextView;
import android.widget.TextView;

import com.github.monxalo.android.widget.SectionCursorAdapter;

import org.owntracks.android.R;
import org.owntracks.android.db.MessageDao;


public class MessageAdapter extends SectionCursorAdapter {
    private int[] priorities = new int[3];

    public MessageAdapter(Context context, Cursor c, int headerLayout, int groupColumn) {
        super(context, c, headerLayout, groupColumn);

        priorities[0] = context.getResources().getColor(R.color.priority0);
        priorities[1] = context.getResources().getColor(R.color.priority1);
        priorities[2] = context.getResources().getColor(R.color.priority2);

    }



    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_pager, null);
        IconTextView ic = (IconTextView) view.findViewById(R.id.image);
        TextView tv = (TextView) view.findViewById(R.id.title);
        TextView iv = (TextView) view.findViewById(R.id.subtitle);
        view.setTag(R.id.image, ic);
        view.setTag(R.id.title, tv);
        view.setTag(R.id.subtitle, iv);

        return view;    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((IconTextView) view.getTag(R.id.image)).setText("{"+cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Icon.columnName))+"}");
        ((IconTextView) view.getTag(R.id.image)).setBackgroundColor(priorities[cursor.getInt(cursor.getColumnIndex(MessageDao.Properties.Priority.columnName))]);

        ((TextView) view.getTag(R.id.title)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Title.columnName)));
        ((TextView) view.getTag(R.id.subtitle)).setText(cursor.getString(cursor.getColumnIndex(MessageDao.Properties.Description.columnName)));
    }
}