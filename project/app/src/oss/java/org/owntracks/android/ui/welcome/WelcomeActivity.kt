package org.owntracks.android.ui.welcome

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {
    override fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter) {
        welcomeAdapter.setupFragments(
            introFragment,
            versionFragment,
            finishFragment
        )
    }
}