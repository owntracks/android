package org.owntracks.android.ui.welcome.finish

import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@InstallIn(FragmentComponent::class)
@Module
abstract class FinishFragmentModule {
    @Binds
    @FragmentScoped
    abstract fun bindSupportFragment(f: FinishFragment?): Fragment?
}