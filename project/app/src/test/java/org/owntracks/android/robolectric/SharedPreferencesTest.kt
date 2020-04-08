package org.owntracks.android.robolectric

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.owntracks.android.R
import org.owntracks.android.support.Preferences
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesTest {
    @Test
    fun preferenceRequestForWrongTypeReturnsDefault() {
        val preferenceKey = "testKey"
        val context = ApplicationProvider.getApplicationContext<Context>()

        val sharedPreferences = context.getSharedPreferences("org.owntracks.android.preferences.private", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(preferenceKey, 0).commit()

        val testValue = Preferences(context, null).getString(preferenceKey, R.string.valEmpty)
        Assert.assertEquals("", testValue)
    }
}