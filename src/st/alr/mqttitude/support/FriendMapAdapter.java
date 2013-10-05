package st.alr.mqttitude.support;

import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FriendMapAdapter extends MapAdapter<String, Friend> {

	public FriendMapAdapter(Context context, Map<String, Friend> map) {
	    super(context, new HashMap<String, Friend>(map));
	}

	static class ViewHolder {
		public TextView title;
	      public TextView subtitle;

	}

	public void setMap(Map<String, Friend> map) {
	    this.map = map;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		ViewHolder holder;

		if (rowView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rowView = inflater.inflate(R.layout.two_line_row_layout, null);

			holder = new ViewHolder();
			holder.title = (TextView) rowView.findViewById(android.R.id.text1);
	        holder.subtitle = (TextView) rowView.findViewById(android.R.id.text2);

			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
        Log.v(this.toString(), holder.title.toString());
		Log.v(this.toString(), map.toString());
        
		holder.title.setText(((Friend) getItem(position)).toString());
		if(((Friend)getItem(position)).getLocation() != null)
		    holder.subtitle.setText(((Friend) getItem(position)).getLocation().toLatLonString());
		else 
		    holder.subtitle.setText("n/a");
		return rowView;

	}



}
