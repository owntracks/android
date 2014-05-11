package st.alr.mqttitude.adapter;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

import st.alr.mqttitude.R;
import st.alr.mqttitude.model.Contact;

public abstract class MultitypeAdapter extends BaseAdapter {

    public static final int ROW_TYPE_HEADER = 0;
    public static final int ROW_TYPE_ITEM = 1;

    protected LayoutInflater inflater;
    protected Context context;

    protected LongSparseArray<DelegateAdapter> delegateAdapters;
    protected ArrayList<Pair<Integer, Object>>  rows;


    public MultitypeAdapter(Context context) {
        this.context = context;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        rows =  new ArrayList<Pair<Integer, Object>>();
        delegateAdapters =  new LongSparseArray<DelegateAdapter>();

        delegateAdapters.put(ROW_TYPE_HEADER, new HeaderDelegateAdapter());
        delegateAdapters.put(ROW_TYPE_ITEM, getItemDelegateAdapter());
    }

    public abstract DelegateAdapter getItemDelegateAdapter();

    public void addHeader(String str) {
        rows.add(new Pair<Integer, Object>(ROW_TYPE_HEADER, str));
        notifyDataSetChanged();
    }


    public void addItem(Object o) {
        rows.add(new Pair<Integer, Object>(ROW_TYPE_ITEM, o));
        notifyDataSetChanged();
    }

    public Object removeItem(Object o) {

        int i = findItemIndex(o);
        if(i != -1)
            return removeItem(i);
        return null;
    }

    public Object removeItem(int position) {
        Object tmp = rows.remove(position).second;
        notifyDataSetChanged();
        return tmp;
    }

    public int findItemIndex(Object o) {
        for(int i = 0; i < rows.size(); i++ )
            if(rows.get(i).first.equals(ROW_TYPE_ITEM) && rows.get(i).second.equals(o))
                return i;
        return -1;
    }

    public void updateItem(Object o){
        int i = findItemIndex(o);
        if (i != -1) {
            rows.set(i, new Pair<Integer, Object>(ROW_TYPE_ITEM, o));
            notifyDataSetChanged();
        }
    }
    @Override
    public int getItemViewType(int position) {
        return rows.get(position).first;
    }

    @Override
    public int getViewTypeCount() {
        return delegateAdapters.size();
    }

    public int getCount() {
        return rows.size();
    }

    public long getItemId(int position) {
        return position;
    }


    public Object getItem(int position) {
        return rows.get(position).second;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return delegateAdapters.get(getItemViewType(position)).getView(position, convertView, parent, inflater, getItem(position));
    }

    public interface DelegateAdapter {
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Object item);
    }

    public abstract class ItemDelegateAdapter implements DelegateAdapter {}

    public static class HeaderHolder {
        TextView header;
    }

    public class HeaderDelegateAdapter implements DelegateAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Object item) {
            HeaderHolder holder;
            if (convertView == null) {
                holder = new HeaderHolder();
                convertView = inflater.inflate(R.layout.row_header, null);
                holder.header = (TextView)convertView.findViewById(R.id.header);

            } else {
                holder = (HeaderHolder)convertView.getTag();
            }
            convertView.setTag(holder);
            holder.header.setText((String)item);
            return convertView;
        }
    }
}
