package st.alr.mqttitude.adapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import st.alr.mqttitude.App;
import st.alr.mqttitude.R;
import st.alr.mqttitude.db.Waypoint;
import st.alr.mqttitude.db.WaypointDao;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.support.Defaults;
import st.alr.mqttitude.support.Events;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;

import de.greenrobot.event.EventBus;

public class ContactAdapter extends BaseAdapter {
	private LinkedHashMap<String, Contact> map;
	private String[] keys;
	private Context context;

	public ContactAdapter(Context c) {
		super();
		this.context = c;
		this.map = new LinkedHashMap<String, Contact>();
		setKeysFromMap();
		notifyDataSetChanged();
	}

	private void setKeysFromMap() {
		this.keys = this.map.keySet().toArray(new String[this.map.size()]);
	}
	
	public Iterator<Entry<String, Contact>> getIterator() {
		return map.entrySet().iterator();
	}
	
	static class ViewHolder {

		public TextView title;
		public TextView subtitle;
		public ImageView image;

	}

	@Override
	public int getCount() {
		return this.map.size();
	}

	
	@Override
	public Contact getItem(int position) {
		return getItem(keys[position]);
	}

	public Contact getItem(String key) {
		return this.map.get(key);
	}
	
	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.row_contact, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.title = (TextView) rowView.findViewById(R.id.title);
			viewHolder.subtitle = (TextView) rowView.findViewById(R.id.subtitle);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.image);

			rowView.setTag(viewHolder);
		}

		Contact c = getItem(position);

		ViewHolder holder = (ViewHolder) rowView.getTag();
		holder.title.setText(c.toString());
		holder.subtitle.setText(c.getLocation().toString());
		holder.image.setImageBitmap(c.getUserImage());
		
		return rowView;

	}

	public void update(Contact c) {
		notifyDataSetChanged();
	}

	public void add(Contact c) {
		map.put(c.getTopic(), c);
		setKeysFromMap();
		notifyDataSetChanged();
	}

	public void remove(Contact c) {
		map.remove(c.getTopic());
		setKeysFromMap();
		notifyDataSetChanged();
	}
	public void clear() {
		map.clear();
		setKeysFromMap();
		notifyDataSetChanged();

	}
	
	

}
