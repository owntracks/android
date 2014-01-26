
package st.alr.mqttitude.adapter;

import java.util.Collection;
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

    public synchronized void addItem(K k, T object) {
        this.map.put(k, object);
        this.notifyDataSetChanged();
    }

    public synchronized void removeItem(T object) {
        this.map.remove(object.toString());
        this.notifyDataSetChanged();
    }

    public synchronized void clearItems() {
        this.map.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public synchronized int getCount() {
        return this.map.size();
    }

    public Collection<T> getValues() {
        return this.map.values();
    }

    @Override
    public synchronized Object getItem(int position) {
        return this.map.values().toArray()[position];
    }

    @Override
    public synchronized long getItemId(int position) {
        return 0;
    }
}
