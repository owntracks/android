package org.owntracks.android.di

import androidx.databinding.DataBindingComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import org.owntracks.android.support.ContactImageBindingAdapter

@EntryPoint
@BindingScoped
@InstallIn(CustomBindingComponent::class)
interface CustomBindingEntryPoint : DataBindingComponent {
  override fun getContactImageBindingAdapter(): ContactImageBindingAdapter
}
