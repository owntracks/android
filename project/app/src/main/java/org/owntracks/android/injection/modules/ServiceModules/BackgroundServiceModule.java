package org.owntracks.android.injection.modules.ServiceModules;

import android.app.Service;
import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.modules.ServiceModule;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.services.BackgroundService;
import org.owntracks.android.ui.preferences.editor.EditorActivity;
import org.owntracks.android.ui.preferences.editor.EditorMvvm;
import org.owntracks.android.ui.preferences.editor.EditorViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ServiceModule.class)
public abstract class BackgroundServiceModule {

    @Binds
    @PerActivity
    abstract Service bindService(BackgroundService s);

    @Binds abstract EditorMvvm.ViewModel bindViewModel(EditorViewModel viewModel);
}
