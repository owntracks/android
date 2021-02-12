package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.ui.status.StatusActivity
import org.owntracks.android.ui.status.StatusMvvm
import org.owntracks.android.ui.status.StatusViewModel

@Module
abstract class StatusActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: StatusActivity?): AppCompatActivity?
    @Binds
    abstract fun bindViewModel(viewModel: StatusViewModel): StatusMvvm.ViewModel<StatusMvvm.View>
}