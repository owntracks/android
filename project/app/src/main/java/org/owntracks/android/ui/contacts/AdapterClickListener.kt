package org.owntracks.android.ui.contacts

import android.view.View

interface AdapterClickListener<T> {
  fun onClick(item: T, view: View, longClick: Boolean)
}
