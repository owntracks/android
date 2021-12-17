package org.owntracks.android.ui.preferences.load;

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.owntracks.android.support.InMemoryPreferencesStore
import org.owntracks.android.support.Parser
import org.owntracks.android.support.Preferences
import org.owntracks.android.support.PreferencesGettersAndSetters
import org.owntracks.android.support.preferences.PreferenceDataStoreShim
import java.net.URI

class LoadViewModelTest {
    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferenceDataStoreShim


    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Before
    fun createMocks() {
        mockResources = PreferencesGettersAndSetters.getMockResources()
        mockContext = mock {
            on { resources } doReturn mockResources
            on { packageName } doReturn javaClass.canonicalName
        }
        preferencesStore = InMemoryPreferencesStore()

    }

    @ExperimentalCoroutinesApi
    @Test
    fun `When invalid JSON on an inline owntracks config URL, then the error is correctly set`() {
        val parser = Parser(null)
        val preferences = Preferences(mockContext, null, preferencesStore)
        val vm = LoadViewModel(
            preferences, parser, InMemoryWaypointsRepo(),
            TestCoroutineDispatcher()
        )

        vm.extractPreferences(URI("owntracks:///config?inline=e30k"))
        assertEquals("", vm.displayedConfiguration.value)
        assertEquals(
            """Message is not a valid configuration message""",
            vm.importError.value
        )
    }
}

