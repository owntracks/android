package org.owntracks.android.support

import android.content.Context
import android.content.res.Resources
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesTest {
    @Test
    fun preferencesExport() {

        val mockResources = mock<Resources> {
            on { getInteger(any()) } doReturn 0
            on { getString(any()) } doReturn ""
        }
        val mockContext = mock<Context> {
            on { resources } doReturn mockResources
        }


        val preferences = Preferences(mockContext, null, null)
        preferences.clientId = "TestClient"

        val messageConfiguration = preferences.exportToMessage()

        assertEquals("{\"type\":\"_configuration\",\"clientId\",\"TestClient\"}", messageConfiguration.toString())
    }
}