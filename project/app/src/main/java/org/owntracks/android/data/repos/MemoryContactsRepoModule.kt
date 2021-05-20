package org.owntracks.android.data.repos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class MemoryContactsRepoModule {
    @Binds
    abstract fun bindMemoryContactsRepo(memoryContactsRepo: MemoryContactsRepo): ContactsRepo
}