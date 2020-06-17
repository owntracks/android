package org.owntracks.android.support

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test

class PreferencesTest {

    @Test
    @Ignore
    fun export() {
        val mock = mock<Context> {}
        val preferences = Preferences(mock, null)
        preferences.clientId = "TestClient"

        val messageConfiguration = preferences.exportToMessage()
        assertEquals("", messageConfiguration.toString())
    }
}