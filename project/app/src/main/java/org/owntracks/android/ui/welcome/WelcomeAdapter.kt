package org.owntracks.android.ui.welcome

import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.hilt.android.scopes.ActivityScoped

@ActivityScoped
class WelcomeAdapter constructor(private val welcomeActivity: BaseWelcomeActivity) :
    FragmentStateAdapter(welcomeActivity) {
    private val fragments = ArrayList<WelcomeFragment>()
    fun setupFragments(welcomeFragments: List<WelcomeFragment>) {
        welcomeFragments.filter { it.shouldBeDisplayed(welcomeActivity) }.forEach(fragments::add)
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): WelcomeFragment {
        return fragments[position]
    }
}
