package org.owntracks.android.ui.welcome

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.finish.FinishFragmentModule
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.intro.IntroFragmentModule
import org.owntracks.android.ui.welcome.permission.PermissionFragment
import org.owntracks.android.ui.welcome.permission.PermissionFragmentModule
import org.owntracks.android.ui.welcome.permission.PlayFragment
import org.owntracks.android.ui.welcome.permission.PlayFragmentModule
import org.owntracks.android.ui.welcome.version.VersionFragment
import org.owntracks.android.ui.welcome.version.VersionFragmentModule

@Module
abstract class WelcomeActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: WelcomeActivity?): AppCompatActivity?

    @Binds
    @PerActivity
    abstract fun bindViewModel(viewModel: WelcomeViewModel?): BaseViewModel<WelcomeMvvm.View?>?

    @ContributesAndroidInjector(modules = [IntroFragmentModule::class])
    @PerFragment
    abstract fun bindIntroFragment(): IntroFragment?

    @ContributesAndroidInjector(modules = [VersionFragmentModule::class])
    @PerFragment
    abstract fun bindVersionFragment(): VersionFragment?

    @ContributesAndroidInjector(modules = [PermissionFragmentModule::class])
    @PerFragment
    abstract fun bindPermissionFragment(): PermissionFragment?

    @ContributesAndroidInjector(modules = [FinishFragmentModule::class])
    @PerFragment
    abstract fun bindFinishFragment(): FinishFragment?

    @ContributesAndroidInjector(modules = [PlayFragmentModule::class])
    @PerFragment
    abstract fun bindPlayFragment(): PlayFragment?
}