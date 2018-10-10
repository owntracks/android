package org.owntracks.android.data.repos;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.App;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.owntracks.android.support.Runner;

import java.util.Collection;
import java.util.HashMap;

import javax.inject.Inject;

import timber.log.Timber;

@PerApplication
public class MemoryContactsRepo implements ContactsRepo {
    private static final int LOAD_FACTOR = 20;
    public static final long MAJOR_STEP = 1000000;

    private final HashMap<String, FusedContact> mMap;
    private final EventBus eventBus;
    private final ContactImageProvider contactImageProvider;

    private long majorRevision = 0;
    private long revision = 0;

    @Inject
    public MemoryContactsRepo(EventBus eventBus, ContactImageProvider contactImageProvider) {
        this.eventBus = eventBus;
        this.contactImageProvider = contactImageProvider; 
        mMap = new HashMap<>(LOAD_FACTOR);
        this.eventBus.register(this);
    }



    @Override
    public HashMap<String, FusedContact> getAll() {
        return mMap;
    }

    @Override
    public Collection<FusedContact> getAllAsList() {
        return mMap.values();
    }

    @Override
    public FusedContact getById(String id) {
        return mMap.get(id);
    }


    private synchronized void put(String id, final FusedContact contact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.getTrackerId());
        mMap.put(id, contact);
    }

    @Override
    @MainThread
    public synchronized void clearAll() {
        mMap.clear();

        majorRevision-=MAJOR_STEP;
        revision = 0;
    }

    @Override
    public synchronized void remove(String id) {
        Timber.v("removing contact: %s", id);
        final FusedContact c = mMap.remove(id);


        if(c != null) {
            c.setDeleted();
            eventBus.post(new Events.FusedContactRemoved(c));

            majorRevision-=MAJOR_STEP;
            revision = 0;
        }
    }

    @Override
    public void update(@NonNull String id, @NonNull MessageCard m) {
        FusedContact c = getById(id);

        if (c != null) {
            c.setMessageCard(m);
            contactImageProvider.invalidateCacheLevelCard(c.getId());

            revision++;
            eventBus.post(c);

        } else {
            c = new FusedContact(id);
            c.setMessageCard(m);
            contactImageProvider.invalidateCacheLevelCard(c.getId());

            put(id, c);
            revision++;
            eventBus.post(new Events.FusedContactAdded(c));
        }
    }

    @Override
    public synchronized void update(@NonNull String id, @NonNull MessageLocation m) {
        FusedContact c = getById(id);

        if (c != null) {
            c.setMessageLocation(m);
            revision++;
            eventBus.post(c);

        } else {
            c = new FusedContact(id);
            c.setMessageLocation(m);
            put(id, c);
            revision++;
            eventBus.post(new Events.FusedContactAdded(c));
        }
    }

    // Allows for quickly checking if items in the repo were updated/added (getRevison > savedRevision) or removed (getRevision < savedRevision)
    public long getRevision() {
        return majorRevision+revision;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventMainThread(Events.ModeChanged e) {
        clearAll();
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventMainThread(Events.EndpointChanged e) {
        clearAll();
    }
}
