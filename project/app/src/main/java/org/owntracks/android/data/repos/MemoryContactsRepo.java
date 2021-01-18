package org.owntracks.android.data.repos;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.owntracks.android.injection.scopes.PerApplication;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.messages.MessageCard;
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import timber.log.Timber;

@PerApplication
public class MemoryContactsRepo implements ContactsRepo {
    public static final long MAJOR_STEP = 1000000;

    private final EventBus eventBus;
    private final ContactImageProvider contactImageProvider;

    private final MutableLiveData<Map<String, FusedContact>> contacts = new MutableLiveData<>(new HashMap<>());

    private long majorRevision = 0;
    private long revision = 0;

    @Inject
    public MemoryContactsRepo(EventBus eventBus, ContactImageProvider contactImageProvider) {
        this.eventBus = eventBus;
        this.contactImageProvider = contactImageProvider;
        this.eventBus.register(this);
    }

    @Override
    public MutableLiveData<Map<String, FusedContact>> getAll() {
        return contacts;
    }

    @Override
    public FusedContact getById(String id) {
        return contacts.getValue().get(id);
    }


    private synchronized void put(String id, final FusedContact contact) {
        Timber.v("new contact allocated id:%s, tid:%s", id, contact.getTrackerId());
        Map<String, FusedContact> map = contacts.getValue();
        map.put(id,contact);
        contacts.postValue(map);
    }

    @Override
    @MainThread
    public synchronized void clearAll() {
        contacts.getValue().clear();

        majorRevision -= MAJOR_STEP;
        revision = 0;
        contactImageProvider.invalidateCache();
    }

    @Override
    public synchronized void remove(String id) {
        Timber.v("removing contact: %s", id);
        final FusedContact c = contacts.getValue().remove(id);

        if (c != null) {
            c.setDeleted();
            eventBus.post(new Events.FusedContactRemoved(c));

            majorRevision -= MAJOR_STEP;
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
    public synchronized void update(@NonNull String id, @NonNull MessageLocation messageLocation) {
        FusedContact fusedContact = getById(id);

        if (fusedContact != null) {
            // If timestamp of last location message is <= the new location message, skip update. We either received an old or already known message.
            if (fusedContact.setMessageLocation(messageLocation)) {
                revision++;
                eventBus.post(fusedContact);
            }
        } else {
            fusedContact = new FusedContact(id);
            fusedContact.setMessageLocation(messageLocation);
            put(id, fusedContact);
            revision++;
            eventBus.post(new Events.FusedContactAdded(fusedContact));
        }
    }

    // Allows for quickly checking if items in the repo were updated/added (getRevison > savedRevision) or removed (getRevision < savedRevision)
    public long getRevision() {
        return majorRevision + revision;
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
