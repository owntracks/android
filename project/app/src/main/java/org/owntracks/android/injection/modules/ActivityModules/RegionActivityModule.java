package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.map.MapViewModel;
import org.owntracks.android.ui.region.RegionActivity;
import org.owntracks.android.ui.region.RegionMvvm;
import org.owntracks.android.ui.region.RegionViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class RegionActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(RegionActivity a);

    @Binds abstract RegionMvvm.ViewModel bindViewModel(RegionViewModel viewModel);
}