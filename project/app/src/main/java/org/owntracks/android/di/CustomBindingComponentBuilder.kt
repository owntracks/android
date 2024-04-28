package org.owntracks.android.di

import dagger.hilt.DefineComponent

@DefineComponent.Builder
interface CustomBindingComponentBuilder {
  fun build(): CustomBindingComponent
}
