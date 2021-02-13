package org.owntracks.android.ui.regions

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class RegionsActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: RegionsActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: RegionsViewModel): RegionsMvvm.ViewModel<RegionsMvvm.View>
}