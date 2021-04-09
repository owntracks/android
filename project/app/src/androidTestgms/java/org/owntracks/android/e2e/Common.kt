package org.owntracks.android.e2e

import android.os.Build
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.owntracks.android.R

internal fun doWelcomeProcess() {
    clickOn(R.id.btn_next)
    /* TODO Once test isolation is possible we'll have to grant the priv each test */
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.M) {
        clickOn(R.id.fix_permissions_button)
//        LocationPermissionGranter.allowPermissionsIfNeeded(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    clickOn(R.id.btn_next)
    clickOn(R.id.done)
}