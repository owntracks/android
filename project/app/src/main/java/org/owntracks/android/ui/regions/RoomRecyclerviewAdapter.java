package org.owntracks.android.ui.regions;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.owntracks.android.R;
import org.owntracks.android.db.room.WaypointModel;
import org.owntracks.android.support.widgets.BindingConversions;

import java.util.List;
import java.util.Locale;

public class RoomRecyclerviewAdapter extends RecyclerView.Adapter<RoomRecyclerviewAdapter.RecyclerViewHolder> implements Observer<List<WaypointModel>>{

    private List<WaypointModel> waypointList;
    private View.OnClickListener clickListener;
    private View.OnLongClickListener longClickListener;

    public RoomRecyclerviewAdapter(List<WaypointModel> borrowModelList, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        this.waypointList = borrowModelList;
        this.longClickListener = longClickListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.ui_row_waypoint, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RoomRecyclerviewAdapter.RecyclerViewHolder holder, int position) {
        WaypointModel model = waypointList.get(position);

        holder.itemView.setTag(model);
        holder.itemView.setOnLongClickListener(longClickListener);
        holder.itemView.setOnClickListener(clickListener);

        holder.title.setText(model.getDescription());
        holder.subtitle.setText(String.format(Locale.getDefault(), "Timestamp: %s", model.getId()));
        if(model.getLastTransition() > 0)
            BindingConversions.setRelativeTimeSpanString(holder.meta,model.getLastTransition());
    }

    @Override
    public int getItemCount() {
        return waypointList.size();
    }

    @Override
    public void onChanged(@Nullable List<WaypointModel> list) {
        this.waypointList = list;
        notifyDataSetChanged();
    }

    public void addItems(List<WaypointModel> borrowModelList) {
    }

    static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView subtitle;
        private TextView meta;

        RecyclerViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title);
            subtitle = (TextView) view.findViewById(R.id.text);
            meta = (TextView) view.findViewById(R.id.meta);
        }
    }
}