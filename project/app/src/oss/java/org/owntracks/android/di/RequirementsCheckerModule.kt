package org.owntracks.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.support.OSSRequirementsChecker
import org.owntracks.android.support.RequirementsChecker

@InstallIn(SingletonComponent::class)
@Module
class RequirementsCheckerModule {
  @Provides
  fun provideRequirementsChecker(@ApplicationContext context: Context): RequirementsChecker =
      OSSRequirementsChecker(context)
}
