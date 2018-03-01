package org.owntracks.android.data.repos;

import android.databinding.ObservableList;
import android.support.v4.util.SimpleArrayMap;
import android.util.ArrayMap;

import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


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