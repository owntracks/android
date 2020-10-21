package org.owntracks.android.support

import android.content.Context
import android.content.res.Resources
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.owntracks.android.model.messages.MessageConfiguration
import org.owntracks.android.support.preferences.PreferencesStore

class PreferenceSetterInvalid {
    private lateinit var mockResources: Resources
    private lateinit var mockContext: Context
    private lateinit var preferencesStore: PreferencesStore

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
    fun `when given an import value of null, the config value should be cleared`() {
        val preferences = Preferences(mockContext, null, preferencesStore)
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["host"] = null
        preferences.host = "testHost"
        preferences.importFromMessage(messageConfiguration)
        Assert.assertEquals("", preferences.host)
    }

    @Test
    fun `when given an invalid preference key, it should be ignored`() {
        val preferences = Preferences(mockContext, null, preferencesStore)
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["Invalid"] = "invalid"
        preferences.importFromMessage(messageConfiguration)
        Assert.assertFalse(preferences.exportToMessage().keys.contains("Invalid"))
    }

    @Test
    fun `when given an import value of the wrong type, it should be ignored`() {
        val preferences = Preferences(mockContext, null, preferencesStore)
        val messageConfiguration = MessageConfiguration()
        messageConfiguration["host"] = 4
        preferences.importFromMessage(messageConfiguration)
        Assert.assertEquals("", preferences.host)
    }
}