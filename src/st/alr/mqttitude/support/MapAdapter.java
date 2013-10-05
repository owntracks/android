package st.alr.mqttitude.support;

import java.util.Map;

import android.content.Context;
import android.widget.BaseAdapter;

public abstract class MapAdapter<K, T> extends BaseAdapter {
	protected Map<K, T> map;
	protected Context context; 
	
	public MapAdapter(Context c, Map<K, T> map) {
	    this.map = map;
	    this.context = c;
	}

	@SuppressWarnings("unchecked")
    public void addItem(K k, T object) {
		this.map.put(k, object);
		this.notifyDataSetChanged();
	}
	
	public void removeItem(T object) { 
		this.map.remove(object.toString());
		this.notifyDataSetChanged();
	}
	
	public void clearItems() {
		this.map.clear();
		this.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.map.size();
	}

	@Override
	public Object getItem(int position) {
		return this.map.values().toArray()[position];
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
}
