package org.owntracks.android.ui.welcome

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {
    @Inject
    lateinit var playFragment: PlayFragment

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PlayFragment.PLAY_SERVICES_RESOLUTION_REQUEST) {
            playFragment.onPlayServicesResolutionResult()
        }
    }

    override fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter) {
        welcomeAdapter.setupFragments(listOf(
            introFragment,
            connectionSetupFragment,
            versionFragment,
            playFragment,
            finishFragment
        ))
    }
}