package org.owntracks.android.ui.base

import android.view.View

typealias ClickHasBeenHandled = Boolean

interface ClickListener<T> {
  fun onClick(thing: T, view: View, longClick: Boolean): ClickHasBeenHandled
}
