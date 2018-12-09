package org.owntracks.android.data.repos;

import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;

import java.util.Collection;
import java.util.HashMap;


public interface ContactsRepo {
    HashMap<String, FusedContact> getAll();
    Collection<FusedContact> getAllAsList();

    FusedContact getById(String id);

    void clearAll();
    void remove(String id);

    void update(String id, MessageLocation m);
    void update(String id, MessageCard m);

    long getRevision();
}