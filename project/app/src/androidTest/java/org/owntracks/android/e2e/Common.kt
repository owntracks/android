package org.owntracks.android.e2e

import com.schibsted.spain.barista.interaction.BaristaClickInteractions
import org.owntracks.android.R

internal fun doWelcomeProcess() {
    BaristaClickInteractions.clickOn(R.id.btn_next)
/* TODO Once test isolation is possible we'll have to grant the priv each test */
//        clickOn(R.id.btn_next)
//        clickOn(R.id.fix_permissions_button)
//        LocationPermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    BaristaClickInteractions.clickOn(R.id.btn_next)
    BaristaClickInteractions.clickOn(R.id.done)
}