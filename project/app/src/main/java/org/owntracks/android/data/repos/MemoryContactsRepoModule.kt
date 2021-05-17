package org.owntracks.android.data.repos

import dagger.Binds
import dagger.Module

@Module
abstract class MemoryContactsRepoModule {
    @Binds
    abstract fun bindMemoryContactsRepo(memoryContactsRepo: MemoryContactsRepo): ContactsRepo
}