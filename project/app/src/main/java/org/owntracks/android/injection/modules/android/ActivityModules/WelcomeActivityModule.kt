package org.owntracks.android.injection.modules.android.ActivityModules

import androidx.appcompat.app.AppCompatActivity
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.owntracks.android.injection.modules.android.FragmentModules.*
import org.owntracks.android.injection.scopes.PerActivity
import org.owntracks.android.injection.scopes.PerFragment
import org.owntracks.android.ui.base.viewmodel.BaseViewModel
import org.owntracks.android.ui.welcome.WelcomeActivity
import org.owntracks.android.ui.welcome.WelcomeMvvm
import org.owntracks.android.ui.welcome.WelcomeViewModel
import org.owntracks.android.ui.welcome.finish.FinishFragment
import org.owntracks.android.ui.welcome.intro.IntroFragment
import org.owntracks.android.ui.welcome.permission.PermissionFragment
import org.owntracks.android.ui.welcome.play.PlayFragment
import org.owntracks.android.ui.welcome.version.VersionFragment

@Module(includes = [BaseActivityModule::class])
abstract class WelcomeActivityModule {
    @Binds
    @PerActivity
    abstract fun bindActivity(a: WelcomeActivity?): AppCompatActivity?

    @Binds
    @PerActivity
    abstract fun bindViewModel(viewModel: WelcomeViewModel?): BaseViewModel<WelcomeMvvm.View?>?

    @ContributesAndroidInjector(modules = [PlayFragmentModule::class])
    @PerFragment
    abstract fun bindPlayFragment(): PlayFragment?

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
}