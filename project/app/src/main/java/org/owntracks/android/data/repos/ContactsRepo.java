package org.owntracks.android.data.repos;

import android.database.Observable;
import android.databinding.ObservableList;
import android.databinding.ObservableMap;

import org.owntracks.android.data.model.Contact;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;


public interface ContactsRepo {
    ObservableMap<String, FusedContact> getAllAsMap();
    ObservableList<FusedContact> getAllAsList();

    FusedContact getById(String id);

    void clearAll();
    void remove(String id);

    void update(String id, MessageLocation m);
    void update(String id, MessageCard m);

}