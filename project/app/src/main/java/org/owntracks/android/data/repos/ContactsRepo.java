package org.owntracks.android.data.repos;

import android.database.Observable;
import android.databinding.ObservableMap;

import org.owntracks.android.data.model.Contact;
import org.owntracks.android.model.FusedContact;


public interface ContactsRepo {
    ObservableMap<String, FusedContact> getAll();
    FusedContact getById(String id);
    void put(String id, FusedContact contact);
    void clearAll();
    void remove(String id);
}
