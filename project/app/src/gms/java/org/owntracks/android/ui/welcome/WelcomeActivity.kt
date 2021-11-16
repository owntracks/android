package org.owntracks.android.ui.welcome

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import org.owntracks.android.ui.welcome.permission.PlayFragment
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeActivity : BaseWelcomeActivity() {
    @Inject
    lateinit var playFragment: PlayFragment
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PlayFragment.PLAY_SERVICES_RESOLUTION_REQUEST) {
            playFragment.onPlayServicesResolutionResult()
        }
    }

    override fun addFragmentsToAdapter(welcomeAdapter: WelcomeAdapter) {
        welcomeAdapter.setupFragments(
            introFragment,
            versionFragment,
            playFragment,
            finishFragment
        )
    }
}
