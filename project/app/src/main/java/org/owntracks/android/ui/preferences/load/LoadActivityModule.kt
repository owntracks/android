package org.owntracks.android.ui.preferences.load

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class LoadActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: LoadActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: LoadViewModel): LoadMvvm.ViewModel<LoadMvvm.View>?
}