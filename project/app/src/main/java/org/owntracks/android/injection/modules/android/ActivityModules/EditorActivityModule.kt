package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.preferences.editor.EditorActivity
import org.owntracks.android.ui.preferences.editor.EditorMvvm
import org.owntracks.android.ui.preferences.editor.EditorViewModel

@Module
abstract class EditorActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: EditorActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: EditorViewModel): EditorMvvm.ViewModel<EditorMvvm.View>
}