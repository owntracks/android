package org.owntracks.android.ui.preferences.connection

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class ConnectionActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: ConnectionActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: ConnectionViewModel): ConnectionMvvm.ViewModel<ConnectionMvvm.View>
}