package org.owntracks.android.ui.region

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class RegionActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: RegionActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: RegionViewModel): RegionMvvm.ViewModel<RegionMvvm.View>
}