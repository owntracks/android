package st.alr.mqttitude.adapter;

import java.util.ArrayList;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;

import de.greenrobot.event.EventBus;

public class WaypointAdapter extends BaseAdapter {
	private WaypointDao dao;
	private Activity context;
	private ArrayList<Waypoint> list;

	public WaypointAdapter(Activity c) {
		super();
		this.context = c;
		this.dao = App.getWaypointDao();
		this.list = new ArrayList<Waypoint>(this.dao.loadAll());
		notifyDataSetChanged();
	}

	static class ViewHolder {

		public TextView description;
		public TextView location;
		public ImageView enter;
		public ImageView leave;

	}

	@Override
	public int getCount() {
		return this.list.size();
	}

	@Override
	public Waypoint getItem(int position) {
		return this.list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = this.context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.row_waypoint, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.description = (TextView) rowView
					.findViewById(R.id.description);
			viewHolder.location = (TextView) rowView
					.findViewById(R.id.location);
			viewHolder.enter = (ImageView) rowView.findViewById(R.id.enter);
			viewHolder.leave = (ImageView) rowView.findViewById(R.id.leave);

			rowView.setTag(viewHolder);
		}

		Waypoint w = getItem(position);

		ViewHolder holder = (ViewHolder) rowView.getTag();
		holder.description.setText(w.getDescription());
		holder.location.setText(w.getLatitude() + ":" + w.getLongitude());

		if ((w.getRadius() != null) && (w.getRadius() > 0)) {

			if ((w.getTransitionType() & Geofence.GEOFENCE_TRANSITION_ENTER) != 0)
				holder.enter.setVisibility(View.VISIBLE);
			else
				holder.enter.setVisibility(View.GONE);

			if ((w.getTransitionType() & Geofence.GEOFENCE_TRANSITION_EXIT) != 0)
				holder.leave.setVisibility(View.VISIBLE);
			else
				holder.leave.setVisibility(View.GONE);

		} else {
			holder.enter.setVisibility(View.GONE);
			holder.leave.setVisibility(View.GONE);
		}

		return rowView;

	}

	public void update(Waypoint w) {
		this.dao.update(w);
		this.list = new ArrayList<Waypoint>(this.dao.loadAll());
		notifyDataSetChanged();
		EventBus.getDefault().post(new Events.WaypointUpdated(w));
	}

	public void add(Waypoint w) {
		this.dao.insert(w);
		this.list = new ArrayList<Waypoint>(this.dao.loadAll());
		notifyDataSetChanged();
		EventBus.getDefault().post(new Events.WaypointAdded(w));
	}

	public void remove(Waypoint w) {
		this.dao.delete(w);
		this.list = new ArrayList<Waypoint>(this.dao.loadAll());
		notifyDataSetChanged();
		EventBus.getDefault().post(new Events.WaypointRemoved(w));
	}

}
