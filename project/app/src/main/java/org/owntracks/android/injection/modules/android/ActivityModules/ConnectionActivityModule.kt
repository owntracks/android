package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.preferences.connection.ConnectionActivity
import org.owntracks.android.ui.preferences.connection.ConnectionMvvm
import org.owntracks.android.ui.preferences.connection.ConnectionViewModel

@Module(includes = [BaseActivityModule::class])
abstract class ConnectionActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: ConnectionActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: ConnectionViewModel): ConnectionMvvm.ViewModel<ConnectionMvvm.View>
}