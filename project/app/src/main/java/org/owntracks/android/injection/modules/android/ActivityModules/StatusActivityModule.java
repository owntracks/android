package org.owntracks.android.injection.modules.android.ActivityModules;

import androidx.appcompat.app.AppCompatActivity;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.status.StatusActivity;
import org.owntracks.android.ui.status.StatusMvvm;
import org.owntracks.android.ui.status.StatusViewModel;

import dagger.Binds;
import dagger.Module;


@Module(includes = BaseActivityModule.class)
public abstract class StatusActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(StatusActivity a);

    @Binds abstract StatusMvvm.ViewModel bindViewModel(StatusViewModel viewModel);
}