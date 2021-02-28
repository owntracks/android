package org.owntracks.android.ui.preferences.about

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class AboutActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: AboutActivity?): AppCompatActivity?
}