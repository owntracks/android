package org.owntracks.android.data.repos;

import android.content.res.Resources;
import android.util.DisplayMetrics;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.greenrobot.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.owntracks.android.App;
import org.owntracks.android.model.FusedContact;
import org.owntracks.android.model.messages.MessageLocation;
import org.owntracks.android.support.ContactImageProvider;
import org.owntracks.android.support.Events;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({App.class})
public class MemoryContactsRepoTest {
    @Mock
        App app;

    @Rule
    InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    private MessageLocation messageLocation;
    private ContactsRepo contactsRepo;
    private final static String CONTACT_ID = "abcd1234";
    private EventBus eventBus;



    @Before
    public void setup() {
        eventBus = EventBus.getDefault();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.densityDpi = 160;
        Resources myResources = mock(Resources.class);
        when(myResources.getDisplayMetrics()).thenReturn(displayMetrics);
        when(app.getResources()).thenReturn(myResources);
        ContactImageProvider contactImageProvider = new ContactImageProvider(app);

        mockStatic(App.class);

        messageLocation = new MessageLocation();
        messageLocation.setAccuracy(10);
        messageLocation.setAltitude(20);
        messageLocation.setBattery(30);
        messageLocation.setConn("TestConn");
        messageLocation.setLatitude(50.1);
        messageLocation.setLongitude(60.2);
        messageLocation.setTimestamp(123456789);

        contactsRepo = new MemoryContactsRepo(eventBus, contactImageProvider);

    }

    @Test
    public void repoCorrectlyRegistersToEventBus() {
        assertTrue(eventBus.isRegistered(contactsRepo));
    }

    @Test
    public void repoCorrectlyUpdatesContactWithMessageLocation() {
        assertEquals(0, contactsRepo.getRevision());
        contactsRepo.update(CONTACT_ID, messageLocation);
        assertEquals(1, contactsRepo.getRevision());

        FusedContact c = contactsRepo.getById(CONTACT_ID);
        assertEquals(messageLocation, c.getMessageLocation().getValue());
        assertEquals(messageLocation.getTimestamp(), c.getTst());
        assertEquals(CONTACT_ID, c.getId());
    }

    @Test
    public void repoCorrectlyRemovesContactById() {
        assertEquals(0, contactsRepo.getRevision());
        contactsRepo.update(CONTACT_ID, messageLocation);
        assertEquals(1, contactsRepo.getRevision());

        FusedContact c = contactsRepo.getById(CONTACT_ID);
        assertFalse(c.isDeleted());

        contactsRepo.remove(CONTACT_ID);
        assertEquals(-MemoryContactsRepo.MAJOR_STEP, contactsRepo.getRevision());
        assertTrue(c.isDeleted());
        assertNull(contactsRepo.getById(CONTACT_ID));
    }


    @Test
    public void repoCorrectlyHandlesEventModeChanged() {
        contactsRepo.update(CONTACT_ID, messageLocation);
        ((MemoryContactsRepo) contactsRepo).onEventMainThread(new Events.ModeChanged(0,1));
        assertTrue( contactsRepo.getAll().getValue().isEmpty());

    }

    @Test
    public void repoCorrectlyHandlesEventEndpointChanged() {
        contactsRepo.update(CONTACT_ID, messageLocation);
        ((MemoryContactsRepo) contactsRepo).onEventMainThread(new Events.EndpointChanged());
        assertTrue( contactsRepo.getAll().getValue().isEmpty());
    }
}