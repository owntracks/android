package org.owntracks.android.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import org.owntracks.android.R;
import org.owntracks.android.model.Contact;


public class ContactAdapter extends MultitypeAdapter{

    public ContactAdapter(Context context, ArrayList<Contact> contacts) {
        super(context);

        set(contacts);
    }

    public static class ContactHolder {
        TextView name;
        TextView location;
        ImageView image;
    }


    public void set(ArrayList<Contact> contacts) {
        if(contacts != null)
            for (Contact c : contacts)
                addItem(c);
    }

    @Override
    public DelegateAdapter getItemDelegateAdapter() {
        return new ItemDelegateAdapter();
    }

    public class ItemDelegateAdapter implements DelegateAdapter {

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, Object item) {
            ContactHolder holder;
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
            holder.name.setText(((Contact)item).getDisplayName());
            holder.location.setText(((Contact)item).getLocation().toString());
            holder.image.setImageBitmap(((Contact)item).getFace());

            return convertView;
        }
    }
}



