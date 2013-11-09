
package st.alr.mqttitude.support;

import java.util.HashMap;
import java.util.Map;

import st.alr.mqttitude.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactAdapter extends MapAdapter<String, Contact> {

    public ContactAdapter(Context context, Map<String, Contact> map) {
        super(context, new HashMap<String, Contact>(map));
    }

    static class ViewHolder {
        public TextView title;
        public TextView subtitle;
        public ImageView image;

    }

    public void setMap(Map<String, Contact> map) {
        this.map = map;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        ViewHolder holder;

        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.friend_list_item, null);

            holder = new ViewHolder();
            holder.title = (TextView) rowView.findViewById(R.id.title);
            holder.subtitle = (TextView) rowView.findViewById(R.id.subtitle);
            holder.image = (ImageView) rowView.findViewById(R.id.image);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }
        Contact f = (Contact) getItem(position);

        holder.title.setText(f.toString());
        if (f.getLocation() != null)
            holder.subtitle.setText(f.getLocation().toLatLonString());
        else
            holder.subtitle.setText("n/a");

        holder.image.setImageBitmap(f.getUserImage());
        return rowView;

    }

    public Contact get(String topic) {
        Log.v(this.toString(), "Contacts: " + map);
        Log.v(this.toString(), "Request to return: " + topic);

        return map.get(topic);
    }

}
