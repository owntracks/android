package org.owntracks.android.injection.modules.android.ActivityModules;

import android.support.v7.app.AppCompatActivity;

import org.owntracks.android.injection.scopes.PerActivity;
import org.owntracks.android.ui.preferences.editor.EditorActivity;
import org.owntracks.android.ui.preferences.editor.EditorMvvm;
import org.owntracks.android.ui.preferences.editor.EditorViewModel;

import dagger.Binds;
import dagger.Module;

@Module(includes = BaseActivityModule.class)
public abstract class EditorActivityModule {

    @Binds
    @PerActivity
    abstract AppCompatActivity bindActivity(EditorActivity a);

    @Binds abstract EditorMvvm.ViewModel bindViewModel(EditorViewModel viewModel);
}
