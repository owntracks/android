package org.owntracks.android.injection.modules.android.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.regions.RegionsActivity;
import org.owntracks.android.ui.regions.RegionsMvvm;
import org.owntracks.android.ui.regions.RegionsViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseActivityModule.class)
public abstract class RegionsActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(RegionsActivity a);

    @Binds abstract RegionsMvvm.ViewModel bindViewModel(RegionsViewModel viewModel);
}