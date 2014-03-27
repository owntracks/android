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

    public Object getItemObject(int position) {
        return rows.get(position).second;
    }
    public Object getItem(int position) {
        return rows.get(position);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        System.out.println("getView " + position + " " + convertView + " type = " + getItemViewType(position));
        return delegateAdapters.get(getItemViewType(position)).getView(position, convertView, parent, inflater, (Pair<Integer, Object>)getItem(position));
    }

    public interface DelegateAdapter {
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Pair<Integer, Object> item);
    }

    public abstract class ItemDelegateAdapter implements DelegateAdapter {}

    public static class HeaderHolder {
        TextView header;
    }

    public class HeaderDelegateAdapter implements DelegateAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Pair<Integer, Object> item) {
            HeaderHolder holder;
            if (convertView == null) {
                holder = new HeaderHolder();
                convertView = inflater.inflate(R.layout.row_header, null);
                holder.header = (TextView)convertView.findViewById(R.id.header);

            } else {
                holder = (HeaderHolder)convertView.getTag();
            }
            convertView.setTag(holder);
            holder.header.setText((String)item.second);
            return convertView;
        }
    }






}
