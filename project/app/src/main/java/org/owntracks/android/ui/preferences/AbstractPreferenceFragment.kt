package org.owntracks.android.ui.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.takisoft.preferencex.PreferenceFragmentCompat
import org.owntracks.android.support.Preferences
import javax.inject.Inject

abstract class AbstractPreferenceFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferences: Preferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val hostActivity = requireActivity() as AppCompatActivity

        val actionBar = hostActivity.supportActionBar
        actionBar?.run {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(false)
        }
    }
}