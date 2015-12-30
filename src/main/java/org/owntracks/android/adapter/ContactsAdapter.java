package org.owntracks.android.adapter;


import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import org.owntracks.android.BR;
import org.owntracks.android.R;
import org.owntracks.android.model.Contact;
import org.owntracks.android.model.FusedContact;

import java.util.HashMap;
import java.util.List;



public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    private static final String TAG = "ContactsAdapter";
    private ArrayMap<String, FusedContact> contacts;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private ViewDataBinding binding;
          /*  private TextView name;
            private TextView location;
            private ImageView image;*/

            public ViewHolder(View view) {
                super(view);
                binding = DataBindingUtil.bind(view);
/*
                name = (TextView) view.findViewById(R.id.name);
                location = (TextView) view.findViewById(R.id.location);
                image = (ImageView) view.findViewById(R.id.image);
*/

            }
            public ViewDataBinding getBinding() {
                return binding;
            }

        }

        public ContactsAdapter(ArrayMap<String, FusedContact> contacts) {
            this.contacts = contacts;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.binding_row_contact, parent, false);
           return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final FusedContact contact = contacts.get(contacts.keyAt(position));

           // holder.getBinding().setVariable(BR.user, contact);
           // holder.getBinding().executePendingBindings();

            //holder.name.setText(contact.getDisplayName());
            //holder.location.setText(contact.getGeocoder());
            //contact.displayFaceInViewAsync(holder.image);

        }

        @Override
        public int getItemCount() {
            return contacts.size();
        }
}
