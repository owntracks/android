package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.map.MapViewModel;
import org.owntracks.android.ui.region.RegionActivity;
import org.owntracks.android.ui.regions.RegionsActivity;
import org.owntracks.android.ui.regions.RegionsMvvm;
import org.owntracks.android.ui.regions.RegionsViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class RegionsActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(RegionsActivity a);

    @Binds abstract RegionsMvvm.ViewModel bindViewModel(RegionsViewModel viewModel);
}