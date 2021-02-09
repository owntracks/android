package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.regions.RegionsActivity
import org.owntracks.android.ui.regions.RegionsMvvm
import org.owntracks.android.ui.regions.RegionsViewModel

@Module(includes = [BaseActivityModule::class])
abstract class RegionsActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: RegionsActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: RegionsViewModel): RegionsMvvm.ViewModel<RegionsMvvm.View>
}