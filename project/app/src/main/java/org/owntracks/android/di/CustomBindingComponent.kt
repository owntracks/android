package org.owntracks.android.di

import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent

@BindingScoped @DefineComponent(parent = SingletonComponent::class) interface CustomBindingComponent
