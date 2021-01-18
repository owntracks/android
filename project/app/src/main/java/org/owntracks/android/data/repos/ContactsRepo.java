package org.owntracks.android.data.repos;

import androidx.lifecycle.MutableLiveData;

import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.messages.MessageCard;
import org.owntracks.android.model.messages.MessageLocation;

import java.util.Map;


public interface ContactsRepo {
    MutableLiveData<Map<String, FusedContact>> getAll();

    FusedContact getById(String id);

    void clearAll();
    void remove(String id);

    void update(String id, MessageLocation m);
    void update(String id, MessageCard m);

    long getRevision();
}