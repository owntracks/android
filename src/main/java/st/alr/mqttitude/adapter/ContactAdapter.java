package st.alr.mqttitude.adapter;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import st.alr.mqttitude.R;
import st.alr.mqttitude.model.Contact;
import st.alr.mqttitude.model.GeocodableLocation;
import st.alr.mqttitude.services.ServiceProxy;


public class ContactAdapter extends MultitypeAdapter{

    public ContactAdapter(Context context, ArrayList<Contact> contacts) {
        super(context);

        if(contacts != null)
            for (Contact c : contacts)
                addItem(c);


        notifyDataSetChanged();
    }

    public static class ContactHolder {
        TextView name;
        TextView location;
        ImageView image;
    }

    @Override
    public DelegateAdapter getItemDelegateAdapter() {
        return new ItemDelegateAdapter();
    }

    public class ItemDelegateAdapter implements DelegateAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Pair<Integer, Object> item) {
            ContactHolder holder;
            Contact c = (Contact)item.second;
            if (convertView == null) {
                holder = new ContactHolder();
                convertView = inflater.inflate(R.layout.row_contact, null);
                holder.name = (TextView)convertView.findViewById(R.id.name);
                holder.location = (TextView)convertView.findViewById(R.id.location);
                holder.image = (ImageView)convertView.findViewById(R.id.image);

            } else {
                holder = (ContactHolder)convertView.getTag();
            }
            convertView.setTag(holder);
            holder.name.setText(c.toString());
            holder.location.setText(c.getLocation().toString());
            holder.image.setImageBitmap(c.getUserImage());

            return convertView;
        }
    }




//    public static class MyLocationHolder {
//        TextView myLocation;
//    }
//
//    public class MyLocationDelegateAdapter implements DelegateAdapter {
//
//        @Override
//        public View getView(final int position, View convertView, ViewGroup parent, LayoutInflater inflater, final Pair<Integer, Object> item) {
//            MyLocationHolder holder;
//
//            if (convertView == null) {
//                holder = new MyLocationHolder();
//                convertView = inflater.inflate(R.layout.row_mylocation, null);
//                holder.myLocation = (TextView)convertView.findViewById(R.id.currentLocation);
//
//            } else {
//                holder = (MyLocationHolder)convertView.getTag();
//            }
//            convertView.setTag(holder);
//            if(myLocation != null)
//                holder.myLocation.setText(myLocation.toString());
//            else
//                holder.myLocation.setText(context.getResources().getString(R.string.na));
//            return convertView;
//        }
//    }
//
//    public void updateCurrentLocation(GeocodableLocation l) {
//        myLocation = l;
//        notifyDataSetChanged();
//    }

}



