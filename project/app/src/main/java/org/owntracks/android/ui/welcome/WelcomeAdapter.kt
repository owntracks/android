package org.owntracks.android.ui.welcome

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.support.RequirementsChecker
import java.util.*

@PerActivity
class WelcomeAdapter constructor(welcomeActivity: WelcomeActivity, private val requirementsChecker: RequirementsChecker) : FragmentStateAdapter(welcomeActivity) {
    private val fragments = ArrayList<Fragment>()
    fun setupFragments(introFragment: Fragment, versionFragment: Fragment, playFragment: Fragment, permissionFragment: Fragment, finishFragment: Fragment) {
        fragments.add(introFragment)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) fragments.add(versionFragment)
        if (!requirementsChecker.isPlayServicesCheckPassed()) {
            fragments.add(playFragment)
        }
        if (!requirementsChecker.isPermissionCheckPassed()) {
            fragments.add(permissionFragment)
        }
        fragments.add(finishFragment)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}