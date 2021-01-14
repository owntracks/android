package org.owntracks.android.ui.preferences.load;

import android.content.Context
import android.content.res.Resources
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.objectbox.android.ObjectBoxLiveData
import io.objectbox.query.Query
import org.greenrobot.eventbus.EventBus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.owntracks.android.data.WaypointModel
import org.owntracks.android.data.repos.WaypointsRepo
import org.owntracks.android.support.InMemoryPreferencesStore
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.PreferencesGettersAndSetters
import org.owntracks.android.support.preferences.PreferencesStore
import java.net.URI

class LoadViewModelTest {
    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore
    private val eventBus: EventBus = mock {}

    @Before
    fun createMocks() {
        mockResources = PreferencesGettersAndSetters.getMockResources()
        mockContext = mock {
            on { resources } doReturn mockResources
            on { packageName } doReturn javaClass.canonicalName
        }
        preferencesStore = InMemoryPreferencesStore()

    }

    @Test
    fun `When invalid JSON on an inline owntracks config URL, then the error is correctly set`() {
        val parser = Parser(null)
        val preferences = Preferences(mockContext, null, preferencesStore)
        val vm = LoadViewModel(preferences, parser, InMemoryWaypointsRepo(eventBus))

        vm.extractPreferences(URI("owntracks:///config?inline=e30k"))
        assertEquals("""Import failed: Message is not a valid configuration message""", vm.displayedConfiguration)
    }
}

class InMemoryWaypointsRepo(eventBus: EventBus?) : WaypointsRepo(eventBus) {
    override fun get(tst: Long): WaypointModel {
        TODO("Not yet implemented")
    }

    override fun getAll(): MutableList<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllWithGeofences(): MutableList<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllLive(): ObjectBoxLiveData<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun getAllQuery(): Query<WaypointModel> {
        TODO("Not yet implemented")
    }

    override fun insert_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

    override fun update_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

    override fun delete_impl(w: WaypointModel?) {
        TODO("Not yet implemented")
    }

}