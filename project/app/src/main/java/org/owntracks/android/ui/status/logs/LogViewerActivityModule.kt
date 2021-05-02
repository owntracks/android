package org.owntracks.android.ui.status.logs

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class LogViewerActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: LogViewerActivity): AppCompatActivity
}