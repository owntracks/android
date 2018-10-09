package org.owntracks.android.injection.modules.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.modules.ActivityModule;
import org.owntracks.android.injection.qualifier.ActivityContext;
import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.map.MapMvvm;
import org.owntracks.android.ui.preferences.editor.EditorActivity;
import org.owntracks.android.ui.preferences.editor.EditorMvvm;
import org.owntracks.android.ui.preferences.editor.EditorViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = ActivityModule.class)
public abstract class EditorActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity appCompatActivity(EditorActivity a);

    @Binds abstract EditorMvvm.ViewModel bindViewModel(EditorViewModel viewModel);
}
