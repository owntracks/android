package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.BaseActivityModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.region.RegionActivity;
import org.owntracks.android.ui.region.RegionMvvm;
import org.owntracks.android.ui.region.RegionViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseActivityModule.class)
public abstract class RegionActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(RegionActivity a);

    @Binds abstract RegionMvvm.ViewModel bindViewModel(RegionViewModel viewModel);
}