package org.owntracks.android.ui.welcome

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.permission.PermissionFragment
import org.owntracks.android.ui.welcome.permission.PlayFragment
import org.owntracks.android.ui.welcome.version.VersionFragment

@InstallIn(SingletonComponent::class)
@Module
abstract class WelcomeActivityModule {
//    @Binds
//    @ActivityScoped
//    abstract fun bindActivity(a: WelcomeActivity?): AppCompatActivity?

    @Binds
    @ActivityScoped
    abstract fun bindViewModel(viewModel: WelcomeViewModel?): BaseViewModel<WelcomeMvvm.View?>?

    @Binds
    @ActivityScoped
    abstract fun bindIntroFragment(introFragment: IntroFragment): Fragment

    @Binds
    @ActivityScoped
    abstract fun bindVersionFragment(versionFragment: VersionFragment): Fragment

    @Binds
    @ActivityScoped
    abstract fun bindPermissionFragment(permissionFragment: PermissionFragment): Fragment

    @Binds
    @ActivityScoped
    abstract fun bindFinishFragment(finishFragment: FinishFragment): Fragment

    @Binds
    @ActivityScoped
    abstract fun bindPlayFragment(playFragment: PlayFragment): Fragment
}