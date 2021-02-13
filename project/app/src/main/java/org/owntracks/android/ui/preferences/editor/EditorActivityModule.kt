package org.owntracks.android.ui.preferences.editor

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity

@Module
abstract class EditorActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: EditorActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: EditorViewModel): EditorMvvm.ViewModel<EditorMvvm.View>
}