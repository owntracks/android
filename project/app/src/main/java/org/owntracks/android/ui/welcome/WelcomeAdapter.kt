package org.owntracks.android.ui.welcome

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.owntracks.android.injection.qualifier.ActivityContext
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.support.RequirementsChecker
import timber.log.Timber
import java.util.*

@PerActivity class WelcomeAdapter constructor(@ActivityContext welcomeActivity: WelcomeActivity, private val requirementsChecker: RequirementsChecker) : FragmentStateAdapter(welcomeActivity) {
    private val fragments = ArrayList<Fragment>()
    fun setupFragments(introFragment: Fragment, versionFragment: Fragment, playFragment: Fragment, permissionFragment: Fragment, finishFragment: Fragment) {
        fragments.add(introFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) fragments.add(versionFragment)
        if (!requirementsChecker.isPlayCheckPassed) {
            fragments.add(playFragment)
        }
        if (!requirementsChecker.isPermissionCheckPassed) {
            fragments.add(permissionFragment)
        }
        fragments.add(finishFragment)
    }

    val lastItemPosition: Int
        get() = fragments.size - 1

    fun getFragment(position: Int): WelcomeFragmentMvvm.View {
        return getItem(position) as WelcomeFragmentMvvm.View
    }

    private fun getItem(position: Int): Fragment {
        if (position >= fragments.size) {
            Timber.e("Welcome position %d is out of bounds for fragment list length %d", position, fragments.size)
            throw IndexOutOfBoundsException()
        }
        Timber.v("position:%s fragment:%s", position, fragments[position].toString())
        return fragments[position]
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}