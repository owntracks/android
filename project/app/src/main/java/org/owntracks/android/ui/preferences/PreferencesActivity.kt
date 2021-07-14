package org.owntracks.android.ui.preferences

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.R
import org.owntracks.android.databinding.UiPreferencesBinding
import org.owntracks.android.support.DrawerProvider
import javax.inject.Inject

@AndroidEntryPoint
open class PreferencesActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Inject
    lateinit var drawerProvider: DrawerProvider
    protected open val startFragment: Fragment = PreferencesFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ui_preferences)
        val binding: UiPreferencesBinding =
                DataBindingUtil.setContentView(this, R.layout.ui_preferences)
        binding.lifecycleOwner = this
        setSupportActionBar(binding.appbar.toolbar)

        drawerProvider.attach(binding.appbar.toolbar)
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.fragments.isEmpty()) {
                binding.appbar.toolbar.title = title
            } else {
                binding.appbar.toolbar.title =
                        (supportFragmentManager.fragments[0] as PreferenceFragmentCompat).preferenceScreen.title
            }
        }
        val fragmentTransaction = supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, startFragment, null)
        fragmentTransaction.commit()
        supportFragmentManager.executePendingTransactions()
    }


    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        )
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(pref.key)
                .commit()
        return true
    }
}