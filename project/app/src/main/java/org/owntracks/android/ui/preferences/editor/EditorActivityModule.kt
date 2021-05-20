package org.owntracks.android.ui.preferences.editor

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@InstallIn(ActivityComponent::class)
@Module
abstract class EditorActivityModule {
//    @Binds
//    @ActivityScoped
//    abstract fun bindActivity(a: EditorActivity?): AppCompatActivity?

    @Binds
    abstract fun bindViewModel(viewModel: EditorViewModel): EditorMvvm.ViewModel<EditorMvvm.View>
}