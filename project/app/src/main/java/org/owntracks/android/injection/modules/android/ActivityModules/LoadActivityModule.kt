package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.preferences.load.LoadActivity
import org.owntracks.android.ui.preferences.load.LoadMvvm
import org.owntracks.android.ui.preferences.load.LoadViewModel

@Module
abstract class LoadActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: LoadActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: LoadViewModel): LoadMvvm.ViewModel<LoadMvvm.View>?
}