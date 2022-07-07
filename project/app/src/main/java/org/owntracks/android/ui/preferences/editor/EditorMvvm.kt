package org.owntracks.android.ui.preferences.editor

import androidx.databinding.Bindable
import org.owntracks.android.ui.base.view.MvvmView
import org.owntracks.android.ui.base.viewmodel.MvvmViewModel

interface EditorMvvm {
    interface View : MvvmView {
        fun displayLoadFailed()
        fun exportConfigurationToFile(): Boolean
    }

    interface ViewModel<V : MvvmView?> : MvvmViewModel<V> {
        fun onPreferencesValueForKeySetSuccessful()

        @get:Bindable
        val effectiveConfiguration: String?
    }
}
