package org.owntracks.android.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;



public abstract class LoaderSectionCursorAdapter extends CursorAdapter {

    private static final String TAG = "SectionCursorAdapter";
    private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

    private static final int TYPE_NORMAL = 1;
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_COUNT = 2;

    private final int mHeaderRes;
    private String mGroupColumnName;

    private final LayoutInflater mLayoutInflater;

    private SparseArray<String> mSectionsIndexer;

    public static class ViewHolder {
        public TextView textView;
    }

    public LoaderSectionCursorAdapter(Context context, int headerLayout, String groupName) {
        super(context, null, 0);

        mSectionsIndexer = new SparseArray<String>();

        mHeaderRes = headerLayout;
        mGroupColumnName = groupName;
        mLayoutInflater = LayoutInflater.from(context);
    }

    private DataSetObserver mDataSetObserver = new DataSetObserver() {
        public void onChanged() {
            calculateSectionHeaders();
        }

        ;

        public void onInvalidated() {
            mSectionsIndexer.clear();
        }

        ;
    };

    /**
     * <p>This method serve as an intercepter before the sections are calculated so you can transform some computer data into human readable,
     * e.g. format a unix timestamp, or a status.</p>
     * <p/>
     * <p>By default this method returns the original data for the group column.</p>
     *
     * @param groupData
     * @return
     */
    protected String getCustomGroup(String groupData) {
        return groupData;
    }

    private void calculateSectionHeaders() {

        int i = 0;

        String previous = "";
        int count = 0;

        final Cursor c = getCursor();

        mSectionsIndexer.clear();

        if (c == null || c.isClosed()) {
            return;
        }

        c.moveToPosition(-1);

        while (c.moveToNext()) {
            // CHANGE: Loads group column index dynamically from cursor
            final String group = getCustomGroup(c.getString(getCursor().getColumnIndex(mGroupColumnName)));

            if (!previous.equals(group)) {
                mSectionsIndexer.put(i + count, group);
                previous = group;

                if (LOGV)
                    Log.v(TAG, "Group " + group + "at position: " + (i + count));

                count++;
            }

            i++;
        }
    }

    public String getGroupCustomFormat(Object obj) {
        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int viewType = getItemViewType(position);

        if (viewType == TYPE_NORMAL) {
            Cursor c = (Cursor) getItem(position);

            if (c == null) {
                if (LOGV) Log.d(TAG, "getItem(" + position + ") = null");
                return mLayoutInflater.inflate(mHeaderRes, parent, false);
            }

            final int mapCursorPos = getSectionForPosition(position);
            c.moveToPosition(mapCursorPos);

            return super.getView(mapCursorPos, convertView, parent);
        } else {

            ViewHolder holder = null;

            if (convertView == null) {
                if (LOGV)
                    Log.v(TAG, "Creating new view for section");

                holder = new ViewHolder();
                convertView = mLayoutInflater.inflate(mHeaderRes, parent, false);
                holder.textView = (TextView) convertView;

                convertView.setTag(holder);
            } else {
                if (LOGV)
                    Log.v(TAG, "Reusing view for section");

                holder = (ViewHolder) convertView.getTag();
            }

            TextView sectionText = holder.textView;

            final String group = mSectionsIndexer.get(position);
            final String customFormat = getGroupCustomFormat(group);

            sectionText.setText(customFormat == null ? group : customFormat);

            return sectionText;
        }
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getCount() {
        return super.getCount() + mSectionsIndexer.size();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == TYPE_NORMAL;
    }

    public int getPositionForSection(int section) {
        if (mSectionsIndexer.get(section, null) != null) {
            return section + 1;
        }
        return section;
    }

    public int getSectionForPosition(int position) {
        int offset = 0;

        for (int i = 0; i < mSectionsIndexer.size(); i++) {
            int key = mSectionsIndexer.keyAt(i);

            if (position > key) {
                offset++;
            } else {
                break;
            }
        }

        return position - offset;
    }

    @Override
    public Object getItem(int position) {
        if (getItemViewType(position) == TYPE_NORMAL) {
            return super.getItem(getSectionForPosition(position));
        }
        return super.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_NORMAL) {
            return super.getItemId(getSectionForPosition(position));
        }
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getPositionForSection(position)) {
            return TYPE_NORMAL;
        }
        return TYPE_HEADER;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        final Cursor old = swapCursor(cursor);

        if (old != null) {
            old.close();
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        if (getCursor() != null) {
            getCursor().unregisterDataSetObserver(mDataSetObserver);
        }

        final Cursor oldCursor = super.swapCursor(newCursor);

        calculateSectionHeaders();

        if (newCursor != null) {
            newCursor.registerDataSetObserver(mDataSetObserver);
        }

        return oldCursor;
    }
}