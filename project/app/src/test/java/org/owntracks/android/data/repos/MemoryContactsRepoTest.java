package org.owntracks.android.data.repos;

import android.util.EventLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.owntracks.android.App;
import org.owntracks.android.injection.components.AppComponent;
import org.owntracks.android.injection.components.DaggerAppComponent;
import org.owntracks.android.injection.modules.AppModule;
import org.owntracks.android.messages.MessageCard;
import org.owntracks.android.messages.MessageLocation;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.support.Events;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import it.cosenonjaviste.daggermock.DaggerMockRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({App.class})
public class MemoryContactsRepoTest {
    @Mock
    App app;

    private MessageLocation messageLocation;
    private ContactsRepo contactsRepo;
    private final static String CONTACT_ID = "abcd1234";

    private AppComponent appComponent;

    @Before
    public void setup() {
        appComponent = DaggerAppComponent.builder().appModule(new AppModule(app)).build();

        messageLocation = new MessageLocation();
        messageLocation.setAcc(10);
        messageLocation.setAlt(20);
        messageLocation.setBatt(30);
        messageLocation.setConn("TestConn");
        messageLocation.setLat(50.1);
        messageLocation.setLon(60.2);
        messageLocation.setTst(123456789);

        contactsRepo = appComponent.contactsRepo();
    }

    @Test
    public void repoCorrectlyRegistersToEventBus() {
        assertTrue(appComponent.eventBus().isRegistered(contactsRepo));
    }

    @Test
    public void repoCorrectlyUpdatesContactWithMessageLocation() {
        assertEquals(0, contactsRepo.getRevision());
        contactsRepo.update(CONTACT_ID, messageLocation);
        assertEquals(1, contactsRepo.getRevision());

        FusedContact c = contactsRepo.getById(CONTACT_ID);
        assertEquals(messageLocation, c.getMessageLocation());
        assertEquals(messageLocation.getTst(), c.getTst());
        assertEquals(CONTACT_ID, c.getId());
    }

    @Test
    public void repoCorrectlyRemovesContactById() {
        assertEquals(0, contactsRepo.getRevision());
        contactsRepo.update(CONTACT_ID, messageLocation);
        assertEquals(1, contactsRepo.getRevision());

        FusedContact c = contactsRepo.getById(CONTACT_ID);
        assertEquals(false, c.isDeleted());

        contactsRepo.remove(CONTACT_ID);
        assertEquals(-MemoryContactsRepo.MAJOR_STEP, contactsRepo.getRevision());
        assertTrue(c.isDeleted());
        assertNull(contactsRepo.getById(CONTACT_ID));
    }


    @Test
    public void repoCorrectlyHandlesEventModeChanged() {
        contactsRepo.update(CONTACT_ID, messageLocation);
        MemoryContactsRepo.class.cast(contactsRepo).onEventMainThread(new Events.ModeChanged(0,1));
        assertEquals(0, contactsRepo.getAll().size());
    }

    @Test
    public void repoCorrectlyHandlesEventEndpointChanged() {
        contactsRepo.update(CONTACT_ID, messageLocation);
        MemoryContactsRepo.class.cast(contactsRepo).onEventMainThread(new Events.EndpointChanged());
        assertEquals(0, contactsRepo.getAll().size());

    }


}