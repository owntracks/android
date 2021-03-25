package org.owntracks.android.support

import android.content.Context
import dagger.Module
import dagger.Provides
import org.owntracks.android.BuildConfig
import org.owntracks.android.gms.GMSRequirementsChecker
import org.owntracks.android.injection.qualifier.AppContext

@Module
class RequirementsCheckerModule {
    @Provides
    fun provideRequirementsChecker(preferences: Preferences, @AppContext context: Context): RequirementsChecker {
        return when (BuildConfig.FLAVOR) {
            "gms" -> GMSRequirementsChecker(preferences, context)
            else -> OSSRequirementsChecker(preferences, context)
        }
    }
}