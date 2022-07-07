package org.owntracks.android.ui.welcome

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {
    override fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter) {
        welcomeAdapter.setupFragments(
            listOf(
                introFragment,
                connectionSetupFragment,
                versionFragment,
                finishFragment
            )
        )
    }
}
