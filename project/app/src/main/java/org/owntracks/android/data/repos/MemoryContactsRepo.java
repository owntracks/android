package org.owntracks.android.data.repos;

import android.annotation.SuppressLint;
import android.database.Observable;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableMap;

import org.owntracks.android.data.model.Contact;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.model.FusedContact;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

@PerApplication
public class MemoryContactsRepo implements ContactsRepo {
    ObservableMap<String, FusedContact> mMap;

    @Inject
    public MemoryContactsRepo() {
        Timber.v("constructor");
        mMap = new ObservableArrayMap<>();
    }

    @Override
    public ObservableMap<String, FusedContact> getAll() {
        return mMap;
    }

    @Override
    public FusedContact getById(String id) {
        return mMap.get(id);
    }

    @Override
    public void put(String id, FusedContact contact) {
        mMap.put(id, contact);
    }

    @Override
    public void clearAll() {
        mMap.clear();
    }

    @Override
    public void remove(String id) {
        mMap.remove(id);
    }
}
