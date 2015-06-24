package org.owntracks.android.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.owntracks.android.R;
import org.owntracks.android.db.Message;
import org.owntracks.android.db.MessageDao;

import java.security.Key;
import java.util.HashMap;


public abstract class LoaderSectionCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "LoaderSectionCursorAdap";

   // private static final int TYPE_HEADER =     private static final int TYPE_COUNT = 2;
    private SparseArray<String> mSectionsIndexer;

    public static class SectionViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public SectionViewHolder(View view) {
            super(view);
            mTextView = (TextView)view.findViewById(R.id.section_text);
        }
    }

    private Cursor mCursor;
    private boolean mDataValid;


    public LoaderSectionCursorAdapter(Context context) {

        mCursor = null;
        mDataValid = false;
    }



    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }

        return 0;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public abstract void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position);


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }

        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        onBindViewHolder(viewHolder, mCursor, position);
    }

    public abstract RecyclerView.ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return onCreateItemViewHolder(parent,viewType);
    }



    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public void changeCursor(Cursor cursor) {
        final Cursor old = swapCursor(cursor);

        if (old != null) {
            old.close();
        }
    }


    /**
     * Swap in a new Cursor, returning the old Cursor.  Unlike
     * {@link #changeCursor(Cursor)}, the returned old Cursor is <em>not</em>
     * closed.
     */

    public void itemAdded(Message m) {

    }

    public Cursor swapCursor(Cursor newCursor) {
        if (newCursor == mCursor) {
            return null;
        }
        final Cursor oldCursor = mCursor;
        mCursor = newCursor;

        if (mCursor != null) {
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            mDataValid = false;
            notifyDataSetChanged();
        }
        return oldCursor;
    }
}