package org.owntracks.android.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.owntracks.android.R;


public abstract class AdapterCursorLoader extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "AdapterCursorLoader";
    private static final String ID_COLUMN = "_id";
    private OnViewHolderClickListener onViewHolderClickListener;

    public static class ClickableViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public View rootView;
        private OnViewHolderClickListener<ClickableViewHolder> onClickListener;

        private Drawable defaultBackground;

        public ClickableViewHolder(View view) {
            super(view);
            this.rootView = view;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setSelected(boolean selected) {
            if(selected && !this.rootView.isSelected()) {
                defaultBackground = this.rootView.getBackground();
                this.rootView.setBackgroundResource(R.drawable.selectable_row);
                this.rootView.setSelected(true);
            } else if(!selected && this.rootView.isSelected()) {
                defaultBackground.setVisible(false, false);
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.setBackgroundDrawable(defaultBackground);
                } else {
                    rootView.setBackground(defaultBackground);
                }

                this.rootView.setSelected(false);
                defaultBackground.setVisible(true, false);
            }
        }

        @Override
        public void onClick(View v) {
            if(onClickListener != null)
                this.onClickListener.onViewHolderClick(v, this);

        }

        @Override
        public boolean onLongClick(View v) {
            return onClickListener != null && this.onClickListener.onViewHolderLongClick(v, this);
        }

        public void setOnViewHolderClickListener(OnViewHolderClickListener listener) {
            this.onClickListener = listener;
        }


    }


    private Cursor mCursor;
    private boolean mDataValid;


    AdapterCursorLoader(Context context) {
        mCursor = null;
        mDataValid = false;
        setHasStableIds(true);
    }



    @Override
    public int getItemCount() {
        if (mDataValid && mCursor != null) {
            return mCursor.getCount();
        }

        return 0;
    }

    private Cursor getCursor() {
        return mCursor;
    }

    protected abstract void onBindViewHolder(RecyclerView.ViewHolder viewHolder, Cursor cursor, int position);


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

    protected abstract ClickableViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType);

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ClickableViewHolder v = onCreateItemViewHolder(parent,viewType);
        if(onViewHolderClickListener != null) {
            v.setOnViewHolderClickListener(onViewHolderClickListener);
        }

        return v;
    }

    public interface OnViewHolderClickListener<T extends ClickableViewHolder> {
        void onViewHolderClick(View rootView, T viewHolder);
        boolean onViewHolderLongClick(View rootView, T viewHolder);

    }


    public void setOnViewHolderClickListener(OnViewHolderClickListener l) {
        this.onViewHolderClickListener = l;
    }


    @Override
    public long getItemId(int position) {
        Cursor cursor = getCursor();
        getCursor().moveToPosition(position);
        return cursor.getLong(getCursor().getColumnIndex(ID_COLUMN));
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