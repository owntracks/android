package org.owntracks.android.injection.components

import android.content.Context
import dagger.Binds
import dagger.Module
import org.owntracks.android.App
import org.owntracks.android.injection.qualifier.AppContext
import javax.inject.Singleton

@Module
abstract class AppContextModule {
    @Binds
    @AppContext
    @Singleton
    abstract fun provideContext(app: App): Context
}