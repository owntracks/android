package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.region.RegionActivity
import org.owntracks.android.ui.region.RegionMvvm
import org.owntracks.android.ui.region.RegionViewModel

@Module(includes = [BaseActivityModule::class])
abstract class RegionActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: RegionActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: RegionViewModel): RegionMvvm.ViewModel<RegionMvvm.View>
}