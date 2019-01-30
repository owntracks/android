package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.preferences.load.LoadActivity;
import org.owntracks.android.ui.preferences.load.LoadMvvm;
import org.owntracks.android.ui.preferences.load.LoadViewModel;

import dagger.Binds;
import dagger.Module;


@Module(includes = BaseActivityModule.class)
public abstract class LoadActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(LoadActivity a);

    @Binds abstract LoadMvvm.ViewModel bindViewModel(LoadViewModel viewModel);
}
