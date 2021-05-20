package org.owntracks.android.support

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.BuildConfig
import org.owntracks.android.gms.GMSRequirementsChecker

@InstallIn(SingletonComponent::class)
@Module
class RequirementsCheckerModule {
    @Provides
    fun provideRequirementsChecker(
        preferences: Preferences,
        @ApplicationContext context: Context
    ): RequirementsChecker {
        return when (BuildConfig.FLAVOR) {
            "gms" -> GMSRequirementsChecker(preferences, context)
            else -> OSSRequirementsChecker(preferences, context)
        }
    }
}